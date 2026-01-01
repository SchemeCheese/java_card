'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    // Check if column exists first
    const tableDescription = await queryInterface.describeTable('cards');
    
    if (!tableDescription.encrypted_aes_key) {
      // Add encrypted_aes_key field to cards table only if it doesn't exist
      await queryInterface.addColumn('cards', 'encrypted_aes_key', {
        type: Sequelize.TEXT,
        allowNull: true,
        comment: 'Encrypted AES key for this card (encrypted by card RSA public key, Base64 encoded)'
      });
      console.log('Added encrypted_aes_key column to cards table');
    } else {
      console.log('encrypted_aes_key column already exists, skipping');
    }
  },

  async down(queryInterface, Sequelize) {
    // Remove encrypted_aes_key column
    await queryInterface.removeColumn('cards', 'encrypted_aes_key');
  }
};
