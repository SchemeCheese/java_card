'use strict';

/** @type {import('sequelize-cli').Migration} */
module.exports = {
  async up(queryInterface, Sequelize) {
    await queryInterface.addColumn('borrowed_books', 'fine_paid', {
      type: Sequelize.BOOLEAN,
      allowNull: false,
      defaultValue: false
    });

    await queryInterface.addColumn('borrowed_books', 'fine_paid_at', {
      type: Sequelize.DATE,
      allowNull: true
    });

    await queryInterface.addIndex('borrowed_books', ['student_id', 'fine_paid'], {
      name: 'idx_borrowed_student_fine_paid'
    });
  },

  async down(queryInterface) {
    await queryInterface.removeIndex('borrowed_books', 'idx_borrowed_student_fine_paid');
    await queryInterface.removeColumn('borrowed_books', 'fine_paid_at');
    await queryInterface.removeColumn('borrowed_books', 'fine_paid');
  }
};
