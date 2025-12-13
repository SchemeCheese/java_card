const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transactionController');

// Transaction routes
router.post('/', transactionController.createTransaction);
router.get('/student/:studentId', transactionController.getTransactionsByStudent);
router.get('/student/:studentId/stats', transactionController.getTransactionStats);
router.get('/:transactionId', transactionController.getTransactionById);
router.get('/', transactionController.getAllTransactions);

module.exports = router;

