/**
 * Script to seed database with sample data for MySQL
 * Run: node scripts/seed.js
 */

require('dotenv').config();
const { Card, Book, BorrowedBook, Transaction, sequelize, syncDatabase } = require('../models');
const crypto = require('crypto');

// Helper function to generate PIN hash
const generatePinHash = (pin, salt) => {
    return crypto.pbkdf2Sync(pin, salt, 10000, 32, 'sha256').toString('hex');
};

// Sample data
const sampleCards = [
    {
        studentId: '2021600001',
        holderName: 'Nguyễn Văn A',
        email: 'nguyenvana@example.com',
        department: 'Công nghệ thông tin',
        birthDate: '01/01/2000',
        address: '123 Đường ABC, TP.HCM',
        pin: '123456',
        balance: 150000
    },
    {
        studentId: '2021600002',
        holderName: 'Trần Thị B',
        email: 'tranthib@example.com',
        department: 'Điện tử viễn thông',
        birthDate: '15/05/1999',
        address: '456 Đường XYZ, Hà Nội',
        pin: '111111',
        balance: 200000
    },
    {
        studentId: '2021600003',
        holderName: 'Lê Văn C',
        email: 'levanc@example.com',
        department: 'Cơ khí',
        birthDate: '20/08/2001',
        address: '789 Đường PQR, Đà Nẵng',
        pin: '222222',
        balance: 50000
    },
    {
        studentId: '2021600004',
        holderName: 'Phạm Thị D',
        email: 'phamthid@example.com',
        department: 'Kinh tế',
        birthDate: '10/12/2000',
        address: '321 Đường MNO, Hải Phòng',
        pin: '333333',
        balance: 300000
    },
    {
        studentId: '2021600005',
        holderName: 'Hoàng Văn E',
        email: 'hoangvane@example.com',
        department: 'Công nghệ thông tin',
        birthDate: '25/03/1998',
        address: '654 Đường STU, Cần Thơ',
        pin: '444444',
        balance: 100000
    }
];

// Sample library books
const libraryBooks = [
    {
        bookId: 'BOOK001',
        title: 'Lập trình Java căn bản',
        author: 'Nguyễn Văn X',
        isbn: '978-604-0-00001-1',
        publisher: 'NXB Giáo Dục',
        publishYear: 2020,
        category: 'Lập trình',
        description: 'Giáo trình Java cơ bản cho sinh viên',
        totalCopies: 10,
        availableCopies: 8,
        location: 'Kệ A-12'
    },
    {
        bookId: 'BOOK002',
        title: 'Cấu trúc dữ liệu và giải thuật',
        author: 'Trần Thị Y',
        isbn: '978-604-0-00002-2',
        publisher: 'NXB Đại Học Quốc Gia',
        publishYear: 2019,
        category: 'Lập trình',
        description: 'Các cấu trúc dữ liệu cơ bản và giải thuật',
        totalCopies: 8,
        availableCopies: 7,
        location: 'Kệ A-13'
    },
    {
        bookId: 'BOOK003',
        title: 'Design Patterns',
        author: 'Gang of Four',
        isbn: '978-0-201-63361-0',
        publisher: 'Addison-Wesley',
        publishYear: 1994,
        category: 'Kỹ thuật phần mềm',
        description: 'Các mẫu thiết kế phần mềm',
        totalCopies: 5,
        availableCopies: 4,
        location: 'Kệ B-05'
    },
    {
        bookId: 'BOOK004',
        title: 'Clean Code',
        author: 'Robert C. Martin',
        isbn: '978-0-132-35088-4',
        publisher: 'Prentice Hall',
        publishYear: 2008,
        category: 'Kỹ thuật phần mềm',
        description: 'Nghệ thuật viết code sạch',
        totalCopies: 7,
        availableCopies: 7,
        location: 'Kệ B-06'
    },
    {
        bookId: 'BOOK005',
        title: 'Introduction to Algorithms',
        author: 'Thomas H. Cormen',
        isbn: '978-0-262-03384-8',
        publisher: 'MIT Press',
        publishYear: 2009,
        category: 'Lập trình',
        description: 'Sách giáo khoa về giải thuật',
        totalCopies: 6,
        availableCopies: 5,
        location: 'Kệ A-14'
    },
    {
        bookId: 'BOOK006',
        title: 'Database System Concepts',
        author: 'Abraham Silberschatz',
        isbn: '978-0-073-52332-3',
        publisher: 'McGraw-Hill',
        publishYear: 2010,
        category: 'Cơ sở dữ liệu',
        description: 'Khái niệm về hệ quản trị cơ sở dữ liệu',
        totalCopies: 8,
        availableCopies: 8,
        location: 'Kệ C-01'
    },
    {
        bookId: 'BOOK007',
        title: 'Computer Networks',
        author: 'Andrew S. Tanenbaum',
        isbn: '978-0-132-12678-2',
        publisher: 'Pearson',
        publishYear: 2011,
        category: 'Mạng máy tính',
        description: 'Giáo trình mạng máy tính',
        totalCopies: 10,
        availableCopies: 9,
        location: 'Kệ C-10'
    },
    {
        bookId: 'BOOK008',
        title: 'Operating System Concepts',
        author: 'Abraham Silberschatz',
        isbn: '978-1-118-06333-0',
        publisher: 'Wiley',
        publishYear: 2012,
        category: 'Hệ điều hành',
        description: 'Khái niệm hệ điều hành',
        totalCopies: 9,
        availableCopies: 9,
        location: 'Kệ C-05'
    },
    {
        bookId: 'BOOK009',
        title: 'Artificial Intelligence: A Modern Approach',
        author: 'Stuart Russell',
        isbn: '978-0-136-04259-4',
        publisher: 'Pearson',
        publishYear: 2020,
        category: 'Trí tuệ nhân tạo',
        description: 'Giáo trình AI hiện đại',
        totalCopies: 5,
        availableCopies: 5,
        location: 'Kệ D-01'
    },
    {
        bookId: 'BOOK010',
        title: 'Machine Learning',
        author: 'Tom Mitchell',
        isbn: '978-0-070-42807-2',
        publisher: 'McGraw-Hill',
        publishYear: 1997,
        category: 'Trí tuệ nhân tạo',
        description: 'Học máy và ứng dụng',
        totalCopies: 4,
        availableCopies: 4,
        location: 'Kệ D-02'
    }
];

