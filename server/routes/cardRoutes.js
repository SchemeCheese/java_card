const express = require('express');
const router = express.Router();
const cardController = require('../controllers/cardController');

// Card routes
router.post('/', cardController.createCard);
router.get('/', cardController.getAllCards);
router.get('/:studentId', cardController.getCard);
router.put('/:studentId', cardController.updateCard);
router.delete('/:studentId', cardController.deleteCard);
router.put('/:studentId/balance', cardController.updateBalance);
router.put('/:studentId/rsa-key', cardController.updateRSAPublicKey);
router.get('/:studentId/rsa-key', cardController.getRSAPublicKey);

module.exports = router;



