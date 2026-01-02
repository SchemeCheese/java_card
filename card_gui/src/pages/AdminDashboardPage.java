package pages;

import api.ApiServiceManager;
import api.CardApiService;
import api.BookApiService;
import api.TransactionApiService;
import constants.AppConstants;
import models.CardInfo;
import models.BorrowedBook;
import models.Transaction;
import service.SimulatorService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Admin Dashboard - Modern UI with real data
 */
public class AdminDashboardPage extends JPanel {
    
    // Colors
    private static final Color PRIMARY_BLUE = new Color(59, 130, 246);
    private static final Color PRIMARY_GREEN = new Color(34, 197, 94);
    private static final Color PRIMARY_ORANGE = new Color(249, 115, 22);
    private static final Color PRIMARY_PURPLE = new Color(139, 92, 246);
    private static final Color PRIMARY_RED = new Color(239, 68, 68);
    private static final Color PRIMARY_CYAN = new Color(6, 182, 212);
    
    // Gradient colors
    private static final Color GRADIENT_BLUE_START = new Color(59, 130, 246);
    private static final Color GRADIENT_BLUE_END = new Color(37, 99, 235);
    private static final Color GRADIENT_GREEN_START = new Color(34, 197, 94);
    private static final Color GRADIENT_GREEN_END = new Color(22, 163, 74);
    private static final Color GRADIENT_ORANGE_START = new Color(249, 115, 22);
    private static final Color GRADIENT_ORANGE_END = new Color(234, 88, 12);
    private static final Color GRADIENT_PURPLE_START = new Color(139, 92, 246);
    private static final Color GRADIENT_PURPLE_END = new Color(124, 58, 237);
    
    private SimulatorService simulatorService;
    private ApiServiceManager apiManager;
    private CardApiService cardApi;
    private BookApiService bookApi;
    private TransactionApiService transactionApi;
    
    // Statistics data
    private int totalCards = 0;
    private int activeCards = 0;
    private int lockedCards = 0;
    private int totalBorrowedBooks = 0;
    private int overdueBooks = 0;
    private long totalBalance = 0;
    private List<Transaction> recentTransactions = new ArrayList<>();
    private List<BorrowedBook> recentBorrows = new ArrayList<>();
    private List<CardInfo> allCards = new ArrayList<>();
    
    // Monthly data for chart
    private int[] monthlyBorrowed = new int[6];
    private int[] monthlyReturned = new int[6];
    
    // Animation
    private javax.swing.Timer animationTimer;
    private float animationProgress = 0f;
    
    // UI Components
    private JLabel totalCardsLabel;
    private JLabel activeCardsLabel;
    private JLabel borrowedBooksLabel;
    private JLabel totalBalanceLabel;
    private JPanel recentActivityPanel;
    private JPanel chartPanel;
    
    // Callback to switch tabs
    private Runnable onNavigateToSettings;
    private Runnable onNavigateToBookManagement;
    
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;
    
