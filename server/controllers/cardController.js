const { Card, sequelize } = require('../models');
const crypto = require('crypto');
const { parsePagination, formatPaginatedResponse } = require('../utils/pagination');

// Helper function to generate PIN hash using PBKDF2
const generatePinHash = (pin, salt) => {
    return crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256').toString('hex');
};

// Create a new card
exports.createCard = async (req, res) => {
    try {
        const {
            studentId,
            holderName,
            email,
            department,
            birthDate,
            address,
            pin
        } = req.body;

        // Validate required fields
        if (!studentId || !holderName || !email || !department || !birthDate || !address || !pin) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp đầy đủ thông tin'
            });
        }

        // Check if card already exists
        const existingCard = await Card.findOne({ where: { studentId } });
        if (existingCard) {
            return res.status(400).json({
                success: false,
                message: 'Thẻ với MSSV này đã tồn tại'
            });
        }

        // Generate salt and hash PIN
        const pinSalt = crypto.randomBytes(16).toString('hex');
        const pinHash = generatePinHash(pin, pinSalt);

        // Create new card
        const newCard = await Card.create({
            studentId,
            holderName,
            email,
            department,
            birthDate,
            address,
            pinHash,
            pinSalt,
            pinTries: 3,
            balance: 0,
            borrowedBooksCount: 0,
            status: 'Hoạt động'
        });

        res.status(201).json({
            success: true,
            message: 'Tạo thẻ thành công',
            data: {
                studentId: newCard.studentId,
                holderName: newCard.holderName,
                email: newCard.email,
                department: newCard.department,
                birthDate: newCard.birthDate,
                address: newCard.address,
                status: newCard.status,
                balance: newCard.balance,
                borrowedBooksCount: newCard.borrowedBooksCount
            }
        });
    } catch (error) {
        console.error('Create card error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi tạo thẻ',
            error: error.message
        });
    }
};

// Get card by student ID
exports.getCard = async (req, res) => {
    try {
        const { studentId } = req.params;

        const card = await Card.findOne({ 
            where: { studentId },
            attributes: { exclude: ['pinHash', 'pinSalt'] }
        });
        
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        res.json({
            success: true,
            data: card
        });
    } catch (error) {
        console.error('Get card error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy thông tin thẻ',
            error: error.message
        });
    }
};

// Get all cards
exports.getAllCards = async (req, res) => {
    try {
        const { page, limit, offset } = parsePagination(req.query, {
            defaultLimit: 20,
            maxLimit: 100
        });

        const result = await Card.findAndCountAll({
            attributes: { exclude: ['pinHash', 'pinSalt'] },
            order: [[sequelize.col('created_at'), 'DESC']],
            limit,
            offset
        });

        res.json(formatPaginatedResponse(result, page, limit));
    } catch (error) {
        console.error('Get all cards error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy danh sách thẻ',
            error: error.message
        });
    }
};

// Update card information
exports.updateCard = async (req, res) => {
    try {
        const { studentId } = req.params;
        const updates = req.body;

        // Don't allow direct PIN updates through this endpoint
        delete updates.pinHash;
        delete updates.pinSalt;
        delete updates.pinTries;

        const [updated] = await Card.update(updates, {
            where: { studentId }
        });

        if (!updated) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        const card = await Card.findOne({ 
            where: { studentId },
            attributes: { exclude: ['pinHash', 'pinSalt'] }
        });

        res.json({
            success: true,
            message: 'Cập nhật thông tin thẻ thành công',
            data: card
        });
    } catch (error) {
        console.error('Update card error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi cập nhật thông tin thẻ',
            error: error.message
        });
    }
};

// Delete card
exports.deleteCard = async (req, res) => {
    try {
        const { studentId } = req.params;

        const deleted = await Card.destroy({
            where: { studentId }
        });

        if (!deleted) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        res.json({
            success: true,
            message: 'Xóa thẻ thành công'
        });
    } catch (error) {
        console.error('Delete card error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi xóa thẻ',
            error: error.message
        });
    }
};

// Update card balance
exports.updateBalance = async (req, res) => {
    try {
        const { studentId } = req.params;
        const { amount } = req.body;

        if (amount === undefined) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp số tiền'
            });
        }

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        card.balance = parseInt(card.balance) + parseInt(amount);
        await card.save();

        res.json({
            success: true,
            message: 'Cập nhật số dư thành công',
            data: {
                studentId: card.studentId,
                balance: card.balance
            }
        });
    } catch (error) {
        console.error('Update balance error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi cập nhật số dư',
            error: error.message
        });
    }
};
