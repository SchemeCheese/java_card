package models;

/**
 * Model class for borrowed book information
 */
public class BorrowedBook {
    private int id; // Borrow record ID (from database)
    private String bookId;
    private String bookName;
    private String borrowDate;
    private String dueDate;
    private String status;
    private int overdueDays;
    private long fine;
    private boolean finePaid;

    public BorrowedBook(String bookId, String bookName, String borrowDate, String dueDate, String status, int overdueDays) {
        this.id = 0; // Default, will be set from API
        this.bookId = bookId;
        this.bookName = bookName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
        this.overdueDays = overdueDays;
        this.fine = 0;
        this.finePaid = false;
    }
    
    public BorrowedBook(int id, String bookId, String bookName, String borrowDate, String dueDate, String status, int overdueDays) {
        this.id = id;
        this.bookId = bookId;
        this.bookName = bookName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
        this.overdueDays = overdueDays;
        this.fine = 0;
        this.finePaid = false;
    }

    public long getFine() {
        return fine;
    }

    public void setFine(long fine) {
        this.fine = fine;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
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
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
}
