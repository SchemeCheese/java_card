package models;

/**
 * Model class for borrowed book information
 */
public class BorrowedBook {
    private String bookId;
    private String bookName;
    private String borrowDate;
    private String dueDate;
    private String status;
    private int overdueDays;

    public BorrowedBook(String bookId, String bookName, String borrowDate, String dueDate, String status, int overdueDays) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
        this.overdueDays = overdueDays;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(String borrowDate) {
        this.borrowDate = borrowDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }
}
