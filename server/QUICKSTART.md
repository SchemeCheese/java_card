# Quick Start Guide - Chạy Server trong 5 phút

## Bước 1: Cài đặt Node.js (nếu chưa có)

**Windows:**
1. Tải Node.js từ: https://nodejs.org/
2. Chọn version LTS (khuyến nghị)
3. Chạy file cài đặt, next → next → finish
4. Mở Command Prompt/PowerShell, kiểm tra:
```bash
node --version
npm --version
```

## Bước 2: Cài đặt MySQL

### Cách 1: XAMPP (Khuyến nghị - Dễ nhất)

1. Download XAMPP: https://www.apachefriends.org/download.html
2. Cài đặt XAMPP
3. Mở XAMPP Control Panel
4. Start **MySQL** (click button "Start")
5. Click "Admin" bên cạnh MySQL → Mở phpMyAdmin
6. Click "SQL" tab
7. Copy & paste nội dung file `server/database/schema.sql` và chạy
8. File `.env`:
   ```env
   PORT=3000
   NODE_ENV=development
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=library_card_db
   DB_USER=root
   DB_PASSWORD=
   JWT_SECRET=your_secret_key
   SALT_ROUNDS=10
   ```

### Cách 2: MySQL Community Server (Standalone)

1. Download: https://dev.mysql.com/downloads/mysql/
2. Cài đặt MySQL Server
3. Nhớ password của root user
4. Mở MySQL Workbench hoặc command line
5. Tạo database:
   ```sql
   CREATE DATABASE library_card_db;
   ```
6. File `.env`:
   ```env
   PORT=3000
   NODE_ENV=development
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=library_card_db
   DB_USER=root
   DB_PASSWORD=your_mysql_password
   JWT_SECRET=your_secret_key
   SALT_ROUNDS=10
   ```

## Bước 3: Cài đặt Dependencies

Mở Terminal/Command Prompt trong thư mục `server`:

```bash
# Di chuyển vào thư mục server
cd E:\Code\javacard\server

# Cài đặt các packages
npm install
```

Đợi khoảng 1-2 phút để npm tải các packages.

## Bước 4: Chạy Server

```bash
npm run dev
```

Nếu thành công, bạn sẽ thấy:
```
✅ Connected to MongoDB successfully
🚀 Server is running on http://localhost:3000
📊 Environment: development
```

## Bước 5: Test Server

### Test 1: Health Check

Mở browser, truy cập: http://localhost:3000/api/health

Kết quả:
```json
{
  "status": "OK",
  "message": "Library Card Server is running",
  "timestamp": "2024-01-15T..."
}
```

### Test 2: Tạo dữ liệu mẫu

Mở terminal mới (giữ server đang chạy), chạy:
```bash
cd E:\Code\javacard\server
node scripts/seed.js
```

Kết quả:
```
✅ Connected to MongoDB
✅ Cleared existing data
📝 Creating sample cards...
  ✓ Created card for Nguyễn Văn A (2021600001)
  ...
✅ Database seeded successfully!
```

### Test 3: Test API

**Dùng browser:**
- http://localhost:3000/api/cards
- Sẽ thấy danh sách 5 thẻ

**Dùng curl:**
```bash
curl http://localhost:3000/api/cards
```

**Dùng Postman:**
1. Download Postman: https://www.postman.com/downloads/
2. Import file `postman_collection.json`
3. Chạy request "Get All Cards"

## Bước 6: Test các chức năng chính

### Test Verify PIN
```bash
curl -X POST http://localhost:3000/api/pin/verify ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021600001\",\"pin\":\"123456\"}"
```

Kết quả:
```json
{
  "success": true,
  "message": "Xác thực PIN thành công",
  "triesRemaining": 3
}
```

### Test Create Transaction
```bash
curl -X POST http://localhost:3000/api/transactions ^
  -H "Content-Type: application/json" ^
  -d "{\"studentId\":\"2021600001\",\"type\":\"Nạp tiền\",\"amount\":50000,\"description\":\"Test\"}"
```

### Test Get Card Info
```bash
curl http://localhost:3000/api/cards/2021600001
```

## Xem Database

### MongoDB Atlas:
1. Vào https://cloud.mongodb.com/
2. Login → Chọn cluster
3. Click "Browse Collections"
4. Xem các collections: `cards`, `borrowedbooks`, `transactions`

### MongoDB Compass (Local):
1. Download: https://www.mongodb.com/try/download/compass
2. Connect với URI: `mongodb://localhost:27017`
3. Chọn database `library_card_db`

## Dừng Server

Trong terminal đang chạy server, nhấn: `Ctrl + C`

## Chạy lại Server

```bash
cd E:\Code\javacard\server
npm run dev
```

## Xóa dữ liệu và tạo lại

```bash
# Chạy lại seed script
node scripts/seed.js
```

## Troubleshooting

### Lỗi: `npm: command not found`
→ Node.js chưa được cài hoặc chưa thêm vào PATH. Cài lại Node.js.

### Lỗi: `Access denied for user`
→ MySQL password sai hoặc user không tồn tại. Kiểm tra:
- MySQL service đã start chưa (trong XAMPP)
- Username/password trong `.env` đúng chưa
- Database `library_card_db` đã được tạo chưa

### Lỗi: `ER_BAD_DB_ERROR: Unknown database`
→ Database chưa được tạo. Chạy file `database/schema.sql` trong phpMyAdmin hoặc MySQL Workbench

### Lỗi: `Port 3000 already in use`
→ Port 3000 đang được dùng. Đổi PORT trong `.env` thành `3001` hoặc số khác.

### Lỗi: `Cannot find module`
→ Chạy lại `npm install`

### Server chạy nhưng API trả về 404
→ Kiểm tra URL, phải có `/api/` prefix:
- ✅ `http://localhost:3000/api/cards`
- ❌ `http://localhost:3000/cards`

## Next Steps

Sau khi server chạy thành công:

1. Đọc `API_GUIDE.md` để hiểu cách test API
2. Đọc `INTEGRATION_GUIDE.md` để tích hợp với Java GUI
3. Import `postman_collection.json` vào Postman để test dễ hơn

## Thông tin đăng nhập mẫu

Sau khi chạy seed script, có 5 thẻ mẫu:

| MSSV       | Họ tên        | PIN    |
|------------|---------------|--------|
| 2021600001 | Nguyễn Văn A  | 123456 |
| 2021600002 | Trần Thị B    | 111111 |
| 2021600003 | Lê Văn C      | 222222 |
| 2021600004 | Phạm Thị D    | 333333 |
| 2021600005 | Hoàng Văn E   | 444444 |

## Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra server logs trong terminal
2. Kiểm tra MongoDB connection
3. Xem file `SETUP.md` để troubleshooting chi tiết

