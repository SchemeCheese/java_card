package pages;

import constants.AppConstants;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * PIN & Bảo Mật Page - UI cải tiến
 */
public class PinPage extends JPanel {
    
    // Colors
    private static final Color GOLD_COLOR = new Color(234, 179, 8);
    private static final Color LIGHT_GRAY_BG = new Color(249, 250, 251);
    
    public PinPage() {
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        
        // Center wrapper
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppConstants.BACKGROUND);
        
        // Main card - flexible size
        JPanel mainCard = new JPanel();
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));
        mainCard.setBackground(Color.WHITE);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 16),
            new EmptyBorder(40, 50, 40, 50)
        ));
        mainCard.setPreferredSize(new Dimension(950, 700));
        mainCard.setMinimumSize(new Dimension(800, 600));
        
        // Header
        mainCard.add(createHeader());
        mainCard.add(Box.createVerticalStrut(35));
        
        // Verify PIN Section
        mainCard.add(createVerifySection());
        mainCard.add(Box.createVerticalStrut(35));
        
        // Separator
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(229, 231, 235));
        mainCard.add(sep);
        mainCard.add(Box.createVerticalStrut(35));
        
        // Change PIN Section
        mainCard.add(createChangeSection());
        
        wrapper.add(mainCard);
        add(wrapper, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Title row with icon
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Icon panel (lock symbol)
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GOLD_COLOR);
                // Lock body
                g2.fillRoundRect(6, 14, 20, 16, 4, 4);
                // Lock shackle
                g2.setStroke(new BasicStroke(3));
                g2.drawArc(8, 4, 16, 14, 0, 180);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(Color.WHITE);
        
        JLabel title = new JLabel("PIN & Bảo Mật");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 12, 0, 0));
        
        titleRow.add(iconPanel);
        titleRow.add(title);
        
        // Subtitle
        JLabel subtitle = new JLabel("Quản lý mã PIN và các cài đặt bảo mật cho thẻ của bạn.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        panel.add(titleRow);
        panel.add(subtitle);
        
        return panel;
    }
    
    private JPanel createVerifySection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Section title
        JLabel sectionTitle = new JLabel("Xác Thực PIN");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(20));
        
        // Content row - input on left, status on right
        JPanel contentRow = new JPanel();
        contentRow.setLayout(new BoxLayout(contentRow, BoxLayout.X_AXIS));
        contentRow.setBackground(Color.WHITE);
        contentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Left - Input and button
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        JLabel inputLabel = new JLabel("Nhập mã PIN của bạn");
        inputLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputLabel.setForeground(AppConstants.TEXT_SECONDARY);
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(inputLabel);
        leftPanel.add(Box.createVerticalStrut(8));
        
        JPasswordField pinField = createPasswordField("Nhập 6-8 ký tự số", 380);
        leftPanel.add(pinField);
        leftPanel.add(Box.createVerticalStrut(18));
        
        JButton verifyBtn = createPrimaryButton("Xác Thực", AppConstants.PRIMARY_COLOR, 380, 46);
        leftPanel.add(verifyBtn);
        
        // Right - Status card
        JPanel statusCard = new JPanel();
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));
        statusCard.setBackground(LIGHT_GRAY_BG);
        statusCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 10),
            new EmptyBorder(18, 22, 18, 22)
        ));
        statusCard.setAlignmentY(Component.TOP_ALIGNMENT);
        statusCard.setPreferredSize(new Dimension(260, 95));
        statusCard.setMaximumSize(new Dimension(260, 95));
        
        JPanel row1 = createStatusRow("Trạng thái thẻ:", "Active", AppConstants.SUCCESS_COLOR);
        JPanel row2 = createStatusRow("Số lần nhập sai còn lại:", "3/3", AppConstants.TEXT_PRIMARY);
        
        statusCard.add(row1);
        statusCard.add(Box.createVerticalStrut(12));
        statusCard.add(row2);
        
        contentRow.add(leftPanel);
        contentRow.add(Box.createHorizontalStrut(35));
        contentRow.add(statusCard);
        contentRow.add(Box.createHorizontalGlue());
        
        panel.add(contentRow);
        
        return panel;
    }
    
    private JPanel createChangeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Section title
        JLabel sectionTitle = new JLabel("Đổi PIN");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(20));
        
        // Content
        JPanel contentRow = new JPanel();
        contentRow.setLayout(new BoxLayout(contentRow, BoxLayout.X_AXIS));
        contentRow.setBackground(Color.WHITE);
        contentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Fields panel
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        fieldsPanel.add(createLabeledField("PIN hiện tại", "Nhập PIN hiện tại của bạn"));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledField("PIN mới", "6-8 ký tự số"));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledField("Xác nhận PIN mới", "Nhập lại PIN mới"));
        
        // Button panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        btnPanel.add(Box.createVerticalStrut(28)); // Align with first input
        
        JButton changeBtn = createPrimaryButton("Đổi PIN", AppConstants.SUCCESS_COLOR, 180, 46);
        btnPanel.add(changeBtn);
        
        contentRow.add(fieldsPanel);
        contentRow.add(Box.createHorizontalStrut(35));
        contentRow.add(btnPanel);
        contentRow.add(Box.createHorizontalGlue());
        
        panel.add(contentRow);
        
        return panel;
    }
    
    private JPanel createStatusRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(LIGHT_GRAY_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComp.setForeground(AppConstants.TEXT_SECONDARY);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueComp.setForeground(valueColor);
        
        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);
        
        return row;
    }
    
    private JPanel createLabeledField(String label, String placeholder) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelComp.setForeground(AppConstants.TEXT_SECONDARY);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelComp);
        panel.add(Box.createVerticalStrut(8));
        
        JPasswordField field = createPasswordField(placeholder, 380);
        panel.add(field);
        
        return panel;
    }
    
    private JPasswordField createPasswordField(String placeholder, int width) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
            new EmptyBorder(12, 14, 12, 14)
        ));
        field.setPreferredSize(new Dimension(width, 46));
        field.setMaximumSize(new Dimension(width, 46));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('\u2022');
                    field.setForeground(AppConstants.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
        
        return field;
    }
    
    private JButton createPrimaryButton(String text, Color bgColor, int width, int height) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setMaximumSize(new Dimension(width, height));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
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

