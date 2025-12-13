-- Library Card Database Schema for MySQL
-- Create database
CREATE DATABASE IF NOT EXISTS library_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_card_db;

-- Table: books (Thông tin sách trong thư viện)
CREATE TABLE IF NOT EXISTS books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_id VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    publisher VARCHAR(255),
    publish_year INT,
    category VARCHAR(100),
    description TEXT,
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    status ENUM('Có sẵn', 'Hết sách', 'Ngừng cho mượn') DEFAULT 'Có sẵn',
    location VARCHAR(100) COMMENT 'Vị trí sách trong thư viện',
    cover_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_id (book_id),
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: cards
CREATE TABLE IF NOT EXISTS cards (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    holder_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    birth_date VARCHAR(10) NOT NULL,
    address VARCHAR(255) NOT NULL,
    status ENUM('Hoạt động', 'Khóa', 'Tạm khóa') DEFAULT 'Hoạt động',
    borrowed_books INT DEFAULT 0,
    -- ⚠️ PIN fields removed - PIN should only be stored on card (applet) for security
    balance BIGINT DEFAULT 0,
    image_path VARCHAR(255) DEFAULT '',
    rsa_public_key TEXT COMMENT 'RSA Public Key (PEM format) for card authentication',
    rsa_key_created_at TIMESTAMP NULL COMMENT 'Thời gian tạo khóa RSA',
    aes_master_key_hash VARCHAR(255) COMMENT 'Hash of AES master key for this card',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: borrowed_books
CREATE TABLE IF NOT EXISTS borrowed_books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    book_id VARCHAR(50) NOT NULL,
    book_name VARCHAR(255) NOT NULL,
    borrow_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    status ENUM('Đang mượn', 'Quá hạn', 'Đã trả') DEFAULT 'Đang mượn',
    overdue_days INT DEFAULT 0,
    fine BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    FOREIGN KEY (student_id) REFERENCES cards(student_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: transactions
CREATE TABLE IF NOT EXISTS transactions (
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
    INDEX idx_student_id (student_id),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type),
    FOREIGN KEY (student_id) REFERENCES cards(student_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample queries for testing
-- SELECT * FROM cards;
-- SELECT * FROM borrowed_books WHERE student_id = '2021600001';
-- SELECT * FROM transactions WHERE student_id = '2021600001' ORDER BY created_at DESC;