    public AdminDashboardPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.apiManager = ApiServiceManager.getInstance();
        this.cardApi = apiManager.getCardApiService();
        this.bookApi = apiManager.getBookApiService();
        this.transactionApi = apiManager.getTransactionApiService();
        
        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        
        initializeUI();
        loadData();
        startAnimations();
    }
    
    public void setNavigationCallbacks(Runnable onSettings, Runnable onBookManagement) {
        this.onNavigateToSettings = onSettings;
        this.onNavigateToBookManagement = onBookManagement;
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(AppConstants.BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(25, 35, 25, 35));
        
        mainPanel.add(createHeader());
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(createStatisticsRow());
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(createChartsAndActivityRow());
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(createQuickActionsRow());
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppConstants.BACKGROUND);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(AppConstants.BACKGROUND);
        
        JLabel welcomeLabel = new JLabel("Xin chào, Admin!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcomeLabel.setForeground(AppConstants.TEXT_PRIMARY);
        
        JLabel subtitleLabel = new JLabel("Đây là tổng quan hệ thống thư viện của bạn");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(AppConstants.TEXT_SECONDARY);
        
        leftPanel.add(welcomeLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(subtitleLabel);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(AppConstants.BACKGROUND);
        
        Calendar cal = Calendar.getInstance();
        String[] days = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"};
        String dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1];
        String dateStr = String.format("%s, %02d tháng %d, %d", 
            dayName, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
        
        JLabel dateLabel = new JLabel(dateStr);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateLabel.setForeground(AppConstants.TEXT_SECONDARY);
        
        JButton refreshBtn = createTextButton("[Làm mới]", PRIMARY_BLUE);
        refreshBtn.addActionListener(e -> {
            loadData();
            showNotification("Đã cập nhật dữ liệu!");
        });
        
        rightPanel.add(dateLabel);
        rightPanel.add(refreshBtn);
        
        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }

    private JPanel createStatisticsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 20, 0));
        row.setBackground(AppConstants.BACKGROUND);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        
        JPanel card1 = createStatCard("USERS", "Tổng số thẻ", "0", "Tổng số thẻ trong hệ thống",
            GRADIENT_BLUE_START, GRADIENT_BLUE_END);
        totalCardsLabel = findValueLabel(card1);
        
        JPanel card2 = createStatCard("CHECK", "Thẻ hoạt động", "0", "Đang hoạt động bình thường",
            GRADIENT_GREEN_START, GRADIENT_GREEN_END);
        activeCardsLabel = findValueLabel(card2);
        
        JPanel card3 = createStatCard("BOOK", "Sách đang mượn", "0", "Tổng số sách đang được mượn",
            GRADIENT_ORANGE_START, GRADIENT_ORANGE_END);
        borrowedBooksLabel = findValueLabel(card3);
        
        JPanel card4 = createStatCard("MONEY", "Tổng số dư", "0 đ", "Tổng số dư tất cả thẻ",
            GRADIENT_PURPLE_START, GRADIENT_PURPLE_END);
        totalBalanceLabel = findValueLabel(card4);
        
        row.add(card1);
        row.add(card2);
        row.add(card3);
        row.add(card4);
        
        return row;
    }

    private JPanel createStatCard(String iconType, String title, String value, String subtitle,
                                   Color gradientStart, Color gradientEnd) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                g2.setColor(new Color(0, 0, 0, 25));
                g2.fillRoundRect(3, 3, w - 3, h - 3, 16, 16);
                
                GradientPaint gradient = new GradientPaint(0, 0, gradientStart, w, h, gradientEnd);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, w - 3, h - 3, 16, 16);
                
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(w - 70, -25, 90, 90);
                g2.fillOval(w - 40, h - 35, 70, 70);
                
                g2.dispose();
            }
        };
        
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setOpaque(false);
        
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(Color.WHITE);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                
                switch (iconType) {
                    case "USERS":
                        g2.fillOval(cx - 8, cy - 12, 10, 10);
                        g2.fillRoundRect(cx - 10, cy, 14, 10, 4, 4);
                        g2.fillOval(cx + 2, cy - 10, 8, 8);
                        g2.fillRoundRect(cx + 1, cy + 1, 10, 8, 3, 3);
                        break;
                    case "CHECK":
                        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(cx - 8, cy, cx - 2, cy + 6);
                        g2.drawLine(cx - 2, cy + 6, cx + 10, cy - 6);
                        break;
                    case "BOOK":
                        g2.fillRoundRect(cx - 10, cy - 10, 20, 20, 3, 3);
                        g2.setColor(gradientStart);
                        g2.fillRect(cx - 1, cy - 8, 2, 16);
                        break;
                    case "MONEY":
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                        g2.drawString("đ", cx - 7, cy + 8);
                        break;
                }
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(45, 45));
        iconPanel.setOpaque(false);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(255, 255, 255, 200));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setName("valueLabel");
        
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subtitleLabel.setForeground(new Color(255, 255, 255, 150));
        
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitleLabel);
        
        card.add(iconPanel, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        
        return card;
    }
    
    private JLabel findValueLabel(JPanel card) {
        for (Component c : card.getComponents()) {
            if (c instanceof JPanel) {
                for (Component c2 : ((JPanel) c).getComponents()) {
                    if (c2 instanceof JLabel && "valueLabel".equals(c2.getName())) {
                        return (JLabel) c2;
                    }
                }
            }
        }
        return null;
    }
    
    private JPanel createChartsAndActivityRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setBackground(AppConstants.BACKGROUND);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        row.setPreferredSize(new Dimension(800, 320));
        
        row.add(createChartCard());
        row.add(createRecentActivityCard());
        
        return row;
    }
    
    private JPanel createChartCard() {
        JPanel card = createWhiteCard("Thống kê mượn sách theo tháng");
        
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        legendPanel.setBackground(Color.WHITE);
        legendPanel.add(createLegendItem("Mượn", PRIMARY_BLUE));
        legendPanel.add(createLegendItem("Trả", PRIMARY_GREEN));
        
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart(g);
            }
        };
        chartPanel.setBackground(Color.WHITE);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.add(legendPanel, BorderLayout.NORTH);
        contentPanel.add(chartPanel, BorderLayout.CENTER);
        
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createLegendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setBackground(Color.WHITE);
        
        JPanel colorBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 12, 12, 3, 3);
                g2.dispose();
            }
        };
        colorBox.setPreferredSize(new Dimension(12, 12));
        colorBox.setOpaque(false);
        
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(AppConstants.TEXT_SECONDARY);
        
        item.add(colorBox);
        item.add(label);
        
        return item;
    }
    
    private void drawBarChart(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();
        int padding = 40;
        int chartWidth = w - padding * 2;
        int chartHeight = h - padding - 20;
        
        String[] months = {"T7", "T8", "T9", "T10", "T11", "T12"};
        int maxValue = 10;
        
        for (int i = 0; i < 6; i++) {
            maxValue = Math.max(maxValue, Math.max(monthlyBorrowed[i], monthlyReturned[i]) + 5);
        }
        
        int barWidth = chartWidth / months.length / 3;
        int gap = barWidth / 2;
        
        g2.setColor(new Color(229, 231, 235));
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4}, 0));
        for (int i = 0; i <= 4; i++) {
            int y = padding + (chartHeight * i / 4);
            g2.drawLine(padding, y, w - padding, y);
            
            g2.setColor(AppConstants.TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int val = maxValue - (maxValue * i / 4);
            g2.drawString(String.valueOf(val), 10, y + 4);
            g2.setColor(new Color(229, 231, 235));
        }
        
        g2.setStroke(new BasicStroke(1));
        for (int i = 0; i < months.length; i++) {
            int x = padding + (chartWidth * i / months.length) + gap;
            
            int borrowedHeight = (int) ((monthlyBorrowed[i] * chartHeight / maxValue) * animationProgress);
            if (borrowedHeight > 0) {
                GradientPaint blueGradient = new GradientPaint(
                    x, h - padding - borrowedHeight, GRADIENT_BLUE_START,
                    x, h - padding, GRADIENT_BLUE_END
                );
                g2.setPaint(blueGradient);
                g2.fillRoundRect(x, h - padding - borrowedHeight, barWidth, borrowedHeight, 4, 4);
            }
            
            int returnedHeight = (int) ((monthlyReturned[i] * chartHeight / maxValue) * animationProgress);
            x += barWidth + 4;
            if (returnedHeight > 0) {
                GradientPaint greenGradient = new GradientPaint(
                    x, h - padding - returnedHeight, GRADIENT_GREEN_START,
                    x, h - padding, GRADIENT_GREEN_END
                );
                g2.setPaint(greenGradient);
                g2.fillRoundRect(x, h - padding - returnedHeight, barWidth, returnedHeight, 4, 4);
            }
            
            g2.setColor(AppConstants.TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            int labelX = padding + (chartWidth * i / months.length) + gap + barWidth / 2;
            g2.drawString(months[i], labelX, h - 15);
        }
    }

    private JPanel createRecentActivityCard() {
        JPanel card = createWhiteCard("Hoạt động gần đây");
        
        recentActivityPanel = new JPanel();
        recentActivityPanel.setLayout(new BoxLayout(recentActivityPanel, BoxLayout.Y_AXIS));
        recentActivityPanel.setBackground(Color.WHITE);
        recentActivityPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        JScrollPane scrollPane = new JScrollPane(recentActivityPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        card.add(scrollPane, BorderLayout.CENTER);
        
        return card;
    }

    private void updateRecentActivity() {
        recentActivityPanel.removeAll();
        
        List<ActivityItem> activities = new ArrayList<>();
        
        for (Transaction t : recentTransactions) {
            String action = "";
            Color color = PRIMARY_BLUE;
            
            if ("Nạp tiền".equals(t.getType())) {
                action = "đã nạp " + currencyFormat.format(t.getAmount()) + "đ vào thẻ";
                color = PRIMARY_PURPLE;
            } else if ("Trả phạt".equals(t.getType())) {
                action = "đã trả phạt " + currencyFormat.format(Math.abs(t.getAmount())) + "đ";
                color = PRIMARY_RED;
            } else {
                action = t.getType() + " " + currencyFormat.format(t.getAmount()) + "đ";
                color = PRIMARY_CYAN;
            }
            
            activities.add(new ActivityItem("Người dùng", action, t.getDate(), color));
        }
        
        for (BorrowedBook b : recentBorrows) {
            String action = "";
            Color color = PRIMARY_BLUE;
            
            if ("Đang mượn".equals(b.getStatus())) {
                action = "đã mượn sách '" + b.getBookName() + "'";
                color = PRIMARY_BLUE;
            } else if ("Đã trả".equals(b.getStatus())) {
                action = "đã trả sách '" + b.getBookName() + "'";
                color = PRIMARY_GREEN;
            } else if ("Quá hạn".equals(b.getStatus())) {
                action = "quá hạn trả sách '" + b.getBookName() + "'";
                color = PRIMARY_RED;
            }
            
            activities.add(new ActivityItem("Sinh viên", action, b.getBorrowDate(), color));
        }
        
        activities.sort((a, b) -> b.time.compareTo(a.time));
        int count = Math.min(6, activities.size());
        
        if (count == 0) {
            JLabel emptyLabel = new JLabel("Chưa có hoạt động nào");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            emptyLabel.setForeground(AppConstants.TEXT_SECONDARY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(new EmptyBorder(30, 0, 30, 0));
            recentActivityPanel.add(emptyLabel);
        } else {
            for (int i = 0; i < count; i++) {
                ActivityItem item = activities.get(i);
                addActivityItem(item.name, item.action, getTimeAgo(item.time), item.color);
            }
        }
        
        recentActivityPanel.revalidate();
        recentActivityPanel.repaint();
    }
    
    private String getTimeAgo(String dateStr) {
        try {
            Date date = dateFormat.parse(dateStr);
            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);
            
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private static class ActivityItem {
        String name, action, time;
        Color color;
        
        ActivityItem(String name, String action, String time, Color color) {
            this.name = name;
            this.action = action;
            this.time = time;
            this.color = color;
        }
    }
    
    private void addActivityItem(String name, String action, String time, Color accentColor) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(10, 10, 10, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        item.setPreferredSize(new Dimension(400, 55));
        
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
                g2.fillOval(x, y, size, size);
                
                g2.setColor(accentColor);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initial)) / 2;
                int ty = y + (size + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, tx, ty);
                
                g2.dispose();
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
            
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(36, 36);
            }
        };
        avatar.setOpaque(false);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        
        JLabel nameLabel = new JLabel("<html><b>" + name + "</b> " + action + "</html>");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nameLabel.setForeground(AppConstants.TEXT_PRIMARY);
        
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(AppConstants.TEXT_SECONDARY);
        
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(timeLabel);
        
        item.add(avatar, BorderLayout.WEST);
        item.add(content, BorderLayout.CENTER);
        
        recentActivityPanel.add(item);
        
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(243, 244, 246));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        recentActivityPanel.add(sep);
    }
    
    private JPanel createQuickActionsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 20, 0));
        row.setBackground(AppConstants.BACKGROUND);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row.setPreferredSize(new Dimension(800, 110));
        
        row.add(createQuickActionCard("ADD", "Tạo thẻ mới", "Đăng ký thẻ cho sinh viên", PRIMARY_BLUE, () -> {
            if (onNavigateToSettings != null) {
                onNavigateToSettings.run();
            }
        }));
        
        row.add(createQuickActionCard("BOOKS", "Quản lý sách", "Thêm, sửa, xóa sách", PRIMARY_GREEN, () -> {
            if (onNavigateToBookManagement != null) {
                onNavigateToBookManagement.run();
            }
        }));
        
        row.add(createQuickActionCard("CHART", "Báo cáo", "Xem báo cáo chi tiết", PRIMARY_ORANGE, () -> {
            showNotification("Tính năng đang phát triển");
        }));
        
        row.add(createQuickActionCard("GEAR", "Cài đặt", "Cấu hình hệ thống", PRIMARY_PURPLE, () -> {
            showNotification("Tính năng đang phát triển");
        }));
        
        return row;
    }

    private JPanel createQuickActionCard(String iconType, String title, String subtitle, Color accentColor, Runnable onClick) {
        JPanel card = new JPanel() {
            private boolean hovered = false;
            
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        repaint();
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                    
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        onClick.run();
                    }
                });
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 2, w - 2, h - 2, 14, 14);
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 2, h - 2, 14, 14);
                
                if (hovered) {
                    g2.setColor(accentColor);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(1, 1, w - 4, h - 4, 14, 14);
                }
                
                g2.dispose();
            }
        };
        
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(15, 18, 15, 18));
        card.setOpaque(false);
        
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, 40, 40, 10, 10);
                
                g2.setColor(accentColor);
                int cx = 20, cy = 20;
                
                switch (iconType) {
                    case "ADD":
                        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(cx, cy - 8, cx, cy + 8);
                        g2.drawLine(cx - 8, cy, cx + 8, cy);
                        break;
                    case "BOOKS":
                        g2.fillRoundRect(cx - 10, cy - 8, 8, 16, 2, 2);
                        g2.fillRoundRect(cx - 1, cy - 10, 8, 18, 2, 2);
                        g2.fillRoundRect(cx + 8, cy - 6, 6, 14, 2, 2);
                        break;
                    case "CHART":
                        g2.fillRect(cx - 10, cy + 2, 5, 8);
                        g2.fillRect(cx - 3, cy - 4, 5, 14);
                        g2.fillRect(cx + 4, cy - 8, 5, 18);
                        break;
                    case "GEAR":
                        g2.setStroke(new BasicStroke(2));
                        g2.drawOval(cx - 6, cy - 6, 12, 12);
                        for (int i = 0; i < 8; i++) {
                            double angle = i * Math.PI / 4;
                            int x1 = (int) (cx + 8 * Math.cos(angle));
                            int y1 = (int) (cy + 8 * Math.sin(angle));
                            int x2 = (int) (cx + 11 * Math.cos(angle));
                            int y2 = (int) (cy + 11 * Math.sin(angle));
                            g2.drawLine(x1, y1, x2, y2);
                        }
                        break;
                }
                
                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(40, 40));
        iconPanel.setMaximumSize(new Dimension(40, 40));
        iconPanel.setOpaque(false);
        iconPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(AppConstants.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(iconPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(subtitleLabel);
        
        return card;
    }

    private JPanel createWhiteCard(String title) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 2, w - 2, h - 2, 14, 14);
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 2, h - 2, 14, 14);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 18, 15, 18));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        card.add(titleLabel, BorderLayout.NORTH);
        
        return card;
    }
    
    private JButton createTextButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setBackground(null);
        btn.setBorder(null);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(color);
            }
        });
        
        return btn;
    }

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    // Load all cards
                    CardApiService.PaginationResult cardResult = cardApi.getAllCardsWithPagination(1, 1000);
                    allCards = cardResult.getCards();
                    totalCards = allCards.size();
                    activeCards = 0;
                    lockedCards = 0;
                    totalBalance = 0;
                    
                    for (CardInfo card : allCards) {
                        String status = card.getStatus();
                        // Check status từ DB: 'Hoạt động', 'Khóa', 'Tạm khóa'
                        if ("Hoạt động".equals(status) || "Active".equals(status)) {
                            activeCards++;
                        } else if ("Khóa".equals(status) || "Tạm khóa".equals(status) || "Locked".equals(status)) {
                            lockedCards++;
                        }
                        totalBalance += card.getBalance();
                    }
                    
                    // Load borrowed books
                    List<BorrowedBook> allBorrows = bookApi.getAllBorrowedBooks(null, 1, 1000);
                    totalBorrowedBooks = 0;
                    overdueBooks = 0;
                    recentBorrows.clear();
                    
                    for (int i = 0; i < 6; i++) {
                        monthlyBorrowed[i] = 0;
                        monthlyReturned[i] = 0;
                    }
                    
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH);
                    int currentYear = cal.get(Calendar.YEAR);
                    
                    for (BorrowedBook book : allBorrows) {
                        String status = book.getStatus();
                        if ("Đang mượn".equals(status)) {
                            totalBorrowedBooks++;
                        } else if ("Quá hạn".equals(status)) {
                            totalBorrowedBooks++;
                            overdueBooks++;
                        }
                        
                        try {
                            String borrowDateStr = book.getBorrowDate();
                            if (borrowDateStr != null && !borrowDateStr.isEmpty()) {
                                String[] parts = borrowDateStr.split("/");
                                if (parts.length >= 2) {
                                    int month = Integer.parseInt(parts[1]) - 1;
                                    int year = parts.length >= 3 ? Integer.parseInt(parts[2]) : currentYear;
                                    
                                    int monthDiff = (currentYear - year) * 12 + (currentMonth - month);
                                    if (monthDiff >= 0 && monthDiff < 6) {
                                        int idx = 5 - monthDiff;
                                        monthlyBorrowed[idx]++;
                                        
                                        if ("Đã trả".equals(status)) {
                                            monthlyReturned[idx]++;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid dates
                        }
                    }
                    
                    recentBorrows = allBorrows.subList(0, Math.min(10, allBorrows.size()));
                    
                    // Load recent transactions
                    recentTransactions = transactionApi.getAllTransactions(null, null, null, null, 1, 10);
                    
                } catch (Exception e) {
                    System.err.println("[AdminDashboard] Error loading data: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                refreshDashboardUI();
            }
        };
        worker.execute();
    }
    
    private void refreshDashboardUI() {
        if (totalCardsLabel != null) {
            animateNumber(totalCardsLabel, totalCards);
        }
        if (activeCardsLabel != null) {
            animateNumber(activeCardsLabel, activeCards);
        }
        if (borrowedBooksLabel != null) {
            animateNumber(borrowedBooksLabel, totalBorrowedBooks);
        }
        if (totalBalanceLabel != null) {
            animateBalance(totalBalanceLabel, totalBalance);
        }
        
        updateRecentActivity();
        
        if (chartPanel != null) {
            chartPanel.repaint();
        }
    }
    
    private void animateNumber(JLabel label, int targetValue) {
        final int[] currentValue = {0};
        final int steps = 20;
        final int delay = 30;
        
        javax.swing.Timer timer = new javax.swing.Timer(delay, null);
        timer.addActionListener(e -> {
            currentValue[0] += Math.max(1, targetValue / steps);
            if (currentValue[0] >= targetValue) {
                currentValue[0] = targetValue;
                timer.stop();
            }
            label.setText(String.valueOf(currentValue[0]));
        });
        timer.start();
    }
    
    private void animateBalance(JLabel label, long targetValue) {
        final long[] currentValue = {0};
        final int steps = 20;
        final int delay = 30;
        
        javax.swing.Timer timer = new javax.swing.Timer(delay, null);
        timer.addActionListener(e -> {
            currentValue[0] += Math.max(1, targetValue / steps);
            if (currentValue[0] >= targetValue) {
                currentValue[0] = targetValue;
                timer.stop();
            }
            label.setText(currencyFormat.format(currentValue[0]) + " đ");
        });
        timer.start();
    }
    
    private void startAnimations() {
        animationProgress = 0f;
        animationTimer = new javax.swing.Timer(16, e -> {
            animationProgress += 0.05f;
            if (animationProgress >= 1f) {
                animationProgress = 1f;
                animationTimer.stop();
            }
            if (chartPanel != null) {
                chartPanel.repaint();
            }
        });
        animationTimer.start();
    }
    
    private void showNotification(String message) {
        JWindow notification = new JWindow(SwingUtilities.getWindowAncestor(this));
        notification.setBackground(new Color(0, 0, 0, 0));
        
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(50, 50, 50, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));
        
        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(Color.WHITE);
        panel.add(label);
        
        notification.add(panel);
        notification.pack();
        
        Point loc = getLocationOnScreen();
        int x = loc.x + (getWidth() - notification.getWidth()) / 2;
        int y = loc.y + getHeight() - notification.getHeight() - 50;
        notification.setLocation(x, y);
        notification.setVisible(true);
        
        javax.swing.Timer hideTimer = new javax.swing.Timer(2000, e -> {
            notification.dispose();
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
}
