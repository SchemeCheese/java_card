const express = require('express');
const router = express.Router();
const oauthMiddleware = require('../middleware/oauthMiddleware');

/**
 * OAuth 2.0 Token Endpoint
 * Sepay sẽ gọi endpoint này để lấy access token
 */
router.post('/token', oauthMiddleware.issueToken);

module.exports = router;
