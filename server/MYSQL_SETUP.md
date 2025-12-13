# Hướng dẫn Setup MySQL chi tiết

## Option 1: XAMPP (Khuyến nghị cho Windows - Dễ nhất)

### Bước 1: Cài đặt XAMPP

1. **Download XAMPP:**
   - Truy cập: https://www.apachefriends.org/download.html
   - Chọn version cho Windows
   - Download file installer (khoảng 150MB)

2. **Cài đặt:**
   - Chạy file installer
   - Chọn components: ✅ MySQL, ✅ phpMyAdmin (Apache tùy chọn)
   - Chọn thư mục cài đặt (mặc định: `C:\xampp`)
   - Next → Install → Finish

3. **Khởi động MySQL:**
   - Mở **XAMPP Control Panel**
   - Tìm dòng "MySQL"
   - Click button **"Start"**
   - Đợi status chuyển sang màu xanh

### Bước 2: Tạo Database

**Cách 1: Dùng phpMyAdmin (GUI - Dễ)**

1. Trong XAMPP Control Panel, click "Admin" bên cạnh MySQL
2. Trình duyệt sẽ mở phpMyAdmin (`http://localhost/phpmyadmin`)
3. Click tab **"SQL"** ở menu trên
4. Mở file `server/database/schema.sql` trong notepad
5. Copy toàn bộ nội dung
6. Paste vào ô SQL trong phpMyAdmin
7. Click button **"Go"** (hoặc "Thực thi")
8. Kiểm tra: Bên trái sẽ xuất hiện database `library_card_db`

**Cách 2: Dùng Command Line**

```bash
# Mở Command Prompt/PowerShell
cd C:\xampp\mysql\bin

# Login vào MySQL (password mặc định trống)
.\mysql.exe -u root -p
# Press Enter (không cần nhập password)

# Chạy script
source E:\Code\javacard\server\database\schema.sql

# Kiểm tra
SHOW DATABASES;
USE library_card_db;
SHOW TABLES;
```

### Bước 3: Cấu hình .env

Tạo file `server/.env`:
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

**Chú ý:** `DB_PASSWORD=` để trống (không có password mặc định)

---

## Option 2: MySQL Community Server (Standalone)

### Bước 1: Cài đặt MySQL Server

1. **Download:**
   - Truy cập: https://dev.mysql.com/downloads/mysql/
   - Chọn "MySQL Installer for Windows"
   - Chọn "mysql-installer-community" (khoảng 400MB)

2. **Cài đặt:**
   - Chạy installer
   - Chọn "Developer Default" hoặc "Server only"
   - Next → Execute (đợi download các components)
   - Cấu hình:
     - **Type and Networking:** Mặc định (Port 3306)
     - **Authentication Method:** "Use Strong Password Encryption"
     - **Accounts and Roles:** 
       - Root Password: Nhập password mạnh (GHI NHỚ!)
       - (Có thể tạo thêm user khác)
     - **Windows Service:**
       - ✅ Configure MySQL Server as a Windows Service
       - ✅ Start the MySQL Server at System Startup
   - Next → Execute → Finish

3. **Cài MySQL Workbench (GUI Tool):**
   - Đã được cài cùng MySQL Installer
   - Hoặc download riêng: https://dev.mysql.com/downloads/workbench/

### Bước 2: Tạo Database

**Cách 1: Dùng MySQL Workbench**

1. Mở **MySQL Workbench**
2. Click vào connection "Local instance MySQL80"
3. Nhập root password
4. Click menu **File → Open SQL Script**
5. Chọn file `server/database/schema.sql`
6. Click icon **⚡ Execute** (hoặc Ctrl+Shift+Enter)
7. Kiểm tra: Bên trái (SCHEMAS) sẽ có `library_card_db`

**Cách 2: Dùng Command Line**

```bash
# Mở Command Prompt as Administrator
# Thêm MySQL vào PATH nếu chưa có:
set PATH=%PATH%;C:\Program Files\MySQL\MySQL Server 8.0\bin

# Login
mysql -u root -p
# Nhập password bạn đã set

# Chạy script
source E:\Code\javacard\server\database\schema.sql;

# Kiểm tra
SHOW DATABASES;
USE library_card_db;
SHOW TABLES;
```

