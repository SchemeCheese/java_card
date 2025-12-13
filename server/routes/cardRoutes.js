const express = require('express');
const router = express.Router();
const cardController = require('../controllers/cardController');

// Card routes
router.post('/', cardController.createCard);
router.get('/', cardController.getAllCards);
router.get('/:studentId', cardController.getCard);
router.put('/:studentId', cardController.updateCard);
router.delete('/:studentId', cardController.deleteCard);
router.patch('/:studentId/balance', cardController.updateBalance);

module.exports = router;



