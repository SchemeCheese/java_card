const { Sequelize } = require('sequelize');
require('dotenv').config();

// Khởi tạo Sequelize connection
// Set timezone: 'Asia/Ho_Chi_Minh' cho Việt Nam hoặc '+07:00'
// Nếu muốn dùng UTC, set timezone: '+00:00'
const sequelize = new Sequelize(
    process.env.DB_NAME || 'library_card_db',
    process.env.DB_USER || 'root',
    process.env.DB_PASSWORD || '',
    {
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 3306,
        dialect: 'mysql',
        timezone: process.env.DB_TIMEZONE || '+07:00', // Asia/Ho_Chi_Minh (UTC+7)
        logging: process.env.NODE_ENV === 'development' ? console.log : false,
        pool: {
            max: 5,
            min: 0,
            acquire: 30000,
            idle: 10000
        },
        define: {
            timestamps: true,
            underscored: false,
            freezeTableName: false
        },
        dialectOptions: {
            // Set timezone cho MySQL connection
            timezone: 'local', // Sử dụng timezone của MySQL server
            // Hoặc có thể set cụ thể: timezone: '+07:00'
            dateStrings: false,
            typeCast: true
        }
    }
);

// Test connection và set timezone
const testConnection = async () => {
    try {
        await sequelize.authenticate();
        console.log('MySQL connection has been established successfully.');
        
        // Set timezone cho MySQL session (Asia/Ho_Chi_Minh = UTC+7)
        // Có thể dùng: SET time_zone = '+07:00' hoặc SET time_zone = 'Asia/Ho_Chi_Minh'
        try {
            await sequelize.query("SET time_zone = '+07:00'");
            console.log('MySQL timezone set to Asia/Ho_Chi_Minh (UTC+7)');
        } catch (tzError) {
            console.warn('Warning: Could not set MySQL timezone:', tzError.message);
            console.warn('Timestamps will use MySQL server default timezone');
        }
        
        return true;
    } catch (error) {
        console.error('Unable to connect to MySQL database:', error.message);
        return false;
    }
};

module.exports = { sequelize, testConnection };