### Bước 3: Cấu hình .env

```env
PORT=3000
NODE_ENV=development
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_card_db
DB_USER=root
DB_PASSWORD=your_mysql_root_password
JWT_SECRET=your_secret_key
SALT_ROUNDS=10
```

**Chú ý:** Thay `your_mysql_root_password` bằng password bạn đã set

---

## Option 3: MySQL trên macOS/Linux

### macOS (Homebrew)
```bash
# Cài đặt
brew install mysql

# Start service
brew services start mysql

# Secure installation (optional but recommended)
mysql_secure_installation

# Login
mysql -u root -p

# Tạo database
source /path/to/server/database/schema.sql;
```

### Ubuntu/Debian
```bash
# Cài đặt
sudo apt update
sudo apt install mysql-server

# Start service
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure installation
sudo mysql_secure_installation

# Login
sudo mysql -u root -p

# Tạo database
source /path/to/server/database/schema.sql;
```

---

## Kiểm tra MySQL đã chạy

### Windows (XAMPP)
- Mở XAMPP Control Panel
- MySQL phải hiển thị màu xanh

### Windows (Standalone)
```bash
# Mở Services (Win+R → services.msc)
# Tìm "MySQL80" → Status phải là "Running"
```

### Command Line (All platforms)
```bash
# Test connection
mysql -u root -p -e "SELECT VERSION();"
```

---

## Xem dữ liệu trong Database

### phpMyAdmin (XAMPP)
1. http://localhost/phpmyadmin
2. Click `library_card_db`
3. Xem các tables: `cards`, `borrowed_books`, `transactions`

### MySQL Workbench
1. Mở MySQL Workbench
2. Connect tới Local instance
3. Expand `library_card_db` trong SCHEMAS
4. Right-click table → "Select Rows"

### Command Line
```sql
mysql -u root -p

USE library_card_db;

-- Xem tất cả tables
SHOW TABLES;

-- Xem dữ liệu
SELECT * FROM cards;
SELECT * FROM borrowed_books;
SELECT * FROM transactions;

-- Xem cấu trúc table
DESCRIBE cards;
```

---

## Troubleshooting

### Port 3306 already in use
**Lỗi:** MySQL không start được, port 3306 đang được sử dụng

**Giải pháp:**
1. Kiểm tra process đang dùng port:
   ```bash
   netstat -ano | findstr :3306
   ```
2. Kill process hoặc đổi port MySQL
3. Hoặc: Có thể bạn đã cài 2 MySQL (XAMPP + Standalone)

### Access denied for user 'root'@'localhost'
**Lỗi:** Password sai hoặc user không tồn tại

**Giải pháp XAMPP:**
- Password mặc định: để trống
- File `.env`: `DB_PASSWORD=` (không có gì)

**Giải pháp Standalone:**
- Reset password: https://dev.mysql.com/doc/refman/8.0/en/resetting-permissions.html

### Can't connect to MySQL server
**Lỗi:** Service không chạy

**Giải pháp:**
```bash
# Windows XAMPP
- Mở XAMPP Control Panel
- Click "Start" MySQL

# Windows Standalone
net start MySQL80

# Linux/Mac
sudo systemctl start mysql
```

### Unknown database 'library_card_db'
**Lỗi:** Database chưa được tạo

**Giải pháp:**
- Chạy lại file `database/schema.sql`

---

## Gỡ bỏ MySQL (nếu cần cài lại)

### XAMPP
1. Stop MySQL trong XAMPP Control Panel
2. Uninstall XAMPP từ Control Panel → Programs

### Standalone
1. Control Panel → Programs → Uninstall MySQL
2. Xóa thư mục: `C:\Program Files\MySQL`
3. Xóa thư mục data: `C:\ProgramData\MySQL`

---

## Next Steps

Sau khi MySQL chạy thành công:
1. Chạy `npm install` trong thư mục `server`
2. Tạo file `.env` với config MySQL
3. Chạy `npm run dev` để start server
4. Chạy `node scripts/seed.js` để tạo dữ liệu mẫu



