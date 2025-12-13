'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    await queryInterface.createTable('cards', {
      id: {
        type: Sequelize.INTEGER,
        primaryKey: true,
        autoIncrement: true,
        allowNull: false
      },
      student_id: {
        type: Sequelize.STRING(20),
        allowNull: false,
        unique: true
      },
      holder_name: {
        type: Sequelize.STRING(100),
        allowNull: false
      },
      email: {
        type: Sequelize.STRING(100),
        allowNull: false
      },
      department: {
        type: Sequelize.STRING(100),
        allowNull: false
      },
      birth_date: {
        type: Sequelize.STRING(10),
        allowNull: false
      },
      address: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      status: {
        type: Sequelize.ENUM('Hoạt động', 'Khóa', 'Tạm khóa'),
        defaultValue: 'Hoạt động'
      },
      borrowed_books: {
        type: Sequelize.INTEGER,
        defaultValue: 0
      },
      pin_hash: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      pin_salt: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      pin_tries: {
        type: Sequelize.INTEGER,
        defaultValue: 3
      },
      balance: {
        type: Sequelize.BIGINT,
        defaultValue: 0
      },
      image_path: {
        type: Sequelize.STRING(255),
        defaultValue: ''
      },
      created_at: {
        type: Sequelize.DATE,
        allowNull: false,
        defaultValue: Sequelize.literal('CURRENT_TIMESTAMP')
      },
      updated_at: {
        type: Sequelize.DATE,
        allowNull: false,
        defaultValue: Sequelize.literal('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
      }
    });

    // Add indexes
    await queryInterface.addIndex('cards', ['student_id'], {
      name: 'idx_student_id',
      unique: true
    });
    await queryInterface.addIndex('cards', ['email'], {
      name: 'idx_email'
    });
  },

  async down(queryInterface, Sequelize) {
    await queryInterface.dropTable('cards');
  }
};

