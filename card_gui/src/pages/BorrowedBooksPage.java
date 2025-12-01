package pages;

import constants.AppConstants;
import models.BorrowedBook;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Mượn / Trả Sách Page - UI cải tiến
 */
public class BorrowedBooksPage extends JPanel {
    
    private List<BorrowedBook> borrowedBooks;
    
    public BorrowedBooksPage(List<BorrowedBook> borrowedBooks) {
        this.borrowedBooks = borrowedBooks;
        
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        
        // Main content with 2 columns
        JPanel mainPanel = new JPanel(new BorderLayout(25, 0));
        mainPanel.setBackground(AppConstants.BACKGROUND);
        
        // Left - Header + Book list
        JPanel leftPanel = createLeftPanel();
        
        // Right - Forms
        JPanel rightPanel = createRightPanel();
        rightPanel.setPreferredSize(new Dimension(340, 0));
        rightPanel.setMinimumSize(new Dimension(300, 400));
        
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 16),
            new EmptyBorder(30, 35, 30, 35)
        ));
        
        // Header section
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setBackground(Color.WHITE);
        
        // Title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Book icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Stack of books
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
        
        JLabel subtitle = new JLabel("Quản lý việc mượn và trả sách của thành viên một cách hiệu quả.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 0, 25, 0));
        
        // Section title
        JLabel sectionTitle = new JLabel("Danh Sách Sách Đang Mượn");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Search row
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setBackground(Color.WHITE);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        searchRow.setBorder(new EmptyBorder(15, 0, 20, 0));
        
        JTextField searchField = createTextField("Tìm kiếm theo Mã sách, Tên sách...");
        
        JButton refreshBtn = createButton("Làm Mới", AppConstants.PRIMARY_COLOR, Color.WHITE, 110, 42);
        
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(refreshBtn, BorderLayout.EAST);
        
        headerSection.add(titleRow);
        headerSection.add(subtitle);
        headerSection.add(sectionTitle);
        headerSection.add(searchRow);
        
        panel.add(headerSection, BorderLayout.NORTH);
        
        // Table
        JPanel tablePanel = createTablePanel();
        panel.add(tablePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        String[] columns = {"MÃ SÁCH", "TÊN SÁCH", "NGÀY MƯỢN", "HẠN TRẢ", "TRẠNG THÁI", "SỐ NGÀY TRỄ"};
        
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        for (BorrowedBook book : borrowedBooks) {
            model.addRow(new Object[]{
                book.getBookId(),
                book.getBookName(),
                book.getBorrowDate(),
                book.getDueDate(),
                book.getStatus(),
                book.getOverdueDays()
            });
        }
        
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(52);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(239, 246, 255));
        
        // Header styling
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
                return label;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppConstants.BORDER_COLOR));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppConstants.BACKGROUND);
        
        // Borrow section
        panel.add(createBorrowSection());
        panel.add(Box.createVerticalStrut(20));
        
        // Return section
        panel.add(createReturnSection());
        panel.add(Box.createVerticalStrut(20));
        
        // Success message
        panel.add(createSuccessMessage());
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
        panel.setMinimumSize(new Dimension(280, 180));
        
        JLabel title = new JLabel("Mượn Sách Mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Mã Sách / Quét Mã Vạch");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(15, 0, 8, 0));
        
        JTextField field = createTextField("Nhập hoặc quét mã sách...");
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton borrowBtn = createButton("(+) Mượn Sách", AppConstants.SUCCESS_COLOR, Color.WHITE, Integer.MAX_VALUE, 44);
        borrowBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        borrowBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        
        panel.add(title);
        panel.add(label);
        panel.add(field);
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
        panel.setMinimumSize(new Dimension(280, 180));
        
        JLabel title = new JLabel("Trả Sách");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel("Mã Sách / Quét Mã Vạch");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(AppConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(15, 0, 8, 0));
        
        JTextField field = createTextField("Nhập hoặc quét mã sách...");
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton returnBtn = createButton("Trả Sách", AppConstants.DANGER_COLOR, Color.WHITE, Integer.MAX_VALUE, 44);
        returnBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        returnBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        
        panel.add(title);
        panel.add(label);
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
        panel.add(returnBtn);
        
        return panel;
    }
    
    private JPanel createSuccessMessage() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(new Color(240, 253, 244));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(187, 247, 208), 1, 8),
            new EmptyBorder(5, 12, 5, 12)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        
        // Check icon
        JPanel checkIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.SUCCESS_COLOR);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(4, 10, 8, 14);
                g2.drawLine(8, 14, 16, 6);
                g2.dispose();
            }
        };
        checkIcon.setPreferredSize(new Dimension(20, 20));
        checkIcon.setBackground(new Color(240, 253, 244));
        
        JLabel msg = new JLabel("Mượn sách thành công!");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg.setForeground(new Color(22, 101, 52));
        
        panel.add(checkIcon);
        panel.add(msg);
        
        return panel;
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
