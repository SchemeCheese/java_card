'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    await queryInterface.createTable('borrowed_books', {
      id: {
        type: Sequelize.INTEGER,
        primaryKey: true,
        autoIncrement: true,
        allowNull: false
      },
      student_id: {
        type: Sequelize.STRING(20),
        allowNull: false,
        references: {
          model: 'cards',
          key: 'student_id'
        },
        onDelete: 'CASCADE'
      },
      book_id: {
        type: Sequelize.STRING(50),
        allowNull: false,
        references: {
          model: 'books',
          key: 'book_id'
        },
        onDelete: 'RESTRICT'
      },
      book_name: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      borrow_date: {
        type: Sequelize.DATE,
        allowNull: false,
        defaultValue: Sequelize.literal('CURRENT_TIMESTAMP')
      },
      due_date: {
        type: Sequelize.DATE,
        allowNull: false
      },
      return_date: {
        type: Sequelize.DATE,
        allowNull: true
      },
      status: {
        type: Sequelize.ENUM('Đang mượn', 'Quá hạn', 'Đã trả'),
        defaultValue: 'Đang mượn'
      },
      overdue_days: {
        type: Sequelize.INTEGER,
        defaultValue: 0
      },
      fine: {
        type: Sequelize.BIGINT,
        defaultValue: 0
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
    await queryInterface.addIndex('borrowed_books', ['student_id'], {
      name: 'idx_borrowed_student_id'
    });
    await queryInterface.addIndex('borrowed_books', ['book_id'], {
      name: 'idx_borrowed_book_id'
    });
    await queryInterface.addIndex('borrowed_books', ['status'], {
      name: 'idx_borrowed_status'
    });
  },

  async down(queryInterface, Sequelize) {
    await queryInterface.dropTable('borrowed_books');
  }
};

