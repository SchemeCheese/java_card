const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const bodyParser = require('body-parser');
require('dotenv').config();

// Set timezone cho Node.js process (Asia/Ho_Chi_Minh = UTC+7)
// Nếu muốn dùng UTC, comment dòng này
process.env.TZ = process.env.TZ || 'Asia/Ho_Chi_Minh';

// Import database and models
const { sequelize, testConnection, syncDatabase } = require('./models');

// Import routes
const authRoutes = require('./routes/authRoutes');
const cardRoutes = require('./routes/cardRoutes');
const bookInventoryRoutes = require('./routes/bookInventoryRoutes');
const bookRoutes = require('./routes/bookRoutes');
const transactionRoutes = require('./routes/transactionRoutes');
const pinRoutes = require('./routes/pinRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(morgan('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Static file serving - serve uploaded avatars
const path = require('path');
app.use('/uploads/avatars', express.static(path.join(__dirname, 'uploads/avatars')));

// Initialize database and start server
const initializeServer = async () => {
    try {
        // Test MySQL connection
        const connected = await testConnection();
        if (!connected) {
            console.error('Failed to connect to MySQL. Please check your configuration.');
            process.exit(1);
        }

        // Sync database (create tables if not exist)
        // Use { force: true } to drop and recreate tables (only for development!)
        await syncDatabase(false);

        // Routes
        app.use('/api/auth', authRoutes);                    // Authentication routes
        app.use('/api/cards', cardRoutes);
        app.use('/api/library/books', bookInventoryRoutes);  // Quản lý sách trong thư viện
        app.use('/api/books', bookRoutes);                   // Mượn/trả sách
        app.use('/api/transactions', transactionRoutes);
        app.use('/api/pin', pinRoutes);

        // Health check endpoint
        app.get('/api/health', (req, res) => {
            const now = new Date();
            res.json({
                status: 'OK',
                message: 'Library Card Server is running',
                database: 'MySQL',
                timestamp: now.toISOString(), // UTC
                localTime: now.toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }), // Local time (VN)
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone // Current timezone
            });
        });

        // Error handling middleware
        app.use((err, req, res, next) => {
            console.error(err.stack);
            res.status(500).json({
                success: false,
                message: 'Internal Server Error',
                error: process.env.NODE_ENV === 'development' ? err.message : {}
            });
        });

        // 404 handler
        app.use((req, res) => {
            res.status(404).json({
                success: false,
                message: 'Route not found'
            });
        });

        // Start server
        app.listen(PORT, () => {
            console.log(`Server is running on http://localhost:${PORT}`);
            console.log(`Environment: ${process.env.NODE_ENV}`);
            console.log(`Database: MySQL (${process.env.DB_HOST}:${process.env.DB_PORT}/${process.env.DB_NAME})`);
        });

    } catch (error) {
        console.error('Failed to initialize server:', error);
        process.exit(1);
    }
};

// Start the server
initializeServer();

module.exports = app;
