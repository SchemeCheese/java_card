import com.licel.jcardsim.base.Simulator;
import javacard.framework.AID;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GUI kết nối với JCardSim Simulator (thẻ ảo)
 * Không cần thẻ thật hay đầu đọc
 */
public class LibraryCardSimulatorGUI extends JFrame {
    
    private JTabbedPane tabbedPane;
    private JTextField cardIdField, nameField, expiryDateField;
    private JPasswordField pinField, oldPinField, newPinField, confirmPinField;
    private JTextField bookIdField;
    private JTextArea logArea;
    private JTable borrowedBooksTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel, triesLabel;
    private JButton connectButton, disconnectButton;
    
    // Simulator
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    
    // AID của Applet
    private static final byte[] APPLET_AID = {
        (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78, (byte)0x90, (byte)0x00
    };
    
    // Các INS code
    private static final byte INS_CREATE_PIN = (byte)0x10;
    private static final byte INS_VERIFY_PIN = (byte)0x20;
    private static final byte INS_CHANGE_PIN = (byte)0x30;
    private static final byte INS_SET_CARD_INFO = (byte)0x40;
    private static final byte INS_GET_CARD_INFO = (byte)0x50;
    private static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    private static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    private static final byte INS_RETURN_BOOK = (byte)0x80;
    
    public LibraryCardSimulatorGUI() {
        super("Hệ Thống Quản Lý Thẻ Thư Viện - Simulator");
        initializeUI();
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Main Content
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(Color.BLACK);
        tabbedPane.addTab("🔌 Kết Nối", createConnectionPanel());
        tabbedPane.addTab("🔐 Quản Lý PIN", createPinManagementPanel());
        tabbedPane.addTab("📇 Thông Tin Thẻ", createCardInfoPanel());
        tabbedPane.addTab("📚 Quản Lý Mượn Sách", createBorrowingPanel());
        tabbedPane.addTab("📊 Nhật Ký", createLogPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status Bar
        JPanel statusPanel = createStatusBar();
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));
        
        JLabel titleLabel = new JLabel("HỆ THỐNG QUẢN LÝ THẺ THƯ VIỆN - SIMULATOR", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.BLACK);
        
        JLabel subtitleLabel = new JLabel("Powered by JCardSim (Virtual Card)", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(80, 80, 80));
        
        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel infoLabel = new JLabel("<html><h2 style='color:black;'>Thẻ Ảo (JCardSim Simulator)</h2>" +
                "<p style='color:black;'>Applet chạy trong bộ nhớ, không cần thẻ thật hay đầu đọc.</p></html>");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(infoLabel, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        connectButton = new JButton("Khởi Động Simulator");
        connectButton.setBackground(new Color(46, 204, 113));
        connectButton.setForeground(Color.BLACK);
        connectButton.setFont(new Font("Arial", Font.BOLD, 14));
        
        disconnectButton = new JButton("Dừng Simulator");
        disconnectButton.setBackground(new Color(231, 76, 60));
        disconnectButton.setForeground(Color.BLACK);
        disconnectButton.setFont(new Font("Arial", Font.BOLD, 14));
        disconnectButton.setEnabled(false);
        
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        
        gbc.gridy = 1;
        panel.add(buttonPanel, gbc);
        
        connectButton.addActionListener(e -> connectToSimulator());
        disconnectButton.addActionListener(e -> disconnectFromSimulator());
        
        return panel;
    }
    
    private JPanel createPinManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Create PIN
        JPanel createPinPanel = new JPanel(new GridBagLayout());
        createPinPanel.setBackground(Color.WHITE);
        TitledBorder createPinBorder = BorderFactory.createTitledBorder("Tạo PIN Mới (Chỉ lần đầu)");
        createPinBorder.setTitleColor(Color.BLACK);
        createPinPanel.setBorder(createPinBorder);
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.insets = new Insets(5, 5, 5, 5);
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        
        gbc1.gridx = 0; gbc1.gridy = 0;
        JLabel pinLabel1 = new JLabel("PIN mới (4-8 ký tự):");
        pinLabel1.setForeground(Color.BLACK);
        createPinPanel.add(pinLabel1, gbc1);
        gbc1.gridx = 1;
        JPasswordField createPinField = new JPasswordField(20);
        createPinField.setBackground(Color.WHITE);
        createPinField.setForeground(Color.BLACK);
        createPinPanel.add(createPinField, gbc1);
        
        gbc1.gridx = 0; gbc1.gridy = 1; gbc1.gridwidth = 2;
        JButton createPinButton = new JButton("Tạo PIN");
        createPinButton.setBackground(new Color(46, 204, 113));
        createPinButton.setForeground(Color.BLACK);
        createPinPanel.add(createPinButton, gbc1);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(createPinPanel, gbc);
        
        // Verify PIN
        JPanel verifyPinPanel = new JPanel(new GridBagLayout());
        verifyPinPanel.setBackground(Color.WHITE);
        TitledBorder verifyPinBorder = BorderFactory.createTitledBorder("Xác Thực PIN");
        verifyPinBorder.setTitleColor(Color.BLACK);
        verifyPinPanel.setBorder(verifyPinBorder);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        
        gbc2.gridx = 0; gbc2.gridy = 0;
        JLabel pinLabel2 = new JLabel("Nhập PIN:");
        pinLabel2.setForeground(Color.BLACK);
        verifyPinPanel.add(pinLabel2, gbc2);
        gbc2.gridx = 1;
        pinField = new JPasswordField(20);
        pinField.setBackground(Color.WHITE);
        pinField.setForeground(Color.BLACK);
        verifyPinPanel.add(pinField, gbc2);
        
        gbc2.gridx = 0; gbc2.gridy = 1; gbc2.gridwidth = 2;
        JButton verifyButton = new JButton("Xác Thực");
        verifyButton.setBackground(new Color(52, 152, 219));
        verifyButton.setForeground(Color.BLACK);
        verifyPinPanel.add(verifyButton, gbc2);
        
        gbc2.gridy = 2;
        triesLabel = new JLabel("Số lần thử còn lại: --");
        triesLabel.setFont(new Font("Arial", Font.BOLD, 12));
        triesLabel.setForeground(Color.BLACK);
        verifyPinPanel.add(triesLabel, gbc2);
        
        gbc.gridy = 1;
        panel.add(verifyPinPanel, gbc);
        
        // Change PIN
        JPanel changePinPanel = new JPanel(new GridBagLayout());
        changePinPanel.setBackground(Color.WHITE);
        TitledBorder changePinBorder = BorderFactory.createTitledBorder("Đổi PIN");
        changePinBorder.setTitleColor(Color.BLACK);
        changePinPanel.setBorder(changePinBorder);
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.insets = new Insets(5, 5, 5, 5);
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        
        gbc3.gridx = 0; gbc3.gridy = 0;
        JLabel oldPinLabel = new JLabel("PIN cũ:");
        oldPinLabel.setForeground(Color.BLACK);
        changePinPanel.add(oldPinLabel, gbc3);
        gbc3.gridx = 1;
        oldPinField = new JPasswordField(20);
        oldPinField.setBackground(Color.WHITE);
        oldPinField.setForeground(Color.BLACK);
        changePinPanel.add(oldPinField, gbc3);
        
        gbc3.gridx = 0; gbc3.gridy = 1;
        JLabel newPinLabel = new JLabel("PIN mới:");
        newPinLabel.setForeground(Color.BLACK);
        changePinPanel.add(newPinLabel, gbc3);
        gbc3.gridx = 1;
        newPinField = new JPasswordField(20);
        newPinField.setBackground(Color.WHITE);
        newPinField.setForeground(Color.BLACK);
        changePinPanel.add(newPinField, gbc3);
        
        gbc3.gridx = 0; gbc3.gridy = 2;
        JLabel confirmPinLabel = new JLabel("Xác nhận PIN:");
        confirmPinLabel.setForeground(Color.BLACK);
        changePinPanel.add(confirmPinLabel, gbc3);
        gbc3.gridx = 1;
        confirmPinField = new JPasswordField(20);
        confirmPinField.setBackground(Color.WHITE);
        confirmPinField.setForeground(Color.BLACK);
        changePinPanel.add(confirmPinField, gbc3);
        
        gbc3.gridx = 0; gbc3.gridy = 3; gbc3.gridwidth = 2;
        JButton changePinButton = new JButton("Đổi PIN");
        changePinButton.setBackground(new Color(230, 126, 34));
        changePinButton.setForeground(Color.BLACK);
        changePinPanel.add(changePinButton, gbc3);
        
        gbc.gridy = 2;
        panel.add(changePinPanel, gbc);
        
        // Event Handlers
        createPinButton.addActionListener(e -> createPin(createPinField));
        verifyButton.addActionListener(e -> verifyPin());
        changePinButton.addActionListener(e -> changePin());
        
        return panel;
    }
    
