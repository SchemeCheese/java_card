const express = require('express');
const router = express.Router();
const pinController = require('../controllers/pinController');

// PIN routes
router.post('/verify', pinController.verifyPin);
router.post('/change', pinController.changePin);
router.get('/tries/:studentId', pinController.getPinTries);
router.get('/is-default/:studentId', pinController.isDefaultPin);
router.post('/reset/:studentId', pinController.resetPinTries);

module.exports = router;



