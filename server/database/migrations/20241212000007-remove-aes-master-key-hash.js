'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    // Remove aes_master_key_hash column if it exists
    // This field is not used in the application
    await queryInterface.removeColumn('cards', 'aes_master_key_hash');
  },

  async down(queryInterface, Sequelize) {
    // Re-add the column if needed (rollback)
    await queryInterface.addColumn('cards', 'aes_master_key_hash', {
      type: Sequelize.STRING(255),
      allowNull: true,
      comment: 'Hash of AES master key for this card'
    });
  }
};

