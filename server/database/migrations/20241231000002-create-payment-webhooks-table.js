'use strict';

module.exports = {
  up: async (queryInterface, Sequelize) => {
    await queryInterface.createTable('PaymentWebhooks', {
      id: {
        type: Sequelize.INTEGER,
        autoIncrement: true,
        primaryKey: true
      },
      paymentId: {
        type: Sequelize.UUID,
        allowNull: true,
        field: 'payment_id',
        references: {
          model: 'Payments',
          key: 'id'
        },
        onUpdate: 'CASCADE',
        onDelete: 'SET NULL',
        comment: 'ID payment liên quan'
      },
      provider: {
        type: Sequelize.STRING(50),
        comment: 'Nhà cung cấp webhook (vietqr, bank, etc.)'
      },
      eventType: {
        type: Sequelize.STRING(50),
        field: 'event_type',
        comment: 'Loại sự kiện (payment, refund, etc.)'
      },
      payload: {
        type: Sequelize.JSON,
        comment: 'Dữ liệu webhook nhận được'
      },
      signature: {
        type: Sequelize.STRING(255),
        comment: 'Chữ ký webhook để verify'
      },
      verified: {
        type: Sequelize.BOOLEAN,
        defaultValue: false,
        comment: 'Đã verify signature chưa'
      },
      processed: {
        type: Sequelize.BOOLEAN,
        defaultValue: false,
        comment: 'Đã xử lý webhook chưa'
      },
      createdAt: {
        type: Sequelize.DATE,
        allowNull: false,
        field: 'created_at'
      }
    });

    // Add indexes
    await queryInterface.addIndex('PaymentWebhooks', ['payment_id'], {
      name: 'idx_payment_webhooks_payment_id'
    });
    
    await queryInterface.addIndex('PaymentWebhooks', ['processed'], {
      name: 'idx_payment_webhooks_processed'
    });
    
    await queryInterface.addIndex('PaymentWebhooks', ['created_at'], {
      name: 'idx_payment_webhooks_created_at'
    });
  },

  down: async (queryInterface, Sequelize) => {
    await queryInterface.dropTable('PaymentWebhooks');
  }
};
