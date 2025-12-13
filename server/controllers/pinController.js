const { Card } = require('../models');
const crypto = require('crypto');

// Helper function to generate PIN hash using PBKDF2
const generatePinHash = (pin, salt) => {
    return crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256').toString('hex');
};

// Verify PIN
exports.verifyPin = async (req, res) => {
    try {
        const { studentId, pin } = req.body;

        if (!studentId || !pin) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp MSSV và PIN'
            });
        }

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // Check if card is locked (0 tries remaining)
        if (card.pinTries === 0) {
            return res.status(403).json({
                success: false,
                message: 'Thẻ đã bị khóa do nhập sai PIN quá nhiều lần',
                triesRemaining: 0
            });
        }

        // Verify PIN
        const pinHash = generatePinHash(pin, card.pinSalt);
        
        if (pinHash === card.pinHash) {
            // Reset tries on successful verification
            card.pinTries = 3;
            await card.save();

            return res.json({
                success: true,
                message: 'Xác thực PIN thành công',
                triesRemaining: 3
            });
        } else {
            // Decrement tries
            card.pinTries -= 1;
            await card.save();

            return res.status(401).json({
                success: false,
                message: `PIN không chính xác. Còn ${card.pinTries} lần thử`,
                triesRemaining: card.pinTries
            });
        }
    } catch (error) {
        console.error('Verify PIN error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi xác thực PIN',
            error: error.message
        });
    }
};

// Change PIN
exports.changePin = async (req, res) => {
    try {
        const { studentId, oldPin, newPin } = req.body;

        if (!studentId || !oldPin || !newPin) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng cung cấp đầy đủ thông tin'
            });
        }

        if (newPin.length !== 6 || !/^\d+$/.test(newPin)) {
            return res.status(400).json({
                success: false,
                message: 'PIN mới phải là 6 chữ số'
            });
        }

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // Check if card is locked
        if (card.pinTries === 0) {
            return res.status(403).json({
                success: false,
                message: 'Thẻ đã bị khóa do nhập sai PIN quá nhiều lần'
            });
        }

        // Verify old PIN
        const oldPinHash = generatePinHash(oldPin, card.pinSalt);
        
        if (oldPinHash !== card.pinHash) {
            card.pinTries -= 1;
            await card.save();

            return res.status(401).json({
                success: false,
                message: `PIN cũ không chính xác. Còn ${card.pinTries} lần thử`,
                triesRemaining: card.pinTries
            });
        }

        // Generate new salt and hash for new PIN
        const newSalt = crypto.randomBytes(16).toString('hex');
        const newPinHash = generatePinHash(newPin, newSalt);

        card.pinHash = newPinHash;
        card.pinSalt = newSalt;
        card.pinTries = 3;
        await card.save();

        res.json({
            success: true,
            message: 'Đổi PIN thành công'
        });
    } catch (error) {
        console.error('Change PIN error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi đổi PIN',
            error: error.message
        });
    }
};

// Get PIN tries remaining
exports.getPinTries = async (req, res) => {
    try {
        const { studentId } = req.params;

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        res.json({
            success: true,
            triesRemaining: card.pinTries
        });
    } catch (error) {
        console.error('Get PIN tries error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi lấy số lần thử PIN',
            error: error.message
        });
    }
};

// Reset PIN tries (admin only)
exports.resetPinTries = async (req, res) => {
    try {
        const { studentId } = req.params;

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        card.pinTries = 3;
        await card.save();

        res.json({
            success: true,
            message: 'Reset số lần thử PIN thành công',
            triesRemaining: 3
        });
    } catch (error) {
        console.error('Reset PIN tries error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi reset số lần thử PIN',
            error: error.message
        });
    }
};
