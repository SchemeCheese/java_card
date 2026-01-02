'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    await queryInterface.createTable('books', {
      id: {
        type: Sequelize.INTEGER,
        primaryKey: true,
        autoIncrement: true,
        allowNull: false
      },
      book_id: {
        type: Sequelize.STRING(50),
        allowNull: false,
        unique: true
      },
      title: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      author: {
        type: Sequelize.STRING(255),
        allowNull: false
      },
      isbn: {
        type: Sequelize.STRING(20),
        allowNull: true,
        unique: true
      },
      publisher: {
        type: Sequelize.STRING(255),
        allowNull: true
      },
      publish_year: {
        type: Sequelize.INTEGER,
        allowNull: true
      },
      category: {
        type: Sequelize.STRING(100),
        allowNull: true
      },
      description: {
        type: Sequelize.TEXT,
        allowNull: true
      },
      total_copies: {
        type: Sequelize.INTEGER,
        allowNull: false,
        defaultValue: 1
      },
      available_copies: {
        type: Sequelize.INTEGER,
        allowNull: false,
        defaultValue: 1
      },
      status: {
        type: Sequelize.ENUM('Có sẵn', 'Hết sách', 'Ngừng cho mượn'),
        defaultValue: 'Có sẵn'
      },
      location: {
        type: Sequelize.STRING(100),
        allowNull: true,
        comment: 'Vị trí sách trong thư viện'
      },
      cover_image: {
        type: Sequelize.STRING(255),
        allowNull: true
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

    // Add indexes (skip book_id as it already has unique constraint)
    await queryInterface.addIndex('books', ['title'], {
      name: 'idx_title'
    });
    await queryInterface.addIndex('books', ['author'], {
      name: 'idx_author'
    });
    await queryInterface.addIndex('books', ['category'], {
      name: 'idx_category'
    });
    await queryInterface.addIndex('books', ['status'], {
      name: 'idx_status'
    });
  },

  async down(queryInterface, Sequelize) {
    await queryInterface.dropTable('books');
  }
};

