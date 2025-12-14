'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    // Add RSA and AES fields to cards table
    await queryInterface.addColumn('cards', 'rsa_public_key', {
      type: Sequelize.TEXT,
      allowNull: true,
      comment: 'RSA Public Key (PEM format) for card authentication'
    });

    await queryInterface.addColumn('cards', 'rsa_key_created_at', {
      type: Sequelize.DATE,
      allowNull: true,
      comment: 'Thời gian tạo khóa RSA'
    });

    await queryInterface.addColumn('cards', 'aes_master_key_hash', {
      type: Sequelize.STRING(255),
      allowNull: true,
      comment: 'Hash of AES master key for this card'
    });

    // Add index for faster lookups (MySQL specific)
    await queryInterface.sequelize.query(
      "CREATE INDEX idx_rsa_public_key ON cards(rsa_public_key(100))"
    );
  },

  async down(queryInterface, Sequelize) {
    // Remove index
    await queryInterface.sequelize.query(
      "DROP INDEX IF EXISTS idx_rsa_public_key ON cards"
    );

    // Remove columns
    await queryInterface.removeColumn('cards', 'aes_master_key_hash');
    await queryInterface.removeColumn('cards', 'rsa_key_created_at');
    await queryInterface.removeColumn('cards', 'rsa_public_key');
  }
};