const sampleBooks = [
    {
        studentId: '2021600001',
        bookId: 'BOOK001',
        bookName: 'Lập trình Java căn bản',
        borrowDate: new Date('2024-01-01'),
        dueDate: new Date('2024-02-01'),
        status: 'Đang mượn'
    },
    {
        studentId: '2021600001',
        bookId: 'BOOK002',
        bookName: 'Cấu trúc dữ liệu và giải thuật',
        borrowDate: new Date('2024-01-05'),
        dueDate: new Date('2024-02-05'),
        status: 'Đang mượn'
    },
    {
        studentId: '2021600002',
        bookId: 'BOOK003',
        bookName: 'Design Patterns',
        borrowDate: new Date('2023-12-15'),
        dueDate: new Date('2024-01-15'),
        status: 'Quá hạn'
    },
    {
        studentId: '2021600003',
        bookId: 'BOOK004',
        bookName: 'Clean Code',
        borrowDate: new Date('2023-12-01'),
        dueDate: new Date('2024-01-01'),
        returnDate: new Date('2024-01-02'),
        status: 'Đã trả',
        overdueDays: 1,
        fine: 5000
    },
    {
        studentId: '2021600004',
        bookId: 'BOOK005',
        bookName: 'Introduction to Algorithms',
        borrowDate: new Date('2024-01-10'),
        dueDate: new Date('2024-02-10'),
        status: 'Đang mượn'
    }
];

