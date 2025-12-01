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
    private JPasswordField pinField;
    private JButton verifyBtn;
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
        pinField.setEnabled(false);
        pinField.setText("Thẻ đã bị khóa!");
        verifyBtn.setEnabled(false);
        verifyBtn.setBackground(Color.GRAY);

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
        pinField.setEnabled(false);
        pinField.setText("PIN đã được xác thực");
        pinField.setBackground(new Color(240, 253, 244)); // Xanh nhạt
        verifyBtn.setEnabled(false);
        verifyBtn.setText("Đã Xác Thực");
        verifyBtn.setBackground(AppConstants.SUCCESS_COLOR);

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
        pinField.setEnabled(true);
        pinField.setText("Nhập 6-8 ký tự số");
        pinField.setBackground(Color.WHITE);
        verifyBtn.setEnabled(true);
        verifyBtn.setText("Xác Thực");
        verifyBtn.setBackground(AppConstants.PRIMARY_COLOR);

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

        pinField = createPasswordField("Nhập 6-8 ký tự số", 380);
        leftPanel.add(pinField);
        leftPanel.add(Box.createVerticalStrut(18));

        verifyBtn = createPrimaryButton("Xác Thực", AppConstants.PRIMARY_COLOR, 380, 46);

        // [UPDATED] Xử lý sự kiện click nút Xác Thực
        verifyBtn.addActionListener((ActionEvent e) -> {
            if (simulatorService == null) return;

            char[] pin = pinField.getPassword();
            // Nếu người dùng chưa nhập gì hoặc nhập placeholder
            if (pin.length == 0 || String.valueOf(pin).equals("Nhập 6-8 ký tự số")) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập PIN!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                boolean success = simulatorService.verifyPin(pin);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Xác thực thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    // Cập nhật giao diện sang trạng thái verified
                    setVerifiedState();
                } else {
                    int tries = simulatorService.getPinTriesRemaining();
                    String msg = "PIN sai! Số lần thử còn lại: " + tries;
                    if (tries == 0) {
                        msg = "Thẻ đã bị khóa! Vui lòng liên hệ Admin.";
                        lockInterfaceState(); // Khóa giao diện ngay lập tức
                    } else {
                        setUnverifiedState(); // Cập nhật lại số lần thử
                    }
                    JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                // Nếu dính exception thẻ bị khóa (0x6983)
                if (ex.getMessage().contains("khóa") || simulatorService.getPinTriesRemaining() == 0) {
                    lockInterfaceState();
                }
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
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

            char[] oldPin = oldPinField.getPassword();
            char[] newPin = newPinField.getPassword();
            char[] confirm = confirmPinField.getPassword();

            if (!String.valueOf(newPin).equals(String.valueOf(confirm))) {
                JOptionPane.showMessageDialog(this, "PIN mới và xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Verify PIN cũ trước khi đổi (Server check)
                boolean verified = simulatorService.verifyPin(oldPin);
                if (!verified) {
                    int tries = simulatorService.getPinTriesRemaining();
                    if (tries == 0) lockInterfaceState();
                    else setUnverifiedState(); // Sai PIN cũ -> mất trạng thái verified

                    JOptionPane.showMessageDialog(this, "PIN cũ không đúng! Số lần thử còn lại: " + tries, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean success = simulatorService.changePin(oldPin, newPin);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Đổi PIN thành công! Vui lòng đăng nhập lại.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    oldPinField.setText("");
                    newPinField.setText("");
                    confirmPinField.setText("");

                    // Reset về trạng thái chưa xác thực, buộc user nhập lại PIN mới
                    setUnverifiedState();
                }
            } catch (Exception ex) {
                if (ex.getMessage().contains("khóa") || simulatorService.getPinTriesRemaining() == 0) {
                    lockInterfaceState();
                }
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
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
                if (btn.isEnabled()) btn.setBackground(original.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(original);
            }
        });

        return btn;
    }
}