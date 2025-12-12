package models;

/**
 * Model class for library card information
 * Contains all student/cardholder data stored on the JavaCard
 * [UPDATED] Added pinRetryCount logic
 */
public class CardInfo {
    private String studentId;      // MSSV - Mã số sinh viên
    private String holderName;     // Họ và tên
    private String email;          // Email
    private String department;     // Khoa / Viện
    private String birthDate;      // Ngày sinh (DD/MM/YYYY)
    private String address;        // Địa chỉ
    private String status;         // Trạng thái thẻ (Hoạt động / Khóa)
    private int borrowedBooks;     // Số sách đang mượn
    private String pin;            // Mã PIN của sinh viên
    private long balance;          // Số dư tài khoản
    private String imagePath;      // Đường dẫn đến ảnh đại diện
    private int pinRetryCount;

    public CardInfo() {
        this.studentId = "";
        this.holderName = "";
        this.email = "";
        this.department = "";
        this.birthDate = "";
        this.address = "";
        this.status = "Hoạt động";
        this.borrowedBooks = 0;
        this.pin = "000000";  // PIN mặc định
        this.balance = 0;
        this.imagePath = "";
        this.pinRetryCount = 3;
    }

    public CardInfo(String studentId, String holderName, String email,
                    String department, String birthDate, String address) {
        this.studentId = studentId;
        this.holderName = holderName;
        this.email = email;
        this.department = department;
        this.birthDate = birthDate;
        this.address = address;
        this.status = "Hoạt động";
        this.borrowedBooks = 0;
        this.pin = "000000";  // PIN mặc định
        this.balance = 0;
        this.imagePath = "";
        this.pinRetryCount = 3;
    }

    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBorrowedBooks() {
        return borrowedBooks;
    }

    public void setBorrowedBooks(int borrowedBooks) {
        this.borrowedBooks = borrowedBooks;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getPinRetryCount() {
        return pinRetryCount;
    }

    public void setPinRetryCount(int pinRetryCount) {
        this.pinRetryCount = pinRetryCount;
    }

    public boolean isInitialized() {
        return studentId != null && !studentId.isEmpty()
                && holderName != null && !holderName.isEmpty();
    }

    @Override
    public String toString() {
        return "CardInfo{" +
                "studentId='" + studentId + '\'' +
                ", holderName='" + holderName + '\'' +
                ", status='" + status + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", pinRetryCount=" + pinRetryCount +
                '}';
    }
}
