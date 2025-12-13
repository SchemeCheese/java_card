const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

const Card = sequelize.define('Card', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    studentId: {
        type: DataTypes.STRING(20),
        allowNull: false,
        unique: true,
        field: 'student_id'
    },
    holderName: {
        type: DataTypes.STRING(100),
        allowNull: false,
        field: 'holder_name'
    },
    email: {
        type: DataTypes.STRING(100),
        allowNull: false
    },
    department: {
        type: DataTypes.STRING(100),
        allowNull: false
    },
    birthDate: {
        type: DataTypes.STRING(10),
        allowNull: false,
        field: 'birth_date'
    },
    address: {
        type: DataTypes.STRING(255),
        allowNull: false
    },
    status: {
        type: DataTypes.ENUM('Hoạt động', 'Khóa', 'Tạm khóa'),
        defaultValue: 'Hoạt động'
    },
    borrowedBooksCount: {
        type: DataTypes.INTEGER,
        defaultValue: 0,
        field: 'borrowed_books'
    },
    // ⚠️ PIN fields removed - PIN should only be stored on card (applet) for security
    // PIN verification must be done on card, not on server
    balance: {
        type: DataTypes.BIGINT,
        defaultValue: 0
    },
    imagePath: {
        type: DataTypes.STRING(255),
        defaultValue: '',
        field: 'image_path'
    },
    rsaPublicKey: {
        type: DataTypes.TEXT,
        allowNull: true,
        field: 'rsa_public_key',
        comment: 'RSA Public Key (PEM format) for card authentication'
    },
    rsaKeyCreatedAt: {
        type: DataTypes.DATE,
        allowNull: true,
        field: 'rsa_key_created_at'
    },
    aesMasterKeyHash: {
        type: DataTypes.STRING(255),
        allowNull: true,
        field: 'aes_master_key_hash',
        comment: 'Hash of AES master key for this card'
    }
}, {
    tableName: 'cards',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    indexes: [
        {
            unique: true,
            fields: ['student_id']
        },
        {
            fields: ['email']
        }
    ]
});

module.exports = Card;
