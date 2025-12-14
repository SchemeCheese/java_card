package pages;

import api.ApiServiceManager;
import api.CardApiService;
import com.google.gson.JsonObject;
import constants.AppConstants;
import models.CardInfo;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Thông Tin Bạn Đọc Page - UI cải tiến
 * [UPDATED] Hiển thị thông tin từ thẻ sinh viên
 */
public class CardInfoPage extends JPanel {
    
    private SimulatorService simulatorService;
    private ApiServiceManager apiManager;
    private CardApiService cardApi;
    private CardInfo cardInfo;
    
    // Editable fields
    private JTextField txtHolderName;
    private JTextField txtBirthDate;
    private JTextField txtAddress;
    
    // Avatar components
    private JLabel avatarLabel;
    private ImageIcon currentImageIcon;
    
    public CardInfoPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.apiManager = ApiServiceManager.getInstance();
        this.cardApi = apiManager.getCardApiService();
        
        // Load card info từ API hoặc SimulatorService
        loadCardInfo();
        
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
    
    /**
     * Load card info từ API hoặc SimulatorService
     */
    private void loadCardInfo() {
        String currentStudentCode = simulatorService.getCurrentStudentCode();
        
        if (apiManager.isServerAvailable()) {
            try {
                // Load từ API
                CardInfo card = cardApi.getCard(currentStudentCode);
                if (card != null) {
                    this.cardInfo = card;
                    // Load ảnh từ server nếu có imagePath
                    if (card.getImagePath() != null && !card.getImagePath().isEmpty()) {
                        // loadImage sẽ tự động detect URL và load từ server
                        loadImage(card.getImagePath());
                    }
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error loading card from API: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Fallback về SimulatorService
        this.cardInfo = simulatorService.getCardByStudentCode(currentStudentCode);
        // Load ảnh nếu có
        if (this.cardInfo != null && this.cardInfo.getImagePath() != null && !this.cardInfo.getImagePath().isEmpty()) {
            loadImage(this.cardInfo.getImagePath());
        }
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
        
        // Avatar container with image or initial
        String imagePath = cardInfo != null && cardInfo.getImagePath() != null ? cardInfo.getImagePath() : "";
        
        JPanel avatarBox = new JPanel(new BorderLayout());
        avatarBox.setPreferredSize(new Dimension(160, 170));
        avatarBox.setMaximumSize(new Dimension(160, 170));
        avatarBox.setBorder(BorderFactory.createLineBorder(AppConstants.SUCCESS_COLOR, 3));
        avatarBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarBox.setBackground(new Color(243, 244, 246));
        
        // Avatar label to display image or initial
        avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Draw background
                g2.setColor(new Color(243, 244, 246));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                
                // If image exists, draw it
                if (currentImageIcon != null && currentImageIcon.getImage() != null) {
                    Image img = currentImageIcon.getImage();
                    int imgW = img.getWidth(null);
                    int imgH = img.getHeight(null);
                    
                    // Calculate scaling to fit while maintaining aspect ratio
                    double scale = Math.min((double)w / imgW, (double)h / imgH);
                    int scaledW = (int)(imgW * scale);
                    int scaledH = (int)(imgH * scale);
                    int x = (w - scaledW) / 2;
                    int y = (h - scaledH) / 2;
                    
                    g2.drawImage(img, x, y, scaledW, scaledH, null);
                } else {
                    // Draw initial letter - get current holder name from cardInfo
                    String holderName = cardInfo != null ? cardInfo.getHolderName() : "";
                    g2.setColor(AppConstants.PRIMARY_COLOR);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 60));
                    String initial = "";
                    if (holderName != null && !holderName.isEmpty()) {
                        if (holderName.contains(" ")) {
                            String[] parts = holderName.split(" ");
                            if (parts.length > 0 && !parts[parts.length - 1].isEmpty()) {
                                initial = parts[parts.length - 1].substring(0, 1).toUpperCase();
                            }
                        } else {
                            initial = holderName.substring(0, 1).toUpperCase();
                        }
                    }
                    if (!initial.isEmpty()) {
                        FontMetrics fm = g2.getFontMetrics();
                        int x = (w - fm.stringWidth(initial)) / 2;
                        int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(initial, x, y);
                    }
                }
                
                g2.dispose();
            }
        };
        avatarLabel.setPreferredSize(new Dimension(160, 170));
        avatarLabel.setOpaque(false);
        
