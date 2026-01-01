const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const Payment = require('../models/Payment');
const PaymentWebhook = require('../models/PaymentWebhook');
const { Op } = require('sequelize');

class PaymentService {
  /**
   * Tạo payment record và generate VietQR code
   */
  async createPayment(data) {
    const { userId, amount, description, metadata } = data;
    
    // Tạo unique order ID
    const orderId = `PAY${Date.now()}${Math.random().toString(36).substr(2, 6).toUpperCase()}`;
    
    // Lấy thông tin ngân hàng từ env
    const bankCode = process.env.VIETQR_BANK_CODE || '970422';
    const accountNumber = process.env.VIETQR_ACCOUNT_NUMBER;
    const accountName = process.env.VIETQR_ACCOUNT_NAME;
    
    if (!accountNumber || !accountName) {
      throw new Error('VietQR credentials not configured. Please set VIETQR_ACCOUNT_NUMBER and VIETQR_ACCOUNT_NAME in .env');
    }
    
    // Tạo nội dung chuyển khoản (đặt orderId lên đầu để tránh bị cắt, và tối giản nội dung)
    const transferContent = `${orderId} ${description || 'Thanh toan'}`.substring(0, 50);
    
    // Generate QR code URL using VietQR API
    // Doc: https://www.vietqr.io/danh-sach-api
    const qrCodeUrl = this.generateVietQRUrl({
      bankCode,
      accountNumber,
      accountName,
      amount,
      description: transferContent
    });
    
    console.log('🎫 VietQR Payment Created:');
    console.log('   Order ID:', orderId);
    console.log('   Amount:', amount, 'VND');
    console.log('   QR Code URL:', qrCodeUrl);
    console.log('   Expires:', moment().add(15, 'minutes').format('YYYY-MM-DD HH:mm:ss'));
    
    // Tạo payment record
    const payment = await Payment.create({
      id: uuidv4(),
      userId,
      orderId,
      amount,
      currency: 'VND',
      status: 'PENDING',
      paymentMethod: 'VIETQR',
      qrCode: qrCodeUrl,
      qrDataUrl: qrCodeUrl,
      bankCode,
      accountNumber,
      accountName,
      description: transferContent,
      metadata,
      expiredAt: moment().add(15, 'minutes').toDate()
    });
    
    return {
      paymentId: payment.id,
      orderId: payment.orderId,
      amount: payment.amount,
      qrCode: qrCodeUrl,
      bankInfo: {
        bankCode,
        accountNumber,
        accountName
      },
      transferContent,
      expiredAt: payment.expiredAt
    };
  }
  
  /**
   * Generate VietQR URL sử dụng img.vietqr.io API
   * Template: compact2 (QR code đẹp với logo ngân hàng)
   */
  generateVietQRUrl({ bankCode, accountNumber, accountName, amount, description }) {
    const baseUrl = 'https://img.vietqr.io/image';
    const template = 'qr_only'; // qr_only = QR thuần, compact2 = có logo ngân hàng
    
    // Build URL
    const params = new URLSearchParams({
      amount: amount,
      addInfo: description,
      accountName: accountName
    });
    
    return `${baseUrl}/${bankCode}-${accountNumber}-${template}.jpg?${params.toString()}`;
  }
  
  /**
   * Cập nhật trạng thái payment
   */
  async updatePaymentStatus(paymentId, status, additionalData = {}) {
    const updates = {
      status,
      ...additionalData
    };
    
    if (status === 'SUCCESS') {
      updates.paidAt = new Date();
    }
    
    await Payment.update(updates, {
      where: { id: paymentId }
    });
    
    // Get updated payment
    const payment = await Payment.findByPk(paymentId);
    
    // Emit SSE event to connected clients
    const { sseManager } = require('../controllers/sseController');
    sseManager.sendPaymentUpdate(paymentId, status, payment);
    
    // Trigger business logic
    if (status === 'SUCCESS') {
      await this.onPaymentSuccess(paymentId);
    }
    
    return true;
  }
  
  /**
   * Business logic khi thanh toán thành công
   */
  async onPaymentSuccess(paymentId) {
    const payment = await Payment.findByPk(paymentId);
    
    if (!payment) {
      console.error('Payment not found:', paymentId);
      return;
    }
    
    console.log(`✅ Payment ${payment.orderId} succeeded: ${payment.amount} VND`);
    
    // TODO: Implement business logic
    // - Cập nhật balance cho user/card
    // - Gửi notification
    // - Gửi email
    // - Tích hợp với các service khác
    
    // Example: Update card balance if metadata contains cardId
    if (payment.metadata && payment.metadata.cardId) {
      const Card = require('../models/Card');
      const card = await Card.findByPk(payment.metadata.cardId);
      if (card) {
        await card.update({
          balance: card.balance + parseInt(payment.amount)
        });
        console.log(`✅ Updated card ${payment.metadata.cardId} balance: +${payment.amount}`);
      }
    }
  }
  
  /**
   * Tìm payment theo order ID
   */
  async findPaymentByOrderId(orderId) {
    return await Payment.findOne({
      where: { orderId }
    });
  }
  
  /**
   * Lấy payment theo ID
   */
  async getPaymentById(paymentId) {
    return await Payment.findByPk(paymentId);
  }
  
  /**
   * Lấy danh sách payments với filtering
   */
  async getPayments(filters = {}) {
    const { userId, status, limit = 50, offset = 0 } = filters;
    
    const where = {};
    if (userId) where.userId = userId;
    if (status) where.status = status;
    
    const { count, rows } = await Payment.findAndCountAll({
      where,
      limit,
      offset,
      order: [['createdAt', 'DESC']]
    });
    
    return {
      total: count,
      payments: rows
    };
  }
  
  /**
   * Expire các payment quá hạn (chạy bằng cron job)
   */
  async expireOldPayments() {
    const result = await Payment.update(
      { status: 'EXPIRED' },
      {
        where: {
          status: 'PENDING',
          expiredAt: {
            [Op.lt]: new Date()
          }
        }
      }
    );
    
    console.log(`Expired ${result[0]} old payments`);
    return result[0];
  }
}

module.exports = new PaymentService();
