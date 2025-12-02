package pages;

import constants.AppConstants;
import models.CardInfo;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Thông Tin Bạn Đọc Page - UI cải tiến
 * [UPDATED] Hiển thị thông tin từ thẻ sinh viên
 */
public class CardInfoPage extends JPanel {
    
    private SimulatorService simulatorService;
    private CardInfo cardInfo;
    
    // Editable fields
    private JTextField txtHolderName;
    private JTextField txtBirthDate;
    private JTextField txtAddress;
    
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
        row1.add(createInfoField("Mã Sinh Viên:", studentId, false));
        row1.add(createEditableField("Họ Tên:", holderName, "holderName"));
        
        // Row 2
        JPanel row2 = new JPanel(new GridLayout(1, 2, 40, 0));
        row2.setBackground(Color.WHITE);
        row2.setMaximumSize(new Dimension(550, 65));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(createEditableField("Ngày Sinh:", birthDate, "birthDate"));
        row2.add(createInfoField("Email:", email, false));
        
        // Row 3
        JPanel row3 = new JPanel(new GridLayout(1, 2, 40, 0));
        row3.setBackground(Color.WHITE);
        row3.setMaximumSize(new Dimension(550, 65));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        row3.add(createInfoField("Khoa/Viện:", department, false));
        row3.add(createInfoField("Sách đang mượn:", String.valueOf(borrowedBooks) + " cuốn", false));
        
        // Row 4 - Address full width
        JPanel row4 = createEditableField("Địa Chỉ:", address, "address");
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
    
