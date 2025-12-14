const express = require('express');
const router = express.Router();
const pinController = require('../controllers/pinController');

// PIN routes
// ✅ PIN verification should be done on card (applet), not on server
// See: SimulatorService.verifyPin() for card-based verification

router.post('/change', pinController.changePin);
router.get('/tries/:studentId', pinController.getPinTries);
router.post('/reset-tries/:studentId', pinController.resetPinTries);

// Admin routes - Reset PIN when card is lost
router.put('/admin/reset/:studentId', pinController.resetPin);

module.exports = router;



