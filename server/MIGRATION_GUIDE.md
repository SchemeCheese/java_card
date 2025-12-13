# Migration Guide - Hướng dẫn chạy Migrations

## 📋 Tổng quan

Thay vì phải chạy SQL trực tiếp trong database, bạn có thể sử dụng **Sequelize Migrations** để tự động tạo và quản lý schema.

## 🚀 Cách sử dụng

### 1. Cài đặt dependencies

```bash
cd server
npm install
```

### 2. Cấu hình database

Tạo file `.env` với thông tin database:

```env
# Database
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=yourpassword
DB_NAME=library_card_db

# Server
PORT=3000
NODE_ENV=development
```

### 3. Tạo database (chỉ cần làm 1 lần)

Đảm bảo MySQL đang chạy, sau đó tạo database:

```bash
mysql -u root -p
```

```sql
CREATE DATABASE library_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### 4. Chạy migrations

Tự động tạo tất cả các bảng:

```bash
npm run migrate
```

Lệnh này sẽ chạy tất cả các migration files và tạo các bảng:
- ✅ `books` - Thông tin sách trong thư viện
- ✅ `cards` - Thông tin thẻ sinh viên
- ✅ `borrowed_books` - Sách đã mượn
- ✅ `transactions` - Lịch sử giao dịch

### 5. Seed dữ liệu mẫu (tùy chọn)

```bash
npm run seed
```

Lệnh này sẽ tạo:
- 10 thẻ sinh viên mẫu
- 10 sách trong thư viện
- Các bản ghi mượn sách
- Các giao dịch mẫu

## 📝 Các lệnh Migration

### Chạy tất cả migrations chưa chạy
```bash
npm run migrate
```

### Rollback migration gần nhất
```bash
npm run migrate:undo
```

### Rollback tất cả migrations
```bash
npm run migrate:undo:all
```

### Xem trạng thái migrations
```bash
npx sequelize-cli db:migrate:status
```

## 🔧 Cấu trúc thư mục

```
server/
├── database/
│   ├── migrations/           # Migration files
│   │   ├── 20241212000001-create-books-table.js
│   │   ├── 20241212000002-create-cards-table.js
│   │   ├── 20241212000003-create-borrowed-books-table.js
│   │   └── 20241212000004-create-transactions-table.js
│   ├── seeders/             # Seeder files (nếu có)
│   └── schema.sql           # SQL schema (backup/reference)
├── .sequelizerc             # Sequelize CLI config
└── package.json
```

## 📊 Thứ tự tạo bảng

Migrations sẽ chạy theo thứ tự:

1. **books** - Phải tạo trước vì `borrowed_books` tham chiếu đến nó
2. **cards** - Phải tạo trước vì `borrowed_books` và `transactions` tham chiếu đến nó
3. **borrowed_books** - Có foreign keys đến `books` và `cards`
4. **transactions** - Có foreign key đến `cards`

## ⚠️ Lưu ý quan trọng

### Foreign Keys
- `borrowed_books.student_id` → `cards.student_id` (ON DELETE CASCADE)
- `borrowed_books.book_id` → `books.book_id` (ON DELETE RESTRICT)
- `transactions.student_id` → `cards.student_id` (ON DELETE CASCADE)

### Charset & Collation
Tất cả bảng đều sử dụng:
- Character set: `utf8mb4`
- Collation: `utf8mb4_unicode_ci`

Điều này đảm bảo hỗ trợ đầy đủ tiếng Việt và emoji.

## 🔄 Workflow thông thường

### Lần đầu setup
```bash
# 1. Cài đặt
npm install

# 2. Tạo database
mysql -u root -p -e "CREATE DATABASE library_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Chạy migrations
npm run migrate

# 4. Seed dữ liệu mẫu
npm run seed

# 5. Khởi động server
npm run dev
```

### Reset database (để test lại)
```bash
# Rollback tất cả
npm run migrate:undo:all

# Chạy lại migrations
npm run migrate

# Seed lại dữ liệu
npm run seed
```

## 📚 Tài liệu tham khảo

- [Sequelize Migrations](https://sequelize.org/docs/v6/other-topics/migrations/)
- [Sequelize CLI](https://github.com/sequelize/cli)

## ❓ Troubleshooting

### Lỗi: "Access denied for user"
- Kiểm tra username/password trong `.env`
- Đảm bảo user có quyền tạo database

### Lỗi: "Database does not exist"
- Chạy lệnh tạo database trước khi migrate
- Hoặc dùng: `npm run db:create` (nếu có script)

### Lỗi: "Table already exists"
- Xóa bảng cũ hoặc rollback migrations
- Hoặc drop database và tạo lại:
  ```sql
  DROP DATABASE library_card_db;
  CREATE DATABASE library_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### Lỗi: Foreign key constraint fails
- Kiểm tra thứ tự migrations (books và cards phải tạo trước)
- Đảm bảo không có dữ liệu cũ conflict

## 🎯 Next Steps

Sau khi chạy migrations thành công, xem:
- [QUICKSTART.md](./QUICKSTART.md) - Hướng dẫn sử dụng API
- [README.md](./README.md) - Documentation đầy đủ
- [MYSQL_SETUP.md](./MYSQL_SETUP.md) - Chi tiết về MySQL setup

