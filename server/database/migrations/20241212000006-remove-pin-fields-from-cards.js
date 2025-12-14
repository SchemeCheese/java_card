'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    // ⚠️ WARNING: Removing PIN fields from database
    // PIN should only be stored on the card (applet) for security
    // This migration removes pin_hash, pin_salt, and pin_tries from cards table
    
    // Remove index on pin_hash if exists
    try {
      await queryInterface.sequelize.query(
        "DROP INDEX IF EXISTS idx_pin_hash ON cards"
      );
    } catch (error) {
      // Index might not exist, ignore error
      console.log('Index idx_pin_hash does not exist, skipping...');
    }

    // Remove PIN-related columns
    await queryInterface.removeColumn('cards', 'pin_hash');
    await queryInterface.removeColumn('cards', 'pin_salt');
    await queryInterface.removeColumn('cards', 'pin_tries');
  },

  async down(queryInterface, Sequelize) {
    // Rollback: Add PIN fields back
    await queryInterface.addColumn('cards', 'pin_hash', {
      type: Sequelize.STRING(255),
      allowNull: true,  // Allow null for existing records
      comment: 'PIN hash (PBKDF2) - DEPRECATED: PIN should be stored on card only'
    });

    await queryInterface.addColumn('cards', 'pin_salt', {
      type: Sequelize.STRING(255),
      allowNull: true,
      comment: 'PIN salt for PBKDF2 - DEPRECATED: PIN should be stored on card only'
    });

    await queryInterface.addColumn('cards', 'pin_tries', {
      type: Sequelize.INTEGER,
      allowNull: true,
      defaultValue: 3,
      comment: 'PIN tries remaining - DEPRECATED: Should be managed on card'
    });
  }
};

