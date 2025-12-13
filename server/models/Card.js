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
    pinHash: {
        type: DataTypes.STRING(255),
        allowNull: false,
        field: 'pin_hash'
    },
    pinSalt: {
        type: DataTypes.STRING(255),
        allowNull: false,
        field: 'pin_salt'
    },
    pinTries: {
        type: DataTypes.INTEGER,
        defaultValue: 3,
        field: 'pin_tries'
    },
    balance: {
        type: DataTypes.BIGINT,
        defaultValue: 0
    },
    imagePath: {
        type: DataTypes.STRING(255),
        defaultValue: '',
        field: 'image_path'
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
