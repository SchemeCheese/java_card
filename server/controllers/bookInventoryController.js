const { Book, sequelize } = require('../models');
const { Op } = require('sequelize');
const { parsePagination, formatPaginatedResponse } = require('../utils/pagination');

// Get all books in library
exports.getAllBooks = async (req, res) => {
    try {
        const { category, status, search } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 50,
            maxLimit: 100
        });

        let whereClause = {};

        if (category) {
            whereClause.category = category;
        }

        if (status) {
            whereClause.status = status;
        }

        if (search) {
            whereClause[Op.or] = [
                { title: { [Op.like]: `%${search}%` } },
                { author: { [Op.like]: `%${search}%` } },
                { bookId: { [Op.like]: `%${search}%` } }
            ];
        }

        const result = await Book.findAndCountAll({
            where: whereClause,
            limit,
            offset,
            order: [[sequelize.col('created_at'), 'DESC']]
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get all books error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách sách',
            error: error.message
        });
    }
};

// Get book by ID
exports.getBookById = async (req, res) => {
    try {
        const { bookId } = req.params;

        const book = await Book.findOne({ where: { bookId } });

        if (!book) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy sách'
            });
        }

        res.json({
            success: true,
            data: book
        });
    } catch (error) {
        console.error('Get book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy thông tin sách',
            error: error.message
        });
    }
};

// Create a new book
exports.createBook = async (req, res) => {
    try {
        const {
            bookId,
            title,
            author,
            isbn,
            publisher,
            publishYear,
            category,
            description,
            totalCopies,
            location,
            coverImage
        } = req.body;

        if (!bookId || !title || !author) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp đầy đủ thông tin: bookId, title, author'
            });
        }

        // Check if book already exists
        const existingBook = await Book.findOne({ where: { bookId } });
        if (existingBook) {
            return res.status(400).json({
                success: false,
                message: 'Sách với ID này đã tồn tại'
            });
        }

        const copies = totalCopies || 1;

        const book = await Book.create({
            bookId,
            title,
            author,
            isbn,
            publisher,
            publishYear,
            category,
            description,
            totalCopies: copies,
            availableCopies: copies,
            status: copies > 0 ? 'Có sẵn' : 'Hết sách',
            location,
            coverImage
        });

        res.status(201).json({
            success: true,
            message: 'Thêm sách thành công',
            data: book
        });
    } catch (error) {
        console.error('Create book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi thêm sách',
            error: error.message
        });
    }
};

// Update book information
exports.updateBook = async (req, res) => {
    try {
        const { bookId } = req.params;
        const updates = req.body;

        // Don't allow changing bookId
        delete updates.bookId;

        const [updated] = await Book.update(updates, {
            where: { bookId }
        });

        if (!updated) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy sách'
            });
        }

        const book = await Book.findOne({ where: { bookId } });

        res.json({
            success: true,
            message: 'Cập nhật thông tin sách thành công',
            data: book
        });
    } catch (error) {
        console.error('Update book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi cập nhật thông tin sách',
            error: error.message
        });
    }
};

// Delete book
exports.deleteBook = async (req, res) => {
    try {
        const { bookId } = req.params;

        const deleted = await Book.destroy({
            where: { bookId }
        });

        if (!deleted) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy sách'
            });
        }

        res.json({
            success: true,
            message: 'Xóa sách thành công'
        });
    } catch (error) {
        console.error('Delete book error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi xóa sách',
            error: error.message
        });
    }
};

// Update book copies (when adding/removing physical copies)
exports.updateCopies = async (req, res) => {
    try {
        const { bookId } = req.params;
        const { totalCopies, availableCopies } = req.body;

        if (totalCopies === undefined && availableCopies === undefined) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp số lượng sách'
            });
        }

        const book = await Book.findOne({ where: { bookId } });

        if (!book) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy sách'
            });
        }

        if (totalCopies !== undefined) {
            book.totalCopies = totalCopies;
        }

        if (availableCopies !== undefined) {
            if (availableCopies > book.totalCopies) {
                return res.status(400).json({
                    success: false,
                    message: 'Số sách có sẵn không thể lớn hơn tổng số sách'
                });
            }
            book.availableCopies = availableCopies;
        }

        // Update status based on available copies
        if (book.availableCopies === 0) {
            book.status = 'Hết sách';
        } else if (book.status === 'Hết sách') {
            book.status = 'Có sẵn';
        }

        await book.save();

        res.json({
            success: true,
            message: 'Cập nhật số lượng sách thành công',
            data: book
        });
    } catch (error) {
        console.error('Update copies error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi cập nhật số lượng sách',
            error: error.message
        });
    }
};

// Search books
exports.searchBooks = async (req, res) => {
    try {
        const { query, category } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 20,
            maxLimit: 100
        });

        if (!query) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp từ khóa tìm kiếm'
            });
        }

        let whereClause = {
            [Op.or]: [
                { title: { [Op.like]: `%${query}%` } },
                { author: { [Op.like]: `%${query}%` } },
                { bookId: { [Op.like]: `%${query}%` } },
                { isbn: { [Op.like]: `%${query}%` } }
            ]
        };

        if (category) {
            whereClause.category = category;
        }

        const result = await Book.findAndCountAll({
            where: whereClause,
            limit,
            offset,
            order: [['title', 'ASC']]
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Search books error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi tìm kiếm sách',
            error: error.message
        });
    }
};

// Get book categories
exports.getCategories = async (req, res) => {
    try {
        const categories = await Book.findAll({
            attributes: ['category'],
            group: ['category'],
            where: {
                category: { [Op.not]: null }
            }
        });

        const categoryList = categories.map(c => c.category);

        res.json({
            success: true,
            data: categoryList
        });
    } catch (error) {
        console.error('Get categories error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh mục sách',
            error: error.message
        });
    }
};

// Get book statistics
exports.getBookStats = async (req, res) => {
    try {
        const totalBooks = await Book.count();
        const availableBooks = await Book.count({ where: { status: 'Có sẵn' } });
        const outOfStock = await Book.count({ where: { status: 'Hết sách' } });
        
        const totalCopies = await Book.sum('totalCopies') || 0;
        const availableCopies = await Book.sum('availableCopies') || 0;

        res.json({
            success: true,
            data: {
                totalBooks,
                availableBooks,
                outOfStock,
                totalCopies,
                availableCopies,
                borrowedCopies: totalCopies - availableCopies
            }
        });
    } catch (error) {
        console.error('Get book stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy thống kê sách',
            error: error.message
        });
    }
};


