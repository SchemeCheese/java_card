const express = require('express');
const router = express.Router();
const paymentController = require('../controllers/paymentController');
const oauthMiddleware = require('../middleware/oauthMiddleware');
const { sseController } = require('../controllers/sseController');

// Public routes (authentication optional)
// Tạo payment mới
router.post('/create', paymentController.createPayment);

// Lấy thông tin payment theo ID (để polling status)
router.get('/:paymentId', paymentController.getPayment);

// SSE stream để nhận real-time updates
router.get('/:paymentId/stream', sseController.streamPaymentStatus);

// Lấy danh sách payments (có filter)
router.get('/', paymentController.getPayments);

// Protected webhook endpoint
// Option 1: Authenticate via OAuth 2.0 Token (Disabled)
/*
router.post('/webhook/callback', 
  oauthMiddleware.verifyToken, 
  paymentController.handleWebhook
);
*/

// Option 2: Authenticate via API Key (Authorization: Apikey <KEY>)
router.post('/webhook/callback', 
  oauthMiddleware.verifyApiKey,
  (req, res) => paymentController.handleWebhook(req, res)
);

module.exports = router;