        // Load image if path exists
        if (imagePath != null && !imagePath.isEmpty()) {
            loadImage(imagePath);
        }
        
        avatarBox.add(avatarLabel, BorderLayout.CENTER);
        
        // Upload/Change image button
        JButton uploadBtn = new JButton("Đổi ảnh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        uploadBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uploadBtn.setForeground(Color.WHITE);
        uploadBtn.setBackground(AppConstants.PRIMARY_COLOR);
        uploadBtn.setPreferredSize(new Dimension(140, 32));
        uploadBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        uploadBtn.setFocusPainted(false);
        uploadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploadBtn.setContentAreaFilled(false);
        uploadBtn.setOpaque(false);
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.addActionListener(e -> handleUploadImage());
        
        Color originalBtnColor = AppConstants.PRIMARY_COLOR;
        uploadBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                uploadBtn.setBackground(originalBtnColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                uploadBtn.setBackground(originalBtnColor);
            }
        });
        
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
        panel.add(Box.createVerticalStrut(10));
        panel.add(uploadBtn);
        panel.add(Box.createVerticalStrut(5));
        panel.add(idLabel);
        panel.add(statusLabel);
        panel.add(balanceLabel);
        
        return panel;
    }
    
    /**
     * Xử lý upload/đổi ảnh
     */
    private void handleUploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đại diện");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Ảnh (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Load and display image locally first
            if (loadImage(selectedFile.getAbsolutePath())) {
                // Upload to server
                try {
                    String studentCode = simulatorService.getCurrentStudentCode();
                    
                    if (apiManager != null && apiManager.isServerAvailable()) {
                        // Upload to server
                        System.out.println("[CardInfoPage] Uploading avatar to server...");
                        CardInfo updated = cardApi.uploadAvatar(studentCode, selectedFile);
                        
                        if (updated != null && updated.getImagePath() != null) {
                            // Update cardInfo với imagePath từ server
                            this.cardInfo = updated;
                            
                            // Load ảnh từ server URL
                            String imageUrl = api.ApiClient.SERVER_URL + "/" + updated.getImagePath();
                            loadImageFromUrl(imageUrl);
                            
                            // Update local service
                            if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                                simulatorService.setCardInfo(cardInfo);
                            } else {
                                simulatorService.addCardToList(cardInfo);
                            }
                            
                            JOptionPane.showMessageDialog(this,
                                "Đã upload ảnh đại diện lên server thành công!",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                    }
                    
                    // Fallback: Lưu local nếu server không available
                    if (cardInfo != null) {
                        cardInfo.setImagePath(selectedFile.getAbsolutePath());
                        
                        if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                            simulatorService.setCardInfo(cardInfo);
                        } else {
                            simulatorService.addCardToList(cardInfo);
                        }
                        
                        JOptionPane.showMessageDialog(this,
                            "Đã cập nhật ảnh đại diện (local)!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    System.err.println("[CardInfoPage] Error uploading avatar: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Lỗi khi upload ảnh: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Không thể tải ảnh! Vui lòng chọn file ảnh hợp lệ.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Load image from file path or URL
     */
    private boolean loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            currentImageIcon = null;
            if (avatarLabel != null) {
                avatarLabel.repaint();
            }
            return false;
        }
        
        try {
            // Check if it's a URL (starts with http:// or https://)
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return loadImageFromUrl(imagePath);
            }
            
            // Check if it's a server path (starts with uploads/avatars/)
            if (imagePath.startsWith("uploads/avatars/") || imagePath.startsWith("/uploads/avatars/")) {
                String url = api.ApiClient.SERVER_URL + "/" + imagePath.replaceAll("^/+", "");
                return loadImageFromUrl(url);
            }
            
            // Local file path
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                currentImageIcon = null;
                if (avatarLabel != null) {
                    avatarLabel.repaint();
                }
                return false;
            }
            
            ImageIcon icon = new ImageIcon(imagePath);
            Image image = icon.getImage();
            
            // Scale image to fit avatar size (160x170)
            int targetWidth = 160;
            int targetHeight = 170;
            
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);
            
            if (imgWidth > 0 && imgHeight > 0) {
                double scale = Math.min((double)targetWidth / imgWidth, (double)targetHeight / imgHeight);
                int scaledWidth = (int)(imgWidth * scale);
                int scaledHeight = (int)(imgHeight * scale);
                
                // Create scaled image
                BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = scaledImage.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                g2.dispose();
                
                currentImageIcon = new ImageIcon(scaledImage);
            } else {
                currentImageIcon = icon;
            }
            
            if (avatarLabel != null) {
                avatarLabel.repaint();
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            currentImageIcon = null;
            if (avatarLabel != null) {
                avatarLabel.repaint();
            }
            return false;
        }
    }
    
    /**
     * Load image from URL (server)
     */
    private boolean loadImageFromUrl(String imageUrl) {
        try {
            System.out.println("[CardInfoPage] Loading image from URL: " + imageUrl);
            
            // Download image from URL
            java.net.URL url = new java.net.URL(imageUrl);
            java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage(url);
            
            // Wait for image to load
            java.awt.MediaTracker tracker = new java.awt.MediaTracker(this);
            tracker.addImage(image, 0);
            try {
                tracker.waitForAll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            if (tracker.isErrorAny()) {
                System.err.println("[CardInfoPage] Error loading image from URL");
                currentImageIcon = null;
                if (avatarLabel != null) {
                    avatarLabel.repaint();
                }
                return false;
            }
            
            // Scale image to fit avatar size (160x170)
            int targetWidth = 160;
            int targetHeight = 170;
            
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);
            
            if (imgWidth > 0 && imgHeight > 0) {
                double scale = Math.min((double)targetWidth / imgWidth, (double)targetHeight / imgHeight);
                int scaledWidth = (int)(imgWidth * scale);
                int scaledHeight = (int)(imgHeight * scale);
                
                // Create scaled image
                BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = scaledImage.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                g2.dispose();
                
                currentImageIcon = new ImageIcon(scaledImage);
            } else {
                currentImageIcon = new ImageIcon(image);
            }
            
            if (avatarLabel != null) {
                avatarLabel.repaint();
            }
            
            System.out.println("[CardInfoPage] Image loaded successfully from URL");
            return true;
        } catch (Exception e) {
            System.err.println("[CardInfoPage] Error loading image from URL: " + e.getMessage());
            e.printStackTrace();
            currentImageIcon = null;
            if (avatarLabel != null) {
                avatarLabel.repaint();
            }
            return false;
        }
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
        // Image path is already saved when uploaded, no need to update here
        
        // Save to API hoặc SimulatorService
        try {
            String studentCode = simulatorService.getCurrentStudentCode();
            
            if (apiManager.isServerAvailable()) {
                try {
                    // Lưu qua API
                    JsonObject updates = new JsonObject();
                    updates.addProperty("holderName", holderName);
                    if (!birthDate.isEmpty()) {
                        updates.addProperty("birthDate", birthDate);
                    }
                    if (!address.isEmpty()) {
                        updates.addProperty("address", address);
                    }
                    
                    CardInfo updated = cardApi.updateCard(studentCode, updates);
                    if (updated != null) {
                        this.cardInfo = updated;
                        // Refresh avatar display
                        if (avatarLabel != null) {
                            avatarLabel.repaint();
                        }
                        JOptionPane.showMessageDialog(this,
                            "Đã lưu thông tin thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                } catch (Exception apiEx) {
                    System.err.println("Error saving to API: " + apiEx.getMessage());
                    // Fallback to SimulatorService
                }
            }
            
            // Fallback về SimulatorService
            if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                boolean saved = simulatorService.setCardInfo(cardInfo);
                if (saved) {
                    // Refresh avatar display
                    if (avatarLabel != null) {
                        avatarLabel.repaint();
                    }
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
                // Refresh avatar display
                if (avatarLabel != null) {
                    avatarLabel.repaint();
                }
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
