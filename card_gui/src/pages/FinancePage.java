package pages;

import api.ApiServiceManager;
import api.BookApiService;
import api.CardApiService;
import api.TransactionApiService;
import constants.AppConstants;
import models.BorrowedBook;
import models.CardInfo;
import models.FinePaymentResult;
import models.Transaction;
import service.SimulatorService;
import ui.RoundedBorder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tài Chính Page - UI cải tiến
 * [UPDATED] Tích hợp SimulatorService
 */
public class FinancePage extends JPanel {
    
    private SimulatorService simulatorService;
    private ApiServiceManager apiManager;
    private CardApiService cardApi;
    private BookApiService bookApi;
    private TransactionApiService transactionApi;
    private String studentCode;
    private long balance;
    private List<Transaction> transactions;
    private List<BorrowedBook> outstandingFines;
    private JLabel balanceValue;
    private DefaultTableModel historyModel;
    private DefaultTableModel finesModel;
    private JTable finesTable; // Add reference to fines table
    private static final Color GOLD_COLOR = new Color(234, 179, 8);
    
    public FinancePage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.apiManager = ApiServiceManager.getInstance();
        this.cardApi = apiManager.getCardApiService();
        this.bookApi = apiManager.getBookApiService();
        this.transactionApi = apiManager.getTransactionApiService();
        this.studentCode = simulatorService.getCurrentStudentCode();
        
        // Load data từ API hoặc SimulatorService
        loadFinanceData();
        loadOutstandingFines();
        
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(AppConstants.BACKGROUND);
        
        // Header
        mainPanel.add(createHeader());
        mainPanel.add(Box.createVerticalStrut(25));
        
        // Top row - Balance + Topup
        mainPanel.add(createTopRow());
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Fines section
        mainPanel.add(createFinesSection());
        mainPanel.add(Box.createVerticalStrut(20));
        