    private JPanel createCardInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("Thông Tin Thẻ Thư Viện");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.BLACK);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel cardIdLabel = new JLabel("Mã thẻ:");
        cardIdLabel.setForeground(Color.BLACK);
        panel.add(cardIdLabel, gbc);
        gbc.gridx = 1;
        cardIdField = new JTextField(20);
        cardIdField.setBackground(Color.WHITE);
        cardIdField.setForeground(Color.BLACK);
        panel.add(cardIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel nameLabel = new JLabel("Họ và tên:");
        nameLabel.setForeground(Color.BLACK);
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        nameField.setBackground(Color.WHITE);
        nameField.setForeground(Color.BLACK);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel expiryLabel = new JLabel("Ngày hết hạn (DDMMYYYY):");
        expiryLabel.setForeground(Color.BLACK);
        panel.add(expiryLabel, gbc);
        gbc.gridx = 1;
        expiryDateField = new JTextField(20);
        expiryDateField.setBackground(Color.WHITE);
        expiryDateField.setForeground(Color.BLACK);
        panel.add(expiryDateField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Lưu Thông Tin");
        saveButton.setBackground(new Color(46, 204, 113));
        saveButton.setForeground(Color.BLACK);
        JButton loadButton = new JButton("Tải Thông Tin");
        loadButton.setBackground(new Color(52, 152, 219));
        loadButton.setForeground(Color.BLACK);
        
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        
        saveButton.addActionListener(e -> saveCardInfo());
        loadButton.addActionListener(e -> loadCardInfo());
        
        return panel;
    }
    
    private JPanel createBorrowingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);
        JLabel bookLabel = new JLabel("Mã sách:");
        bookLabel.setForeground(Color.BLACK);
        topPanel.add(bookLabel);
        bookIdField = new JTextField(15);
        bookIdField.setBackground(Color.WHITE);
        bookIdField.setForeground(Color.BLACK);
        topPanel.add(bookIdField);
        
        JButton borrowButton = new JButton("Mượn Sách");
        borrowButton.setBackground(new Color(46, 204, 113));
        borrowButton.setForeground(Color.BLACK);
        topPanel.add(borrowButton);
        
        JButton returnButton = new JButton("Trả Sách");
        returnButton.setBackground(new Color(231, 76, 60));
        returnButton.setForeground(Color.BLACK);
        topPanel.add(returnButton);
        
        JButton refreshButton = new JButton("Làm Mới");
        refreshButton.setBackground(new Color(52, 152, 219));
        refreshButton.setForeground(Color.BLACK);
        topPanel.add(refreshButton);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        String[] columnNames = {"STT", "Mã Sách", "Trạng Thái"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        borrowedBooksTable = new JTable(tableModel);
        borrowedBooksTable.setBackground(Color.WHITE);
        borrowedBooksTable.setForeground(Color.BLACK);
        borrowedBooksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(borrowedBooksTable);
        scrollPane.setBackground(Color.WHITE);
        TitledBorder booksBorder = BorderFactory.createTitledBorder("Danh Sách Sách Đang Mượn");
        booksBorder.setTitleColor(Color.BLACK);
        scrollPane.setBorder(booksBorder);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        borrowButton.addActionListener(e -> borrowBook());
        returnButton.addActionListener(e -> returnBook());
        refreshButton.addActionListener(e -> loadBorrowedBooks());
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(Color.BLACK);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBackground(Color.WHITE);
        TitledBorder logBorder = BorderFactory.createTitledBorder("Nhật Ký Hệ Thống");
        logBorder.setTitleColor(Color.BLACK);
        scrollPane.setBorder(logBorder);
        
        JButton clearButton = new JButton("Xóa Nhật Ký");
        clearButton.setBackground(new Color(200, 200, 200));
        clearButton.setForeground(Color.BLACK);
        clearButton.addActionListener(e -> logArea.setText(""));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(clearButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        
        statusLabel = new JLabel("Chưa khởi động simulator");
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(statusLabel, BorderLayout.WEST);
        
        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(Color.BLACK);
        timeLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(timeLabel, BorderLayout.EAST);
        
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            timeLabel.setText(sdf.format(new Date()));
        });
        timer.start();
        
        return panel;
    }
    
    // Simulator Methods
    private void connectToSimulator() {
        try {
            log("Đang khởi động simulator...");
            simulator = new Simulator();
            
            AID aid = new AID(APPLET_AID, (short)0, (byte)APPLET_AID.length);
            
            // Load class LibraryCardApplet (modular version)
            String appletClassName = "applet.LibraryCardApplet";
            Class<?> appletClass = Class.forName(appletClassName);
            
            @SuppressWarnings("unchecked")
            Class<? extends javacard.framework.Applet> appletClassCasted = 
                (Class<? extends javacard.framework.Applet>) appletClass;
            
            simulator.installApplet(aid, appletClassCasted);
            simulator.selectApplet(aid);
            
            isConnected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            statusLabel.setText("✓ Simulator đã khởi động");
            statusLabel.setForeground(new Color(0, 150, 0));
            
            showSuccess("Simulator đã khởi động thành công!");
            log("✓ Simulator khởi động thành công");
            
        } catch (Exception e) {
            showError("Lỗi khởi động simulator: " + e.getMessage());
            log("✗ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void disconnectFromSimulator() {
        simulator = null;
        isConnected = false;
        isPinVerified = false;
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        statusLabel.setText("Đã dừng simulator");
        statusLabel.setForeground(Color.BLACK);
        log("Đã dừng simulator");
    }
    
    // APDU Methods
    private byte[] sendCommand(byte[] command) {
        if (simulator == null) {
            throw new RuntimeException("Simulator chưa được khởi động!");
        }
        return simulator.transmitCommand(command);
    }
    
    private int getSW(byte[] response) {
        if (response.length < 2) return 0;
        int sw1 = response[response.length - 2] & 0xFF;
        int sw2 = response[response.length - 1] & 0xFF;
        return (sw1 << 8) | sw2;
    }
    
    // PIN Methods
    private void createPin(JPasswordField field) {
        if (!checkConnection()) return;
        
        char[] pinChars = field.getPassword();
        if (pinChars.length < 4 || pinChars.length > 8) {
            showError("PIN phải có từ 4-8 ký tự!");
            return;
        }
        
        try {
            byte[] cmd = new byte[5 + pinChars.length + 1];
            cmd[0] = 0x00;
            cmd[1] = INS_CREATE_PIN;
            cmd[2] = 0x00;
            cmd[3] = 0x00;
            cmd[4] = (byte)(pinChars.length + 1);
            cmd[5] = (byte)pinChars.length;
            for (int i = 0; i < pinChars.length; i++) {
                cmd[6 + i] = (byte)pinChars[i];
            }
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000) {
                showSuccess("Tạo PIN thành công!");
                log("✓ Tạo PIN thành công");
                field.setText("");
            } else {
                showError("Tạo PIN thất bại! SW: " + String.format("%04X", getSW(resp)));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi tạo PIN: " + e.getMessage());
        } finally {
            Arrays.fill(pinChars, ' ');
        }
    }
    
    private void verifyPin() {
        if (!checkConnection()) return;
        
        char[] pinChars = pinField.getPassword();
        
        try {
            byte[] cmd = new byte[5 + pinChars.length + 1];
            cmd[0] = 0x00;
            cmd[1] = INS_VERIFY_PIN;
            cmd[2] = 0x00;
            cmd[3] = 0x00;
            cmd[4] = (byte)(pinChars.length + 1);
            cmd[5] = (byte)pinChars.length;
            for (int i = 0; i < pinChars.length; i++) {
                cmd[6 + i] = (byte)pinChars[i];
            }
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000 && resp.length > 2 && resp[0] == 0x01) {
                isPinVerified = true;
                showSuccess("Xác thực PIN thành công!");
                log("✓ Xác thực PIN thành công");
                pinField.setText("");
                triesLabel.setText("PIN đã xác thực ✓");
                triesLabel.setForeground(new Color(0, 150, 0));
            } else if (getSW(resp) == 0x9000 && resp.length > 2) {
                int tries = resp[1] & 0xFF;
                showError("PIN không đúng! Số lần thử còn lại: " + tries);
                triesLabel.setText("Số lần thử còn lại: " + tries);
                triesLabel.setForeground(new Color(200, 0, 0));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi xác thực PIN: " + e.getMessage());
        } finally {
            Arrays.fill(pinChars, ' ');
        }
    }
    
    private void changePin() {
        if (!checkConnection()) return;
        
        char[] oldPin = oldPinField.getPassword();
        char[] newPin = newPinField.getPassword();
        char[] confirmPin = confirmPinField.getPassword();
        
        if (!Arrays.equals(newPin, confirmPin)) {
            showError("PIN mới và xác nhận không khớp!");
            return;
        }
        
        if (newPin.length < 4 || newPin.length > 8) {
            showError("PIN mới phải có từ 4-8 ký tự!");
            return;
        }
        
        try {
            byte[] cmd = new byte[5 + oldPin.length + newPin.length + 2];
            int offset = 0;
            cmd[offset++] = 0x00;
            cmd[offset++] = INS_CHANGE_PIN;
            cmd[offset++] = 0x00;
            cmd[offset++] = 0x00;
            cmd[offset++] = (byte)(oldPin.length + newPin.length + 2);
            cmd[offset++] = (byte)oldPin.length;
            for (char c : oldPin) {
                cmd[offset++] = (byte)c;
            }
            cmd[offset++] = (byte)newPin.length;
            for (char c : newPin) {
                cmd[offset++] = (byte)c;
            }
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000) {
                showSuccess("Đổi PIN thành công!");
                log("✓ Đổi PIN thành công");
                oldPinField.setText("");
                newPinField.setText("");
                confirmPinField.setText("");
                isPinVerified = false;
            } else {
                showError("Đổi PIN thất bại! SW: " + String.format("%04X", getSW(resp)));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi đổi PIN: " + e.getMessage());
        } finally {
            Arrays.fill(oldPin, ' ');
            Arrays.fill(newPin, ' ');
            Arrays.fill(confirmPin, ' ');
        }
    }
    
    // Card Info Methods
    private void saveCardInfo() {
        if (!checkConnection()) return;
        if (!isPinVerified) {
            showError("Vui lòng xác thực PIN trước!");
            return;
        }
        
        String cardId = cardIdField.getText();
        String name = nameField.getText();
        String expiry = expiryDateField.getText();
        
        if (cardId.length() < 1 || name.length() < 1 || expiry.length() != 8) {
            showError("Thông tin không hợp lệ!");
            return;
        }
        
        try {
            byte[] nameBytes = name.getBytes("UTF-8");
            byte[] cmd = new byte[5 + cardId.length() + 1 + nameBytes.length + expiry.length()];
            int offset = 0;
            
            cmd[offset++] = 0x00;
            cmd[offset++] = INS_SET_CARD_INFO;
            cmd[offset++] = 0x00;
            cmd[offset++] = 0x00;
            cmd[offset++] = (byte)(cardId.length() + 1 + nameBytes.length + expiry.length());
            
            System.arraycopy(cardId.getBytes(), 0, cmd, offset, cardId.length());
            offset += cardId.length();
            
            cmd[offset++] = (byte)nameBytes.length;
            System.arraycopy(nameBytes, 0, cmd, offset, nameBytes.length);
            offset += nameBytes.length;
            
            System.arraycopy(expiry.getBytes(), 0, cmd, offset, expiry.length());
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000) {
                showSuccess("Lưu thông tin thành công!");
                log("✓ Lưu thông tin: " + cardId);
                isPinVerified = false;
            } else {
                showError("Lưu thông tin thất bại! SW: " + String.format("%04X", getSW(resp)));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi lưu thông tin: " + e.getMessage());
        }
    }
    
    private void loadCardInfo() {
        if (!checkConnection()) return;
        
        try {
            byte[] cmd = {0x00, INS_GET_CARD_INFO, 0x00, 0x00, 0x00};
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000 && resp.length > 2) {
                int offset = 0;
                
                // Card ID (assume 8 bytes based on test)
                int cardIdLen = 8;
                String cardId = new String(resp, offset, cardIdLen);
                cardIdField.setText(cardId.trim());
                offset += cardIdLen;
                
                // Name
                int nameLen = resp[offset++] & 0xFF;
                String name = new String(resp, offset, nameLen, "UTF-8");
                nameField.setText(name);
                offset += nameLen;
                
                // Expiry
                String expiry = new String(resp, offset, 8);
                expiryDateField.setText(expiry);
                
                showSuccess("Tải thông tin thành công!");
                log("✓ Tải thông tin: " + cardId);
            } else {
                showError("Tải thông tin thất bại!");
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi tải thông tin: " + e.getMessage());
        }
    }
    
    // Borrowing Methods
    private void borrowBook() {
        if (!checkConnection()) return;
        if (!isPinVerified) {
            showError("Vui lòng xác thực PIN trước!");
            return;
        }
        
        String bookId = bookIdField.getText();
        if (bookId.length() != 8) {
            showError("Mã sách phải có 8 ký tự!");
            return;
        }
        
        try {
            byte[] cmd = new byte[5 + 8];
            cmd[0] = 0x00;
            cmd[1] = INS_ADD_BORROWED_BOOK;
            cmd[2] = 0x00;
            cmd[3] = 0x00;
            cmd[4] = 0x08;
            System.arraycopy(bookId.getBytes(), 0, cmd, 5, 8);
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000) {
                showSuccess("Mượn sách thành công!");
                log("✓ Mượn sách: " + bookId);
                bookIdField.setText("");
                loadBorrowedBooks();
                isPinVerified = false;
            } else if (getSW(resp) == 0x6A84) {
                showError("Đã đạt giới hạn số sách mượn!");
            } else {
                showError("Mượn sách thất bại! SW: " + String.format("%04X", getSW(resp)));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi mượn sách: " + e.getMessage());
        }
    }
    
    private void returnBook() {
        if (!checkConnection()) return;
        if (!isPinVerified) {
            showError("Vui lòng xác thực PIN trước!");
            return;
        }
        
        int selectedRow = borrowedBooksTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Vui lòng chọn sách cần trả!");
            return;
        }
        
        String bookId = (String)tableModel.getValueAt(selectedRow, 1);
        
        try {
            byte[] cmd = new byte[5 + 8];
            cmd[0] = 0x00;
            cmd[1] = INS_RETURN_BOOK;
            cmd[2] = 0x00;
            cmd[3] = 0x00;
            cmd[4] = 0x08;
            System.arraycopy(bookId.getBytes(), 0, cmd, 5, 8);
            
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000) {
                showSuccess("Trả sách thành công!");
                log("✓ Trả sách: " + bookId);
                loadBorrowedBooks();
                isPinVerified = false;
            } else {
                showError("Trả sách thất bại! SW: " + String.format("%04X", getSW(resp)));
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi trả sách: " + e.getMessage());
        }
    }
    
    private void loadBorrowedBooks() {
        if (!checkConnection()) return;
        
        try {
            byte[] cmd = {0x00, INS_GET_BORROWED_BOOKS, 0x00, 0x00, 0x00};
            byte[] resp = sendCommand(cmd);
            
            if (getSW(resp) == 0x9000 && resp.length > 2) {
                tableModel.setRowCount(0);
                
                int numBooks = resp[0] & 0xFF;
                int offset = 1;
                
                for (int i = 0; i < numBooks; i++) {
                    String bookId = new String(resp, offset, 8);
                    tableModel.addRow(new Object[]{
                        i + 1,
                        bookId,
                        "Đang mượn"
                    });
                    offset += 8;
                }
                
                log("✓ Tải danh sách: " + numBooks + " cuốn");
            }
            
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
            log("✗ Lỗi tải danh sách: " + e.getMessage());
        }
    }
    
    // Utility Methods
    private boolean checkConnection() {
        if (!isConnected) {
            showError("Chưa khởi động simulator!");
            return false;
        }
        return true;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(message);
    }
    
    private void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        logArea.append("[" + sdf.format(new Date()) + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            LibraryCardSimulatorGUI gui = new LibraryCardSimulatorGUI();
            gui.setVisible(true);
        });
    }
}

