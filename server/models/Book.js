const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

const Book = sequelize.define('Book', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    bookId: {
        type: DataTypes.STRING(50),
        allowNull: false,
        field: 'book_id'
    },
    title: {
        type: DataTypes.STRING(255),
        allowNull: false
    },
    author: {
        type: DataTypes.STRING(255),
        allowNull: false
    },
    isbn: {
        type: DataTypes.STRING(20),
        allowNull: true,
        unique: true
    },
    publisher: {
        type: DataTypes.STRING(255),
        allowNull: true
    },
    publishYear: {
        type: DataTypes.INTEGER,
        allowNull: true,
        field: 'publish_year'
    },
    category: {
        type: DataTypes.STRING(100),
        allowNull: true
    },
    description: {
        type: DataTypes.TEXT,
        allowNull: true
    },
    totalCopies: {
        type: DataTypes.INTEGER,
        allowNull: false,
        defaultValue: 1,
        field: 'total_copies'
    },
    availableCopies: {
        type: DataTypes.INTEGER,
        allowNull: false,
        defaultValue: 1,
        field: 'available_copies'
    },
    status: {
        type: DataTypes.ENUM('Có sẵn', 'Hết sách', 'Ngừng cho mượn'),
        defaultValue: 'Có sẵn'
    },
    location: {
        type: DataTypes.STRING(100),
        allowNull: true,
        comment: 'Vị trí sách trong thư viện (VD: Kệ A-12)'
    },
    coverImage: {
        type: DataTypes.STRING(255),
        allowNull: true,
        field: 'cover_image'
    }
}, {
    tableName: 'books',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at'
    // [REMOVED] All indexes to fix "Too many keys" error
    // Unique constraints on bookId and isbn are already defined in field definitions
});

module.exports = Book;
