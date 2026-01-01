const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

const Payment = sequelize.define('Payment', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: true,
    field: 'user_id'
  },
  orderId: {
    type: DataTypes.STRING(100),
    allowNull: false,
    unique: true,
    field: 'order_id'
  },
  amount: {
    type: DataTypes.DECIMAL(15, 2),
    allowNull: false
  },
  currency: {
    type: DataTypes.STRING(3),
    defaultValue: 'VND'
  },
  status: {
    type: DataTypes.ENUM('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED'),
    defaultValue: 'PENDING'
  },
  paymentMethod: {
    type: DataTypes.STRING(50),
    defaultValue: 'VIETQR',
    field: 'payment_method'
  },
  qrCode: {
    type: DataTypes.TEXT,
    field: 'qr_code'
  },
  qrDataUrl: {
    type: DataTypes.TEXT,
    field: 'qr_data_url'
  },
  bankCode: {
    type: DataTypes.STRING(20),
    field: 'bank_code'
  },
  accountNumber: {
    type: DataTypes.STRING(50),
    field: 'account_number'
  },
  accountName: {
    type: DataTypes.STRING(255),
    field: 'account_name'
  },
  transactionRef: {
    type: DataTypes.STRING(100),
    field: 'transaction_ref'
  },
  bankTransactionId: {
    type: DataTypes.STRING(100),
    field: 'bank_transaction_id'
  },
  description: {
    type: DataTypes.TEXT
  },
  metadata: {
    type: DataTypes.JSON
  },
  expiredAt: {
    type: DataTypes.DATE,
    field: 'expired_at'
  },
  paidAt: {
    type: DataTypes.DATE,
    field: 'paid_at'
  }
}, {
  tableName: 'Payments',
  timestamps: true,
  createdAt: 'created_at',
  updatedAt: 'updated_at'
});

module.exports = Payment;
