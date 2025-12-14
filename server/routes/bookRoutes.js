const express = require('express');
const router = express.Router();
const bookController = require('../controllers/bookController');
const authController = require('../controllers/authController');

// Book routes
// User routes (require authentication - user có thể mượn/trả sách của mình)
router.post('/borrow', authController.authenticate, bookController.borrowBook);
router.patch('/return/:borrowId', authController.authenticate, bookController.returnBook);
router.get('/student/:studentId', authController.authenticate, bookController.getBorrowedBooksByStudent);

// Admin only routes (chỉ admin mới được truy cập)
router.get('/', authController.authenticateAdmin, bookController.getAllBorrowedBooks); // Xem tất cả sách đã mượn - admin only
router.delete('/:borrowId', authController.authenticateAdmin, bookController.deleteBorrowedBook); // Xóa record mượn sách - admin only

module.exports = router;