const seedDatabase = async () => {
    try {
        // Connect to MySQL
        await sequelize.authenticate();
        console.log('Connected to MySQL');

        // Sync database (recreate tables)
        console.log('Dropping and recreating tables...');
        await syncDatabase(true); // force: true will drop tables
        console.log('Tables created');

        // Create cards
        console.log('Creating sample cards...');
        const createdCards = [];
        for (const cardData of sampleCards) {
            const pinSalt = crypto.randomBytes(16).toString('hex');
            const pinHash = generatePinHash(cardData.pin, pinSalt);
            
            const card = await Card.create({
                studentId: cardData.studentId,
                holderName: cardData.holderName,
                email: cardData.email,
                department: cardData.department,
                birthDate: cardData.birthDate,
                address: cardData.address,
                pinHash: pinHash,
                pinSalt: pinSalt,
                pinTries: 3,
                balance: cardData.balance || 0,
                status: 'Hoạt động'
            });
            
            createdCards.push(card);
            console.log(`  Created card for ${card.holderName} (${card.studentId})`);
        }

        // Create library books
        console.log('Creating library books...');
        const createdLibraryBooks = [];
        for (const bookData of libraryBooks) {
            const book = await Book.create(bookData);
            createdLibraryBooks.push(book);
            console.log(`  Added "${book.title}" by ${book.author}`);
        }

        // Create borrowed books
        console.log('\nCreating borrowed books records...');
        const createdBorrowedBooks = [];
        for (const bookData of sampleBooks) {
            const borrowedBook = await BorrowedBook.create(bookData);
            createdBorrowedBooks.push(borrowedBook);
            console.log(`  ${bookData.studentId} borrowed "${bookData.bookName}"`);
            
            // Update card's borrowed books count
            if (bookData.status !== 'Đã trả') {
                const card = await Card.findOne({ where: { studentId: bookData.studentId } });
                if (card) {
                    card.borrowedBooksCount += 1;
                    await card.save();
                }
            }

            // Update library book's available copies
            const libraryBook = await Book.findOne({ where: { bookId: bookData.bookId } });
            if (libraryBook && bookData.status !== 'Đã trả') {
                libraryBook.availableCopies -= 1;
                if (libraryBook.availableCopies === 0) {
                    libraryBook.status = 'Hết sách';
                }
                await libraryBook.save();
            }
        }

        // Create sample transactions
        console.log('Creating sample transactions...');
        const transactionTypes = [
            { studentId: '2021600001', type: 'Nạp tiền', amount: 100000, desc: 'Nạp tiền ban đầu' },
            { studentId: '2021600001', type: 'Nạp tiền', amount: 50000, desc: 'Nạp thêm tiền' },
            { studentId: '2021600002', type: 'Nạp tiền', amount: 200000, desc: 'Nạp tiền' },
            { studentId: '2021600003', type: 'Nạp tiền', amount: 100000, desc: 'Nạp tiền' },
            { studentId: '2021600003', type: 'Trả phạt', amount: 50000, desc: 'Trả phạt trễ hạn' },
            { studentId: '2021600004', type: 'Nạp tiền', amount: 300000, desc: 'Nạp tiền' },
            { studentId: '2021600005', type: 'Nạp tiền', amount: 100000, desc: 'Nạp tiền' }
        ];

        for (const txData of transactionTypes) {
            const card = await Card.findOne({ where: { studentId: txData.studentId } });
            const amount = txData.amount;
            const balanceBefore = parseInt(card.balance) - (txData.type === 'Nạp tiền' ? amount : -amount);
            
            const transaction = await Transaction.create({
                studentId: txData.studentId,
                type: txData.type,
                amount: amount,
                balanceBefore: balanceBefore,
                balanceAfter: parseInt(card.balance),
                status: 'Thành công',
                description: txData.desc
            });
            
            console.log(`  ${txData.studentId}: ${txData.type} ${amount.toLocaleString('vi-VN')} VND`);
        }

        console.log('\nDatabase seeded successfully!');
        console.log('\nSummary:');
        console.log(`  - Cards: ${createdCards.length}`);
        console.log(`  - Library Books: ${createdLibraryBooks.length}`);
        console.log(`  - Borrowed Books Records: ${createdBorrowedBooks.length}`);
        console.log(`  - Transactions: ${transactionTypes.length}`);
        
        console.log('\nSample login credentials:');
        console.log('  Student ID: 2021600001, PIN: 123456');
        console.log('  Student ID: 2021600002, PIN: 111111');
        console.log('  Student ID: 2021600003, PIN: 222222');
        console.log('  Student ID: 2021600004, PIN: 333333');
        console.log('  Student ID: 2021600005, PIN: 444444');

    } catch (error) {
        console.error('Error seeding database:', error);
    } finally {
        // Close connection
        await sequelize.close();
        console.log('\nConnection closed');
    }
};

// Run seed
seedDatabase();
