const { Transaction, Card, sequelize } = require('../models');
const { Op } = require('sequelize');
const { parsePagination, formatPaginatedResponse } = require('../utils/pagination');

// Create a transaction
exports.createTransaction = async (req, res) => {
    try {
        const { studentId, type, amount, description } = req.body;

        if (!studentId || !type || amount === undefined) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp đầy đủ thông tin'
            });
        }

        // Validate transaction type
        const validTypes = ['Nạp tiền', 'Trả phạt', 'Rút tiền', 'Thanh toán dịch vụ'];
        if (!validTypes.includes(type)) {
            return res.status(400).json({
                success: false,
                message: 'Loại giao dịch không hợp lệ'
            });
        }

        // Find card
        const card = await Card.findOne({ where: { studentId } });
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        const balanceBefore = parseInt(card.balance);
        let balanceAfter = balanceBefore;

        // Calculate balance after transaction
        if (type === 'Nạp tiền') {
            balanceAfter = balanceBefore + parseInt(amount);
        } else {
            // For withdrawal and payment types
            if (balanceBefore < parseInt(amount)) {
                return res.status(400).json({
                    success: false,
                    message: 'Số dư không đủ'
                });
            }
            balanceAfter = balanceBefore - parseInt(amount);
        }

        // Create transaction
        const transaction = await Transaction.create({
            studentId,
            type,
            amount,
            balanceBefore,
            balanceAfter,
            status: 'Thành công',
            description: description || ''
        });

        // Update card balance
        card.balance = balanceAfter;
        await card.save();

        res.status(201).json({
            success: true,
            message: 'Giao dịch thành công',
            data: transaction
        });
    } catch (error) {
        console.error('Create transaction error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi tạo giao dịch',
            error: error.message
        });
    }
};

// Get transactions by student
exports.getTransactionsByStudent = async (req, res) => {
    try {
        const { studentId } = req.params;
        const { type, status, startDate, endDate } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 50,
            maxLimit: 100
        });

        let whereClause = { studentId };

        if (type) {
            whereClause.type = type;
        }

        if (status) {
            whereClause.status = status;
        }

        if (startDate || endDate) {
            whereClause.createdAt = {};
            if (startDate) {
                whereClause.createdAt[Op.gte] = new Date(startDate);
            }
            if (endDate) {
                whereClause.createdAt[Op.lte] = new Date(endDate);
            }
        }

        const result = await Transaction.findAndCountAll({
            where: whereClause,
            order: [[sequelize.col('created_at'), 'DESC']],
            limit,
            offset
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get transactions error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách giao dịch',
            error: error.message
        });
    }
};

// Get all transactions
exports.getAllTransactions = async (req, res) => {
    try {
        const { type, status, startDate, endDate } = req.query;
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 50,
            maxLimit: 100
        });

        let whereClause = {};

        if (type) {
            whereClause.type = type;
        }

        if (status) {
            whereClause.status = status;
        }

        if (startDate || endDate) {
            whereClause.createdAt = {};
            if (startDate) {
                whereClause.createdAt[Op.gte] = new Date(startDate);
            }
            if (endDate) {
                whereClause.createdAt[Op.lte] = new Date(endDate);
            }
        }

        const result = await Transaction.findAndCountAll({
            where: whereClause,
            order: [[sequelize.col('created_at'), 'DESC']],
            limit,
            offset
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get all transactions error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách tất cả giao dịch',
            error: error.message
        });
    }
};

// Get transaction by ID
exports.getTransactionById = async (req, res) => {
    try {
        const { transactionId } = req.params;

        const transaction = await Transaction.findByPk(transactionId);

        if (!transaction) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy giao dịch'
            });
        }

        res.json({
            success: true,
            data: transaction
        });
    } catch (error) {
        console.error('Get transaction error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy thông tin giao dịch',
            error: error.message
        });
    }
};

// Get transaction statistics
exports.getTransactionStats = async (req, res) => {
    try {
        const { studentId } = req.params;

        const stats = await Transaction.findAll({
            where: { studentId },
            attributes: [
                'type',
                [sequelize.fn('SUM', sequelize.col('amount')), 'totalAmount'],
                [sequelize.fn('COUNT', sequelize.col('id')), 'count']
            ],
            group: ['type']
        });

        res.json({
            success: true,
            data: stats
        });
    } catch (error) {
        console.error('Get transaction stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy thống kê giao dịch',
            error: error.message
        });
    }
};
