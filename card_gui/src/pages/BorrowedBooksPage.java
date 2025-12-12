package pages;

import constants.AppConstants;
import models.BorrowedBook;
import service.SimulatorService;
import ui.RoundedBorder;
import ui.UIComponentFactory;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Mượn / Trả Sách Page - Giao diện Split View (2 cột)
 * [UPDATED] Dùng JTable + Checkbox thay vì nhập mã thủ công
 */
public class BorrowedBooksPage extends JPanel {

    private SimulatorService simulatorService;

    // Components cho bảng Sách Đang Mượn (Bên Trái)
    private DefaultTableModel borrowedModel;
    private JTable borrowedTable;
    private JLabel borrowedCountLabel;

    // Components cho bảng Kho Sách (Bên Phải)
    private DefaultTableModel catalogModel;
    private JTable catalogTable;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> catalogSorter;

    // Danh mục sách gốc (Mock Database của thư viện để hiển thị bên phải)
    // Dữ liệu này khớp với AVAILABLE_BOOKS trong SimulatorService
    private static final String[][] LIBRARY_CATALOG = {
            {"NV001", "Nhà Giả Kim", "Paulo Coelho"},
            {"DB002", "Đắc Nhân Tâm", "Dale Carnegie"},
            {"TH003", "Trên Đường Băng", "Tony Buổi Sáng"},
            {"CX004", "Cà Phê Cùng Tony", "Tony Buổi Sáng"},
            {"HP005", "Harry Potter", "J.K. Rowling"},
            {"LT006", "Lược Sử Thời Gian", "Stephen Hawking"},
            {"TN007", "Tuổi Trẻ Đáng Giá Bao Nhiêu", "Rosie Nguyễn"},
            {"NL008", "Nhà Lãnh Đạo Không Chức Danh", "Robin Sharma"}
    };

    public BorrowedBooksPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;

        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Main Container: Grid 1 dòng 2 cột
        JPanel container = new JPanel(new GridLayout(1, 2, 20, 0));
        container.setBackground(AppConstants.BACKGROUND);

        // Panel Trái: Sách đang mượn
        container.add(createLeftPanel());

        // Panel Phải: Kho sách để mượn
        container.add(createRightPanel());

        add(container, BorderLayout.CENTER);

