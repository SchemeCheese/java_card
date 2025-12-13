const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

const Transaction = sequelize.define('Transaction', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    studentId: {
        type: DataTypes.STRING(20),
        allowNull: false,
        field: 'student_id',
        references: {
            model: 'cards',
            key: 'student_id'
        }
    },
    type: {
        type: DataTypes.ENUM('Nạp tiền', 'Trả phạt', 'Rút tiền', 'Thanh toán dịch vụ'),
        allowNull: false
    },
    amount: {
        type: DataTypes.BIGINT,
        allowNull: false
    },
    balanceBefore: {
        type: DataTypes.BIGINT,
        allowNull: false,
        field: 'balance_before'
    },
    balanceAfter: {
        type: DataTypes.BIGINT,
        allowNull: false,
        field: 'balance_after'
    },
    status: {
        type: DataTypes.ENUM('Thành công', 'Thất bại', 'Đang xử lý'),
        defaultValue: 'Thành công'
    },
    description: {
        type: DataTypes.STRING(255),
        defaultValue: ''
    }
}, {
    tableName: 'transactions',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    indexes: [
        {
            fields: ['student_id']
        },
        {
            fields: ['created_at']
        },
        {
            fields: ['type']
        }
    ]
});

module.exports = Transaction;
