package pages;

import api.ApiServiceManager;
import api.AuthApiService;
import api.CardApiService;
import applet.AppletConstants;
import constants.AppConstants;
import service.SimulatorService;
import ui.RoundedBorder;
import ui.UIComponentFactory;
import utils.RSAUtility;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * PIN & Bảo Mật Page - Final Fixed UI
 * [UPDATED]
 * 1. Nút "Mở Khóa (Admin)" rộng hơn (230px).
 * 2. Popup Admin dùng ô nhập chuẩn.
 */
public class PinPage extends JPanel {

    // Colors
    private static final Color GOLD_COLOR = new Color(234, 179, 8);
    private static final Color LIGHT_GRAY_BG = new Color(249, 250, 251);

    private SimulatorService simulatorService;
    private ApiServiceManager apiManager;
    private CardApiService cardApi;
    private AuthApiService authApi;
    private JLabel statusLabel;
    private JLabel triesLabel;

    // Các component
    private JTextField studentCodeField;
    private JPasswordField pinField;
    private JButton loginBtn;
    private JButton logoutBtn;
    private JButton unlockBtn;
    private JButton changeBtn;

    private JPasswordField oldPinField;
    private JPasswordField newPinField;
    private JPasswordField confirmPinField;

    public PinPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.apiManager = ApiServiceManager.getInstance();
        this.cardApi = apiManager.getCardApiService();
        this.authApi = new AuthApiService();

        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppConstants.BACKGROUND);

        JPanel mainCard = new JPanel();
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));
        mainCard.setBackground(Color.WHITE);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 16),
                new EmptyBorder(30, 40, 30, 40)
        ));
        mainCard.setPreferredSize(new Dimension(900, 780));

        mainCard.add(createHeader());
        mainCard.add(Box.createVerticalStrut(20));
        mainCard.add(createVerifySection());
        mainCard.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(229, 231, 235));
        mainCard.add(sep);
        mainCard.add(Box.createVerticalStrut(20));

        mainCard.add(createChangeSection());
        mainCard.add(Box.createVerticalGlue());

        wrapper.add(mainCard);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        checkInitialStatus();
    }

    public PinPage() { this(null); }

    private void checkInitialStatus() {
        if (simulatorService == null) return;

        String currentCode = simulatorService.getCurrentStudentCode();
        if (!currentCode.isEmpty()) {
            models.CardInfo card = simulatorService.getCardByStudentCode(currentCode);
            if (card != null && "Khóa".equals(card.getStatus())) {
                lockInterfaceState();
                return;
            }
        }

        if (simulatorService.isPinVerified()) {
            if (simulatorService.isChangePinRequired()) {
                setForceChangePinState();
            } else {
                setVerifiedState();
            }
        } else {
            setUnverifiedState();
        }
    }

    // --- CÁC TRẠNG THÁI GIAO DIỆN ---

    private void lockInterfaceState() {
        statusLabel.setText("ĐÃ BỊ KHÓA");
        statusLabel.setForeground(AppConstants.DANGER_COLOR);
        triesLabel.setText("0/3");
        triesLabel.setForeground(AppConstants.DANGER_COLOR);

        studentCodeField.setEnabled(false);
        pinField.setEnabled(false);
        pinField.setText("");
        pinField.setBackground(new Color(254, 226, 226));

        loginBtn.setEnabled(false);
        loginBtn.setBackground(Color.GRAY);

        logoutBtn.setVisible(true);
        unlockBtn.setVisible(true);

        disableChangeSection();
    }

    private void setVerifiedState() {
        statusLabel.setText("Đã đăng nhập");
        statusLabel.setForeground(AppConstants.SUCCESS_COLOR);
        triesLabel.setText("3/3");

        studentCodeField.setEnabled(false);
        pinField.setEnabled(false);
        pinField.setText("");
        pinField.setBackground(new Color(240, 253, 244));
        loginBtn.setEnabled(false);
        loginBtn.setText("Đã Đăng Nhập");
        loginBtn.setBackground(AppConstants.SUCCESS_COLOR);

        logoutBtn.setVisible(true);
        unlockBtn.setVisible(false);
        enableChangeSection();
    }

    private void setForceChangePinState() {
        statusLabel.setText("Cần đổi PIN");
        statusLabel.setForeground(AppConstants.WARNING_COLOR);

        studentCodeField.setEnabled(false);
        pinField.setEnabled(false);
        pinField.setText("");
        loginBtn.setEnabled(false);
        loginBtn.setText("Đổi PIN Mặc Định");

        logoutBtn.setVisible(false);
        unlockBtn.setVisible(false);
        enableChangeSection();

        oldPinField.setText(AppletConstants.DEFAULT_PIN);
        newPinField.requestFocus();

        JOptionPane.showMessageDialog(this,
                "CẢNH BÁO BẢO MẬT:\n\nBạn đang sử dụng mã PIN mặc định (000000).\nVui lòng đổi mã PIN mới ngay lập tức.",
                "Yêu Cầu Đổi PIN", JOptionPane.WARNING_MESSAGE);
    }

    private void setUnverifiedState() {
        statusLabel.setText("Chưa đăng nhập");
        statusLabel.setForeground(Color.GRAY);
        triesLabel.setText("3/3");
        triesLabel.setForeground(AppConstants.TEXT_PRIMARY);

        studentCodeField.setEnabled(true);
        pinField.setEnabled(true);
        pinField.setText("");
        pinField.setBackground(Color.WHITE);
        loginBtn.setEnabled(true);
        loginBtn.setText("Đăng Nhập");
        loginBtn.setBackground(AppConstants.PRIMARY_COLOR);

        logoutBtn.setVisible(false);
        unlockBtn.setVisible(false);
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
        oldPinField.setText("");
        newPinField.setText("");
        confirmPinField.setText("");
    }

    // --- UI SECTIONS ---

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(Color.WHITE);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(Color.WHITE);

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GOLD_COLOR);
                g2.fillRoundRect(6, 14, 20, 16, 4, 4);
                g2.setStroke(new BasicStroke(3));
                g2.drawArc(8, 4, 16, 14, 0, 180);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Đăng Nhập & Bảo Mật");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 12, 0, 0));

        leftPanel.add(iconPanel);
        leftPanel.add(title);

        logoutBtn = createPrimaryButton("Đăng Xuất", AppConstants.DANGER_COLOR, 120, 36);
        logoutBtn.setVisible(false);
        logoutBtn.addActionListener(e -> handleLogout());

        titleRow.add(leftPanel, BorderLayout.WEST);
        titleRow.add(logoutBtn, BorderLayout.EAST);

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

        JLabel sectionTitle = new JLabel("Đăng Nhập Thẻ");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(20));

        JPanel contentRow = new JPanel();
        contentRow.setLayout(new BoxLayout(contentRow, BoxLayout.X_AXIS));
        contentRow.setBackground(Color.WHITE);
        contentRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // Input fields (Student Code & PIN)
        leftPanel.add(createLabeledField("Mã số sinh viên (MSSV)",
                studentCodeField = createTextField("Nhập MSSV của bạn", 380)));
        leftPanel.add(Box.createVerticalStrut(15));

        leftPanel.add(createLabeledField("Nhập mã PIN của bạn",
                pinField = createPasswordField("Nhập 6 ký tự số", 380)));
        leftPanel.add(Box.createVerticalStrut(18));

        loginBtn = createPrimaryButton("Đăng Nhập", AppConstants.PRIMARY_COLOR, 380, 46);
        loginBtn.addActionListener(this::handleLogin);
        leftPanel.add(loginBtn);

        // --- STATUS CARD (Bên phải) ---
        JPanel statusCard = new JPanel();
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));
        statusCard.setBackground(LIGHT_GRAY_BG);
        statusCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 10),
                new EmptyBorder(18, 22, 18, 22)
        ));
        statusCard.setAlignmentY(Component.TOP_ALIGNMENT);
        statusCard.setPreferredSize(new Dimension(260, 130));
        statusCard.setMaximumSize(new Dimension(260, 130));

        statusLabel = new JLabel("Chưa đăng nhập");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(AppConstants.TEXT_SECONDARY);

        triesLabel = new JLabel("3/3");
        triesLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        triesLabel.setForeground(AppConstants.TEXT_PRIMARY);

        statusCard.add(createStatusRow("Trạng thái:", statusLabel));
        statusCard.add(Box.createVerticalStrut(12));
        statusCard.add(createStatusRow("Số lần còn lại:", triesLabel));

        // Nút Mở Khóa (Căn giữa)
        statusCard.add(Box.createVerticalStrut(15));

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setBackground(LIGHT_GRAY_BG);

        unlockBtn = UIComponentFactory.createPrimaryButton("Mở Khóa (Admin)");
        unlockBtn.setBackground(new Color(37, 99, 235));
        // [FIX] Tăng width lên 230
        unlockBtn.setPreferredSize(new Dimension(230, 40));
        unlockBtn.setVisible(false);
        unlockBtn.addActionListener(e -> showAdminUnlockDialog());

        btnWrapper.add(unlockBtn);
        statusCard.add(btnWrapper);

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

        JLabel sectionTitle = new JLabel("Đổi PIN");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(AppConstants.TEXT_PRIMARY);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sectionTitle);
        panel.add(Box.createVerticalStrut(20));

        JPanel contentRow = new JPanel();
        contentRow.setLayout(new BoxLayout(contentRow, BoxLayout.X_AXIS));
        contentRow.setBackground(Color.WHITE);
        contentRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        oldPinField = createPasswordField("Nhập PIN hiện tại của bạn", 380);
        newPinField = createPasswordField("6-8 ký tự số", 380);
        confirmPinField = createPasswordField("Nhập lại PIN mới", 380);

        fieldsPanel.add(createLabeledFieldObj("PIN hiện tại", oldPinField));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledFieldObj("PIN mới", newPinField));
        fieldsPanel.add(Box.createVerticalStrut(18));
        fieldsPanel.add(createLabeledFieldObj("Xác nhận PIN mới", confirmPinField));

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        btnPanel.add(Box.createVerticalStrut(28));

        changeBtn = createPrimaryButton("Đổi PIN", AppConstants.SUCCESS_COLOR, 180, 46);
        changeBtn.addActionListener(this::handleChangePin);

        btnPanel.add(changeBtn);

        contentRow.add(fieldsPanel);
        contentRow.add(Box.createHorizontalStrut(35));
        contentRow.add(btnPanel);
        contentRow.add(Box.createHorizontalGlue());

        panel.add(contentRow);

        return panel;
    }

    // --- ACTIONS HANDLERS ---

    private void handleLogin(ActionEvent e) {
        if (simulatorService == null) return;

        String studentCode = studentCodeField.getText().trim();
        if (studentCode.isEmpty() || studentCode.equals("Nhập MSSV của bạn")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã số sinh viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        char[] pin = pinField.getPassword();
        if (pin.length == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập PIN!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Auto-connect if not connected (after logout)
        if (!simulatorService.isConnected()) {
            try {
                simulatorService.connect();
                
                // Special handling for Admin CT060132 - always ensure PIN "000000" exists
                boolean isAdmin = AppletConstants.ADMIN_STUDENT_CODE.equalsIgnoreCase(studentCode);
                if (isAdmin) {
                    // For admin, always ensure default PIN "000000" exists
                    try {
                        simulatorService.createDemoPin();
                        System.out.println("Admin PIN (000000) ensured on card");
                    } catch (Exception ex) {
                        // PIN might already exist, ignore
                        System.out.println("Admin PIN might already exist: " + ex.getMessage());
                    }
                } else {
                    // For students, restore from JSON if available
                    // [REMOVED] restoreCardState - No longer used
                    boolean stateRestored = false;
                    
                    // Only create default PIN if state was NOT restored (no saved PIN)
                    // If state was restored, PIN should already be on the card
                    if (!stateRestored) {
                        try {
                            simulatorService.createDemoPin();
                        } catch (Exception ex) {
                            // PIN might already exist, ignore
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Không thể kết nối với thẻ!\n" + ex.getMessage(),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String pinStr = new String(pin);
        boolean isAdmin = AppletConstants.ADMIN_STUDENT_CODE.equalsIgnoreCase(studentCode);

        if (!isAdmin) {
            if (!simulatorService.isCardExists(studentCode)) {
                JOptionPane.showMessageDialog(this, "Mã số sinh viên không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            models.CardInfo card = simulatorService.getCardByStudentCode(studentCode);
            simulatorService.setCurrentStudentCode(studentCode);

            if ("Khóa".equals(card.getStatus())) {
                lockInterfaceState();
                JOptionPane.showMessageDialog(this,
                        "Thẻ của bạn đã bị KHÓA do nhập sai PIN 3 lần!\nSử dụng nút 'Mở Khóa (Admin)' để mở.",
                        "Thẻ Bị Khóa", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = simulatorService.verifyStudentPin(studentCode, pinStr);

            if (success) {
                simulatorService.setCurrentRole("normal");
                simulatorService.setPinVerified(true);
                
                // RSA Authentication - Auto-generate if not exists on first login
                boolean rsaKeyJustGenerated = false; // Flag to track if we just generated RSA keypair
                try {
                    if (simulatorService.isConnected()) {
                        // Check if RSA keypair exists on card (required for signing)
                        // Also check server for verification (optional)
                        boolean hasRSAKeyOnCard = false;
                        boolean hasRSAKeyOnServer = false;
                        
                        // Check card first (this is the source of truth for authentication)
                        try {
                            byte[] publicKeyData = simulatorService.getRSAPublicKey();
                            hasRSAKeyOnCard = (publicKeyData != null && publicKeyData.length > 0);
                            if (hasRSAKeyOnCard) {
                                System.out.println("RSA keypair found on card");
                            }
                        } catch (Exception cardCheckEx) {
                            hasRSAKeyOnCard = false;
                            System.out.println("RSA keypair check on card failed: " + cardCheckEx.getMessage());
                        }
                        
                        // Check server (for verification, optional)
                        if (apiManager != null && apiManager.isServerAvailable()) {
                            try {
                                models.CardInfo serverCard = cardApi.getCard(studentCode);
                                if (serverCard != null && serverCard.getRsaPublicKey() != null && !serverCard.getRsaPublicKey().isEmpty()) {
                                    hasRSAKeyOnServer = true;
                                    System.out.println("RSA public key found on server");
                                }
                            } catch (Exception serverEx) {
                                // Server error
                            }
                        }
                        
                        // Only authenticate if keypair exists on CARD (required for signing)
                        // Server key is optional but recommended for verification
                        boolean hasRSAKey = hasRSAKeyOnCard;
                        
                        if (hasRSAKeyOnCard && !hasRSAKeyOnServer) {
                            System.out.println("Warning: RSA keypair exists on card but not on server - will use card's key for verification");
                        }
                        
                        if (!hasRSAKey) {
                            // No RSA keypair yet - auto-generate on first login
                            try {
                                System.out.println("RSA keypair not found - generating on first login for: " + studentCode);
                                byte[] publicKeyData = simulatorService.generateRSAKeyPair();
                                if (publicKeyData != null) {
                                    // Extract modulus and exponent
                                    byte[] modulus = new byte[applet.AppletConstants.RSA_MODULUS_SIZE];
                                    byte[] exponent = new byte[3];
                                    System.arraycopy(publicKeyData, 0, modulus, 0, modulus.length);
                                    System.arraycopy(publicKeyData, modulus.length, exponent, 0, exponent.length);
                                    
                                    // Convert to PEM format
                                    String publicKeyPEM = utils.RSAUtility.convertToPEM(modulus, exponent);
                                    
                                    // Save to server if available
                                    if (apiManager != null && apiManager.isServerAvailable()) {
                                        try {
                                            cardApi.updateRSAPublicKey(studentCode, publicKeyPEM);
                                            System.out.println("RSA public key saved to server");
                                        } catch (Exception apiEx) {
                                            System.out.println("Could not save RSA key to server: " + apiEx.getMessage());
                                        }
                                    }
                                    
                                    // Save to CardInfo for later use
                                    models.CardInfo cardInfo = simulatorService.getCardByStudentCode(studentCode);
                                    if (cardInfo != null) {
                                        cardInfo.setRsaPublicKey(publicKeyPEM);
                                    }
                                    
                                    System.out.println("RSA keypair generated successfully on first login");
                                    rsaKeyJustGenerated = true; // Mark that we just generated
                                    // Skip authentication for first login - will authenticate on next login
                                }
                            } catch (Exception genEx) {
                                System.out.println("Could not generate RSA keypair: " + genEx.getMessage());
                                // Continue - RSA is optional
                            }
                        }
                        
                        // Only authenticate if keypair existed BEFORE we checked (not just generated)
                        // CRITICAL: Use original hasRSAKey value, not re-check after generation
                        if (hasRSAKey && !rsaKeyJustGenerated) {
                            // RSA keypair exists - authenticate
                            try {
                                System.out.println("[RSA AUTH] Starting RSA authentication for student: " + studentCode);
                                boolean rsaAuthenticated = authenticateCardWithRSA(studentCode);
                                if (rsaAuthenticated) {
                                    System.out.println("[RSA AUTH] ✓ Authentication successful for student: " + studentCode);
                                    
                                    // Sau khi verify RSA thành công ở client, gọi API login để lấy token
                                    if (apiManager != null && apiManager.isServerAvailable()) {
                                        try {
                                            System.out.println("[AUTH] Calling server login API to get token...");
                                            // Generate challenge và signature để gửi lên server
                                            byte[] challenge = utils.RSAUtility.generateChallenge();
                                            byte[] signature = simulatorService.signRSAChallenge(challenge);
                                            
                                            // Gọi API login
                                            String token = authApi.login(studentCode, challenge, signature);
                                            if (token != null && !token.isEmpty()) {
                                                System.out.println("[AUTH] ✓ Token received from server");
                                                // Token đã được set vào ApiClient trong AuthApiService
                                                // Share token với các API services khác
                                                apiManager.setAuthToken(token);
                                            } else {
                                                System.out.println("[AUTH] ✗ Failed to get token from server");
                                            }
                                        } catch (Exception loginEx) {
                                            System.out.println("[AUTH] ✗ Error calling login API: " + loginEx.getMessage());
                                            loginEx.printStackTrace();
                                            // Không block login nếu API call fail
                                        }
                                    } else {
                                        System.out.println("[AUTH] Server not available, skipping token request");
                                    }
                                } else {
                                    // Keypair exists but verification failed - possible fake card
                                    System.out.println("[RSA AUTH] ✗ Authentication FAILED for student: " + studentCode);
                                    System.out.println("[RSA AUTH] Possible reasons:");
                                    System.out.println("  - Signature verification failed (wrong private key)");
                                    System.out.println("  - Signing error on card (check logs above for error code)");
                                    System.out.println("  - Keypair mismatch between card and server");
                                    // Show warning but allow login (RSA is additional security layer)
                                    JOptionPane.showMessageDialog(this,
                                        "Cảnh báo: Xác thực RSA thất bại. Thẻ có thể bị giả mạo.\n" +
                                        "Bạn vẫn có thể đăng nhập, nhưng hãy kiểm tra lại thẻ.\n\n" +
                                        "Xem console log để biết chi tiết lỗi.",
                                        "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);
                                }
                            } catch (Exception authEx) {
                                // Catch authentication errors separately to avoid confusion
                                System.out.println("[RSA AUTH] ✗ Exception during authentication for student: " + studentCode);
                                System.out.println("[RSA AUTH] Exception type: " + authEx.getClass().getName());
                                System.out.println("[RSA AUTH] Exception message: " + authEx.getMessage());
                                if (authEx.getCause() != null) {
                                    System.out.println("[RSA AUTH] Caused by: " + authEx.getCause().getMessage());
                                }
                                authEx.printStackTrace();
                                // Don't block login for RSA errors - just log
                            }
                        } else if (rsaKeyJustGenerated) {
                            System.out.println("[RSA AUTH] Skipping RSA authentication after keypair generation (first login)");
                        }
                    }
                } catch (Exception rsaEx) {
                    // RSA authentication is optional, continue anyway
                    System.out.println("RSA authentication error: " + rsaEx.getMessage());
                }

                if (AppletConstants.DEFAULT_PIN.equals(pinStr)) {
                    setForceChangePinState();
                } else {
                    setVerifiedState();
                    JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                int remaining = simulatorService.getStudentPinTries(studentCode);
                triesLabel.setText(remaining + "/3");
                triesLabel.setForeground(AppConstants.DANGER_COLOR);

                if (remaining == 0) {
                    lockInterfaceState();
                    JOptionPane.showMessageDialog(this,
                            "Bạn đã nhập sai quá 3 lần. Thẻ đã bị KHÓA!",
                            "ĐÃ KHÓA THẺ", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "PIN không đúng! Bạn còn " + remaining + " lần thử.",
                            "Sai Mật Khẩu", JOptionPane.WARNING_MESSAGE);
                }
            }
        } else {
            // Admin logic - CT060132 is fixed admin account
            try {
                // For admin, always accept PIN "000000" even if card verification fails
                // This ensures admin can always login
                boolean success = false;
                
                // Try to verify on card first
                try {
                    success = simulatorService.verifyPin(pin);
                } catch (Exception verifyEx) {
                    System.out.println("Card PIN verification failed: " + verifyEx.getMessage());
                }
                
                // If card verification failed but PIN is "000000", accept it anyway (admin fallback)
                if (!success && AppletConstants.DEFAULT_PIN.equals(pinStr)) {
                    System.out.println("Admin PIN verification failed on card, but accepting default PIN (000000) as fallback");
                    // Ensure PIN exists on card for next time
                    try {
                        simulatorService.createDemoPin();
                    } catch (Exception createEx) {
                        // Ignore - PIN might already exist
                    }
                    success = true;
                }
                
                if (success) {
                    simulatorService.setCurrentStudentCode(studentCode);
                    simulatorService.setCurrentRole("Admin");
                    simulatorService.setPinVerified(true);
                    
                    // Admin không cần RSA authentication, nhưng vẫn cần token để gọi API
                    System.out.println("Admin login successful (CT060132)");
                    
                    // Gọi API login để lấy token (admin bypass RSA verification)
                    if (apiManager != null && apiManager.isServerAvailable()) {
                        try {
                            System.out.println("[AUTH] Calling server login API for admin...");
                            // Admin login - không cần challenge/signature, server sẽ bypass
                            String token = authApi.login(studentCode, new byte[16], new byte[128]);
                            if (token != null && !token.isEmpty()) {
                                System.out.println("[AUTH] ✓ Token received for admin");
                                apiManager.setAuthToken(token);
                            } else {
                                System.out.println("[AUTH] ✗ Failed to get token for admin");
                            }
                        } catch (Exception loginEx) {
                            System.out.println("[AUTH] ✗ Error calling login API for admin: " + loginEx.getMessage());
                            loginEx.printStackTrace();
                            // Không block login nếu API call fail
                        }
                    }
                    
                    setVerifiedState();
                    JOptionPane.showMessageDialog(this, "Đăng nhập Admin thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int tries = simulatorService.getPinTriesRemaining();
                    triesLabel.setText(tries + "/3");
                    JOptionPane.showMessageDialog(this, "PIN sai! Số lần thử còn lại: " + tries, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleChangePin(ActionEvent e) {
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

        if (AppletConstants.DEFAULT_PIN.equals(newPinStr)) {
            JOptionPane.showMessageDialog(this, "Không được sử dụng lại mã PIN mặc định!", "Lỗi Bảo Mật", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String currentStudentCode = simulatorService.getCurrentStudentCode();
        boolean isAdmin = "Admin".equals(simulatorService.getCurrentRole());

        if (isAdmin) {
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
            // Student: Verify PIN cũ trên thẻ trước
            try {
                boolean verified = simulatorService.verifyPin(oldPinStr.toCharArray());
                if (!verified) {
                    JOptionPane.showMessageDialog(this, "PIN cũ không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Đổi PIN trên thẻ
                boolean success = simulatorService.changeStudentPin(currentStudentCode, oldPinStr, newPinStr);
                if (success) {
                    JOptionPane.showMessageDialog(this, 
                        "Đổi PIN thành công!\nVui lòng đăng nhập lại với PIN mới.", 
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    clearPinFields();
                    setUnverifiedState();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Không thể đổi PIN. Vui lòng thử lại.", 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi khi đổi PIN: " + ex.getMessage(), 
                    "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Check if RSA keypair exists on card or server
     */
    private boolean checkRSAKeyExists(String studentCode) {
        try {
            // Check server first
            if (apiManager != null && apiManager.isServerAvailable()) {
                try {
                    models.CardInfo card = cardApi.getCard(studentCode);
                    if (card != null && card.getRsaPublicKey() != null && !card.getRsaPublicKey().isEmpty()) {
                        return true; // RSA key exists on server
                    }
                } catch (Exception e) {
                    // Server error, check card directly
                }
            }
            
            // Check card directly
            if (simulatorService.isConnected()) {
                try {
                    byte[] publicKeyData = simulatorService.getRSAPublicKey();
                    return publicKeyData != null && publicKeyData.length > 0;
                } catch (Exception e) {
                    // No RSA keypair on card
                    return false;
                }
            }
        } catch (Exception e) {
            // Error checking - assume no key
        }
        return false;
    }
    
    /**
     * Authenticate card with RSA challenge-response
     * NOTE: This method should NOT be called immediately after generating RSA keypair
     * because the card may not be ready to sign challenges yet (error 6700)
     */
    private boolean authenticateCardWithRSA(String studentCode) {
        try {
            // Verify keypair exists and is ready before attempting authentication
            if (!simulatorService.isConnected()) {
                return false;
            }
            
            // First, verify keypair exists on card
            try {
                byte[] testKey = simulatorService.getRSAPublicKey();
                if (testKey == null || testKey.length == 0) {
                    System.out.println("RSA keypair not found on card for authentication");
                    return false;
                }
            } catch (Exception keyCheckEx) {
                System.out.println("RSA keypair check failed: " + keyCheckEx.getMessage());
                return false;
            }
            
            // Get RSA public key from server
            if (apiManager != null && apiManager.isServerAvailable()) {
                try {
                    System.out.println("[RSA AUTH] Fetching public key from server for student: " + studentCode);
                    models.CardInfo card = cardApi.getCard(studentCode);
                    if (card != null && card.getRsaPublicKey() != null && !card.getRsaPublicKey().isEmpty()) {
                        // Use public key from server
                        String publicKeyPEM = card.getRsaPublicKey();
                        System.out.println("[RSA AUTH] Using public key from server (length: " + publicKeyPEM.length() + " chars)");
                        return simulatorService.authenticateCardWithRSA(publicKeyPEM);
                    } else {
                        System.out.println("[RSA AUTH] No public key found on server for student: " + studentCode);
                    }
                } catch (Exception e) {
                    System.out.println("[RSA AUTH] Error getting RSA public key from server: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[RSA AUTH] Server not available, using card's public key");
            }
            
            // Fallback: Get public key directly from card
            // NOTE: This fallback should NOT be called if RSA keypair was just generated
            // because the card may not be ready to sign challenges yet
            // This method should only be called when keypair existed before
            try {
                System.out.println("[RSA AUTH] Getting public key directly from card");
                byte[] publicKeyData = simulatorService.getRSAPublicKey();
                if (publicKeyData != null && publicKeyData.length > 0) {
                    System.out.println("[RSA AUTH] Public key data length: " + publicKeyData.length);
                    // Extract modulus and exponent
                    byte[] modulus = new byte[applet.AppletConstants.RSA_MODULUS_SIZE];
                    byte[] exponent = new byte[3];
                    System.arraycopy(publicKeyData, 0, modulus, 0, modulus.length);
                    System.arraycopy(publicKeyData, modulus.length, exponent, 0, exponent.length);
                    
                    System.out.println("[RSA AUTH] Modulus length: " + modulus.length);
                    System.out.println("[RSA AUTH] Exponent length: " + exponent.length);
                    
                    // Convert to PublicKey and authenticate
                    java.security.PublicKey publicKey = utils.RSAUtility.convertToPublicKey(modulus, exponent);
                    byte[] challenge = utils.RSAUtility.generateChallenge();
                    System.out.println("[RSA AUTH] Generated challenge: " + utils.RSAUtility.bytesToHex(challenge));
                    
                    // Try to sign challenge - catch 6700 error specifically
                    byte[] signature;
                    try {
                        signature = simulatorService.signRSAChallenge(challenge);
                    } catch (Exception signEx) {
                        // 6700 error means keypair may not be ready
                        String errorMsg = signEx.getMessage();
                        System.out.println("[RSA AUTH] Signing failed for student: " + studentCode);
                        System.out.println("[RSA AUTH] Error message: " + errorMsg);
                        if (errorMsg != null && errorMsg.contains("6700")) {
                            System.out.println("[RSA AUTH] Keypair not ready for signing (6700) - skipping authentication");
                            return false;
                        }
                        // For other errors, also return false instead of throwing
                        System.out.println("[RSA AUTH] Signing error details: " + errorMsg);
                        return false;
                    }
                    
                    // Verify signature
                    boolean verified = utils.RSAUtility.verifySignature(publicKey, challenge, signature);
                    if (!verified) {
                        System.out.println("[RSA AUTH] Signature verification failed for student: " + studentCode);
                        System.out.println("[RSA AUTH] Challenge: " + utils.RSAUtility.bytesToHex(challenge));
                        System.out.println("[RSA AUTH] Signature length: " + (signature != null ? signature.length : 0));
                    } else {
                        System.out.println("[RSA AUTH] Signature verification successful for student: " + studentCode);
                    }
                    return verified;
                }
            } catch (Exception cardEx) {
                // If signing fails (e.g., 6700 error), it might be because keypair was just generated
                // or card is not ready - return false silently
                String errorMsg = cardEx.getMessage();
                System.out.println("[RSA AUTH] Exception in card authentication fallback:");
                System.out.println("[RSA AUTH] Exception type: " + cardEx.getClass().getName());
                System.out.println("[RSA AUTH] Exception message: " + errorMsg);
                if (errorMsg != null && errorMsg.contains("6700")) {
                    System.out.println("[RSA AUTH] Keypair not ready (6700) - card may need initialization");
                } else {
                    System.out.println("[RSA AUTH] Authentication from card failed - check logs above for details");
                }
                cardEx.printStackTrace();
                return false;
            }
            
            return false;
        } catch (Exception e) {
            System.out.println("Error in authenticateCardWithRSA: " + e.getMessage());
            // Don't print stack trace for expected errors (e.g., 6700 after key generation)
            return false;
        }
    }
    
    private void handleLogout() {
        if (simulatorService != null) {
            // Disconnect from card and reset all state
            simulatorService.disconnect();
        }
        setUnverifiedState();
        studentCodeField.setText("Nhập MSSV của bạn");
        studentCodeField.setForeground(Color.GRAY);
        studentCodeField.setEnabled(true);
        clearPinFields();
        JOptionPane.showMessageDialog(this, 
            "Đã đăng xuất thành công!\n" +
            "Kết nối với thẻ đã được ngắt.\n" +
            "Vui lòng kết nối lại khi đăng nhập lần sau.",
            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- MODERN POPUP UNLOCK ---

    private void showAdminUnlockDialog() {
        AdminUnlockDialog dialog = new AdminUnlockDialog((JFrame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String adminPin = dialog.getPin();
            String lockedStudentId = studentCodeField.getText().trim();

            boolean unlocked = simulatorService.unlockCardByAdminPin(lockedStudentId, adminPin);

            if (unlocked) {
                // [NEW] Cập nhật thông báo rõ ràng hơn về việc Reset PIN
                JOptionPane.showMessageDialog(this,
                        "Đã mở khóa thẻ thành công!\n" +
                                "- Số lần thử PIN đã được reset.\n" +
                                "- PIN sinh viên đã được đặt lại về 000000.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                setUnverifiedState();
                triesLabel.setText("3/3");
                triesLabel.setForeground(AppConstants.TEXT_PRIMARY);
            } else {
                JOptionPane.showMessageDialog(this, "Mã PIN Admin không đúng hoặc lỗi hệ thống!", "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Dialog nhập PIN Admin - Sử dụng JPasswordField chuẩn để hỗ trợ độ dài tùy ý
     */
    private class AdminUnlockDialog extends JDialog {
        private JPasswordField adminPinField;
        private boolean confirmed = false;

        public AdminUnlockDialog(JFrame parent) {
            super(parent, "Xác thực Admin", true);
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

            JLabel title = new JLabel("Mở Khóa Thẻ");
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(AppConstants.TEXT_PRIMARY);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel("Vui lòng nhập mã PIN Admin");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setForeground(AppConstants.TEXT_SECONDARY);
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            // [FIX] Sử dụng JPasswordField chuẩn thay vì ô chia vạch
            adminPinField = new JPasswordField();
            adminPinField.setFont(new Font("Segoe UI", Font.BOLD, 18));
            adminPinField.setHorizontalAlignment(JTextField.CENTER);
            adminPinField.setEchoChar('\u25CF');
            adminPinField.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppConstants.BORDER_COLOR, 1, 10),
                    new EmptyBorder(10, 15, 10, 15)
            ));
            adminPinField.setMaximumSize(new Dimension(300, 50));
            adminPinField.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Buttons
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            btnPanel.setBackground(Color.WHITE);

            JButton btnCancel = UIComponentFactory.createSecondaryButton("Hủy");
            btnCancel.setPreferredSize(new Dimension(100, 40));
            btnCancel.addActionListener(e -> dispose());

            JButton btnUnlock = UIComponentFactory.createPrimaryButton("Mở Khóa");
            btnUnlock.setPreferredSize(new Dimension(100, 40));
            btnUnlock.addActionListener(e -> {
                if (adminPinField.getPassword().length > 0) {
                    confirmed = true;
                    dispose();
                }
            });

            adminPinField.addActionListener(e -> btnUnlock.doClick());

            btnPanel.add(btnCancel);
            btnPanel.add(btnUnlock);

            contentPanel.add(title);
            contentPanel.add(Box.createVerticalStrut(5));
            contentPanel.add(subtitle);
            contentPanel.add(Box.createVerticalStrut(25));
            contentPanel.add(adminPinField);
            contentPanel.add(Box.createVerticalStrut(30));
            contentPanel.add(btnPanel);

            add(contentPanel, BorderLayout.CENTER);
            pack();
            setLocationRelativeTo(parent);
            setResizable(false);
        }

        public boolean isConfirmed() { return confirmed; }
        public String getPin() { return new String(adminPinField.getPassword()); }
    }

    // --- HELPER UI METHODS ---

    private void clearPinFields() {
        oldPinField.setText("");
        newPinField.setText("");
        confirmPinField.setText("");
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

    private JPanel createLabeledField(String label, JTextField field) {
        return createLabeledFieldObj(label, (JPasswordField)null, field);
    }

    private JPanel createLabeledFieldObj(String label, JPasswordField pField, JTextField tField) {
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
        panel.add(pField != null ? pField : tField);
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
                if (field.isEnabled() && field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(AppConstants.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.isEnabled() && field.getText().isEmpty()) {
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
        field.setEchoChar('\u2022');
        field.setForeground(AppConstants.TEXT_PRIMARY);
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
                if(field.isEnabled()) field.selectAll();
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