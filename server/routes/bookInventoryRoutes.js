const express = require('express');
const router = express.Router();
const bookInventoryController = require('../controllers/bookInventoryController');
const authController = require('../controllers/authController');

// Book inventory routes (quản lý sách trong thư viện)
// Public routes (no authentication required - để user xem catalog)
router.get('/', bookInventoryController.getAllBooks);
router.get('/search', bookInventoryController.searchBooks);
router.get('/categories', bookInventoryController.getCategories);
router.get('/stats', bookInventoryController.getBookStats);
router.get('/:bookId', bookInventoryController.getBookById);

// Protected routes (require admin authentication - chỉ admin mới được tạo/sửa/xóa sách)
router.post('/', authController.authenticateAdmin, bookInventoryController.createBook);
router.put('/:bookId', authController.authenticateAdmin, bookInventoryController.updateBook);
router.patch('/:bookId/copies', authController.authenticateAdmin, bookInventoryController.updateCopies);
router.delete('/:bookId', authController.authenticateAdmin, bookInventoryController.deleteBook);

module.exports = router;