    private JPanel createInfoField(String label, String value, boolean editable) {
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
    
    private JPanel createEditableField(String label, String value, String fieldType) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComp.setForeground(AppConstants.TEXT_SECONDARY);
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField textField = new JTextField(value.equals("N/A") ? "" : value);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textField.setForeground(AppConstants.TEXT_PRIMARY);
        textField.setBackground(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
            new EmptyBorder(6, 10, 6, 10)
        ));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Store reference to text fields
        switch (fieldType) {
            case "holderName":
                txtHolderName = textField;
                break;
            case "birthDate":
                txtBirthDate = textField;
                // Add auto-format for date
                txtBirthDate.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        SwingUtilities.invokeLater(() -> formatBirthDate());
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        SwingUtilities.invokeLater(() -> formatBirthDate());
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        SwingUtilities.invokeLater(() -> formatBirthDate());
                    }
                });
                // Add validation on focus lost
                txtBirthDate.addFocusListener(new java.awt.event.FocusAdapter() {
                    public void focusLost(java.awt.event.FocusEvent e) {
                        validateBirthDate();
                    }
                });
                break;
            case "address":
                txtAddress = textField;
                break;
        }
        
        panel.add(labelComp);
        panel.add(textField);
        
        return panel;
    }
    
    /**
     * Tự động format ngày sinh khi người dùng nhập (DD/MM/YYYY)
     */
    private void formatBirthDate() {
        if (txtBirthDate == null) return;
        
        String text = txtBirthDate.getText();
        if (text.isEmpty()) return;
        
        // Lấy vị trí cursor hiện tại
        int caretPosition = txtBirthDate.getCaretPosition();
        
        // Loại bỏ tất cả ký tự không phải số
        String digitsOnly = text.replaceAll("[^0-9]", "");
        
        // Giới hạn tối đa 8 chữ số (DDMMYYYY)
        if (digitsOnly.length() > 8) {
            digitsOnly = digitsOnly.substring(0, 8);
        }
        
        // Format: DD/MM/YYYY
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i == 2 || i == 4) {
                formatted.append("/");
            }
            formatted.append(digitsOnly.charAt(i));
        }
        
        // Chỉ cập nhật nếu khác với giá trị hiện tại
        String newText = formatted.toString();
        if (!newText.equals(text)) {
            txtBirthDate.setText(newText);
            // Khôi phục vị trí cursor
            int newCaretPos = Math.min(caretPosition + (newText.length() - text.length()), newText.length());
            if (newCaretPos >= 0 && newCaretPos <= newText.length()) {
                txtBirthDate.setCaretPosition(newCaretPos);
            }
        }
    }
    
    /**
     * Validate định dạng ngày sinh (DD/MM/YYYY)
     */
    private boolean validateBirthDate() {
        if (txtBirthDate == null) return true;
        
        String birthDate = txtBirthDate.getText().trim();
        if (birthDate.isEmpty()) {
            txtBirthDate.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
                new EmptyBorder(6, 10, 6, 10)
            ));
            return true;
        }
        
        // Kiểm tra định dạng DD/MM/YYYY
        Pattern datePattern = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
        if (!datePattern.matcher(birthDate).matches()) {
            txtBirthDate.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(Color.RED, 2, 6),
                new EmptyBorder(6, 10, 6, 10)
            ));
            txtBirthDate.setToolTipText("Định dạng không hợp lệ! Vui lòng nhập theo định dạng DD/MM/YYYY");
            return false;
        }
        
        // Kiểm tra tính hợp lệ của ngày
        try {
            String[] parts = birthDate.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            
            // Kiểm tra phạm vi hợp lệ
            if (day < 1 || day > 31 || month < 1 || month > 12 || year < 1900 || year > LocalDate.now().getYear()) {
                throw new IllegalArgumentException("Ngày không hợp lệ");
            }
            
            // Kiểm tra ngày có tồn tại không
            LocalDate date = LocalDate.of(year, month, day);
            
            // Kiểm tra ngày không được trong tương lai
            if (date.isAfter(LocalDate.now())) {
                txtBirthDate.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(Color.RED, 2, 6),
                    new EmptyBorder(6, 10, 6, 10)
                ));
                txtBirthDate.setToolTipText("Ngày sinh không được là ngày trong tương lai!");
                return false;
            }
            
            // Ngày hợp lệ - khôi phục border mặc định
            txtBirthDate.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
                new EmptyBorder(6, 10, 6, 10)
            ));
            txtBirthDate.setToolTipText(null);
            return true;
            
        } catch (Exception e) {
            txtBirthDate.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(Color.RED, 2, 6),
                new EmptyBorder(6, 10, 6, 10)
            ));
            txtBirthDate.setToolTipText("Ngày không hợp lệ! Vui lòng kiểm tra lại.");
            return false;
        }
    }
    
    private JPanel createFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppConstants.BORDER_COLOR),
            new EmptyBorder(20, 0, 0, 0)
        ));
        
        // Info text
        JLabel infoText = new JLabel("Bạn có thể chỉnh sửa Họ tên, Ngày sinh và Địa chỉ.");
        infoText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoText.setForeground(AppConstants.TEXT_SECONDARY);
        
        // Save button
        JButton saveBtn = new JButton("Lưu Thay Đổi") {
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
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(AppConstants.SUCCESS_COLOR);
        saveBtn.setPreferredSize(new Dimension(140, 40));
        saveBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setContentAreaFilled(false);
        saveBtn.setOpaque(false);
        saveBtn.addActionListener(e -> handleSaveChanges());
        
        Color original = AppConstants.SUCCESS_COLOR;
        saveBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                saveBtn.setBackground(original.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                saveBtn.setBackground(original);
            }
        });
        
        panel.add(infoText, BorderLayout.WEST);
        panel.add(saveBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private void handleSaveChanges() {
        if (cardInfo == null) {
            JOptionPane.showMessageDialog(this,
                "Không tìm thấy thông tin thẻ!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate ngày sinh
        if (txtBirthDate != null && !txtBirthDate.getText().trim().isEmpty()) {
            if (!validateBirthDate()) {
                JOptionPane.showMessageDialog(this,
                    "Ngày sinh không hợp lệ! Vui lòng kiểm tra lại.",
                    "Lỗi định dạng", JOptionPane.WARNING_MESSAGE);
                txtBirthDate.requestFocus();
                return;
            }
        }
        
        // Get values from text fields
        String holderName = txtHolderName != null ? txtHolderName.getText().trim() : cardInfo.getHolderName();
        String birthDate = txtBirthDate != null ? txtBirthDate.getText().trim() : cardInfo.getBirthDate();
        String address = txtAddress != null ? txtAddress.getText().trim() : cardInfo.getAddress();
        
        // Validate required fields
        if (holderName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Họ tên không được để trống!",
                "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            if (txtHolderName != null) txtHolderName.requestFocus();
            return;
        }
        
        // Update card info
        cardInfo.setHolderName(holderName);
        if (!birthDate.isEmpty()) {
            cardInfo.setBirthDate(birthDate);
        }
        if (!address.isEmpty()) {
            cardInfo.setAddress(address);
        }
        
        // Save to service
        try {
            String studentCode = simulatorService.getCurrentStudentCode();
            if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                boolean saved = simulatorService.setCardInfo(cardInfo);
                if (saved) {
                    JOptionPane.showMessageDialog(this,
                        "Đã lưu thông tin thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Không thể lưu thông tin vào thẻ!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Update in-memory only
                simulatorService.addCardToList(cardInfo);
                JOptionPane.showMessageDialog(this,
                    "Đã cập nhật thông tin (chế độ demo)!\n" +
                    "Lưu ý: Để lưu vào thẻ thật, vui lòng xác thực PIN trước.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Lỗi khi lưu thông tin: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
