const paymentService = require('../services/paymentService');
const Payment = require('../models/Payment');
const PaymentWebhook = require('../models/PaymentWebhook');
const crypto = require('crypto');

class PaymentController {
  /**
   * POST /api/payment/create
   * Tạo payment mới và generate QR code
   */
  async createPayment(req, res) {
    try {
      const { amount, description, metadata } = req.body;
      const userId = req.user?.id; // From auth middleware (optional)
      
      // Validation
      if (!amount || amount <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền không hợp lệ'
        });
      }
      
      if (amount > 999999999) {
        return res.status(400).json({
          success: false,
          message: 'Số tiền vượt quá giới hạn'
        });
      }
      
      const payment = await paymentService.createPayment({
        userId,
        amount,
        description,
        metadata
      });
      
      res.json({
        success: true,
        data: payment
      });
    } catch (error) {
      console.error('Create payment error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi tạo thanh toán',
        error: process.env.NODE_ENV === 'development' ? error.message : undefined
      });
    }
  }
  
  /**
   * GET /api/payment/:paymentId
   * Lấy thông tin payment (polling để check status)
   */
  async getPayment(req, res) {
    try {
      const { paymentId } = req.params;
      
      const payment = await Payment.findByPk(paymentId);
      
      if (!payment) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy giao dịch'
        });
      }
      
      res.json({
        success: true,
        data: {
          id: payment.id,
          orderId: payment.orderId,
          amount: payment.amount,
          currency: payment.currency,
          status: payment.status,
          qrCode: payment.qrDataUrl,
          bankInfo: {
            bankCode: payment.bankCode,
            accountNumber: payment.accountNumber,
            accountName: payment.accountName
          },
          description: payment.description,
          expiredAt: payment.expiredAt,
          paidAt: payment.paidAt,
          createdAt: payment.createdAt,
          updatedAt: payment.updatedAt
        }
      });
    } catch (error) {
      console.error('Get payment error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi lấy thông tin thanh toán'
      });
    }
  }
  
  /**
   * GET /api/payment
   * Lấy danh sách payments (có filter)
   */
  async getPayments(req, res) {
    try {
      const { userId, status, limit, offset } = req.query;
      
      const result = await paymentService.getPayments({
        userId,
        status,
        limit: limit ? parseInt(limit) : 50,
        offset: offset ? parseInt(offset) : 0
      });
      
      res.json({
        success: true,
        data: result
      });
    } catch (error) {
      console.error('Get payments error:', error);
      res.status(500).json({
        success: false,
        message: 'Lỗi lấy danh sách thanh toán'
      });
    }
  }
  
  /**
   * POST /api/payment/webhook/callback
   * Nhận callback từ bank/payment gateway
   */
  async handleWebhook(req, res) {
    try {
      const payload = req.body;
      const signature = req.headers['x-vietqr-signature'] || req.headers['x-signature'] || req.headers['signature'];
      const authHeader = req.headers['authorization'];
      
      const { logger } = require('../config/logger');
      logger.info('='.repeat(20) + ' WEBHOOK DEBUG ' + '='.repeat(20));
      logger.info(`[Webhook Debug] Payload: ${JSON.stringify(payload, null, 2)}`);
      
      // Note: SEPAY API Key already verified by oauthMiddleware.verifyApiKey
      
      // 1. Verify webhook signature (skip for SEPAY - they only use API Key)
      const isSepayWebhook = payload.gateway || payload.transferAmount;
      
      if (!isSepayWebhook) {
        // Only verify HMAC signature for non-SEPAY webhooks (VietQR, etc.)
        const isValid = this.verifyWebhookSignature(payload, signature);
        if (!isValid) {
          console.error('Invalid webhook signature');
          return res.status(401).json({ success: false, message: 'Invalid signature' });
        }
        console.log('Webhook signature verified');
      } else {
        console.log('Skipping HMAC signature verification for SEPAY (API Key already verified)');
      }
      
      // 2. Parse webhook data (format khác nhau tùy provider)
      const webhookData = this.parseWebhookData(payload);
      logger.info(`[Webhook Debug] Parsed Data: ${JSON.stringify(webhookData, null, 2)}`);

      
      // 3. Tìm payment trong database
      const payment = await paymentService.findPaymentByOrderId(webhookData.orderId);
      
      if (!payment) {
        console.error('❌ Payment not found:', webhookData.orderId);
        
        // Vẫn lưu webhook log để debug
        await PaymentWebhook.create({
          provider: webhookData.provider || 'unknown',
          eventType: webhookData.eventType || 'payment',
          payload,
          signature,
          verified: true,
          processed: false
        });
        
        return res.status(404).json({ success: false, message: 'Payment not found' });
      }
      
      // 4. Lưu webhook log
      await PaymentWebhook.create({
        paymentId: payment.id,
        provider: webhookData.provider || 'vietqr',
        eventType: webhookData.eventType || 'payment',
        payload,
        signature,
        verified: true,
        processed: false
      });
      
      // 5. Kiểm tra duplicate webhook
      if (payment.status === 'SUCCESS') {
        console.log('⚠️ Payment already processed:', payment.id);
        return res.json({ success: true, message: 'Already processed' });
      }
      
      // 6. Validate amount
      if (Math.abs(parseFloat(payment.amount) - webhookData.amount) > 0.01) {
        console.error('❌ Amount mismatch:', payment.amount, webhookData.amount);
        return res.status(400).json({ success: false, message: 'Amount mismatch' });
      }
      
      // 7. Update payment status
      await paymentService.updatePaymentStatus(payment.id, webhookData.status, {
        bankTransactionId: webhookData.transactionId,
        transactionRef: webhookData.transactionRef
      });
      
      // 8. Mark webhook as processed
      await PaymentWebhook.update(
        { processed: true },
        { where: { paymentId: payment.id, processed: false } }
      );
      
      console.log(`✅ Payment ${payment.orderId} updated to ${webhookData.status}`);
      
      res.json({ success: true, message: 'Webhook processed' });
    } catch (error) {
      console.error('❌ Webhook error:', error);
      res.status(500).json({ success: false, message: 'Internal error' });
    }
  }
  
  /**
   * Verify webhook signature bằng HMAC-SHA256
   */
  verifyWebhookSignature(payload, signature) {
    const secret = process.env.WEBHOOK_SECRET;
    
    if (!secret) {
      console.warn('⚠️ WEBHOOK_SECRET not configured, skipping verification');
      return true; // WARNING: Chỉ cho dev, production PHẢI verify
    }
    
    if (!signature) {
      return false;
    }
    
    // Tính signature từ payload
    const data = JSON.stringify(payload);
    const expectedSignature = crypto
      .createHmac('sha256', secret)
      .update(data)
      .digest('hex');
    
    return signature === expectedSignature;
  }
  
  /**
   * Parse webhook data (hỗ trợ nhiều providers)
   */
  parseWebhookData(payload) {
    // Sepay format
    if (payload.gateway || payload.transferAmount) {
      console.log('📨 Detected Sepay webhook format');
      
      // Extract order ID từ description
    // Format 1: PAY + timestamp + random (PAY1735627200ABC123)
    // Format 2: UUID (36 chars) (00662094-4af9-4211-ad67-09e51b1bdc9e)
    let orderId = null;
    const description = payload.description || payload.content || '';
    
    // Regex 1: PAY prefix (Ưu tiên)
    // Tìm chuỗi bắt đầu bằng PAY, theo sau là số và chữ
    // Regex 1: PAY prefix (Ưu tiên)
    // Tìm chuỗi bắt đầu bằng PAY, theo sau là ít nhất 13 ký tự số/chữ
    // VD: PAY1767255022330B1G51H
    const payMatch = description.match(/PAY[A-Z0-9]{10,}/i);
    if (payMatch) {
      orderId = payMatch[0];
    } 
    // Regex 2: Standard UUID v4
    else {
      const uuidMatch = description.match(/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/i);
      if (uuidMatch) {
        orderId = uuidMatch[0];
      }
    }  
      
      return {
        orderId: orderId,
        amount: parseFloat(payload.transferAmount || payload.amount || 0),
        status: 'SUCCESS', // Sepay chỉ gửi webhook khi giao dịch thành công
        transactionId: payload.id || payload.transactionId,
        transactionRef: payload.referenceCode || payload.code,
        provider: 'sepay',
        eventType: 'payment',
        rawDescription: description
      };
    }
    
    // VietQR/Generic format
    return {
      orderId: payload.orderId || payload.order_id || payload.orderCode,
      amount: parseFloat(payload.amount || payload.totalAmount || 0),
      status: this.mapPaymentStatus(payload.status || payload.resultCode),
      transactionId: payload.transactionId || payload.bankTranNo,
      transactionRef: payload.transactionRef || payload.reference,
      provider: payload.provider || 'vietqr',
      eventType: payload.eventType || 'payment'
    };
  }
  
  /**
   * Map status từ provider sang internal status
   */
  mapPaymentStatus(externalStatus) {
    const statusMap = {
      'SUCCESS': 'SUCCESS',
      'PAID': 'SUCCESS',
      'COMPLETED': 'SUCCESS',
      '00': 'SUCCESS',
      'FAILED': 'FAILED',
      'CANCELLED': 'CANCELLED',
      'PENDING': 'PENDING'
    };
    
    return statusMap[externalStatus] || 'FAILED';
  }
}

module.exports = new PaymentController();
