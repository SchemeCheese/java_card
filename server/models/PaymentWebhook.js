const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');
const Payment = require('./Payment');

const PaymentWebhook = sequelize.define('PaymentWebhook', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true
  },
  paymentId: {
    type: DataTypes.UUID,
    allowNull: true,
    field: 'payment_id',
    references: {
      model: Payment,
      key: 'id'
    }
  },
  provider: {
    type: DataTypes.STRING(50)
  },
  eventType: {
    type: DataTypes.STRING(50),
    field: 'event_type'
  },
  payload: {
    type: DataTypes.JSON
  },
  signature: {
    type: DataTypes.STRING(255)
  },
  verified: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  },
  processed: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  }
}, {
  tableName: 'PaymentWebhooks',
  timestamps: true,
  createdAt: 'created_at',
  updatedAt: false
});

// Define associations
PaymentWebhook.belongsTo(Payment, { foreignKey: 'payment_id' });
Payment.hasMany(PaymentWebhook, { foreignKey: 'payment_id' });

module.exports = PaymentWebhook;
