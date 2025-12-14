# Hướng dẫn Chạy Server - Đơn giản nhất

## 📋 Yêu cầu

- **Node.js** (version 14+): https://nodejs.org/
- **MySQL** (XAMPP khuyến nghị): https://www.apachefriends.org/

---

## 🚀 Các bước chạy server

### Bước 1: Cài đặt MySQL (XAMPP)

1. Download và cài XAMPP: https://www.apachefriends.org/
2. Mở **XAMPP Control Panel**
3. Click **Start** cho MySQL
4. Click **Admin** bên cạnh MySQL → Mở phpMyAdmin
5. Click tab **SQL**
6. Chạy lệnh:
   ```sql
   CREATE DATABASE IF NOT EXISTS library_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
7. Click **Go** để tạo database

### Bước 2: Cài đặt thư viện

Mở Terminal/Command Prompt trong thư mục `server`:

```bash
cd E:\Code\javacard\server
npm install
```

Đợi 1-2 phút để cài đặt xong.

### Bước 3: Tạo file `.env`

Tạo file `.env` trong thư mục `server` với nội dung:

```env
PORT=3000
NODE_ENV=development

# MySQL Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_card_db
DB_USER=root
DB_PASSWORD=
DB_TIMEZONE=+07:00

# Optional
JWT_SECRET=your_secret_key_here
ADMIN_SECRET_KEY=ADMIN_DEFAULT_KEY_CHANGE_IN_PRODUCTION
```

**Lưu ý:**
- Nếu MySQL có password, thay `DB_PASSWORD=` thành `DB_PASSWORD=your_password`
- Nếu dùng MySQL standalone (không phải XAMPP), có thể cần thay `DB_USER` và `DB_PASSWORD`

### Bước 4: Chạy Migrations (Tạo tables)

```bash
npm run migrate
```

Kết quả mong đợi:
```
Sequelize CLI [Node: ...]
Loaded configuration file "config/config.js".
Using environment "development".
== 20241212000001-create-books-table: migrating =======
== 20241212000001-create-books-table: migrated (0.xxx s)
...
```

### Bước 5: Chạy Seed (Tạo dữ liệu mẫu)

```bash
npm run seed
```

Kết quả mong đợi:
```
✅ Connected to MySQL
✅ Cleared existing data
📝 Creating sample cards...
  ✓ Created card for Nguyễn Văn A (2021600001)
  ...
✅ Database seeded successfully!
```

### Bước 6: Chạy Server

```bash
npm run dev
```

Kết quả mong đợi:
```
MySQL connection has been established successfully.
MySQL timezone set to Asia/Ho_Chi_Minh (UTC+7)
Database synchronized successfully
Server is running on http://localhost:3000
Environment: development
Database: MySQL (localhost:3306/library_card_db)
```

### Bước 7: Test Server

Mở browser, truy cập: **http://localhost:3000/api/health**

Kết quả:
```json
{
  "status": "OK",
  "message": "Library Card Server is running",
  "database": "MySQL",
  "timestamp": "2024-...",
  "localTime": "...",
  "timezone": "Asia/Ho_Chi_Minh"
}
```

---

## ✅ Hoàn tất!

Server đã chạy thành công! Bạn có thể:

- Test API: http://localhost:3000/api/cards
- Import `postman_collection.json` vào Postman để test đầy đủ
- Kết nối Java GUI với server

---

## 🔄 Các lệnh thường dùng

### Chạy server
```bash
npm run dev
```

### Dừng server
Nhấn `Ctrl + C` trong terminal

### Chạy lại migrations
```bash
npm run migrate
```

### Xóa và tạo lại dữ liệu mẫu
```bash
npm run seed
```

### Rollback migration (nếu cần)
```bash
npm run migrate:undo
```

---

## 🐛 Troubleshooting

### Lỗi: `npm: command not found`
→ Cài Node.js từ https://nodejs.org/

### Lỗi: `Access denied for user 'root'@'localhost'`
→ Kiểm tra:
- MySQL đã start trong XAMPP chưa?
- Password trong `.env` đúng chưa?
- Nếu MySQL không có password, để `DB_PASSWORD=` (rỗng)

### Lỗi: `Unknown database 'library_card_db'`
→ Tạo database trong phpMyAdmin:
```sql
CREATE DATABASE library_card_db;
```

### Lỗi: `Port 3000 already in use`
→ Đổi PORT trong `.env` thành `3001` hoặc số khác

### Lỗi: `Cannot find module`
→ Chạy lại:
```bash
npm install
```

### Server chạy nhưng API trả về 404
→ Kiểm tra URL phải có `/api/`:
- ✅ `http://localhost:3000/api/cards`
- ❌ `http://localhost:3000/cards`

---

## 📝 Dữ liệu mẫu

Sau khi chạy seed, có 5 thẻ mẫu:

| MSSV       | Họ tên        | Email                    |
|------------|---------------|--------------------------|
| 2021600001 | Nguyễn Văn A  | nguyenvana@example.com   |
| 2021600002 | Trần Thị B    | tranthib@example.com     |
| 2021600003 | Lê Văn C      | levanc@example.com       |
| 2021600004 | Phạm Thị D    | phamthid@example.com     |
| 2021600005 | Hoàng Văn E   | hoangvane@example.com    |

**Lưu ý:** PIN không được lưu trên server (chỉ lưu trên card/applet). Để test, tạo thẻ mới trong Java GUI.

---

## 📚 Tài liệu tham khảo

- `README.md` - Tổng quan về server
- `INTEGRATION_GUIDE.md` - Hướng dẫn tích hợp với Java GUI
- `postman_collection.json` - Postman collection để test API
