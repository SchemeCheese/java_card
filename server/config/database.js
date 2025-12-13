const { Sequelize } = require('sequelize');
require('dotenv').config();

// Khởi tạo Sequelize connection
const sequelize = new Sequelize(
    process.env.DB_NAME || 'library_card_db',
    process.env.DB_USER || 'root',
    process.env.DB_PASSWORD || '',
    {
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 3306,
        dialect: 'mysql',
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
        }
    }
);

// Test connection
const testConnection = async () => {
    try {
        await sequelize.authenticate();
        console.log('MySQL connection has been established successfully.');
        return true;
    } catch (error) {
        console.error('Unable to connect to MySQL database:', error.message);
        return false;
    }
};

module.exports = { sequelize, testConnection };


