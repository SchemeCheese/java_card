const { BorrowedBook, Card, Book, sequelize } = require('../models');
const { Op } = require('sequelize');
const {
  parsePagination,
  formatPaginatedResponse,
} = require('../utils/pagination');

// Get outstanding fines by student
exports.getOutstandingFinesByStudent = async (req, res) => {
  try {
    const { studentId } = req.params;

    if (!studentId) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng cung cấp mã sinh viên',
      });
    }

    const fines = await BorrowedBook.findAll({
      where: {
        studentId,
        status: { [Op.in]: ['Quá hạn', 'Đã trả'] },
        fine: { [Op.gt]: 0 },
        finePaid: false,
      },
      order: [[sequelize.col('updated_at'), 'DESC']],
    });

    return res.json({
      success: true,
      data: fines,
    });
  } catch (error) {
    console.error('Get outstanding fines error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi lấy danh sách tiền phạt',
      error: error.message,
    });
  }
};

// Pay all outstanding fines for a student
exports.payOutstandingFines = async (req, res) => {
  const t = await sequelize.transaction();
  try {
    const { studentId } = req.body;

    if (!studentId) {
      await t.rollback();
      return res.status(400).json({
        success: false,
        message: 'Vui lòng cung cấp mã sinh viên',
      });
    }

    const card = await Card.findOne({
      where: { studentId },
      transaction: t,
      lock: t.LOCK.UPDATE,
    });

    if (!card) {
      await t.rollback();
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy thẻ',
      });
    }

    // Only allow paying fines for returned books. Overdue (not returned) fines are still accruing.
    const fineRows = await BorrowedBook.findAll({
      where: {
        studentId,
        status: 'Đã trả',
        fine: { [Op.gt]: 0 },
        finePaid: false,
      },
      transaction: t,
      lock: t.LOCK.UPDATE,
    });

    const totalPaid = fineRows.reduce(
      (sum, row) => sum + parseInt(row.fine || 0),
      0
    );
    const paidCount = fineRows.length;

    if (totalPaid <= 0) {
      await t.commit();
      return res.json({
        success: true,
        message:
          'Không có tiền phạt cần thanh toán (chỉ thanh toán được khi đã trả sách)',
        data: {
          totalPaid: 0,
          paidCount: 0,
          balanceAfter: parseInt(card.balance),
        },
      });
    }

    const balanceBefore = parseInt(card.balance);
    const balanceAfter = balanceBefore - totalPaid;

    const { Transaction } = require('../models');
    await Transaction.create(
      {
        studentId,
        type: 'Trả phạt',
        amount: totalPaid,
        balanceBefore,
        balanceAfter,
        status: 'Thành công',
        description: `Thanh toán tiền phạt (${paidCount} khoản)`,
      },
      { transaction: t }
    );

    card.balance = balanceAfter;
    await card.save({ transaction: t });

    const now = new Date();
    await BorrowedBook.update(
      { finePaid: true, finePaidAt: now },
      {
        where: { id: { [Op.in]: fineRows.map((r) => r.id) } },
        transaction: t,
      }
    );

    await t.commit();
    return res.json({
      success: true,
      message: 'Thanh toán phạt thành công',
      data: {
        totalPaid,
        paidCount,
        balanceAfter,
      },
    });
  } catch (error) {
    try {
      await t.rollback();
    } catch (_) { }
    console.error('Pay outstanding fines error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi thanh toán tiền phạt',
      error: error.message,
    });
  }
};

// Borrow a book
exports.borrowBook = async (req, res) => {
  try {
    const { studentId, bookId, bookName, dueDate } = req.body;

    if (!studentId || !bookId || !bookName || !dueDate) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng cung cấp đầy đủ thông tin',
      });
    }

    // Check if card exists
    const card = await Card.findOne({ where: { studentId } });
    if (!card) {
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy thẻ',
      });
    }

    // Check if card is active
    if (card.status !== 'Hoạt động') {
      return res.status(403).json({
        success: false,
        message: 'Thẻ không ở trạng thái hoạt động',
      });
    }

    // Check if student has reached borrowing limit (e.g., 5 books)
    const activeBorrows = await BorrowedBook.count({
      where: {
        studentId,
        status: { [Op.in]: ['Đang mượn', 'Quá hạn'] },
      },
    });

    if (activeBorrows >= 5) {
      return res.status(400).json({
        success: false,
        message: 'Đã đạt giới hạn mượn sách (tối đa 5 cuốn)',
      });
    }

    // Check if book is already borrowed by this student
    const existingBorrow = await BorrowedBook.findOne({
      where: {
        studentId,
        bookId,
        status: { [Op.in]: ['Đang mượn', 'Quá hạn'] },
      },
    });

    if (existingBorrow) {
      return res.status(400).json({
        success: false,
        message: 'Bạn đã mượn cuốn sách này rồi',
      });
    }

    // Check if book exists and has available copies
    const book = await Book.findOne({ where: { bookId } });
    if (book) {
      if (book.availableCopies <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Sách này đã hết, không còn bản nào có sẵn',
        });
      }
      // Decrease available copies
      book.availableCopies -= 1;
      if (book.availableCopies === 0) {
        book.status = 'Hết sách';
      }
      await book.save();
    }

    // Create borrowed book record
    const borrowedBook = await BorrowedBook.create({
      studentId,
      bookId,
      bookName,
      borrowDate: new Date(),
      dueDate: new Date(dueDate),
      status: 'Đang mượn',
    });

    // Update card's borrowed books count
    card.borrowedBooksCount += 1;
    await card.save();

    res.status(201).json({
      success: true,
      message: 'Mượn sách thành công',
      data: borrowedBook,
    });
  } catch (error) {
    console.error('Borrow book error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi mượn sách',
      error: error.message,
    });
  }
};

