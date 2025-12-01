package pages;

import constants.AppConstants;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Thông Tin Bạn Đọc Page - UI cải tiến
 */
public class CardInfoPage extends JPanel {
    
    private String studentId, studentName, birthDate, email, phone, major, address;
    
    public CardInfoPage(String studentId, String studentName, String birthDate, 
                        String email, String phone, String major, String address) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.birthDate = birthDate;
        this.email = email;
        this.phone = phone;
        this.major = major;
        this.address = address;
        
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        
        // Center wrapper
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppConstants.BACKGROUND);
        
        // Main card
        JPanel mainCard = new JPanel(new BorderLayout());
        mainCard.setBackground(Color.WHITE);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 16),
            new EmptyBorder(40, 50, 30, 50)
        ));
        mainCard.setPreferredSize(new Dimension(1000, 620));
        mainCard.setMinimumSize(new Dimension(850, 550));
        
        // Header
        mainCard.add(createHeader(), BorderLayout.NORTH);
        
        // Content
        mainCard.add(createContent(), BorderLayout.CENTER);
        
        // Footer
        mainCard.add(createFooter(), BorderLayout.SOUTH);
        
        wrapper.add(mainCard);
        add(wrapper, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 30, 0));
        
        // Title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // User icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.PRIMARY_COLOR);
                g2.setStroke(new BasicStroke(2));
                // Head
                g2.drawOval(10, 4, 12, 12);
                // Body
                g2.drawArc(6, 16, 20, 16, 0, 180);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(Color.WHITE);
        
        JLabel title = new JLabel("Thông Tin Bạn Đọc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 12, 0, 0));
        
        titleRow.add(iconPanel);
        titleRow.add(title);
        
        JLabel subtitle = new JLabel("Quản lý và cập nhật thông tin chi tiết của bạn đọc.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        panel.add(titleRow);
        panel.add(subtitle);
        
        return panel;
    }
    
    private JPanel createContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(Color.WHITE);
        
        // Left - Avatar section
        JPanel avatarSection = createAvatarSection();
        avatarSection.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Right - Info fields
        JPanel infoSection = createInfoSection();
        infoSection.setAlignmentY(Component.TOP_ALIGNMENT);
        
        panel.add(avatarSection);
        panel.add(Box.createHorizontalStrut(50));
        panel.add(infoSection);
        panel.add(Box.createHorizontalGlue());
        
        return panel;
    }
    
    private JPanel createAvatarSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        // Avatar with green border
        JPanel avatarBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Background
                g2.setColor(new Color(243, 244, 246));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                
                // Person silhouette
                g2.setColor(new Color(156, 163, 175));
                // Head
                g2.fillOval(w/2 - 22, 35, 44, 44);
                // Body
                g2.fillRoundRect(w/2 - 35, 85, 70, 55, 25, 25);
                
                g2.dispose();
            }
        };
        avatarBox.setPreferredSize(new Dimension(160, 170));
        avatarBox.setMaximumSize(new Dimension(160, 170));
        avatarBox.setBorder(BorderFactory.createLineBorder(AppConstants.SUCCESS_COLOR, 3));
        avatarBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ID
        JLabel idLabel = new JLabel("ID Thẻ: 123456789");
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        idLabel.setForeground(AppConstants.TEXT_PRIMARY);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        idLabel.setBorder(new EmptyBorder(15, 0, 5, 0));
        
        // Expiry
        JLabel expiryLabel = new JLabel("Ngày Hết Hạn: 31/12/2024");
        expiryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expiryLabel.setForeground(AppConstants.SUCCESS_COLOR);
        expiryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Update photo button
        JButton updateBtn = new JButton("Cập Nhật Ảnh");
        updateBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        updateBtn.setForeground(AppConstants.TEXT_PRIMARY);
        updateBtn.setBackground(Color.WHITE);
        updateBtn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
            new EmptyBorder(10, 24, 10, 24)
        ));
        updateBtn.setFocusPainted(false);
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(avatarBox);
        panel.add(idLabel);
        panel.add(expiryLabel);
        panel.add(Box.createVerticalStrut(18));
        panel.add(updateBtn);
        
        return panel;
    }
    
    private JPanel createInfoSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        // Row 1
        JPanel row1 = new JPanel(new GridLayout(1, 2, 40, 0));
        row1.setBackground(Color.WHITE);
        row1.setMaximumSize(new Dimension(550, 65));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(createInfoField("Mã Sinh Viên:", studentId));
        row1.add(createInfoField("Họ Tên:", studentName));
        
        // Row 2
        JPanel row2 = new JPanel(new GridLayout(1, 2, 40, 0));
        row2.setBackground(Color.WHITE);
        row2.setMaximumSize(new Dimension(550, 65));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(createInfoField("Ngày Sinh:", birthDate));
        row2.add(createInfoField("Email:", email));
        
        // Row 3
        JPanel row3 = new JPanel(new GridLayout(1, 2, 40, 0));
        row3.setBackground(Color.WHITE);
        row3.setMaximumSize(new Dimension(550, 65));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        row3.add(createInfoField("Khoa/Lớp:", major));
        row3.add(createInfoField("Số Điện Thoại:", phone));
        
        // Row 4 - Address full width
        JPanel row4 = createInfoField("Địa Chỉ:", address);
        row4.setMaximumSize(new Dimension(550, 65));
        row4.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(row1);
        panel.add(Box.createVerticalStrut(20));
        panel.add(row2);
        panel.add(Box.createVerticalStrut(20));
        panel.add(row3);
        panel.add(Box.createVerticalStrut(20));
        panel.add(row4);
        
        return panel;
    }
    
    private JPanel createInfoField(String label, String value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComp.setForeground(AppConstants.TEXT_SECONDARY);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        valueComp.setForeground(AppConstants.TEXT_PRIMARY);
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueComp.setBorder(new EmptyBorder(6, 0, 0, 0));
        
        panel.add(labelComp);
        panel.add(valueComp);
        
        return panel;
    }
    
    private JPanel createFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppConstants.BORDER_COLOR),
            new EmptyBorder(20, 0, 0, 0)
        ));
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonsPanel.setBackground(Color.WHITE);
        
        // Cancel button
        JButton cancelBtn = new JButton("X  Hủy") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppConstants.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelBtn.setForeground(AppConstants.TEXT_PRIMARY);
        cancelBtn.setBackground(Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(110, 44));
        cancelBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setOpaque(false);
        
        // Save button
        JButton saveBtn = new JButton("Lưu Thay Đổi") {
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
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(AppConstants.PRIMARY_COLOR);
        saveBtn.setPreferredSize(new Dimension(140, 44));
        saveBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setContentAreaFilled(false);
        saveBtn.setOpaque(false);
        
        Color orig = AppConstants.PRIMARY_COLOR;
        saveBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { saveBtn.setBackground(orig.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { saveBtn.setBackground(orig); }
        });
        
        buttonsPanel.add(cancelBtn);
        buttonsPanel.add(saveBtn);
        
        panel.add(buttonsPanel, BorderLayout.EAST);
        
        return panel;
    }
}
