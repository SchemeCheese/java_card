const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

const BorrowedBook = sequelize.define('BorrowedBook', {
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
    bookId: {
        type: DataTypes.STRING(50),
        allowNull: false,
        field: 'book_id'
    },
    bookName: {
        type: DataTypes.STRING(255),
        allowNull: false,
        field: 'book_name'
    },
    borrowDate: {
        type: DataTypes.DATE,
        allowNull: false,
        defaultValue: DataTypes.NOW,
        field: 'borrow_date'
    },
    dueDate: {
        type: DataTypes.DATE,
        allowNull: false,
        field: 'due_date'
    },
    returnDate: {
        type: DataTypes.DATE,
        allowNull: true,
        field: 'return_date'
    },
    status: {
        type: DataTypes.ENUM('Đang mượn', 'Quá hạn', 'Đã trả'),
        defaultValue: 'Đang mượn'
    },
    overdueDays: {
        type: DataTypes.INTEGER,
        defaultValue: 0,
        field: 'overdue_days'
    },
    fine: {
        type: DataTypes.BIGINT,
        defaultValue: 0
    },
    finePaid: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: false,
        field: 'fine_paid'
    },
    finePaidAt: {
        type: DataTypes.DATE,
        allowNull: true,
        field: 'fine_paid_at'
    }
}, {
    tableName: 'borrowed_books',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    indexes: [
        {
            fields: ['student_id']
        },
        {
            fields: ['book_id']
        },
        {
            fields: ['status']
        }
    ],
    hooks: {
        beforeSave: async (borrowedBook) => {
            // Calculate overdue if past due date (keep updated for both "Đang mượn" and "Quá hạn")
            if ((borrowedBook.status === 'Đang mượn' || borrowedBook.status === 'Quá hạn') && new Date() > borrowedBook.dueDate) {
                borrowedBook.status = 'Quá hạn';
                const diffTime = Math.abs(new Date() - borrowedBook.dueDate);
                borrowedBook.overdueDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                borrowedBook.fine = borrowedBook.overdueDays * 5000; // 5000 VND per day
            }
        }
    }
});

module.exports = BorrowedBook;
