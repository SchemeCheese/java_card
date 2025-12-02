package pages;

import constants.AppConstants;
import models.BorrowedBook;
import models.CardInfo;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mượn / Trả Sách Page - Chức năng hoàn chỉnh
 */
public class BorrowedBooksPage extends JPanel {
    
    private SimulatorService simulatorService;
    private List<BorrowedBook> borrowedBooks;
    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel messagePanel;
    private JLabel messageLabel;
    private JTextField borrowField;
    private JTextField returnField;
    private JPanel rightPanel;
    
    // Mock data cho thư viện sách
    private static final Map<String, String> BOOK_CATALOG = new HashMap<>();
    static {
        BOOK_CATALOG.put("NV001", "Nhà Giả Kim");
        BOOK_CATALOG.put("DB002", "Đắc Nhân Tâm");
        BOOK_CATALOG.put("TH003", "Trên Đường Băng");
        BOOK_CATALOG.put("CX004", "Cà Phê Cùng Tony");
        BOOK_CATALOG.put("HP001", "Harry Potter 1");
        BOOK_CATALOG.put("HP002", "Harry Potter 2");
        BOOK_CATALOG.put("TT001", "Tuổi Trẻ Đáng Giá Bao Nhiêu");
        BOOK_CATALOG.put("NG001", "Người Giàu Có Nhất Thành Babylon");
        BOOK_CATALOG.put("DL001", "Đời Ngắn Đừng Ngủ Dài");
        BOOK_CATALOG.put("CS001", "Clean Code");
    }
    
    public BorrowedBooksPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.borrowedBooks = new ArrayList<>();
        
        // Load borrowed books for current student
        loadBorrowedBooks();
        
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        
        JPanel mainPanel = new JPanel(new BorderLayout(25, 0));
        mainPanel.setBackground(AppConstants.BACKGROUND);
        
