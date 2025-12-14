const { Card } = require('../models');
const crypto = require('crypto');

// ⚠️ NOTE: PIN is no longer stored on server - only on card (applet)
// PIN verification must be done on card using SimulatorService.verifyPin()
// Helper function kept for admin reset PIN (if needed in future)
const generatePinHash = (pin, salt) => {
    return crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256').toString('hex');
};

// Change PIN
// ⚠️ DEPRECATED: PIN is no longer stored on server
// ✅ PIN change must be done on card (applet) using SimulatorService.changePin()
// This endpoint is kept for backward compatibility but does nothing
exports.changePin = async (req, res) => {
    try {
        console.warn('⚠️ DEPRECATED: PIN change endpoint. PIN is no longer stored on server.');
        console.warn('⚠️ Use SimulatorService.changePin() to change PIN on card instead.');
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

        // ⚠️ PIN is no longer stored on server
        // PIN change must be done on card using SimulatorService.changePin()
        return res.status(410).json({
            success: false,
            message: 'Endpoint này đã bị vô hiệu hóa. PIN không còn được lưu trên server.',
            note: 'Vui lòng sử dụng SimulatorService.changePin() để đổi PIN trên thẻ.'
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
// ⚠️ DEPRECATED: PIN tries are no longer stored on server
// ✅ PIN tries must be read from card using SimulatorService.getPinTriesRemaining()
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

        // ⚠️ PIN tries are no longer stored on server
        return res.status(410).json({
            success: false,
            message: 'Endpoint này đã bị vô hiệu hóa. PIN tries không còn được lưu trên server.',
            note: 'Vui lòng sử dụng SimulatorService.getPinTriesRemaining() để lấy số lần thử từ thẻ.'
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

// Reset PIN (Admin only)
// ⚠️ DEPRECATED: PIN is no longer stored on server
// ✅ Admin reset PIN must be done on card using SimulatorService.resetPin()
// When card is lost, admin must issue a new card with new PIN
exports.resetPin = async (req, res) => {
    try {
        const { studentId } = req.params;
        const { newPin, adminKey } = req.body;

        // Verify admin key
        const requiredAdminKey = process.env.ADMIN_SECRET_KEY || 'ADMIN_DEFAULT_KEY_CHANGE_IN_PRODUCTION';
        
        if (!adminKey || adminKey !== requiredAdminKey) {
            return res.status(403).json({
                success: false,
                message: 'Unauthorized: Invalid admin key',
                error: 'Admin key is required to reset PIN'
            });
        }

        const card = await Card.findOne({ where: { studentId } });

        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thẻ'
            });
        }

        // ⚠️ PIN is no longer stored on server
        // Admin must reset PIN on card directly
        return res.status(410).json({
            success: false,
            message: 'Endpoint này đã bị vô hiệu hóa. PIN không còn được lưu trên server.',
            note: 'Khi mất thẻ, admin phải cấp thẻ mới và set PIN trực tiếp trên thẻ bằng SimulatorService.resetPin() hoặc tạo thẻ mới.'
        });
    } catch (error) {
        console.error('Reset PIN error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khi reset PIN',
            error: error.message
        });
    }
};

// Reset PIN tries (admin only)
// ⚠️ DEPRECATED: PIN tries are no longer stored on server
// ✅ PIN tries must be reset on card using SimulatorService
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

        // ⚠️ PIN tries are no longer stored on server
        return res.status(410).json({
            success: false,
            message: 'Endpoint này đã bị vô hiệu hóa. PIN tries không còn được lưu trên server.',
            note: 'Vui lòng reset PIN tries trên thẻ bằng SimulatorService hoặc cấp thẻ mới.'
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
