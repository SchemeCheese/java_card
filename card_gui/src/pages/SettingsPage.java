package pages;

import constants.AppConstants;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Settings Page - Quản trị hệ thống - UI cải tiến
 */
public class SettingsPage extends JPanel {
    
    public SettingsPage() {
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(AppConstants.BACKGROUND);
        
        // Header
        mainPanel.add(createHeader());
        mainPanel.add(Box.createVerticalStrut(25));
        
        // Top row - Create Card + Card Management
        mainPanel.add(createTopRow());
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Activity Log section
        mainPanel.add(createActivityLogSection());
        
        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scroll, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(AppConstants.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        // Gear icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.TEXT_SECONDARY);
                // Draw gear shape
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(8, 8, 16, 16);
                g2.fillRect(14, 4, 4, 6);
                g2.fillRect(14, 22, 4, 6);
                g2.fillRect(4, 14, 6, 4);
                g2.fillRect(22, 14, 6, 4);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(AppConstants.BACKGROUND);
        
        JLabel title = new JLabel("Quản Trị Hệ Thống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 12, 0, 0));
        
        panel.add(iconPanel);
        panel.add(title);
        
        // Wrap with subtitle
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(AppConstants.BACKGROUND);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitle = new JLabel("Quản lý thẻ thư viện cho sinh viên và nhân viên.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setBorder(new EmptyBorder(8, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        wrapper.add(panel);
        wrapper.add(subtitle);
        
        return wrapper;
    }
    
    private JPanel createTopRow() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(AppConstants.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create new card - fixed width, taller height
        JPanel leftCard = createNewCardSection();
        leftCard.setPreferredSize(new Dimension(420, 580));
        leftCard.setMinimumSize(new Dimension(400, 560));
        leftCard.setMaximumSize(new Dimension(450, 600));
        
        // Card Management - flexible width, same height
        JPanel rightCard = createCardManagementSection();
        rightCard.setPreferredSize(new Dimension(500, 580));
        rightCard.setMinimumSize(new Dimension(450, 560));
        rightCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        
        panel.add(leftCard);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(rightCard);
        
        return panel;
    }
    
    private JPanel createNewCardSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(20, 24, 20, 24)
        ));
        
        JLabel title = new JLabel("Tạo Thẻ Mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel desc = new JLabel("Nhập thông tin để tạo thẻ thư viện mới");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(AppConstants.TEXT_SECONDARY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(new EmptyBorder(4, 0, 12, 0));
        
        // Form fields - compact layout
        String[][] fields = {
            {"Mã Số Sinh Viên", "Nhập MSSV"},
            {"Họ và Tên", "Nhập họ tên sinh viên"},
            {"Email", "email@example.com"},
            {"Khoa / Viện", "Nhập khoa / viện"},
            {"Ngày Sinh", "DD/MM/YYYY"},
            {"Địa Chỉ", "Nhập địa chỉ"}
        };
        
        panel.add(title);
        panel.add(desc);
        
        for (String[] f : fields) {
            JLabel lbl = new JLabel(f[0]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setForeground(AppConstants.TEXT_SECONDARY);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(new EmptyBorder(4, 0, 3, 0));
            
            JTextField txt = createTextField(f[1]);
            txt.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
            txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            txt.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            panel.add(lbl);
            panel.add(txt);
        }
        
        panel.add(Box.createVerticalStrut(12));
        
        // Submit btn
        JButton submitBtn = createButton("Tạo Thẻ Mới", AppConstants.SUCCESS_COLOR, Color.WHITE, Integer.MAX_VALUE, 40);
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        panel.add(submitBtn);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createCardManagementSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(20, 24, 20, 24)
        ));
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        
        JLabel title = new JLabel("Quản Lý Thẻ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        
        JTextField searchField = createTextField("Tìm theo MSSV hoặc Tên...");
        searchField.setPreferredSize(new Dimension(220, 36));
        
        header.add(title, BorderLayout.WEST);
        header.add(searchField, BorderLayout.EAST);
        
        panel.add(header);
        panel.add(Box.createVerticalStrut(12));
        
        // Card list
        String[][] cards = {
            {"22520001", "Nguyen Van A", "CNTT", "Hoạt động"},
            {"22520002", "Tran Thi B", "Dien Tu", "Hoạt động"},
            {"22520003", "Le Van C", "Co Khi", "Khóa"},
            {"22520004", "Pham Thi D", "Kinh Te", "Hoạt động"},
            {"22520005", "Hoang Van E", "Xay Dung", "Khóa"}
        };
        
        for (String[] card : cards) {
            panel.add(createCardItem(card[0], card[1], card[2], card[3]));
            panel.add(Box.createVerticalStrut(8));
        }
        
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createCardItem(String mssv, String name, String dept, String status) {
        // Dùng GridBagLayout để kiểm soát tốt hơn
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(249, 250, 251));
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 10),
            new EmptyBorder(10, 12, 10, 12)
        ));
        panel.setPreferredSize(new Dimension(400, 70));
        panel.setMinimumSize(new Dimension(350, 70));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Avatar - vẽ trong JLabel để đảm bảo kích thước
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Vẽ nền tròn
                g2.setColor(AppConstants.PRIMARY_COLOR);
                g2.fillOval(0, 0, 40, 40);
                // Vẽ chữ cái đầu
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String initial = "";
                if (name.contains(" ")) {
                    initial = name.substring(name.lastIndexOf(" ") + 1, name.lastIndexOf(" ") + 2).toUpperCase();
                } else if (name.length() > 0) {
                    initial = name.substring(0, 1).toUpperCase();
                }
                FontMetrics fm = g2.getFontMetrics();
                int x = (40 - fm.stringWidth(initial)) / 2;
                int y = ((40 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(40, 40));
        avatar.setMinimumSize(new Dimension(40, 40));
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(avatar, gbc);
        
        // Name label
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(AppConstants.TEXT_PRIMARY);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 2, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        panel.add(nameLabel, gbc);
        
        // Detail label
        JLabel detailLabel = new JLabel("MSSV: " + mssv + " | " + dept);
        detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailLabel.setForeground(AppConstants.TEXT_SECONDARY);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(detailLabel, gbc);
        
        // Status badge
        JLabel statusBadge = new JLabel(status);
        statusBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBadge.setOpaque(true);
        statusBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        if (status.equals("Hoạt động")) {
            statusBadge.setBackground(new Color(220, 252, 231));
            statusBadge.setForeground(new Color(22, 101, 52));
        } else {
            statusBadge.setBackground(new Color(254, 226, 226));
            statusBadge.setForeground(new Color(153, 27, 27));
        }
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 8, 0, 8);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(statusBadge, gbc);
        
        // Toggle button
        String btnText = status.equals("Hoạt động") ? "Khóa Thẻ" : "Mở Khóa";
        Color btnBg = status.equals("Hoạt động") ? AppConstants.DANGER_COLOR : AppConstants.SUCCESS_COLOR;
        JButton toggleBtn = createButton(btnText, btnBg, Color.WHITE, 90, 30);
        
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(toggleBtn, gbc);
        
        return panel;
    }
    
    private JPanel createActivityLogSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(25, 30, 25, 30)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        panel.setMinimumSize(new Dimension(700, 260));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel title = new JLabel("Nhật Ký Hoạt Động");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        
        JButton refreshBtn = new JButton("Làm Mới");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.setForeground(AppConstants.TEXT_PRIMARY);
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
            new EmptyBorder(8, 16, 8, 16)
        ));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        
        // Table
        String[] cols = {"Thời Gian", "Hành Động", "MSSV", "Trạng Thái"};
        Object[][] data = {
            {"15:30:25 - 01/01/2025", "Tạo thẻ mới", "22520006", "Thành công"},
            {"14:22:10 - 01/01/2025", "Khóa thẻ", "22520003", "Thành công"},
            {"13:15:45 - 01/01/2025", "Đổi mã PIN", "22520001", "Thành công"},
            {"12:05:30 - 01/01/2025", "Nạp tiền (500,000 VND)", "22520002", "Thành công"},
            {"11:30:00 - 01/01/2025", "Mở khóa thẻ", "22520005", "Thất bại"}
        };
        
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (Object[] row : data) model.addRow(row);
        
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(45);
        table.setShowGrid(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(AppConstants.TEXT_SECONDARY);
        table.getTableHeader().setBackground(Color.WHITE);
        
        // Status renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, 
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = new JLabel(val.toString(), SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setOpaque(true);
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                
                if (val.toString().equals("Thành công")) {
                    lbl.setBackground(new Color(220, 252, 231));
                    lbl.setForeground(new Color(22, 101, 52));
                } else {
                    lbl.setBackground(new Color(254, 226, 226));
                    lbl.setForeground(new Color(153, 27, 27));
                }
                return lbl;
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
            new EmptyBorder(6, 10, 6, 10)
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
    
    private JButton createButton(String text, Color bgColor, Color fgColor, int w, int h) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fgColor);
        btn.setBackground(bgColor);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setMinimumSize(new Dimension(w, h));
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        
        Color orig = bgColor;
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(orig.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(orig); }
        });
        
        return btn;
    }
}
