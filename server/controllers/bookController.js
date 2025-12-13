const { BorrowedBook, Card, sequelize } = require('../models');
const { Op } = require('sequelize');
const { parsePagination, formatPaginatedResponse } = require('../utils/pagination');

// Borrow a book
exports.borrowBook = async (req, res) => {
    try {
        const { studentId, bookId, bookName, dueDate } = req.body;

        if (!studentId || !bookId || !bookName || !dueDate) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp đầy đủ thông tin'
            });
        }

        // Check if card exists
        const card = await Card.findOne({ where: { studentId } });
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // Check if card is active
        if (card.status !== 'Hoạt động') {
            return res.status(403).json({
                success: false,
                message: 'Thẻ không ở trạng thái hoạt động'
            });
        }

        // Check if student has reached borrowing limit (e.g., 5 books)
        const activeBorrows = await BorrowedBook.count({
            where: {
                studentId,
                status: { [Op.in]: ['Đang mượn', 'Quá hạn'] }
            }
        });

        if (activeBorrows >= 5) {
            return res.status(400).json({
                success: false,
                message: 'Đã đạt giới hạn mượn sách (tối đa 5 cuốn)'
            });
        }

        // Check if book is already borrowed by this student
        const existingBorrow = await BorrowedBook.findOne({
            where: {
                studentId,
                bookId,
                status: { [Op.in]: ['Đang mượn', 'Quá hạn'] }
            }
        });

        if (existingBorrow) {
            return res.status(400).json({
                success: false,
                message: 'Bạn đã mượn cuốn sách này rồi'
            });
        }

        // Create borrowed book record
        const borrowedBook = await BorrowedBook.create({
            studentId,
            bookId,
            bookName,
            borrowDate: new Date(),
            dueDate: new Date(dueDate),
            status: 'Đang mượn'
        });

        // Update card's borrowed books count
        card.borrowedBooksCount += 1;
        await card.save();

        res.status(201).json({
            success: true,
            message: 'Mượn sách thành công',
            data: borrowedBook
        });
    } catch (error) {
        console.error('Borrow book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi mượn sách',
            error: error.message
        });
    }
};

// Return a book
exports.returnBook = async (req, res) => {
    try {
        const { borrowId } = req.params;

        const borrowedBook = await BorrowedBook.findByPk(borrowId);

        if (!borrowedBook) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thông tin mượn sách'
            });
        }

        if (borrowedBook.status === 'Đã trả') {
            return res.status(400).json({
                success: false,
                message: 'Sách này đã được trả rồi'
            });
        }

        // Calculate if overdue
        const now = new Date();
        const dueDate = new Date(borrowedBook.dueDate);
        
        if (now > dueDate) {
            const diffTime = Math.abs(now - dueDate);
            const overdueDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            borrowedBook.overdueDays = overdueDays;
            borrowedBook.fine = overdueDays * 5000; // 5000 VND per day
        }

        borrowedBook.returnDate = now;
        borrowedBook.status = 'Đã trả';
        await borrowedBook.save();

        // Update card's borrowed books count
        const card = await Card.findOne({ where: { studentId: borrowedBook.studentId } });
        if (card) {
            card.borrowedBooksCount = Math.max(0, card.borrowedBooksCount - 1);
            await card.save();
        }

        res.json({
            success: true,
            message: 'Trả sách thành công',
            data: {
                borrowedBook,
                fine: borrowedBook.fine
            }
        });
    } catch (error) {
        console.error('Return book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi trả sách',
            error: error.message
        });
    }
};

// Get borrowed books by student
exports.getBorrowedBooksByStudent = async (req, res) => {
    try {
        const { studentId } = req.params;
        const { status } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 20,
            maxLimit: 100
        });

        let whereClause = { studentId };
        
        if (status) {
            whereClause.status = status;
        }

        const result = await BorrowedBook.findAndCountAll({
            where: whereClause,
            order: [[sequelize.col('borrow_date'), 'DESC']],
            limit,
            offset
        });

        // Update overdue status
        const now = new Date();
        for (let book of result.rows) {
            if (book.status === 'Đang mượn' && now > book.dueDate) {
                book.status = 'Quá hạn';
                const diffTime = Math.abs(now - book.dueDate);
                book.overdueDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                book.fine = book.overdueDays * 5000;
                await book.save();
            }
        }

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get borrowed books error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách sách mượn',
            error: error.message
        });
    }
};

// Get all borrowed books
exports.getAllBorrowedBooks = async (req, res) => {
    try {
        const { status } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 20,
            maxLimit: 100
        });

        let whereClause = {};
        
        if (status) {
            whereClause.status = status;
        }

        const result = await BorrowedBook.findAndCountAll({
            where: whereClause,
            order: [[sequelize.col('borrow_date'), 'DESC']],
            limit,
            offset
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get all borrowed books error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách tất cả sách mượn',
            error: error.message
        });
    }
};

// Delete borrowed book record
exports.deleteBorrowedBook = async (req, res) => {
    try {
        const { borrowId } = req.params;

        const deleted = await BorrowedBook.destroy({
            where: { id: borrowId }
        });

        if (!deleted) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thông tin mượn sách'
            });
        }

        res.json({
            success: true,
            message: 'Xóa thông tin mượn sách thành công'
        });
    } catch (error) {
        console.error('Delete borrowed book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi xóa thông tin mượn sách',
            error: error.message
        });
    }
};
