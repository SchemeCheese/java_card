package pages;

import api.PaymentApiService;
import api.PaymentApiService.PaymentResponse;
import api.SSEClient;
import com.google.gson.JsonObject;
import constants.AppConstants;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Dialog để hiển thị VietQR payment với QR code và SSE real-time updates
 */
public class VietQRPaymentDialog extends JDialog {
    private final PaymentApiService paymentApi;
    private final SSEClient sseClient;
    private final SimulatorService simulatorService; // [NEW]
    private final String cardId; // [NEW]
    private final long amount; // [NEW]
    
    private JLabel qrImageLabel;
    private JPanel amountPanel;
    private JPanel orderIdPanel;
    private JPanel statusPanel;
    private JProgressBar progressBar;
    private JButton closeButton;
    
    private String paymentId;
    private boolean paymentCompleted = false;
    
    public VietQRPaymentDialog(Frame parent, SimulatorService simulatorService, String cardId, long amount) {
        super(parent, "Thanh Toán VietQR", true);
        this.simulatorService = simulatorService;
        this.cardId = cardId;
        this.amount = amount;
        this.paymentApi = new PaymentApiService();
        this.sseClient = new SSEClient();
        
        initUI();
        setSize(500, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Tạo payment ngay khi dialog mở
        createPayment(amount, cardId);
    }
    

    
    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppConstants.BACKGROUND);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(AppConstants.BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        // Header
        JLabel titleLabel = new JLabel("Quét Mã QR Để Thanh Toán");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Sử dụng app ngân hàng để quét mã QR");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(AppConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // QR Code panel
        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(20, 20, 20, 20)
        ));
        qrPanel.setMaximumSize(new Dimension(400, 400));
        qrPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        qrImageLabel = new JLabel("Đang tải QR code...", SwingConstants.CENTER);
        qrImageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        qrImageLabel.setForeground(AppConstants.TEXT_SECONDARY);
        qrImageLabel.setPreferredSize(new Dimension(350, 350));
        qrPanel.add(qrImageLabel, BorderLayout.CENTER);
        
        // Payment info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(249, 250, 251));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
            new EmptyBorder(15, 20, 15, 20)
        ));
        infoPanel.setMaximumSize(new Dimension(400, 120));
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        amountPanel = createInfoRow("Số tiền:", "...");
        orderIdPanel = createInfoRow("Mã đơn hàng:", "...");
        statusPanel = createInfoRow("Trạng thái:", "Chờ thanh toán");
        
        infoPanel.add(amountPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(orderIdPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(statusPanel);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Close button
        closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(AppConstants.TEXT_SECONDARY);
        closeButton.setPreferredSize(new Dimension(120, 40));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        
        // Add components
        contentPanel.add(titleLabel);
       contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(qrPanel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(infoPanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(progressBar);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(closeButton);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createInfoRow(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(249, 250, 251));
        panel.setMaximumSize(new Dimension(400, 25));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelText.setForeground(AppConstants.TEXT_SECONDARY);
        
        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueText.setForeground(AppConstants.TEXT_PRIMARY);
        
        panel.add(labelText, BorderLayout.WEST);
        panel.add(valueText, BorderLayout.EAST);
        
        return panel;
    }
    
    private void updateInfoRow(JPanel panel, String newValue) {
        Component[] comps = panel.getComponents();
        if (comps.length >= 2 && comps[1] instanceof JLabel) {
            JLabel valueLabel = (JLabel)comps[1];
            valueLabel.setText(newValue);
            
            // Update color for status panel
            if (panel == statusPanel) {
                if (newValue.contains("Thành công")) {
                    valueLabel.setForeground(AppConstants.SUCCESS_COLOR);
                } else if (newValue.contains("Thất bại") || newValue.contains("Hết hạn")) {
                    valueLabel.setForeground(AppConstants.DANGER_COLOR);
                } else if (newValue.contains("Chờ")) {
                    valueLabel.setForeground(new Color(234, 179, 8)); // Gold
                }
            }
        }
    }
    
    private void createPayment(long amount, String cardId) {
        SwingWorker<PaymentResponse, Void> worker = new SwingWorker<PaymentResponse, Void>() {
            @Override
            protected PaymentResponse doInBackground() throws Exception {
                NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
                String description = String.format("Nap tien the thu vien - %s VND", fmt.format(amount));
                return paymentApi.createPayment(amount, description, cardId);
            }
            
            @Override
            protected void done() {
                try {
                    PaymentResponse response = get();
                    paymentId = response.paymentId;
                    
                    // Update UI
                    NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
                    updateInfoRow(amountPanel, fmt.format(response.amount) + " VND");
                    updateInfoRow(orderIdPanel, response.orderId);
                    
                    // Load và hiển thị QR code
                    loadQRCode(response.qrCode);
                    
                    // Kết nối SSE để nhận real-time updates
                    connectToSSE();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(VietQRPaymentDialog.this,
                        "Lỗi tạo thanh toán: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };
        worker.execute();
    }
    
    private void loadQRCode(String qrUrl) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                URL url = new URL(qrUrl);
                return ImageIO.read(url);
            }
            
            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null) {
                        // Scale image to fit
                        Image scaledImage = image.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                        qrImageLabel.setIcon(new ImageIcon(scaledImage));
                        qrImageLabel.setText("");
                    }
                } catch (Exception e) {
                    qrImageLabel.setText("Không thể tải QR code");
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Kết nối SSE để nhận real-time updates
     */
    private void connectToSSE() {
        if (paymentId == null) {
            System.err.println("[SSE] Cannot connect: paymentId is null");
            return;
        }
        
        System.out.println("[SSE] Connecting to payment stream: " + paymentId);
        
        sseClient.connectToPaymentStream(
            paymentId,
            this::handleSSEEvent,
            this::handleSSEError
        );
    }
    
    /**
     * Xử lý SSE event
     */
    private void handleSSEEvent(SSEClient.SSEEvent event) {
        System.out.println("[SSE] Received event: " + event.getEventType());
        
        switch (event.getEventType()) {
            case "connected":
                System.out.println("[SSE] Connected successfully");
                updateInfoRow(statusPanel, "Đã kết nối");
                break;
                
            case "payment-status":
                handlePaymentStatusEvent(event);
                break;
                
            case "heartbeat":
                // Keep alive, no action needed
                System.out.println("[SSE] Heartbeat received");
                break;
                
            default:
                System.out.println("[SSE] Unknown event: " + event.getEventType());
        }
    }
    
    /**
     * Xử lý payment status update event
     */
    private void handlePaymentStatusEvent(SSEClient.SSEEvent event) {
        try {
            JsonObject data = event.getDataAsJson();
            String status = data.get("status").getAsString();
            
            System.out.println("[SSE] Payment status: " + status);
            
            switch (status) {
                case "SUCCESS":
                    paymentCompleted = true;
                    sseClient.disconnect();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    
                    // [NEW] Cập nhật số dư vào thẻ (Encrypted)
                    boolean depositSuccess = simulatorService.deposit(cardId, amount);
                    
                    if (depositSuccess) {
                        updateInfoRow(statusPanel, "Thanh toán thành công!");
                        closeButton.setText("Hoàn tất");
                        closeButton.setBackground(AppConstants.SUCCESS_COLOR);
                        
                        JOptionPane.showMessageDialog(VietQRPaymentDialog.this,
                            "Thanh toán thành công!\nSố dư đã được cập nhật vào thẻ.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        updateInfoRow(statusPanel, "Lỗi cập nhật thẻ");
                        closeButton.setBackground(AppConstants.WARNING_COLOR);
                        
                        JOptionPane.showMessageDialog(VietQRPaymentDialog.this,
                            "Thanh toán thành công nhưng lỗi cập nhật thẻ!\nVui lòng kiểm tra kết nối với thẻ.",
                            "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    }
                    break;
                    
                case "FAILED":
                    paymentCompleted = true;
                    sseClient.disconnect();
                    progressBar.setIndeterminate(false);
                    updateInfoRow(statusPanel, "Thanh toán thất bại");
                    closeButton.setBackground(AppConstants.DANGER_COLOR);
                    break;
                    
                case "EXPIRED":
                    paymentCompleted = true;
                    sseClient.disconnect();
                    progressBar.setIndeterminate(false);
                    updateInfoRow(statusPanel, "Đã hết hạn");
                    closeButton.setBackground(AppConstants.DANGER_COLOR);
                    break;
                    
                case "PENDING":
                default:
                    updateInfoRow(statusPanel, "Chờ thanh toán...");
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("[SSE] Error handling payment status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Xử lý SSE error
     */
    private void handleSSEError(Exception error) {
        System.err.println("[SSE] Connection error: " + error.getMessage());
        
        if (!paymentCompleted) {
            SwingUtilities.invokeLater(() -> {
                updateInfoRow(statusPanel, "Mất kết nối");
                
                // Có thể fallback về polling ở đây nếu cần
                // hoặc thử reconnect
            });
        }
    }
    
    @Override
    public void dispose() {
        // Disconnect SSE khi đóng dialog
        if (sseClient.isConnected()) {
            sseClient.disconnect();
        }
        super.dispose();
    }
}
