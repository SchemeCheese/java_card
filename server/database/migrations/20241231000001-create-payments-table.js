'use strict';

module.exports = {
  up: async (queryInterface, Sequelize) => {
    await queryInterface.createTable('Payments', {
      id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
      },
      userId: {
        type: Sequelize.UUID,
        allowNull: true,
        field: 'user_id',
        comment: 'ID của user thực hiện thanh toán (optional)'
      },
      orderId: {
        type: Sequelize.STRING(100),
        allowNull: false,
        unique: true,
        field: 'order_id',
        comment: 'Mã đơn hàng unique'
      },
      amount: {
        type: Sequelize.DECIMAL(15, 2),
        allowNull: false,
        comment: 'Số tiền thanh toán'
      },
      currency: {
        type: Sequelize.STRING(3),
        defaultValue: 'VND',
        comment: 'Đơn vị tiền tệ'
      },
      status: {
        type: Sequelize.ENUM('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED'),
        defaultValue: 'PENDING',
        comment: 'Trạng thái thanh toán'
      },
      paymentMethod: {
        type: Sequelize.STRING(50),
        defaultValue: 'VIETQR',
        field: 'payment_method',
        comment: 'Phương thức thanh toán'
      },
      qrCode: {
        type: Sequelize.TEXT,
        field: 'qr_code',
        comment: 'VietQR URL'
      },
      qrDataUrl: {
        type: Sequelize.TEXT,
        field: 'qr_data_url',
        comment: 'QR code data URL để hiển thị'
      },
      bankCode: {
        type: Sequelize.STRING(20),
        field: 'bank_code',
        comment: 'Mã ngân hàng nhận tiền'
      },
      accountNumber: {
        type: Sequelize.STRING(50),
        field: 'account_number',
        comment: 'Số tài khoản nhận tiền'
      },
      accountName: {
        type: Sequelize.STRING(255),
        field: 'account_name',
        comment: 'Tên tài khoản nhận tiền'
      },
      transactionRef: {
        type: Sequelize.STRING(100),
        field: 'transaction_ref',
        comment: 'Mã tham chiếu giao dịch'
      },
      bankTransactionId: {
        type: Sequelize.STRING(100),
        field: 'bank_transaction_id',
        comment: 'Mã giao dịch từ ngân hàng'
      },
      description: {
        type: Sequelize.TEXT,
        comment: 'Nội dung chuyển khoản'
      },
      metadata: {
        type: Sequelize.JSON,
        comment: 'Thông tin bổ sung dạng JSON'
      },
      expiredAt: {
        type: Sequelize.DATE,
        field: 'expired_at',
        comment: 'Thời hạn thanh toán'
      },
      paidAt: {
        type: Sequelize.DATE,
        field: 'paid_at',
        comment: 'Thời điểm thanh toán thành công'
      },
      createdAt: {
        type: Sequelize.DATE,
        allowNull: false,
        field: 'created_at'
      },
      updatedAt: {
        type: Sequelize.DATE,
        allowNull: false,
        field: 'updated_at'
      }
    });

    // Add indexes for better query performance
    await queryInterface.addIndex('Payments', ['user_id'], {
      name: 'idx_payments_user_id'
    });
    
    await queryInterface.addIndex('Payments', ['order_id'], {
      name: 'idx_payments_order_id'
    });
    
    await queryInterface.addIndex('Payments', ['status'], {
      name: 'idx_payments_status'
    });
    
    await queryInterface.addIndex('Payments', ['created_at'], {
      name: 'idx_payments_created_at'
    });
  },

  down: async (queryInterface, Sequelize) => {
    await queryInterface.dropTable('Payments');
  }
};
