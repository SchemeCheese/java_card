'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    await queryInterface.createTable('transactions', {
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
      type: {
        type: Sequelize.ENUM('Nạp tiền', 'Trả phạt', 'Rút tiền', 'Thanh toán dịch vụ'),
        allowNull: false
      },
      amount: {
        type: Sequelize.BIGINT,
        allowNull: false
      },
      balance_before: {
        type: Sequelize.BIGINT,
        allowNull: false
      },
      balance_after: {
        type: Sequelize.BIGINT,
        allowNull: false
      },
      status: {
        type: Sequelize.ENUM('Thành công', 'Thất bại', 'Đang xử lý'),
        defaultValue: 'Thành công'
      },
      description: {
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
    await queryInterface.addIndex('transactions', ['student_id'], {
      name: 'idx_trans_student_id'
    });
    await queryInterface.addIndex('transactions', ['created_at'], {
      name: 'idx_trans_created_at'
    });
    await queryInterface.addIndex('transactions', ['type'], {
      name: 'idx_trans_type'
    });
  },

  async down(queryInterface, Sequelize) {
    await queryInterface.dropTable('transactions');
  }
};


