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
            rsaPublicKey,
            rsaModulus,
            rsaExponent
        } = req.body;

        // Validate required fields - chỉ cần studentId và holderName
        if (!studentId || !holderName) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp MSSV và Họ tên'
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
        // ⚠️ email, department, birthDate, address are optional - stored on card (applet) via AES encryption

        // Convert RSA key if provided in JavaCard format (modulus + exponent)
        let rsaPublicKeyPEM = rsaPublicKey;
        if (rsaModulus && rsaExponent && !rsaPublicKey) {
            rsaPublicKeyPEM = convertRSAPublicKeyToPEM(rsaModulus, rsaExponent);
        }

        // Create new card - chỉ lưu studentId và holderName, các trường khác là optional
        const newCard = await Card.create({
            studentId,
            holderName,
            email: email || '',
            department: department || '',
            birthDate: birthDate || '',
            address: address || '',
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
        
        // Nếu có imagePath, lưu vào database
        // imagePath có thể là:
        // 1. Relative path từ uploads/avatars (đã upload lên server)
        // 2. Hoặc full URL nếu đã có sẵn
        if (updates.imagePath) {
            // Nếu là relative path, giữ nguyên
            // Nếu là absolute path từ client, convert thành relative path
            const imagePath = updates.imagePath;
            if (imagePath.startsWith('uploads/avatars/') || imagePath.startsWith('/uploads/avatars/')) {
                // Đã là relative path, giữ nguyên
                updates.imagePath = imagePath.replace(/^\/+/, ''); // Remove leading slashes
            } else if (imagePath.includes('uploads/avatars/')) {
                // Extract relative path
                const match = imagePath.match(/uploads\/avatars\/[^\/]+$/);
                if (match) {
                    updates.imagePath = match[0];
                }
            }
            // Nếu là URL (http://...), giữ nguyên
        }

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

// Upload avatar image
exports.uploadAvatar = async (req, res) => {
    try {
        const { studentId } = req.params;
        
        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: 'Không có file ảnh được upload'
            });
        }

        // File đã được lưu bởi multer
        // Tên file: studentId_timestamp.extension
        const filename = req.file.filename;
        const imagePath = `uploads/avatars/${filename}`;
        
        // Cập nhật imagePath vào database
        const [updated] = await Card.update(
            { imagePath: imagePath },
            { where: { studentId } }
        );

        if (!updated) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // Lấy card đã cập nhật
        const card = await Card.findOne({ 
            where: { studentId }
        });

        res.json({
            success: true,
            message: 'Upload ảnh đại diện thành công',
            data: {
                imagePath: imagePath,
                imageUrl: `/uploads/avatars/${filename}`, // URL để truy cập ảnh
                card: card
            }
        });
    } catch (error) {
        console.error('Upload avatar error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi upload ảnh đại diện',
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
