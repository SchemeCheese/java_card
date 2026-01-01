const { sequelize, testConnection } = require('../config/database');
const Card = require('./Card');
const Book = require('./Book');
const BorrowedBook = require('./BorrowedBook');
const Transaction = require('./Transaction');

// Define relationships
Card.hasMany(BorrowedBook, {
    foreignKey: 'studentId',
    sourceKey: 'studentId',
    as: 'borrowedBooks'
});

BorrowedBook.belongsTo(Card, {
    foreignKey: 'studentId',
    targetKey: 'studentId',
    as: 'card'
});

Card.hasMany(Transaction, {
    foreignKey: 'studentId',
    sourceKey: 'studentId',
    as: 'transactions'
});

Transaction.belongsTo(Card, {
    foreignKey: 'studentId',
    targetKey: 'studentId',
    as: 'card'
});

// Relationship between Book and BorrowedBook
Book.hasMany(BorrowedBook, {
    foreignKey: 'bookId',
    sourceKey: 'bookId',
    as: 'borrowRecords'
});

BorrowedBook.belongsTo(Book, {
    foreignKey: 'bookId',
    targetKey: 'bookId',
    as: 'book'
});

// Sync all models
const syncDatabase = async (force = false) => {
    try {
    // Use alter: true to update tables without dropping them
        await sequelize.sync({ force, alter: true });
        console.log('Database synchronized successfully');
    } catch (error) {
        console.error('Error synchronizing database:', error);
        throw error;
    }
};

module.exports = {
    sequelize,
    testConnection,
    Card,
    Book,
    BorrowedBook,
    Transaction,
    syncDatabase
};