        // Load dữ liệu lần đầu
        refreshData();
    }

    // ==========================================
    // PHẦN 1: BÊN TRÁI - DANH SÁCH ĐANG MƯỢN
    // ==========================================
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 1. Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Sách Đang Mượn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppConstants.TEXT_PRIMARY);

        borrowedCountLabel = new JLabel("0 cuốn");
        borrowedCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        borrowedCountLabel.setForeground(AppConstants.PRIMARY_COLOR);

        header.add(title, BorderLayout.WEST);
        header.add(borrowedCountLabel, BorderLayout.EAST);

        // 2. Table
        // Cột 0: Checkbox (Boolean), Cột 1: Mã, Cột 2: Tên, Cột 3: Hạn Trả
        String[] cols = {"", "Mã", "Tên Sách", "Hạn Trả"};
        borrowedModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class; // Cột 0 là Checkbox
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Chỉ cho phép sửa cột checkbox
            }
        };

        borrowedTable = new JTable(borrowedModel);
        setupTableStyle(borrowedTable);

        // Chỉnh độ rộng cột checkbox nhỏ lại
        borrowedTable.getColumnModel().getColumn(0).setMaxWidth(40);
        borrowedTable.getColumnModel().getColumn(1).setMaxWidth(80);

        JScrollPane scroll = new JScrollPane(borrowedTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));

        // 3. Footer (Button Trả)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton returnBtn = UIComponentFactory.createDangerButton("Trả Sách Đã Chọn");
        returnBtn.addActionListener(e -> handleReturnBooks());

        footer.add(returnBtn);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // PHẦN 2: BÊN PHẢI - KHO SÁCH (CATALOG)
    // ==========================================
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // 1. Header & Search
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Thư Viện Sách");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppConstants.SUCCESS_COLOR);

        // Ô tìm kiếm
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm tên sách...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
                new EmptyBorder(5, 10, 5, 10)
        ));

        // Logic tìm kiếm realtime
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    catalogSorter.setRowFilter(null);
                } else {
                    // Tìm kiếm case-insensitive trên cột tên sách (index 2)
                    catalogSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2));
                }
            }
        });

        header.add(title, BorderLayout.NORTH);
        header.add(searchField, BorderLayout.CENTER);

        // 2. Table
        // Cột 0: Checkbox, Cột 1: Mã, Cột 2: Tên, Cột 3: Tác giả
        String[] cols = {"", "Mã", "Tên Sách", "Tác Giả"};
        catalogModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        catalogTable = new JTable(catalogModel);
        setupTableStyle(catalogTable);

        catalogTable.getColumnModel().getColumn(0).setMaxWidth(40);
        catalogTable.getColumnModel().getColumn(1).setMaxWidth(80);

        // Sorter cho tìm kiếm
        catalogSorter = new TableRowSorter<>(catalogModel);
        catalogTable.setRowSorter(catalogSorter);

        JScrollPane scroll = new JScrollPane(catalogTable);
        scroll.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));

        // 3. Footer (Button Mượn)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton borrowBtn = UIComponentFactory.createSuccessButton("Mượn Sách Đã Chọn");
        borrowBtn.addActionListener(e -> handleBorrowBooks());

        footer.add(borrowBtn);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // LOGIC XỬ LÝ & ĐỒNG BỘ DỮ LIỆU
    // ==========================================

    /**
     * Nạp lại dữ liệu cho cả 2 bảng
     * Logic:
     * - Bảng Trái: Lấy từ simulatorService.getBorrowedBooks()
     * - Bảng Phải: Lấy từ LIBRARY_CATALOG trừ đi những cuốn đã có bên Trái
     */
    private void refreshData() {
        String studentCode = simulatorService.getCurrentStudentCode();
        List<BorrowedBook> currentBorrowed = simulatorService.getBorrowedBooks(studentCode);
        List<String> borrowedIDs = new ArrayList<>();

        // 1. Fill Bảng Trái (Đang mượn)
        borrowedModel.setRowCount(0);
        if (currentBorrowed != null) {
            for (BorrowedBook b : currentBorrowed) {
                borrowedIDs.add(b.getBookId());
                borrowedModel.addRow(new Object[]{
                        false, // Checkbox chưa tick
                        b.getBookId(),
                        b.getBookName(),
                        b.getDueDate()
                });
            }
            borrowedCountLabel.setText(currentBorrowed.size() + " cuốn");
        }

        // 2. Fill Bảng Phải (Kho sách)
        catalogModel.setRowCount(0);
        for (String[] book : LIBRARY_CATALOG) {
            String id = book[0];
            // Chỉ thêm vào kho nếu chưa bị mượn
            if (!borrowedIDs.contains(id)) {
                catalogModel.addRow(new Object[]{
                        false, // Checkbox chưa tick
                        id,
                        book[1],
                        book[2]
                });
            }
        }
    }

    private void handleBorrowBooks() {
        String studentCode = simulatorService.getCurrentStudentCode();
        int count = 0;
        List<String> successBooks = new ArrayList<>();

        // Duyệt bảng Kho Sách để tìm dòng được tick
        for (int i = 0; i < catalogTable.getRowCount(); i++) {
            boolean isChecked = (Boolean) catalogTable.getValueAt(i, 0);
            if (isChecked) {
                String bookId = (String) catalogTable.getValueAt(i, 1);
                String result = simulatorService.borrowBook(studentCode, bookId);

                if (result == null) { // Thành công
                    count++;
                    successBooks.add(bookId);
                } else {
                    JOptionPane.showMessageDialog(this, result, "Lỗi mượn sách " + bookId, JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (count > 0) {
            JOptionPane.showMessageDialog(this, "Đã mượn thành công " + count + " cuốn sách!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            refreshData(); // Refresh để chuyển sách từ Phải sang Trái
        } else if (successBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sách muốn mượn bên phải!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleReturnBooks() {
        String studentCode = simulatorService.getCurrentStudentCode();
        int count = 0;
        long totalFine = 0;
        boolean hasFine = false;

        // Duyệt bảng Đang Mượn để tìm dòng được tick
        for (int i = 0; i < borrowedTable.getRowCount(); i++) {
            boolean isChecked = (Boolean) borrowedTable.getValueAt(i, 0);
            if (isChecked) {
                String bookId = (String) borrowedTable.getValueAt(i, 1);
                String result = simulatorService.returnBook(studentCode, bookId);

                if (result == null) {
                    count++;
                } else if (result.startsWith("FINE:")) {
                    hasFine = true;
                    totalFine += Long.parseLong(result.split(":")[1]);
                    count++;
                } else {
                    JOptionPane.showMessageDialog(this, result, "Lỗi trả sách", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (count > 0) {
            String msg = "Đã trả " + count + " cuốn sách.";
            if (hasFine) {
                msg += "\nBạn đã thanh toán phí phạt trễ hạn: " + String.format("%,d", totalFine) + " VND";
            }
            JOptionPane.showMessageDialog(this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            refreshData(); // Refresh để chuyển sách từ Trái về Phải
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sách muốn trả bên trái!", "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- Helper Styling Table ---
    private void setupTableStyle(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(239, 246, 255));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(249, 250, 251));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppConstants.BORDER_COLOR));
    }
}