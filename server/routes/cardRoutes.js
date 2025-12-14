const express = require('express');
const router = express.Router();
const cardController = require('../controllers/cardController');
const authController = require('../controllers/authController');
const upload = require('../middleware/upload');

// Card routes
// Public routes (no authentication required)
router.post('/', cardController.createCard); // Tạo thẻ mới - public
router.put('/:studentId/rsa-key', cardController.updateRSAPublicKey); // Update RSA key - public (cần khi tạo thẻ)

// Protected routes (require authentication - user có thể xem/sửa thông tin của mình)
router.get('/:studentId', authController.authenticate, cardController.getCard);
router.put('/:studentId', authController.authenticate, cardController.updateCard);
router.put('/:studentId/balance', authController.authenticate, cardController.updateBalance);
router.get('/:studentId/rsa-key', authController.authenticate, cardController.getRSAPublicKey);
router.post('/:studentId/avatar', authController.authenticate, upload.single('avatar'), cardController.uploadAvatar); // Upload avatar - authenticated

// Admin only routes (chỉ admin mới được truy cập)
router.get('/', authController.authenticateAdmin, cardController.getAllCards); // Xem tất cả thẻ - admin only
router.delete('/:studentId', authController.authenticateAdmin, cardController.deleteCard); // Xóa thẻ - admin only

module.exports = router;



