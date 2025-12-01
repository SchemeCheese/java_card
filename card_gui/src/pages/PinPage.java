package pages;

import constants.AppConstants;
import service.SimulatorService; // Import Service
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * PIN & Bảo Mật Page - UI cải tiến
 * [UPDATED] Logic xử lý trạng thái Locked/Verified chặt chẽ hơn
 */
public class PinPage extends JPanel {

    // Colors
    private static final Color GOLD_COLOR = new Color(234, 179, 8);
    private static final Color LIGHT_GRAY_BG = new Color(249, 250, 251);

    private SimulatorService simulatorService;
    private JLabel statusLabel;
    private JLabel triesLabel;

    // Các component cần enable/disable
    private JTextField studentCodeField;
    private JPasswordField pinField;
    private JButton verifyBtn;
    private JButton logoutBtn;
    private JButton changeBtn;
    private JPasswordField oldPinField;
    private JPasswordField newPinField;
    private JPasswordField confirmPinField;

    // Constructor nhận SimulatorService
    public PinPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;

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
        mainCard.setPreferredSize(new Dimension(950, 800));
        mainCard.setMinimumSize(new Dimension(800, 750));

        // Header
        mainCard.add(createHeader());
        mainCard.add(Box.createVerticalStrut(25));

        // Verify PIN Section
        mainCard.add(createVerifySection());
        mainCard.add(Box.createVerticalStrut(25));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(229, 231, 235));
        mainCard.add(sep);
        mainCard.add(Box.createVerticalStrut(25));

        // Change PIN Section
        mainCard.add(createChangeSection());
        mainCard.add(Box.createVerticalStrut(20));

        wrapper.add(mainCard);
        add(wrapper, BorderLayout.CENTER);

        // Kiểm tra trạng thái ban đầu ngay khi load trang
        checkInitialStatus();
    }

    // Giữ nguyên constructor mặc định
    public PinPage() {
        this(null);
    }

    private void checkInitialStatus() {
        if (simulatorService == null) return;

        // 1. Kiểm tra nếu thẻ bị khóa
        if (simulatorService.getPinTriesRemaining() == 0) {
            lockInterfaceState();
            return;
        }

        // 2. Kiểm tra nếu đã xác thực
        if (simulatorService.isPinVerified()) {
            setVerifiedState();
        } else {
            setUnverifiedState();
        }
    }

    // Trạng thái: Thẻ bị khóa -> Disable tất cả
    private void lockInterfaceState() {
        statusLabel.setText("ĐÃ BỊ KHÓA");
        statusLabel.setForeground(AppConstants.DANGER_COLOR);
        triesLabel.setText("0/3");
        triesLabel.setForeground(AppConstants.DANGER_COLOR);

        // Disable verify section
        studentCodeField.setEnabled(false);
        pinField.setEnabled(false);
        pinField.setText("");
        pinField.setBackground(new Color(254, 226, 226)); // Đỏ nhạt
        verifyBtn.setEnabled(false);
        verifyBtn.setBackground(Color.GRAY);
        logoutBtn.setVisible(false);

        // Disable change section
        disableChangeSection();

        JOptionPane.showMessageDialog(this,
                "Thẻ của bạn đã bị khóa do nhập sai quá số lần quy định.\nVui lòng liên hệ Admin để mở khóa hoặc khởi động lại ứng dụng.",
                "CẢNH BÁO", JOptionPane.ERROR_MESSAGE);
    }

    // Trạng thái: Đã xác thực thành công
    private void setVerifiedState() {
        statusLabel.setText("Đã xác thực");
        statusLabel.setForeground(AppConstants.SUCCESS_COLOR);
        triesLabel.setText("3/3"); // Reset visual counter

        // Disable input verify vì không cần nữa
        studentCodeField.setEnabled(false);
        pinField.setEnabled(false);
        pinField.setText("");
        pinField.setBackground(new Color(240, 253, 244)); // Xanh nhạt
        verifyBtn.setEnabled(false);
        verifyBtn.setText("Đã Xác Thực");
        verifyBtn.setBackground(AppConstants.SUCCESS_COLOR);
        
        // Hiện nút đăng xuất
        logoutBtn.setVisible(true);

        // Enable change PIN section
        enableChangeSection();
    }

    // Trạng thái: Chưa xác thực (Trạng thái bình thường)
    private void setUnverifiedState() {
        statusLabel.setText("Chưa xác thực");
        statusLabel.setForeground(Color.GRAY);

        if (simulatorService != null) {
            int tries = simulatorService.getPinTriesRemaining();
            triesLabel.setText(tries + "/3");
            if (tries < 3) triesLabel.setForeground(AppConstants.DANGER_COLOR);
        }

        // Enable verify section
        studentCodeField.setEnabled(true);
        pinField.setEnabled(true);
        pinField.setText("");
        pinField.setBackground(Color.WHITE);
        verifyBtn.setEnabled(true);
        verifyBtn.setText("Xác Thực");
        verifyBtn.setBackground(AppConstants.PRIMARY_COLOR);
        
        // Ẩn nút đăng xuất
        logoutBtn.setVisible(false);

        // Disable change section (phải verify trước mới được đổi)
        disableChangeSection();
    }

    private void enableChangeSection() {
        oldPinField.setEnabled(true);
        newPinField.setEnabled(true);
        confirmPinField.setEnabled(true);
        changeBtn.setEnabled(true);
        changeBtn.setBackground(AppConstants.SUCCESS_COLOR);
    }

    private void disableChangeSection() {
        oldPinField.setEnabled(false);
        newPinField.setEnabled(false);
        confirmPinField.setEnabled(false);
        changeBtn.setEnabled(false);
        changeBtn.setBackground(Color.GRAY);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Title row with icon and logout button
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Left side - icon and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(Color.WHITE);

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

        leftPanel.add(iconPanel);
        leftPanel.add(title);

        // Right side - logout button
        logoutBtn = createPrimaryButton("Đăng Xuất", AppConstants.DANGER_COLOR, 120, 36);
        logoutBtn.setVisible(false); // Ẩn khi chưa đăng nhập
        logoutBtn.addActionListener(e -> handleLogout());

        titleRow.add(leftPanel, BorderLayout.WEST);
        titleRow.add(logoutBtn, BorderLayout.EAST);

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

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (simulatorService != null) {
                simulatorService.setPinVerified(false);
                simulatorService.setCurrentStudentCode("");
                simulatorService.setCurrentRole("normal");
            }
            // Reset giao diện
            setUnverifiedState();
            studentCodeField.setText("Nhập MSSV của bạn");
            studentCodeField.setForeground(Color.GRAY);
            studentCodeField.setEnabled(true);
            logoutBtn.setVisible(false);
            clearPinFields();
            
            JOptionPane.showMessageDialog(this,
                    "Đã đăng xuất thành công!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearPinFields() {
        oldPinField.setText("");
        newPinField.setText("");
        confirmPinField.setText("");
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

        // Student Code field
        JLabel studentCodeLabel = new JLabel("Mã số sinh viên (MSSV)");
        studentCodeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentCodeLabel.setForeground(AppConstants.TEXT_SECONDARY);
        studentCodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(studentCodeLabel);
        leftPanel.add(Box.createVerticalStrut(8));

        studentCodeField = createTextField("Nhập MSSV của bạn", 380);
        leftPanel.add(studentCodeField);
        leftPanel.add(Box.createVerticalStrut(15));

        // PIN field
        JLabel inputLabel = new JLabel("Nhập mã PIN của bạn (mặc định: 000000)");
        inputLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputLabel.setForeground(AppConstants.TEXT_SECONDARY);
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(inputLabel);
        leftPanel.add(Box.createVerticalStrut(8));

        pinField = createPasswordField("Nhập 6 ký tự số", 380);
        leftPanel.add(pinField);
        leftPanel.add(Box.createVerticalStrut(18));

        verifyBtn = createPrimaryButton("Xác Thực", AppConstants.PRIMARY_COLOR, 380, 46);

        // [UPDATED] Xử lý sự kiện click nút Xác Thực
        verifyBtn.addActionListener((ActionEvent e) -> {
            if (simulatorService == null) return;

            // Kiểm tra MSSV
            String studentCode = studentCodeField.getText().trim();
            if (studentCode.isEmpty() || studentCode.equals("Nhập MSSV của bạn")) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã số sinh viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            char[] pin = pinField.getPassword();
            // Nếu người dùng chưa nhập gì
            if (pin.length == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập PIN!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Kiểm tra role trước
            boolean isAdmin = applet.AppletConstants.ADMIN_STUDENT_CODE.equalsIgnoreCase(studentCode);
            String pinStr = new String(pin);
            
            // Nếu không phải Admin
            if (!isAdmin) {
                // Kiểm tra thẻ có tồn tại không
                if (!simulatorService.isCardExists(studentCode)) {
                    JOptionPane.showMessageDialog(this, 
                            "Mã số sinh viên không tồn tại trong hệ thống!\nVui lòng liên hệ Admin để được cấp thẻ.", 
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Kiểm tra thẻ có bị khóa không
                models.CardInfo card = simulatorService.getCardByStudentCode(studentCode);
                if (card != null && "Khóa".equals(card.getStatus())) {
                    JOptionPane.showMessageDialog(this, 
                            "Thẻ của bạn đã bị khóa!\nVui lòng liên hệ Admin để được hỗ trợ.", 
                            "Thẻ bị khóa", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Xác thực PIN của sinh viên
                boolean success = simulatorService.verifyStudentPin(studentCode, pinStr);
                if (success) {
                    simulatorService.setCurrentStudentCode(studentCode);
                    simulatorService.setCurrentRole("normal");
                    simulatorService.setPinVerified(true);
                    setVerifiedState();
                    
                    // Hiện thông báo khuyến khích đổi PIN nếu đang dùng PIN mặc định
                    if ("000000".equals(pinStr)) {
                        JOptionPane.showMessageDialog(this, 
                                "Xác thực thành công!\n\n" +
                                "Lưu ý: Bạn đang sử dụng mã PIN mặc định.\n" +
                                "Vui lòng đổi mã PIN để bảo mật tài khoản.", 
                                "Thông báo", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                                "Xác thực thành công!", 
                                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "PIN không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Admin - sử dụng PIN của simulator
                try {
                    boolean success = simulatorService.verifyPin(pin);
                    if (success) {
                        simulatorService.setCurrentStudentCode(studentCode);
                        simulatorService.setCurrentRole("Admin");
                        setVerifiedState();
                        JOptionPane.showMessageDialog(this, 
                                "Xác thực Admin thành công!", 
                                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        int tries = simulatorService.getPinTriesRemaining();
                        JOptionPane.showMessageDialog(this, 
                                "PIN sai! Số lần thử còn lại: " + tries, 
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

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

        // Khởi tạo label status
        statusLabel = new JLabel("Chưa xác thực");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(AppConstants.TEXT_SECONDARY);

        triesLabel = new JLabel("3/3");
        triesLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        triesLabel.setForeground(AppConstants.TEXT_PRIMARY);

        JPanel row1 = createStatusRow("Trạng thái:", statusLabel);
        JPanel row2 = createStatusRow("Số lần còn lại:", triesLabel);

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

        // Tạo các field và gán vào biến class level
        oldPinField = createPasswordField("Nhập PIN hiện tại của bạn", 380);
        newPinField = createPasswordField("6-8 ký tự số", 380);
        confirmPinField = createPasswordField("Nhập lại PIN mới", 380);

        fieldsPanel.add(createLabeledFieldObj("PIN hiện tại", oldPinField));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledFieldObj("PIN mới", newPinField));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledFieldObj("Xác nhận PIN mới", confirmPinField));

        // Button panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        btnPanel.add(Box.createVerticalStrut(28)); // Align with first input

        changeBtn = createPrimaryButton("Đổi PIN", AppConstants.SUCCESS_COLOR, 180, 46);

        // Xử lý sự kiện Đổi PIN
        changeBtn.addActionListener((ActionEvent e) -> {
            if (simulatorService == null) return;

            String oldPinStr = new String(oldPinField.getPassword());
            String newPinStr = new String(newPinField.getPassword());
            String confirmStr = new String(confirmPinField.getPassword());

            if (oldPinStr.isEmpty() || newPinStr.isEmpty() || confirmStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPinStr.equals(confirmStr)) {
                JOptionPane.showMessageDialog(this, "PIN mới và xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newPinStr.length() < 6) {
                JOptionPane.showMessageDialog(this, "PIN mới phải có ít nhất 6 ký tự!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String currentStudentCode = simulatorService.getCurrentStudentCode();
            boolean isAdmin = "Admin".equals(simulatorService.getCurrentRole());

            if (isAdmin) {
                // Admin - đổi PIN của simulator
                try {
                    boolean verified = simulatorService.verifyPin(oldPinStr.toCharArray());
                    if (!verified) {
                        JOptionPane.showMessageDialog(this, "PIN cũ không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    boolean success = simulatorService.changePin(oldPinStr.toCharArray(), newPinStr.toCharArray());
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Đổi PIN Admin thành công!\nVui lòng đăng nhập lại.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        clearPinFields();
                        setUnverifiedState();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Sinh viên - đổi PIN riêng của sinh viên
                boolean success = simulatorService.changeStudentPin(currentStudentCode, oldPinStr, newPinStr);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Đổi PIN thành công!\nVui lòng đăng nhập lại với PIN mới.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    clearPinFields();
                    setUnverifiedState();
                } else {
                    JOptionPane.showMessageDialog(this, "PIN cũ không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnPanel.add(changeBtn);

        contentRow.add(fieldsPanel);
        contentRow.add(Box.createHorizontalStrut(35));
        contentRow.add(btnPanel);
        contentRow.add(Box.createHorizontalGlue());

        panel.add(contentRow);

        return panel;
    }

    private JPanel createStatusRow(String label, Component valueComp) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(LIGHT_GRAY_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelComp.setForeground(AppConstants.TEXT_SECONDARY);

        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);

        return row;
    }

    // Helper method mới để tái sử dụng field object
    private JPanel createLabeledFieldObj(String label, JPasswordField field) {
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

        panel.add(field);

        return panel;
    }

    private JTextField createTextField(String placeholder, int width) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

    private JPasswordField createPasswordField(String placeholder, int width) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setEchoChar('\u2022'); // Luôn ẩn mật khẩu
        field.setForeground(AppConstants.TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
                new EmptyBorder(12, 14, 12, 14)
        ));
        field.setPreferredSize(new Dimension(width, 46));
        field.setMaximumSize(new Dimension(width, 46));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Xóa nội dung khi focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                field.selectAll(); // Chọn tất cả để dễ xóa
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
                if (btn.isEnabled()) btn.setBackground(original.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(original);
            }
        });

        return btn;
    }
}