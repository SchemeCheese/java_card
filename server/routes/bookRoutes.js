const express = require('express');
const router = express.Router();
const bookController = require('../controllers/bookController');

// Book routes
router.post('/borrow', bookController.borrowBook);
router.patch('/return/:borrowId', bookController.returnBook);
router.get('/fines/student/:studentId', bookController.getOutstandingFinesByStudent);
router.post('/fines/pay', bookController.payOutstandingFines);
router.get('/student/:studentId', bookController.getBorrowedBooksByStudent);
router.get('/', bookController.getAllBorrowedBooks);
router.delete('/:borrowId', bookController.deleteBorrowedBook);

module.exports = router;



