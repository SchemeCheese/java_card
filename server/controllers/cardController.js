const { Card, sequelize } = require('../models');
const crypto = require('crypto');
const { parsePagination, formatPaginatedResponse } = require('../utils/pagination');

// Helper function to generate PIN hash using PBKDF2
const generatePinHash = (pin, salt) => {
    return crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256').toString('hex');
};

// Helper function to convert RSA key from JavaCard format (modulus + exponent) to PEM
const convertRSAPublicKeyToPEM = (modulusHex, exponentHex) => {
    try {
        // Convert hex strings to Buffer
        const modulus = Buffer.from(modulusHex, 'hex');
        const exponent = Buffer.from(exponentHex, 'hex');
        
        // Create RSA public key object
        const publicKey = crypto.createPublicKey({
            key: {
                kty: 'RSA',
                n: modulus.toString('base64url'),
                e: exponent.toString('base64url')
            },
            format: 'jwk'
        });
        
        // Export as PEM
        return publicKey.export({
            type: 'spki',
            format: 'pem'
        });
    } catch (error) {
        console.error('Error converting RSA key:', error);
        return null;
    }
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
            pin,
            rsaPublicKey,
            rsaModulus,
            rsaExponent
        } = req.body;

        // Validate required fields (PIN no longer required - stored on card only)
        if (!studentId || !holderName || !email || !department || !birthDate || !address) {
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

        // ⚠️ PIN is no longer stored on server - must be set on card (applet) only
        // PIN verification must be done on card for security

        // Convert RSA key if provided in JavaCard format (modulus + exponent)
        let rsaPublicKeyPEM = rsaPublicKey;
        if (rsaModulus && rsaExponent && !rsaPublicKey) {
            rsaPublicKeyPEM = convertRSAPublicKeyToPEM(rsaModulus, rsaExponent);
        }

        // Create new card (PIN is stored on card only, not on server)
        const newCard = await Card.create({
            studentId,
            holderName,
            email,
            department,
            birthDate,
            address,
            balance: 0,
            borrowedBooksCount: 0,
            status: 'Hoạt động',
            rsaPublicKey: rsaPublicKeyPEM,
            rsaKeyCreatedAt: rsaPublicKeyPEM ? new Date() : null
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
                borrowedBooksCount: newCard.borrowedBooksCount,
                hasRSAKey: !!newCard.rsaPublicKey
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
            where: { studentId }
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

        // ⚠️ PIN fields no longer exist - PIN is stored on card only

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
            where: { studentId }
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

// Update RSA public key for a card
exports.updateRSAPublicKey = async (req, res) => {
    try {
        const { studentId } = req.params;
        const { rsaPublicKey, rsaModulus, rsaExponent } = req.body;

        const card = await Card.findOne({ where: { studentId } });
        
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // Convert RSA key if provided in JavaCard format
        let rsaPublicKeyPEM = rsaPublicKey;
        if (rsaModulus && rsaExponent && !rsaPublicKey) {
            rsaPublicKeyPEM = convertRSAPublicKeyToPEM(rsaModulus, rsaExponent);
        }

        if (!rsaPublicKeyPEM) {
            return res.status(400).json({
                success: false,
                message: 'Không thể chuyển đổi khóa RSA'
            });
        }

        card.rsaPublicKey = rsaPublicKeyPEM;
        card.rsaKeyCreatedAt = new Date();
        await card.save();

        res.json({
            success: true,
            message: 'Cập nhật khóa RSA thành công',
            data: {
                studentId: card.studentId,
                hasRSAKey: true
            }
        });
    } catch (error) {
        console.error('Update RSA key error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi cập nhật khóa RSA',
            error: error.message
        });
    }
};

// Get RSA public key for a card
exports.getRSAPublicKey = async (req, res) => {
    try {
        const { studentId } = req.params;

        const card = await Card.findOne({ 
            where: { studentId },
            attributes: ['studentId', 'rsaPublicKey', 'rsaKeyCreatedAt']
        });
        
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        if (!card.rsaPublicKey) {
            return res.status(404).json({
                success: false,
                message: 'Thẻ chưa có khóa RSA'
            });
        }

        res.json({
            success: true,
            data: {
                studentId: card.studentId,
                rsaPublicKey: card.rsaPublicKey,
                rsaKeyCreatedAt: card.rsaKeyCreatedAt
            }
        });
    } catch (error) {
        console.error('Get RSA key error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy khóa RSA',
            error: error.message
        });
    }
};