// Return a book
exports.returnBook = async (req, res) => {
  try {
    const { borrowId } = req.params;

    const borrowedBook = await BorrowedBook.findByPk(borrowId);

    if (!borrowedBook) {
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy thông tin mượn sách',
      });
    }

    if (borrowedBook.status === 'Đã trả') {
      return res.status(400).json({
        success: false,
        message: 'Sách này đã được trả rồi',
      });
    }

    // Calculate if overdue
    const now = new Date();
    const dueDate = new Date(borrowedBook.dueDate);

    if (now > dueDate) {
      // Calculate days difference using only date part (ignore time)
      const startDate = new Date(
        dueDate.getFullYear(),
        dueDate.getMonth(),
        dueDate.getDate()
      );
      const endDate = new Date(
        now.getFullYear(),
        now.getMonth(),
        now.getDate()
      );
      const diffTime = endDate - startDate;
      const overdueDays = Math.max(
        1,
        Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      );
      borrowedBook.overdueDays = overdueDays;
      borrowedBook.fine = overdueDays * 50; // 50 VND per day
    }

    borrowedBook.returnDate = now;
    borrowedBook.status = 'Đã trả';
    await borrowedBook.save();

    // Update card's borrowed books count
    const card = await Card.findOne({
      where: { studentId: borrowedBook.studentId },
    });
    if (card) {
      card.borrowedBooksCount = Math.max(0, card.borrowedBooksCount - 1);
      await card.save();
    }

    // Increase available copies in Book table
    const book = await Book.findOne({ where: { bookId: borrowedBook.bookId } });
    if (book) {
      book.availableCopies += 1;
      if (book.status === 'Hết sách' && book.availableCopies > 0) {
        book.status = 'Có sẵn';
      }
      await book.save();
    }

    res.json({
      success: true,
      message: 'Trả sách thành công',
      data: {
        borrowedBook,
        fine: borrowedBook.fine,
      },
    });
  } catch (error) {
    console.error('Return book error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi trả sách',
      error: error.message,
    });
  }
};

// Get borrowed books by student
exports.getBorrowedBooksByStudent = async (req, res) => {
  try {
    const { studentId } = req.params;
    const { status } = req.query;
    const { page, limit, offset } = parsePagination(req.query, {
      defaultLimit: 20,
      maxLimit: 100,
    });

    let whereClause = { studentId };

    if (status) {
      whereClause.status = status;
    }

    const result = await BorrowedBook.findAndCountAll({
      where: whereClause,
      order: [[sequelize.col('borrow_date'), 'DESC']],
      limit,
      offset,
    });

    // Update overdue status / fine (keep it updated even if status already "Quá hạn")
    const now = new Date();
    for (let book of result.rows) {
      if (
        (book.status === 'Đang mượn' || book.status === 'Quá hạn') &&
        now > book.dueDate
      ) {
        book.status = 'Quá hạn';
        const diffTime = Math.abs(now - book.dueDate);
        book.overdueDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        book.fine = book.overdueDays * 50;
        await book.save();
      }
    }

    res.json(formatPaginatedResponse(result, page, limit));
  } catch (error) {
    console.error('Get borrowed books error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi lấy danh sách sách mượn',
      error: error.message,
    });
  }
};

// Get all borrowed books
exports.getAllBorrowedBooks = async (req, res) => {
  try {
    const { status } = req.query;
    const { page, limit, offset } = parsePagination(req.query, {
      defaultLimit: 20,
      maxLimit: 100,
    });

    let whereClause = {};

    if (status) {
      whereClause.status = status;
    }

    const result = await BorrowedBook.findAndCountAll({
      where: whereClause,
      order: [[sequelize.col('borrow_date'), 'DESC']],
      limit,
      offset,
    });

    res.json(formatPaginatedResponse(result, page, limit));
  } catch (error) {
    console.error('Get all borrowed books error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi lấy danh sách tất cả sách mượn',
      error: error.message,
    });
  }
};

// Delete borrowed book record
exports.deleteBorrowedBook = async (req, res) => {
  try {
    const { borrowId } = req.params;

    const deleted = await BorrowedBook.destroy({
      where: { id: borrowId },
    });

    if (!deleted) {
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy thông tin mượn sách',
      });
    }

    res.json({
      success: true,
      message: 'Xóa thông tin mượn sách thành công',
    });
  } catch (error) {
    console.error('Delete borrowed book error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi khi xóa thông tin mượn sách',
      error: error.message,
    });
  }
};