        // History section
        mainPanel.add(createHistorySection());
        
        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scroll, BorderLayout.CENTER);
    }
    
    /**
     * Load finance data từ API hoặc SimulatorService
     */
    private void loadFinanceData() {
        // [UPDATED] Ưu tiên lấy số dư từ thẻ nếu đã kết nối và xác thực PIN
        if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
            this.balance = simulatorService.getBalance(studentCode);
        } else if (apiManager.isServerAvailable()) {
            try {
                // Load balance từ API (nếu chưa kết nối thẻ)
                CardInfo card = cardApi.getCard(studentCode);
                if (card != null) {
                    this.balance = card.getBalance();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Load từ SimulatorService (Mock/Offline)
            this.balance = simulatorService.getBalance(studentCode);
        }

        // Load transactions (vẫn lấy từ API/Memory như cũ)
        if (apiManager.isServerAvailable()) {
            try {
                List<Transaction> apiTransactions = transactionApi.getTransactionsByStudent(
                    studentCode, null, null, null, null, 1, 50
                );
                if (apiTransactions != null) {
                    this.transactions = apiTransactions;
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.transactions = new ArrayList<>();
            }
        } else {
            this.transactions = simulatorService.getTransactions(studentCode);
        }
        
        // [FIX] Update UI after loading data
        if (balanceValue != null) {
            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            balanceValue.setText(fmt.format(balance) + " VND");
        }
        if (historyModel != null) {
            refreshHistoryTable();
        }
    }


    /**
     * Load outstanding fines from API (server-backed).
     */
    private void loadOutstandingFines() {
        this.outstandingFines = new ArrayList<>();

        if (!apiManager.isServerAvailable()) {
            return;
        }

        try {
            List<BorrowedBook> fines = bookApi.getOutstandingFinesByStudent(studentCode);
            if (fines != null) {
                this.outstandingFines = fines;
            }
            if (finesModel != null) {
                refreshFinesTable();
            }
        } catch (Exception e) {
            System.err.println("Error loading outstanding fines from API: " + e.getMessage());
        }
    }

    private void refreshFinesTable() {
        if (finesModel == null) return;
        finesModel.setRowCount(0);

        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        for (BorrowedBook b : outstandingFines) {
            String reason;
            if (b.getOverdueDays() > 0) {
                reason = "Trả sách muộn (" + b.getOverdueDays() + " ngày)";
            } else {
                reason = "Phí phạt";
            }
            String amount = fmt.format(Math.max(0, b.getFine())) + " VND";
            String status;
            if ("Quá hạn".equalsIgnoreCase(b.getStatus())) {
                status = "Tạm tính (chưa trả sách)";
            } else if (b.isFinePaid()) {
                status = "Đã thanh toán";
            } else {
                status = "Chưa thanh toán";
            }

            // Add checkbox column (false by default), then other data
            boolean canPay = !b.isFinePaid() && "Đã trả".equalsIgnoreCase(b.getStatus());
            finesModel.addRow(new Object[]{canPay ? false : null, b.getBookId(), reason, amount, status});
        }
    }
    
    /**
     * Refresh history table
     */
    private void refreshHistoryTable() {
        if (historyModel == null) return;
        historyModel.setRowCount(0);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        for (Transaction t : transactions) {
            String amtStr;
            boolean isFine = "Trả phạt".equalsIgnoreCase(t.getType());
            if (isFine) {
                amtStr = "- " + fmt.format(Math.abs(t.getAmount())) + " VND";
            } else {
                amtStr = (t.getAmount() >= 0 ? "+ " : "- ") + fmt.format(Math.abs(t.getAmount())) + " VND";
            }
            historyModel.addRow(new Object[]{t.getDate(), t.getType(), amtStr, t.getStatus()});
        }
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(AppConstants.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        // Money icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GOLD_COLOR);
                g2.fillOval(4, 4, 24, 24);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.drawString("$", 12, 21);
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(32, 32));
        iconPanel.setBackground(AppConstants.BACKGROUND);
        
        JLabel title = new JLabel("Tài Chính");
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
        
        JLabel subtitle = new JLabel("Quản lý số dư, tiền phạt và lịch sử giao dịch của bạn.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(AppConstants.TEXT_SECONDARY);
        subtitle.setBorder(new EmptyBorder(8, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        wrapper.add(panel);
        wrapper.add(subtitle);
        
        return wrapper;
    }
    
    private JPanel createTopRow() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(AppConstants.BACKGROUND);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        panel.setMinimumSize(new Dimension(600, 150));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Balance card
        JPanel balanceCard = new JPanel();
        balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
        balanceCard.setBackground(Color.WHITE);
        balanceCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(25, 30, 25, 30)
        ));
        
        JLabel balanceLabel = new JLabel("Số Dư Hiện Tại");
        balanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        balanceLabel.setForeground(AppConstants.TEXT_SECONDARY);
        balanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        balanceValue = new JLabel(fmt.format(balance) + " VND");
        balanceValue.setFont(new Font("Segoe UI", Font.BOLD, 32));
        balanceValue.setForeground(AppConstants.SUCCESS_COLOR);
        balanceValue.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceValue.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        balanceCard.add(balanceLabel);
        balanceCard.add(balanceValue);
        
        // Topup card
        JPanel topupCard = new JPanel();
        topupCard.setLayout(new BoxLayout(topupCard, BoxLayout.Y_AXIS));
        topupCard.setBackground(Color.WHITE);
        topupCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(20, 25, 20, 25)
        ));
        
        JLabel topupTitle = new JLabel("Nạp Tiền");
        topupTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topupTitle.setForeground(AppConstants.TEXT_PRIMARY);
        topupTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel amountLabel = new JLabel("Số Tiền (VND)");
        amountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        amountLabel.setForeground(AppConstants.TEXT_SECONDARY);
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        amountLabel.setBorder(new EmptyBorder(10, 0, 6, 0));
        
        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(12, 0));
        inputRow.setBackground(Color.WHITE);
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        
        JTextField amountField = createTextField("Nhập số tiền bạn muốn nạp");
        JButton topupBtn = createButton("Nạp Tiền", AppConstants.PRIMARY_COLOR, Color.WHITE, 110, 42);
        topupBtn.addActionListener(e -> {
            // Mở VietQR payment dialog
            String raw = amountField.getText().trim();
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Vui lòng nhập số tiền hợp lệ!", 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            long value = Long.parseLong(digits);
            if (value <= 0) {
                JOptionPane.showMessageDialog(this,
                    "Số tiền phải lớn hơn 0!",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Open VietQR Payment Dialog
            VietQRPaymentDialog dialog = new VietQRPaymentDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                simulatorService,
                studentCode,
                value
            );
            dialog.setVisible(true);
            
            // Refresh balance
            loadFinanceData();
        });
        
        inputRow.add(amountField, BorderLayout.CENTER);
        inputRow.add(topupBtn, BorderLayout.EAST);
        
        // Quick amounts
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickPanel.setBackground(Color.WHITE);
        quickPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        String[] amounts = {"50,000", "100,000", "200,000", "500,000"};
        for (String amt : amounts) {
            JButton btn = new JButton(amt);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setForeground(AppConstants.TEXT_PRIMARY);
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
                new EmptyBorder(6, 12, 6, 12)
            ));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.setBackground(new Color(249, 250, 251));
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.setBackground(Color.WHITE);
                }
            });
            
            // Nạp nhanh bằng VietQR
            btn.addActionListener(e -> {
                amountField.setText(amt);
                String digits = amt.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    long value = Long.parseLong(digits);
                    
                    // Open VietQR Payment Dialog
                    VietQRPaymentDialog dialog = new VietQRPaymentDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(this),
                        simulatorService,
                        studentCode,
                        value
                    );
                    dialog.setVisible(true);
                    
                    // Refresh balance
                    loadFinanceData();
                }
            });
            
            quickPanel.add(btn);
        }
        
        topupCard.add(topupTitle);
        topupCard.add(amountLabel);
        topupCard.add(inputRow);
        topupCard.add(quickPanel);
        
        panel.add(balanceCard);
        panel.add(topupCard);
        
        return panel;
    }
    
    private JPanel createFinesSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(25, 30, 25, 30)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        panel.setMinimumSize(new Dimension(600, 180));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel title = new JLabel("Tiền Phạt Cần Thanh Toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);

        JLabel summary = new JLabel("");
        summary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summary.setForeground(AppConstants.TEXT_SECONDARY);
        
        JButton payBtn = createButton("Thanh Toán Phạt", AppConstants.DANGER_COLOR, Color.WHITE, 160, 38);
        
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setBackground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftHeader.add(title);
        leftHeader.add(Box.createVerticalStrut(4));
        leftHeader.add(summary);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(payBtn, BorderLayout.EAST);
        
        // Table with checkbox column
        String[] cols = {"", "Mã Sách", "Lý Do Phạt", "Số Tiền", "Trạng Thái"};
        finesModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                // Chỉ cho phép tick checkbox nếu chưa thanh toán và đã trả sách
                if (column == 0) {
                    String status = (String) getValueAt(row, 4);
                    return !"Đã thanh toán".equals(status);
                }
                return false;
            }
        };

        JTable table = new JTable(finesModel);
        this.finesTable = table; // Save reference
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(AppConstants.TEXT_PRIMARY);
        table.setBackground(Color.WHITE);
        table.setRowHeight(45);
        table.setShowGrid(false);
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(AppConstants.TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(AppConstants.TEXT_SECONDARY);
        table.getTableHeader().setBackground(Color.WHITE);
        
        // Set checkbox column width
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setMinWidth(40);
        
        // Status renderer
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, 
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = new JLabel(val != null ? val.toString() : "");
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                if (val != null && val.toString().equals("Đã thanh toán")) {
                    lbl.setForeground(new Color(22, 101, 52));
                } else {
                    lbl.setForeground(new Color(220, 38, 38));
                }
                if (!sel) {
                    lbl.setBackground(Color.WHITE);
                }
                return lbl;
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        // Initial load
        refreshFinesTable();

        // Summary text
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        long total = 0;
        int payableCount = 0;
        if (outstandingFines != null) {
            for (BorrowedBook b : outstandingFines) {
                total += Math.max(0, b.getFine());
                if (!b.isFinePaid() && "Đã trả".equalsIgnoreCase(b.getStatus()) && b.getFine() > 0) {
                    payableCount++;
                }
            }
        }
        summary.setText("Tổng: " + fmt.format(total) + " VND • Có thể thanh toán: " + payableCount + " khoản");

        // Pay button handler - thanh toán các khoản đã chọn
        payBtn.addActionListener(e -> {
            if (!apiManager.isServerAvailable()) {
                JOptionPane.showMessageDialog(this,
                    "Chức năng thanh toán phạt cần kết nối server.",
                    "Không thể thực hiện", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (outstandingFines == null || outstandingFines.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Bạn không có khoản phạt nào cần thanh toán.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Thu thập các khoản phạt đã được chọn (checkbox = true)
            List<BorrowedBook> selectedFines = new ArrayList<>();
            for (int i = 0; i < finesTable.getRowCount(); i++) {
                Object checkboxValue = finesTable.getValueAt(i, 0);
                if (checkboxValue instanceof Boolean && (Boolean) checkboxValue) {
                    // Tìm BorrowedBook tương ứng với dòng này
                    String bookId = (String) finesTable.getValueAt(i, 1);
                    for (BorrowedBook b : outstandingFines) {
                        if (b.getBookId().equals(bookId) && !b.isFinePaid()) {
                            selectedFines.add(b);
                            break;
                        }
                    }
                }
            }

            if (selectedFines.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn ít nhất một khoản phạt để thanh toán.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tính tổng tiền sẽ thanh toán
            long totalAmount = 0;
            for (BorrowedBook b : selectedFines) {
                totalAmount += Math.max(0, b.getFine());
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có muốn thanh toán %d khoản phạt?\nTổng tiền: %,d VND", 
                    selectedFines.size(), totalAmount),
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                // Check balance trước (ưu tiên từ card nếu có)
                long currentBalance = 0;
                
                if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                    currentBalance = simulatorService.getBalance(studentCode);
                } else {
                    // Fallback to memory/API
                    models.CardInfo card = simulatorService.getCardByStudentCode(studentCode);
                    if (card != null) {
                        currentBalance = card.getBalance();
                    }
                }
                
                if (currentBalance < totalAmount) {
                    JOptionPane.showMessageDialog(this,
                        String.format("Số dư không đủ để thanh toán tiền phạt.\nSố dư: %,d VND\nCần thanh toán: %,d VND",
                            currentBalance, totalAmount),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                
                FinePaymentResult result = bookApi.payOutstandingFines(studentCode);
                
                // [NEW] Trừ tiền trên thẻ nếu có kết nối
                if (simulatorService.isConnected() && simulatorService.isPinVerified()) {
                    simulatorService.payFine(studentCode, totalAmount);
                }

                // Refresh balance + transactions
                loadFinanceData();
                // Refresh fines list
                loadOutstandingFines();

                // Update summary
                long newTotal = 0;
                int newPayable = 0;
                if (outstandingFines != null) {
                    for (BorrowedBook b : outstandingFines) {
                        newTotal += Math.max(0, b.getFine());
                        if (!b.isFinePaid() && "Đã trả".equalsIgnoreCase(b.getStatus()) && b.getFine() > 0) {
                            newPayable++;
                        }
                    }
                }
                summary.setText("Tổng: " + fmt.format(newTotal) + " VND • Có thể thanh toán: " + newPayable + " khoản");

                JOptionPane.showMessageDialog(this,
                    String.format("Thanh toán thành công!\nSố khoản đã thanh toán: %d\nSố tiền đã trả: %,d VND\nSố dư mới: %,d VND", 
                        result.getPaidCount(), result.getTotalPaid(), result.getBalanceAfter()),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi thanh toán phạt: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
    private JPanel createHistorySection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
            new EmptyBorder(25, 30, 25, 30)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        panel.setMinimumSize(new Dimension(600, 280));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel title = new JLabel("Lịch Sử Giao Dịch");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(AppConstants.TEXT_PRIMARY);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchPanel.setBackground(Color.WHITE);
        
        JTextField searchField = createTextField("Tìm kiếm...");
        searchField.setPreferredSize(new Dimension(150, 36));
        
        JButton exportBtn = new JButton("Xuất Danh Sách");
        exportBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportBtn.setForeground(AppConstants.TEXT_PRIMARY);
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
            new EmptyBorder(8, 16, 8, 16)
        ));
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        searchPanel.add(searchField);
        searchPanel.add(exportBtn);
        
        header.add(title, BorderLayout.WEST);
        header.add(searchPanel, BorderLayout.EAST);
        
        // Table
        String[] cols = {"Thời Gian", "Loại Giao Dịch", "Số Tiền", "Trạng Thái"};
        historyModel = new DefaultTableModel(cols, 0);
        
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        for (Transaction t : transactions) {
            String amtStr = (t.getAmount() >= 0 ? "+ " : "- ") +
                fmt.format(Math.abs(t.getAmount())) + " VND";
            historyModel.addRow(new Object[]{t.getDate(), t.getType(), amtStr, t.getStatus()});
        }
        
        JTable table = new JTable(historyModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(50);
        table.setShowGrid(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(AppConstants.TEXT_SECONDARY);
        table.getTableHeader().setBackground(Color.WHITE);
        
        // Amount renderer
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, 
                    boolean sel, boolean focus, int row, int col) {
                JLabel lbl = new JLabel(val.toString());
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                String type = t.getValueAt(row, 1).toString();
                if ("Trả phạt".equalsIgnoreCase(type)) {
                    lbl.setForeground(AppConstants.DANGER_COLOR); // Always red for fines
                } else if ("Nạp tiền".equalsIgnoreCase(type)) {
                    lbl.setForeground(AppConstants.SUCCESS_COLOR); // Always green for topup
                } else {
                    // Default: use sign
                    lbl.setForeground(val.toString().startsWith("+") ? AppConstants.SUCCESS_COLOR : AppConstants.DANGER_COLOR);
                }
                return lbl;
            }
        });
        
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
                    lbl.setBackground(new Color(254, 243, 199));
                    lbl.setForeground(new Color(133, 77, 14));
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
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
            new EmptyBorder(10, 12, 10, 12)
        ));
        
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
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
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(orig.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(orig);
            }
        });
        
        return btn;
    }

    private void handleTopupFromField(JTextField amountField) {
        String raw = amountField.getText().trim();
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long value = Long.parseLong(digits);
        handleTopup(value);
    }

    private void handleTopup(long amount) {
        if (amount <= 0) {
            JOptionPane.showMessageDialog(this,
                "Số tiền phải lớn hơn 0!",
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (apiManager.isServerAvailable()) {
            try {
                // Chỉ tạo transaction - server sẽ tự động cập nhật balance
                transactionApi.createTransaction(
                    studentCode, "Nạp tiền", amount, "Nạp tiền qua ứng dụng"
                );
                
                // Refresh data
                CardInfo card = cardApi.getCard(studentCode);
                this.balance = card != null ? card.getBalance() : 0;
                if (balanceValue != null) {
                    balanceValue.setText(String.format("%,d VND", balance));
                }
                
                // Reload transactions
                loadFinanceData();
                
                JOptionPane.showMessageDialog(this,
                    "Nạp tiền thành công!\nSố dư mới: " + String.format("%,d VND", balance),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (Exception e) {
                System.err.println("Error topping up via API: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi nạp tiền: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                // Fallback to SimulatorService
            }
        }
        
        // Fallback về SimulatorService
        try {
            boolean success = simulatorService.deposit(studentCode, amount);
            if (!success) {
                JOptionPane.showMessageDialog(this,
                    "Không thể nạp tiền. Vui lòng thử lại!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Cập nhật balance và lịch sử giao dịch
            this.balance = simulatorService.getBalance(studentCode);
            
            if (balanceValue != null) {
                balanceValue.setText(String.format("%,d VND", balance));
            }
            
            // Reload transactions từ SimulatorService
            this.transactions = new ArrayList<>(simulatorService.getTransactions(studentCode));
            refreshHistoryTable();
            
            JOptionPane.showMessageDialog(this,
                "Nạp tiền thành công!\nSố dư mới: " + String.format("%,d VND", balance),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Lỗi khi nạp tiền: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
