# Library Card Server - Express.js Backend

Server Express.js cho hệ thống quản lý thẻ thư viện JavaCard với MySQL database.

## 📋 Yêu cầu

- Node.js >= 14.x
- MySQL >= 5.7 hoặc XAMPP
- npm hoặc yarn

## 🚀 Cài đặt

### 1. Cài đặt dependencies

```bash
cd server
npm install
```

### 2. Cấu hình MySQL

**Cách 1: XAMPP (Khuyến nghị)**
- Tải XAMPP: https://www.apachefriends.org/download.html
- Cài đặt và mở XAMPP Control Panel
- Start MySQL service
- Chạy file `database/schema.sql` trong phpMyAdmin

**Cách 2: MySQL Server (Standalone)**
- Tải MySQL: https://dev.mysql.com/downloads/mysql/
- Cài đặt MySQL Server
- Tạo database bằng MySQL Workbench hoặc command line

### 3. Cấu hình biến môi trường

File `.env` đã được tạo sẵn với cấu hình mặc định. Bạn có thể chỉnh sửa nếu cần:

```env
PORT=3000
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_card_db
DB_USER=root
DB_PASSWORD=
JWT_SECRET=your_jwt_secret_key_change_in_production
SALT_ROUNDS=10
```

### 4. Chạy server

**Development mode (với nodemon - tự động restart khi code thay đổi):**
```bash
npm run dev
```

**Production mode:**
```bash
npm start
```

Server sẽ chạy tại: `http://localhost:3000`

## 📚 API Documentation

### Base URL
```
http://localhost:3000/api
```

### 1. Card APIs

#### Tạo thẻ mới
```http
POST /api/cards
Content-Type: application/json

{
  "studentId": "2021600001",
  "holderName": "Nguyễn Văn A",
  "email": "nguyenvana@example.com",
  "department": "Công nghệ thông tin",
  "birthDate": "01/01/2000",
  "address": "123 Đường ABC, TP.HCM",
  "pin": "123456"
}
```

#### Lấy thông tin thẻ
```http
GET /api/cards/:studentId
```

#### Lấy tất cả thẻ
```http
GET /api/cards
```

#### Cập nhật thông tin thẻ
```http
PUT /api/cards/:studentId
Content-Type: application/json

{
  "email": "newemail@example.com",
  "address": "Địa chỉ mới"
}
```

#### Cập nhật số dư
```http
PATCH /api/cards/:studentId/balance
Content-Type: application/json

{
  "amount": 50000
}
```

#### Xóa thẻ
```http
DELETE /api/cards/:studentId
```

### 2. PIN APIs

#### Xác thực PIN
```http
POST /api/pin/verify
Content-Type: application/json

{
  "studentId": "2021600001",
  "pin": "123456"
}
```

#### Đổi PIN
```http
POST /api/pin/change
Content-Type: application/json

{
  "studentId": "2021600001",
  "oldPin": "123456",
  "newPin": "654321"
}
```

#### Lấy số lần thử còn lại
```http
GET /api/pin/tries/:studentId
```

#### Reset số lần thử (Admin)
```http
POST /api/pin/reset/:studentId
```

### 3. Book APIs

#### Mượn sách
```http
POST /api/books/borrow
Content-Type: application/json

{
  "studentId": "2021600001",
  "bookId": "BOOK001",
  "bookName": "Lập trình Java",
  "dueDate": "2024-01-31"
}
```

#### Trả sách
```http
PATCH /api/books/return/:borrowId
```

#### Lấy sách đã mượn của sinh viên
```http
GET /api/books/student/:studentId
GET /api/books/student/:studentId?status=Đang mượn
```

#### Lấy tất cả sách đã mượn
```http
GET /api/books
GET /api/books?status=Quá hạn
```

#### Xóa bản ghi mượn sách
```http
DELETE /api/books/:borrowId
```

### 4. Transaction APIs

#### Tạo giao dịch
```http
POST /api/transactions
Content-Type: application/json

{
  "studentId": "2021600001",
  "type": "Nạp tiền",
  "amount": 100000,
  "description": "Nạp tiền vào thẻ"
}
```

Các loại giao dịch:
- `Nạp tiền`
- `Trả phạt`
- `Rút tiền`
- `Thanh toán dịch vụ`

#### Lấy giao dịch của sinh viên
```http
GET /api/transactions/student/:studentId
GET /api/transactions/student/:studentId?type=Nạp tiền&limit=20
```

#### Lấy thống kê giao dịch
```http
GET /api/transactions/student/:studentId/stats
```

#### Lấy tất cả giao dịch
```http
GET /api/transactions
GET /api/transactions?type=Nạp tiền&status=Thành công
```

#### Lấy giao dịch theo ID
```http
GET /api/transactions/:transactionId
```

### 5. Health Check
```http
GET /api/health
```

## 🗄️ Database Schema (MySQL)

### Table: cards
```sql
CREATE TABLE cards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) UNIQUE NOT NULL,
    holder_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    birth_date VARCHAR(10) NOT NULL,
    address VARCHAR(255) NOT NULL,
    status ENUM('Hoạt động', 'Khóa', 'Tạm khóa') DEFAULT 'Hoạt động',
    borrowed_books INT DEFAULT 0,
    pin_hash VARCHAR(255) NOT NULL,
    pin_salt VARCHAR(255) NOT NULL,
    pin_tries INT DEFAULT 3,
    balance BIGINT DEFAULT 0,
    image_path VARCHAR(255) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Table: borrowed_books
```sql
CREATE TABLE borrowed_books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    book_name VARCHAR(255) NOT NULL,
    borrow_date DATETIME NOT NULL,
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    status ENUM('Đang mượn', 'Quá hạn', 'Đã trả') DEFAULT 'Đang mượn',
    overdue_days INT DEFAULT 0,
    fine BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES cards(student_id) ON DELETE CASCADE
);
```

### Table: transactions
```sql
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    type ENUM('Nạp tiền', 'Trả phạt', 'Rút tiền', 'Thanh toán dịch vụ') NOT NULL,
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    status ENUM('Thành công', 'Thất bại', 'Đang xử lý') DEFAULT 'Thành công',
    description VARCHAR(255) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES cards(student_id) ON DELETE CASCADE
);
```

## 🔒 Bảo mật

- PIN được hash bằng PBKDF2 với salt ngẫu nhiên
- Giới hạn 3 lần thử PIN, sau đó khóa thẻ
- PIN phải là 6 chữ số
- Tất cả endpoint đều validate input

## 📝 Response Format

### Success Response
```json
{
  "success": true,
  "message": "Thông báo thành công",
  "data": { ... }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Thông báo lỗi",
  "error": "Chi tiết lỗi (chỉ trong development mode)"
}
```

## 🧪 Test APIs với Postman

1. Import collection: Tạo collection với các request trên
2. Set base URL: `http://localhost:3000/api`
3. Test theo thứ tự:
   - Tạo thẻ mới
   - Xác thực PIN
   - Mượn sách
   - Tạo giao dịch
   - Lấy thông tin

## 🔧 Troubleshooting

### Lỗi kết nối MySQL
```
Error: connect ECONNREFUSED 127.0.0.1:3306
```
**Giải pháp:** Kiểm tra MySQL service đã chạy chưa (trong XAMPP hoặc Services)

### Port đã được sử dụng
```
Error: listen EADDRINUSE: address already in use :::3000
```
**Giải pháp:** Đổi PORT trong file `.env` hoặc kill process đang dùng port 3000

## 📞 Liên hệ

Nếu có vấn đề, vui lòng tạo issue hoặc liên hệ qua email.

