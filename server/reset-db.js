const mysql = require('mysql2/promise');
require('dotenv').config();

async function resetDatabase() {
  const connection = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3307,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || ''
  });

  try {
    console.log('🔄 Dropping database...');
    await connection.execute(`DROP DATABASE IF EXISTS ${process.env.DB_NAME || 'library_card_db'}`);
    
    console.log('✅ Database dropped');
    console.log('🔄 Creating database...');
    await connection.execute(`CREATE DATABASE ${process.env.DB_NAME || 'library_card_db'} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`);
    
    console.log('✅ Database created successfully!');
  } catch (error) {
    console.error('❌ Error:', error.message);
  } finally {
    await connection.end();
  }
}

resetDatabase();
