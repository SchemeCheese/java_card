const express = require('express');
const router = express.Router();
const bookInventoryController = require('../controllers/bookInventoryController');

// Book inventory routes (quản lý sách trong thư viện)
router.get('/', bookInventoryController.getAllBooks);
router.get('/search', bookInventoryController.searchBooks);
router.get('/categories', bookInventoryController.getCategories);
router.get('/stats', bookInventoryController.getBookStats);
router.get('/:bookId', bookInventoryController.getBookById);
router.post('/', bookInventoryController.createBook);
router.put('/:bookId', bookInventoryController.updateBook);
router.patch('/:bookId/copies', bookInventoryController.updateCopies);
router.delete('/:bookId', bookInventoryController.deleteBook);

module.exports = router;