        JPanel leftPanel = createLeftPanel();
        rightPanel = createRightPanel();
        rightPanel.setPreferredSize(new Dimension(340, 0));
        rightPanel.setMinimumSize(new Dimension(300, 400));
        rightPanel.setMaximumSize(new Dimension(340, Integer.MAX_VALUE));
        
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void loadBorrowedBooks() {
        String studentCode = simulatorService.getCurrentStudentCode();
        CardInfo card = simulatorService.getCardByStudentCode(studentCode);
        if (card != null) {
            // Get books from service or use demo data
            List<BorrowedBook> books = simulatorService.getBorrowedBooks(studentCode);
            if (books != null) {
                borrowedBooks.addAll(books);
            }
        }
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 16),
            new EmptyBorder(30, 35, 30, 35)
        ));
        
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setBackground(Color.WHITE);
        
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(239, 68, 68));
                g2.fillRoundRect(4, 6, 24, 6, 2, 2);
                g2.setColor(new Color(34, 197, 94));
                g2.fillRoundRect(4, 14, 24, 6, 2, 2);
                g2.setColor(AppConstants.PRIMARY_COLOR);
                g2.fillRoundRect(4, 22, 24, 6, 2, 2);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(Color.WHITE);
        
        JLabel title = new JLabel("Mượn / Trả Sách");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 12, 0, 0));
        
        titleRow.add(iconPanel);
        titleRow.add(title);
        
        JLabel subtitle = new JLabel("Quản lý việc mượn và trả sách. Đang mượn: " + borrowedBooks.size() + " cuốn");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 0, 25, 0));
        
        JLabel sectionTitle = new JLabel("Danh Sách Sách Đang Mượn");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setBackground(Color.WHITE);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        searchRow.setBorder(new EmptyBorder(15, 0, 20, 0));
        
        JButton refreshBtn = createButton("Làm Mới", AppConstants.PRIMARY_COLOR, Color.WHITE, 110, 42);
        refreshBtn.addActionListener(e -> refreshTable());
        
        searchRow.add(refreshBtn, BorderLayout.EAST);
        
        headerSection.add(titleRow);
        headerSection.add(subtitle);
        headerSection.add(sectionTitle);
        headerSection.add(searchRow);
        
        panel.add(headerSection, BorderLayout.NORTH);
        panel.add(createTablePanel(), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        String[] columns = {"MÃ SÁCH", "TÊN SÁCH", "NGÀY MƯỢN", "HẠN TRẢ", "TRẠNG THÁI", "NGÀY TRỄ"};
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        refreshTableData();
        
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(52);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(239, 246, 255));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setForeground(AppConstants.TEXT_SECONDARY);
        header.setBackground(new Color(249, 250, 251));
        header.setPreferredSize(new Dimension(0, 45));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppConstants.BORDER_COLOR));
        
        // Status column renderer
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value.toString(), SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(4, 10, 4, 10));
                
                String status = value.toString();
                switch (status) {
                    case "Đang mượn":
                        label.setBackground(new Color(220, 252, 231));
                        label.setForeground(new Color(22, 101, 52));
                        break;
                    case "Quá hạn":
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(153, 27, 27));
                        break;
                    case "Sắp hết hạn":
                        label.setBackground(new Color(254, 243, 199));
                        label.setForeground(new Color(133, 77, 14));
                        break;
                    default:
                        label.setBackground(Color.WHITE);
                        label.setForeground(AppConstants.TEXT_PRIMARY);
                }
                return label;
            }
        });
        
        // Overdue days column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                int days = Integer.parseInt(value.toString());
                label.setForeground(days > 0 ? new Color(220, 38, 38) : AppConstants.TEXT_PRIMARY);
                label.setText(days > 0 ? days + " ngày" : "-");
                return label;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshTableData() {
        // Disable table updates temporarily to prevent flickering
        tableModel.setRowCount(0);
        
        // Add rows in batch
        Object[][] rowData = new Object[borrowedBooks.size()][6];
        for (int i = 0; i < borrowedBooks.size(); i++) {
            BorrowedBook book = borrowedBooks.get(i);
            rowData[i] = new Object[]{
                book.getBookId(),
                book.getBookName(),
                book.getBorrowDate(),
                book.getDueDate(),
                book.getStatus(),
                book.getOverdueDays()
            };
        }
        
        // Add all rows at once
        for (Object[] row : rowData) {
            tableModel.addRow(row);
        }
        
        // Force table to update layout only once - only revalidate table, not parent panels
        if (table != null) {
            table.revalidate();
            table.repaint();
        }
    }
    
    private void refreshTable() {
        // Refresh table data without affecting layout
        refreshTableData();
        showMessage("Đã làm mới danh sách!", true);
        
        // Ensure right panel maintains its size - no need to revalidate
        // The size constraints are already set in createRightPanel()
        if (rightPanel != null) {
            SwingUtilities.invokeLater(() -> {
                // Just ensure size constraints are maintained, no revalidate needed
                rightPanel.setPreferredSize(new Dimension(340, 0));
                rightPanel.setMaximumSize(new Dimension(340, Integer.MAX_VALUE));
            });
        }
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppConstants.BACKGROUND);
        // Set fixed preferred size to prevent layout shifts
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setMinimumSize(new Dimension(300, 400));
        panel.setMaximumSize(new Dimension(340, Integer.MAX_VALUE));
        
        panel.add(createBorrowSection());
        panel.add(Box.createVerticalStrut(20));
        panel.add(createReturnSection());
        panel.add(Box.createVerticalStrut(20));
        panel.add(createMessagePanel());
        panel.add(Box.createVerticalStrut(20));
        panel.add(createBookCatalogPanel());
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createBorrowSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(22, 22, 22, 22)
        ));
        
        JLabel title = new JLabel("Mượn Sách Mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Mã Sách");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(15, 0, 8, 0));
        
        borrowField = createTextField("VD: NV001, HP001...");
        borrowField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        borrowField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton borrowBtn = createButton("(+) Mượn Sách", AppConstants.SUCCESS_COLOR, Color.WHITE, Integer.MAX_VALUE, 44);
        borrowBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        borrowBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        borrowBtn.addActionListener(e -> {
            borrowBtn.setEnabled(false);
            try {
                handleBorrowBook();
            } finally {
                SwingUtilities.invokeLater(() -> borrowBtn.setEnabled(true));
            }
        });
        
        panel.add(title);
        panel.add(label);
        panel.add(borrowField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(borrowBtn);
        
        return panel;
    }
    
    private JPanel createReturnSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(22, 22, 22, 22)
        ));
        
        JLabel title = new JLabel("Trả Sách");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Mã Sách");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(15, 0, 8, 0));
        
        returnField = createTextField("Nhập mã sách cần trả...");
        returnField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        returnField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton returnBtn = createButton("Trả Sách", AppConstants.DANGER_COLOR, Color.WHITE, Integer.MAX_VALUE, 44);
        returnBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        returnBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        returnBtn.addActionListener(e -> {
            returnBtn.setEnabled(false);
            try {
                handleReturnBook();
            } finally {
                SwingUtilities.invokeLater(() -> returnBtn.setEnabled(true));
            }
        });
        
        panel.add(title);
        panel.add(label);
        panel.add(returnField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(returnBtn);
        
        return panel;
    }
    
    private JPanel createMessagePanel() {
        messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        messagePanel.setBackground(new Color(240, 253, 244));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(187, 247, 208), 1, 8),
            new EmptyBorder(5, 12, 5, 12)
        ));
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        messagePanel.setVisible(false);
        
        messageLabel = new JLabel("");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setForeground(new Color(22, 101, 52));
        
        messagePanel.add(messageLabel);
        
        return messagePanel;
    }
    
    private JPanel createBookCatalogPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel title = new JLabel("Mã sách có sẵn:");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        
        for (Map.Entry<String, String> entry : BOOK_CATALOG.entrySet()) {
            JLabel bookLabel = new JLabel(entry.getKey() + " - " + entry.getValue());
            bookLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bookLabel.setForeground(AppConstants.TEXT_SECONDARY);
            bookLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(bookLabel);
        }
        
        return panel;
    }
    
    private void handleBorrowBook() {
        String bookId = borrowField.getText().trim().toUpperCase();
        if (bookId.isEmpty() || bookId.equals("VD: NV001, HP001...")) {
            showMessage("Vui lòng nhập mã sách!", false);
            return;
        }
        
        // Check if book exists in catalog
        if (!BOOK_CATALOG.containsKey(bookId)) {
            showMessage("Mã sách không tồn tại!", false);
            return;
        }
        
        // Check if already borrowed
        for (BorrowedBook book : borrowedBooks) {
            if (book.getBookId().equals(bookId)) {
                showMessage("Sách này đã được mượn!", false);
                return;
            }
        }
        
        // Check max books limit
        if (borrowedBooks.size() >= 5) {
            showMessage("Đã đạt giới hạn 5 cuốn sách!", false);
            return;
        }
        
        // Create new borrowed book
        String bookName = BOOK_CATALOG.get(bookId);
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(14); // 2 weeks loan
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        BorrowedBook newBook = new BorrowedBook(
            bookId, bookName,
            today.format(formatter),
            dueDate.format(formatter),
            "Đang mượn", 0
        );
        
        borrowedBooks.add(newBook);
        
        // Update card info
        String studentCode = simulatorService.getCurrentStudentCode();
        simulatorService.addBorrowedBook(studentCode, newBook);
        
        // Update UI on EDT to prevent flickering
        SwingUtilities.invokeLater(() -> {
            refreshTableData();
            borrowField.setText("VD: NV001, HP001...");
            borrowField.setForeground(Color.GRAY);
            showMessage("Mượn sách \"" + bookName + "\" thành công!", true);
        });
    }
    
    private void handleReturnBook() {
        String bookId = returnField.getText().trim().toUpperCase();
        if (bookId.isEmpty() || bookId.equals("Nhập mã sách cần trả...")) {
            showMessage("Vui lòng nhập mã sách!", false);
            return;
        }
        
        // Check for late fee và thanh toán qua SimulatorService
        String studentCode = simulatorService.getCurrentStudentCode();
        BorrowedBook foundBook = null;
        for (BorrowedBook book : borrowedBooks) {
            if (book.getBookId().equals(bookId)) {
                foundBook = book;
                break;
            }
        }
        if (foundBook == null) {
            showMessage("Bạn không mượn sách này!", false);
            return;
        }

        int overdueDays = foundBook.getOverdueDays();
        if (overdueDays > 0) {
            int fee = overdueDays * 5000; // 5000 VND per day
            int confirm = JOptionPane.showConfirmDialog(this,
                "Sách trả trễ " + overdueDays + " ngày.\n" +
                "Phí phạt: " + String.format("%,d VND", fee) + "\n\n" +
                "Bạn có muốn trả sách và thanh toán phí phạt?",
                "Phí phạt trả trễ",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            boolean paid = simulatorService.payFine(studentCode, fee);
            if (!paid) {
                showMessage("Số dư không đủ để thanh toán phí phạt!", false);
                return;
            }
        }

        String bookName = foundBook.getBookName();
        borrowedBooks.remove(foundBook);

        // Update service
        simulatorService.removeBorrowedBook(studentCode, bookId);

        // Update UI on EDT to prevent flickering
        SwingUtilities.invokeLater(() -> {
            refreshTableData();
            returnField.setText("Nhập mã sách cần trả...");
            returnField.setForeground(Color.GRAY);
            showMessage("Trả sách \"" + bookName + "\" thành công!", true);
        });
    }
    
    private void showMessage(String text, boolean isSuccess) {
        // Update message on EDT to prevent flickering
        SwingUtilities.invokeLater(() -> {
            messageLabel.setText(text);
            if (isSuccess) {
                messagePanel.setBackground(new Color(240, 253, 244));
                messagePanel.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(new Color(187, 247, 208), 1, 8),
                    new EmptyBorder(5, 12, 5, 12)
                ));
                messageLabel.setForeground(new Color(22, 101, 52));
            } else {
                messagePanel.setBackground(new Color(254, 242, 242));
                messagePanel.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(new Color(254, 202, 202), 1, 8),
                    new EmptyBorder(5, 12, 5, 12)
                ));
                messageLabel.setForeground(new Color(153, 27, 27));
            }
            
            // Revalidate and repaint only if visibility changes
            // Only revalidate messagePanel itself, not parent panels
            boolean wasVisible = messagePanel.isVisible();
            messagePanel.setVisible(true);
            
            if (!wasVisible) {
                // Only repaint messagePanel, avoid revalidate to prevent layout shift
                messagePanel.repaint();
            }
            
            // Auto hide after 3 seconds
            Timer timer = new Timer(3000, e -> {
                SwingUtilities.invokeLater(() -> {
                    messagePanel.setVisible(false);
                    // Only repaint, don't revalidate to prevent layout shift
                    messagePanel.repaint();
                });
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
            new EmptyBorder(10, 14, 10, 14)
        ));
        
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(AppConstants.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
        
        return field;
    }
    
    private JButton createButton(String text, Color bgColor, Color fgColor, int width, int height) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(fgColor);
        btn.setBackground(bgColor);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setMaximumSize(new Dimension(width, height));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Color original = bgColor;
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(original.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(original);
            }
        });
        
        return btn;
    }
}
