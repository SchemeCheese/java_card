package pages;

import constants.AppConstants;
import models.CardInfo;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Thông Tin Bạn Đọc Page - UI cải tiến
 * [UPDATED] Hiển thị thông tin từ thẻ sinh viên
 */
public class CardInfoPage extends JPanel {
    
    private SimulatorService simulatorService;
    private CardInfo cardInfo;
    
    public CardInfoPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        
        // Lấy thông tin thẻ của sinh viên hiện tại
        String currentStudentCode = simulatorService.getCurrentStudentCode();
        this.cardInfo = simulatorService.getCardByStudentCode(currentStudentCode);
        
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
        
        JLabel subtitle = new JLabel("Thông tin chi tiết của bạn đọc.");
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
        String holderName = cardInfo != null ? cardInfo.getHolderName() : "";
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
                
                // Vẽ chữ cái đầu tên
                g2.setColor(AppConstants.PRIMARY_COLOR);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 60));
                String initial = "";
                if (holderName.contains(" ")) {
                    String[] parts = holderName.split(" ");
                    initial = parts[parts.length - 1].substring(0, 1).toUpperCase();
                } else if (!holderName.isEmpty()) {
                    initial = holderName.substring(0, 1).toUpperCase();
                }
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(initial)) / 2;
                int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                
                g2.dispose();
            }
        };
        avatarBox.setPreferredSize(new Dimension(160, 170));
        avatarBox.setMaximumSize(new Dimension(160, 170));
        avatarBox.setBorder(BorderFactory.createLineBorder(AppConstants.SUCCESS_COLOR, 3));
        avatarBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // ID
        String studentId = cardInfo != null ? cardInfo.getStudentId() : "N/A";
        JLabel idLabel = new JLabel("MSSV: " + studentId);
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        idLabel.setForeground(AppConstants.TEXT_PRIMARY);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        idLabel.setBorder(new EmptyBorder(15, 0, 5, 0));
        
        // Status
        String status = cardInfo != null ? cardInfo.getStatus() : "N/A";
        JLabel statusLabel = new JLabel("Trạng thái: " + status);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground("Hoạt động".equals(status) ? AppConstants.SUCCESS_COLOR : AppConstants.DANGER_COLOR);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Balance
        long balance = cardInfo != null ? cardInfo.getBalance() : 0;
        JLabel balanceLabel = new JLabel("Số dư: " + String.format("%,d VND", balance));
        balanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        balanceLabel.setForeground(AppConstants.TEXT_SECONDARY);
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        balanceLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        panel.add(avatarBox);
        panel.add(idLabel);
        panel.add(statusLabel);
        panel.add(balanceLabel);
        
        return panel;
    }
    
    private JPanel createInfoSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        String studentId = cardInfo != null ? cardInfo.getStudentId() : "N/A";
        String holderName = cardInfo != null ? cardInfo.getHolderName() : "N/A";
        String birthDate = cardInfo != null && !cardInfo.getBirthDate().isEmpty() ? cardInfo.getBirthDate() : "N/A";
        String email = cardInfo != null && !cardInfo.getEmail().isEmpty() ? cardInfo.getEmail() : "N/A";
        String department = cardInfo != null && !cardInfo.getDepartment().isEmpty() ? cardInfo.getDepartment() : "N/A";
        String address = cardInfo != null && !cardInfo.getAddress().isEmpty() ? cardInfo.getAddress() : "N/A";
        int borrowedBooks = cardInfo != null ? cardInfo.getBorrowedBooks() : 0;
        
        // Row 1
        JPanel row1 = new JPanel(new GridLayout(1, 2, 40, 0));
        row1.setBackground(Color.WHITE);
        row1.setMaximumSize(new Dimension(550, 65));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(createInfoField("Mã Sinh Viên:", studentId));
        row1.add(createInfoField("Họ Tên:", holderName));
        
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
        row3.add(createInfoField("Khoa/Viện:", department));
        row3.add(createInfoField("Sách đang mượn:", String.valueOf(borrowedBooks) + " cuốn"));
        
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
        
        // Info text
        JLabel infoText = new JLabel("Để cập nhật thông tin, vui lòng liên hệ Admin.");
        infoText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoText.setForeground(AppConstants.TEXT_SECONDARY);
        
        panel.add(infoText, BorderLayout.WEST);
        
        return panel;
    }
}
