package pages;

import constants.AppConstants;
import models.CardInfo;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Settings Page - Quản trị hệ thống
 * [UPDATED] Tích hợp lưu/đọc dữ liệu thực từ JavaCard
 */
public class SettingsPage extends JPanel {
    
    private SimulatorService simulatorService;
    
    // Form fields
    private JTextField txtStudentId;
    private JTextField txtName;
    private JTextField txtEmail;
    private JTextField txtDepartment;
    private JTextField txtBirthDate;
    private JTextField txtAddress;
    private JTextField txtSearch;
    
    // Card list panel
    private JPanel cardListPanel;
    
    // Activity log
    private DefaultTableModel logTableModel;
    private List<String[]> activityLog = new ArrayList<>();
    
    public SettingsPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        
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
        
        // Load existing cards
        refreshCardList();
    }
    
    // Constructor mặc định cho tương thích ngược
    public SettingsPage() {
        this(null);
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
        
        // Create new card - fixed width
        JPanel leftCard = createNewCardSection();
        leftCard.setPreferredSize(new Dimension(420, 580));
        leftCard.setMinimumSize(new Dimension(400, 560));
        leftCard.setMaximumSize(new Dimension(450, 600));
        
        // Card Management - flexible width
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
        
        panel.add(title);
        panel.add(desc);
        
        // Student ID
        panel.add(createFieldLabel("Mã Số Sinh Viên"));
        txtStudentId = createTextField("Nhập MSSV");
        // Add listener to auto-fill email and department when student ID is entered
        txtStudentId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleStudentIdChange());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleStudentIdChange());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> handleStudentIdChange());
            }
        });
        panel.add(txtStudentId);
        
        // Name
        panel.add(createFieldLabel("Họ và Tên"));
        txtName = createTextField("Nhập họ tên sinh viên");
        panel.add(txtName);
        
        // Email
        panel.add(createFieldLabel("Email"));
        txtEmail = createTextField("email@example.com");
        panel.add(txtEmail);
        
        // Department
        panel.add(createFieldLabel("Khoa / Viện"));
        txtDepartment = createTextField("Nhập khoa / viện");
        panel.add(txtDepartment);
        
        // Birth Date
        panel.add(createFieldLabel("Ngày Sinh"));
        txtBirthDate = createTextField("DD/MM/YYYY");
        // Add auto-format and validation for date format
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
        txtBirthDate.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                validateBirthDate();
            }
        });
        panel.add(txtBirthDate);
        
        // Address
        panel.add(createFieldLabel("Địa Chỉ"));
        txtAddress = createTextField("Nhập địa chỉ");
        panel.add(txtAddress);
        
        panel.add(Box.createVerticalStrut(12));
        
        // Submit button
        JButton submitBtn = createButton("Tạo Thẻ Mới", AppConstants.SUCCESS_COLOR, Color.WHITE, Integer.MAX_VALUE, 40);
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        submitBtn.addActionListener(e -> handleCreateCard());
        
        panel.add(submitBtn);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(AppConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(4, 0, 3, 0));
        return lbl;
    }
    
    private void handleCreateCard() {
        // Validate fields
        String studentId = getFieldValue(txtStudentId, "Nhập MSSV");
        String name = getFieldValue(txtName, "Nhập họ tên sinh viên");
        String email = getFieldValue(txtEmail, "email@example.com");
        String department = getFieldValue(txtDepartment, "Nhập khoa / viện");
        String birthDate = getFieldValue(txtBirthDate, "DD/MM/YYYY");
        String address = getFieldValue(txtAddress, "Nhập địa chỉ");
        
        if (studentId.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập MSSV và Họ tên!",
                "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Validate ngày sinh
        if (!birthDate.isEmpty() && !validateBirthDate()) {
            JOptionPane.showMessageDialog(this,
                "Ngày sinh không hợp lệ! Vui lòng nhập theo định dạng DD/MM/YYYY và đảm bảo ngày hợp lệ.",
                "Lỗi định dạng", JOptionPane.WARNING_MESSAGE);
            txtBirthDate.requestFocus();
            return;
        }
        
        try {
            // Create CardInfo object
            CardInfo cardInfo = new CardInfo(studentId, name, email, department, birthDate, address);
            
            // Save to JavaCard (if connected and PIN verified)
            if (simulatorService != null && simulatorService.isConnected() && simulatorService.isPinVerified()) {
                boolean saved = simulatorService.setCardInfo(cardInfo);
                if (saved) {
                    // Also add to in-memory list for display
                    simulatorService.addCardToList(cardInfo);
                    addActivityLog("Tạo thẻ mới", studentId, "Thành công");
                    
                    JOptionPane.showMessageDialog(this,
                        "Đã lưu thông tin thẻ thành công!\n" +
                        "MSSV: " + studentId + "\n" +
                        "Họ tên: " + name,
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    
                    clearForm();
                    refreshCardList();
                } else {
                    addActivityLog("Tạo thẻ mới", studentId, "Thất bại");
                    JOptionPane.showMessageDialog(this,
                        "Không thể lưu thông tin thẻ!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Save to in-memory only (demo mode)
                if (simulatorService != null) {
                    simulatorService.addCardToList(cardInfo);
                }
                addActivityLog("Tạo thẻ mới", studentId, "Thành công");
                
                JOptionPane.showMessageDialog(this,
                    "Đã tạo thẻ mới (chế độ demo)!\n" +
                    "Lưu ý: Để lưu vào thẻ thật, vui lòng xác thực PIN trước.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                
                clearForm();
                refreshCardList();
            }
        } catch (Exception ex) {
            addActivityLog("Tạo thẻ mới", studentId, "Thất bại");
            JOptionPane.showMessageDialog(this,
                "Lỗi khi tạo thẻ: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private String getFieldValue(JTextField field, String placeholder) {
        String value = field.getText().trim();
        if (value.equals(placeholder)) {
            return "";
        }
        return value;
    }
    
    /**
     * Xác định khoa/viện dựa trên 2 ký tự đầu của MSSV
     * CT → CNTT, DT → DTTT, AT → ATTT
     */
    private String getDepartmentFromStudentId(String studentId) {
        if (studentId == null || studentId.length() < 2) {
            return "";
        }
        
        String prefix = studentId.substring(0, 2).toUpperCase();
        switch (prefix) {
            case "CT":
                return "CNTT";
            case "DT":
                return "DTTT";
            case "AT":
                return "ATTT";
            default:
                return "";
        }
    }
    
    /**
     * Tự động điền email và khoa khi nhập mã sinh viên
     */
    private void handleStudentIdChange() {
        String studentId = txtStudentId.getText().trim();
        String placeholder = "Nhập MSSV";
        
        // Chỉ tự động điền khi người dùng đã nhập và không phải placeholder
        if (!studentId.isEmpty() && !studentId.equals(placeholder)) {
            // Tự động điền email: MSSV + @actvn.edu.vn
            String emailPlaceholder = "email@example.com";
            String currentEmail = txtEmail.getText().trim();
            
            // Kiểm tra nếu email đang là placeholder, rỗng, hoặc là email tự động từ MSSV trước đó
            // Nếu email kết thúc bằng @actvn.edu.vn, có thể là email tự động nên cần cập nhật
            boolean isAutoEmail = currentEmail.endsWith("@actvn.edu.vn");
            boolean shouldUpdateEmail = currentEmail.isEmpty() || 
                                       currentEmail.equals(emailPlaceholder) ||
                                       isAutoEmail;
            
            if (shouldUpdateEmail) {
                String autoEmail = studentId + "@actvn.edu.vn";
                // Chỉ cập nhật nếu khác với giá trị hiện tại để tránh flickering
                if (!autoEmail.equals(currentEmail)) {
                    txtEmail.setText(autoEmail);
                    txtEmail.setForeground(AppConstants.TEXT_PRIMARY);
                }
            }
            
            // Tự động điền khoa dựa trên 2 ký tự đầu của MSSV
            String deptPlaceholder = "Nhập khoa / viện";
            String currentDept = txtDepartment.getText().trim();
            
            // Chỉ tự động điền nếu khoa đang là placeholder hoặc rỗng
            if (currentDept.isEmpty() || currentDept.equals(deptPlaceholder)) {
                String department = getDepartmentFromStudentId(studentId);
                if (!department.isEmpty()) {
                    txtDepartment.setText(department);
                    txtDepartment.setForeground(AppConstants.TEXT_PRIMARY);
                }
            } else {
                // Nếu khoa đã được điền, kiểm tra xem có phải là khoa tự động không
                // Nếu là khoa tự động từ MSSV trước đó, cập nhật lại
                String expectedDept = getDepartmentFromStudentId(studentId);
                if (!expectedDept.isEmpty() && 
                    (currentDept.equals("CNTT") || currentDept.equals("DTTT") || currentDept.equals("ATTT"))) {
                    txtDepartment.setText(expectedDept);
                    txtDepartment.setForeground(AppConstants.TEXT_PRIMARY);
                }
            }
        } else {
            // Nếu MSSV bị xóa, có thể reset email về placeholder nếu là email tự động
            String currentEmail = txtEmail.getText().trim();
            if (currentEmail.endsWith("@actvn.edu.vn")) {
                txtEmail.setText("email@example.com");
                txtEmail.setForeground(Color.GRAY);
            }
        }
    }
    
    /**
     * Tự động format ngày sinh khi người dùng nhập (DD/MM/YYYY)
     * Tự động thêm dấu "/" sau 2 số đầu và sau 2 số tiếp theo
     */
    private void formatBirthDate() {
        String text = txtBirthDate.getText();
        String placeholder = "DD/MM/YYYY";
        
        // Bỏ qua nếu là placeholder hoặc đang focus và là placeholder
        if (text.equals(placeholder) || text.isEmpty()) {
            return;
        }
        
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
        
        // Chỉ cập nhật nếu khác với giá trị hiện tại để tránh vòng lặp vô hạn
        String newText = formatted.toString();
        if (!newText.equals(text)) {
            // Lưu vị trí cursor
            int newCaretPos = caretPosition;
            
            // Điều chỉnh vị trí cursor sau khi format
            if (newText.length() > text.length()) {
                // Nếu text dài hơn, cursor sẽ di chuyển theo số lượng dấu "/" được thêm
                int slashesAdded = countOccurrences(newText, '/') - countOccurrences(text, '/');
                newCaretPos += slashesAdded;
            } else if (newText.length() < text.length()) {
                // Nếu text ngắn hơn, điều chỉnh cursor
                newCaretPos = Math.min(newCaretPos, newText.length());
            }
            
            txtBirthDate.setText(newText);
            txtBirthDate.setForeground(AppConstants.TEXT_PRIMARY);
            
            // Khôi phục vị trí cursor
            if (newCaretPos >= 0 && newCaretPos <= newText.length()) {
                txtBirthDate.setCaretPosition(newCaretPos);
            }
        }
    }
    
    /**
     * Đếm số lần xuất hiện của ký tự trong chuỗi
     */
    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Validate định dạng ngày sinh (DD/MM/YYYY)
     */
    private boolean validateBirthDate() {
        String birthDate = txtBirthDate.getText().trim();
        String placeholder = "DD/MM/YYYY";
        
        // Bỏ qua nếu là placeholder hoặc rỗng
        if (birthDate.isEmpty() || birthDate.equals(placeholder)) {
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
            
            // Kiểm tra ngày có tồn tại không (ví dụ: 31/02/2000)
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
    
    private void clearForm() {
        txtStudentId.setText("Nhập MSSV");
        txtStudentId.setForeground(Color.GRAY);
        txtName.setText("Nhập họ tên sinh viên");
        txtName.setForeground(Color.GRAY);
        txtEmail.setText("email@example.com");
        txtEmail.setForeground(Color.GRAY);
        txtDepartment.setText("Nhập khoa / viện");
        txtDepartment.setForeground(Color.GRAY);
        txtBirthDate.setText("DD/MM/YYYY");
        txtBirthDate.setForeground(Color.GRAY);
        txtAddress.setText("Nhập địa chỉ");
        txtAddress.setForeground(Color.GRAY);
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
        
        txtSearch = createTextField("Tìm theo MSSV hoặc Tên...");
        txtSearch.setPreferredSize(new Dimension(220, 36));
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                handleSearch();
            }
        });
        
        header.add(title, BorderLayout.WEST);
        header.add(txtSearch, BorderLayout.EAST);
        
        panel.add(header);
        panel.add(Box.createVerticalStrut(12));
        
        // Card list panel (scrollable)
        cardListPanel = new JPanel();
        cardListPanel.setLayout(new BoxLayout(cardListPanel, BoxLayout.Y_AXIS));
        cardListPanel.setBackground(Color.WHITE);
        
        JScrollPane cardScroll = new JScrollPane(cardListPanel);
        cardScroll.setBorder(null);
        cardScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(cardScroll);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private void handleSearch() {
        String keyword = getFieldValue(txtSearch, "Tìm theo MSSV hoặc Tên...");
        refreshCardList(keyword);
    }
    
    private void refreshCardList() {
        refreshCardList("");
    }
    
    private void refreshCardList(String keyword) {
        cardListPanel.removeAll();
        
        List<CardInfo> cards;
        if (simulatorService != null) {
            if (keyword.isEmpty()) {
                cards = simulatorService.getAllCards();
            } else {
                cards = simulatorService.searchCards(keyword);
            }
        } else {
            cards = new ArrayList<>();
        }
        
        if (cards.isEmpty()) {
            JLabel emptyLabel = new JLabel("Chưa có thẻ nào được tạo");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(AppConstants.TEXT_SECONDARY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardListPanel.add(Box.createVerticalStrut(50));
            cardListPanel.add(emptyLabel);
        } else {
            for (CardInfo card : cards) {
                cardListPanel.add(createCardItem(card));
                cardListPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        cardListPanel.revalidate();
        cardListPanel.repaint();
    }
    
    private JPanel createCardItem(CardInfo card) {
        String mssv = card.getStudentId();
        String name = card.getHolderName();
        String dept = card.getDepartment();
        String status = card.getStatus();
        
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
        
        // Avatar
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppConstants.PRIMARY_COLOR);
                g2.fillOval(0, 0, 40, 40);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String initial = "";
                if (name.contains(" ")) {
                    initial = name.substring(name.lastIndexOf(" ") + 1, name.lastIndexOf(" ") + 2).toUpperCase();
                } else if (!name.isEmpty()) {
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
        JLabel detailLabel = new JLabel("MSSV: " + mssv + " | " + (dept.isEmpty() ? "N/A" : dept));
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
        toggleBtn.addActionListener(e -> handleToggleStatus(card));
        
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(toggleBtn, gbc);
        
        // View details on click
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showCardDetails(card);
                }
            }
        });
        
        return panel;
    }
    
    private void handleToggleStatus(CardInfo card) {
        String action = card.getStatus().equals("Hoạt động") ? "Khóa thẻ" : "Mở khóa thẻ";
        
        if (simulatorService != null) {
            simulatorService.toggleCardStatus(card.getStudentId());
            addActivityLog(action, card.getStudentId(), "Thành công");
            refreshCardList();
        }
    }
    
    private void showCardDetails(CardInfo card) {
        String message = String.format(
            "MSSV: %s\n" +
            "Họ tên: %s\n" +
            "Email: %s\n" +
            "Khoa/Viện: %s\n" +
            "Ngày sinh: %s\n" +
            "Địa chỉ: %s\n" +
            "Trạng thái: %s\n" +
            "Số sách đang mượn: %d",
            card.getStudentId(),
            card.getHolderName(),
            card.getEmail().isEmpty() ? "N/A" : card.getEmail(),
            card.getDepartment().isEmpty() ? "N/A" : card.getDepartment(),
            card.getBirthDate().isEmpty() ? "N/A" : card.getBirthDate(),
            card.getAddress().isEmpty() ? "N/A" : card.getAddress(),
            card.getStatus(),
            card.getBorrowedBooks()
        );
        
        JOptionPane.showMessageDialog(this, message, "Chi tiết thẻ", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void addActivityLog(String action, String mssv, String status) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy"));
        activityLog.add(0, new String[]{time, action, mssv, status});
        
        if (logTableModel != null) {
            logTableModel.insertRow(0, new Object[]{time, action, mssv, status});
            // Keep only last 50 entries
            while (logTableModel.getRowCount() > 50) {
                logTableModel.removeRow(logTableModel.getRowCount() - 1);
            }
        }
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
        refreshBtn.addActionListener(e -> {
            refreshCardList();
            // Try to read card info from JavaCard
            if (simulatorService != null && simulatorService.isConnected()) {
                try {
                    CardInfo cardInfo = simulatorService.getCardInfo();
                    if (cardInfo.isInitialized()) {
                        addActivityLog("Đọc thẻ", cardInfo.getStudentId(), "Thành công");
                        JOptionPane.showMessageDialog(this,
                            "Đã đọc thông tin thẻ từ JavaCard:\n" + cardInfo.toString(),
                            "Thông tin thẻ", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    // Card might not have info yet
                    System.out.println("No card info available: " + ex.getMessage());
                }
            }
        });
        
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        
        // Table
        String[] cols = {"Thời Gian", "Hành Động", "MSSV", "Trạng Thái"};
        logTableModel = new DefaultTableModel(cols, 0);
        
        JTable table = new JTable(logTableModel);
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
        field.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
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
