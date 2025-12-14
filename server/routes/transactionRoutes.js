const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transactionController');
const authController = require('../controllers/authController');

// Transaction routes
// User routes (require authentication - user có thể xem lịch sử của mình)
router.post('/', authController.authenticate, transactionController.createTransaction);
router.get('/student/:studentId', authController.authenticate, transactionController.getTransactionsByStudent);
router.get('/student/:studentId/stats', authController.authenticate, transactionController.getTransactionStats);
router.get('/:transactionId', authController.authenticate, transactionController.getTransactionById);

// Admin only routes (chỉ admin mới được truy cập)
router.get('/', authController.authenticateAdmin, transactionController.getAllTransactions); // Xem tất cả transactions - admin only

module.exports = router;

