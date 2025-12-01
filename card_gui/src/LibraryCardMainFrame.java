import com.formdev.flatlaf.FlatLightLaf;
import constants.AppConstants;
import models.BorrowedBook;
import models.Transaction;
import pages.*;
import service.SimulatorService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Main frame for Library Card Management System
 * Refactored to use modular components
 */
public class LibraryCardMainFrame extends JFrame {
    private JPanel mainContentPanel;
    private SimulatorService simulatorService;
    
    // Data storage
    private String studentId = "";
    private String studentName = "";
    private String birthDate = "";
    private String email = "";
    private String phone = "";
    private String major = "";
    private String address = "";
    private long balance = 500000;
    private List<BorrowedBook> borrowedBooks = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();

    // Tab labels for highlighting
    private Map<String, JLabel> tabLabels = new HashMap<>();
    private String currentPage = "pin";

    public LibraryCardMainFrame() {
        super("Library Management System");
        simulatorService = new SimulatorService();
        initSampleData();
        initializeUI();
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initSampleData() {
        // Sample borrowed books
        borrowedBooks.add(new BorrowedBook("NV001", "Nhà Giả Kim", "15/05/2024", "29/05/2024", "Đang mượn", 0));
        borrowedBooks.add(new BorrowedBook("DB002", "Đắc Nhân Tâm", "01/05/2024", "15/05/2024", "Quá hạn", 12));
        borrowedBooks.add(new BorrowedBook("TH003", "Trên Đường Băng", "20/05/2024", "03/06/2024", "Đang mượn", 0));
        borrowedBooks.add(new BorrowedBook("CX004", "Cà Phê Cùng Tony", "10/05/2024", "24/05/2024", "Sắp hết hạn", 0));

        // Sample transactions
        transactions.add(new Transaction("20/07/2023", "Thanh toán phạt", -15000, "Thành công"));
        transactions.add(new Transaction("15/07/2023", "Nạp tiền", 100000, "Thành công"));
        transactions.add(new Transaction("10/07/2023", "Nạp tiền", 200000, "Thành công"));
        transactions.add(new Transaction("05/07/2023", "Nạp tiền", 50000, "Đang xử lý"));

        // Sample student info
        studentId = "B20DCCN123";
        studentName = "Nguyễn Văn A";
        birthDate = "01/01/2002";
        email = "nguyenvana@email.com";
        phone = "0987654321";
        major = "Công nghệ thông tin / D20CNPM1";
        address = "123 Đường ABC, Phường XYZ, Quận GHI, Thành phố JKL";
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(AppConstants.BACKGROUND);

        // Top header with title and tabs
        JPanel headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);

        // Main content area
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(AppConstants.BACKGROUND);
        add(mainContentPanel, BorderLayout.CENTER);

        // Show PIN page by default
        showPinPage();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppConstants.BORDER_COLOR));

        // Top row: Logo and menu items
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(Color.WHITE);
        topRow.setBorder(new EmptyBorder(15, 30, 0, 30));

        // Left: Logo and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel logoIcon = new JLabel("[LIB]");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoIcon.setForeground(AppConstants.PRIMARY_COLOR);

        JLabel titleLabel = new JLabel("Quản Lý Thẻ Thư Viện");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);

        leftPanel.add(logoIcon);
        leftPanel.add(titleLabel);

        // Right: Menu items
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);

        JLabel fileLabel = new JLabel("File");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileLabel.setForeground(AppConstants.TEXT_SECONDARY);

        JLabel toolsLabel = new JLabel("Công cụ");
        toolsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toolsLabel.setForeground(AppConstants.TEXT_SECONDARY);

        JLabel helpLabel = new JLabel("Trợ giúp");
        helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        helpLabel.setForeground(AppConstants.TEXT_SECONDARY);

        rightPanel.add(fileLabel);
        rightPanel.add(toolsLabel);
        rightPanel.add(helpLabel);

        topRow.add(leftPanel, BorderLayout.WEST);
        topRow.add(rightPanel, BorderLayout.EAST);

        // Bottom row: Tabs
        JPanel tabsRow = createTabsPanel();

        header.add(topRow, BorderLayout.NORTH);
        header.add(tabsRow, BorderLayout.SOUTH);

        return header;
    }

    private JPanel createTabsPanel() {
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabsPanel.setBackground(Color.WHITE);
        tabsPanel.setBorder(new EmptyBorder(0, 30, 0, 30));

        tabLabels.put("pin", createTab("", "PIN & Bảo Mật", true, this::showPinPage));
        tabLabels.put("cardInfo", createTab("", "Thông Tin Bạn Đọc", false, this::showCardInfoPage));
        tabLabels.put("books", createTab("", "Mượn / Trả Sách", false, this::showBorrowedBooksPage));
        tabLabels.put("finance", createTab("", "Tài Chính", false, this::showFinancePage));
        tabLabels.put("settings", createTab("", "Hệ Thống", false, this::showSettingsPage));

        tabLabels.values().forEach(tabsPanel::add);

        return tabsPanel;
    }

    private JLabel createTab(String icon, String text, boolean active, Runnable onClick) {
        String tabText = icon.isEmpty() ? text : icon + " " + text;
        JLabel tab = new JLabel(tabText);
        tab.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tab.setForeground(active ? AppConstants.PRIMARY_COLOR : AppConstants.TEXT_SECONDARY);
        tab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 3 : 0, 0, AppConstants.PRIMARY_COLOR),
            new EmptyBorder(15, 20, 15, 20)
        ));
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));

        tab.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onClick.run();
                updateTabHighlights(tab);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tab.setForeground(AppConstants.PRIMARY_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!isActiveTab(tab)) {
                    tab.setForeground(AppConstants.TEXT_SECONDARY);
                }
            }
        });

        return tab;
    }

    private void updateTabHighlights(JLabel activeTab) {
        for (JLabel tab : tabLabels.values()) {
            if (tab == activeTab) {
                tab.setForeground(AppConstants.PRIMARY_COLOR);
                tab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 3, 0, AppConstants.PRIMARY_COLOR),
                    new EmptyBorder(15, 20, 15, 20)
                ));
            } else {
                tab.setForeground(AppConstants.TEXT_SECONDARY);
                tab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 0, AppConstants.PRIMARY_COLOR),
                    new EmptyBorder(15, 20, 15, 20)
                ));
            }
        }
    }

    private boolean isActiveTab(JLabel tab) {
        Border border = tab.getBorder();
        if (border instanceof CompoundBorder) {
            Border outerBorder = ((CompoundBorder) border).getOutsideBorder();
            if (outerBorder instanceof MatteBorder) {
                MatteBorder matteBorder = (MatteBorder) outerBorder;
                return matteBorder.getBorderInsets().bottom > 0;
            }
        }
        return false;
    }

    // Navigation methods
    private void showPinPage() {
        currentPage = "pin";
        mainContentPanel.removeAll();
        PinPage pinPage = new PinPage();
        mainContentPanel.add(pinPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showCardInfoPage() {
        currentPage = "cardInfo";
        mainContentPanel.removeAll();
        CardInfoPage cardInfoPage = new CardInfoPage(
            studentId, studentName, birthDate, email, phone, major, address
        );
        mainContentPanel.add(cardInfoPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showBorrowedBooksPage() {
        currentPage = "books";
        mainContentPanel.removeAll();
        BorrowedBooksPage booksPage = new BorrowedBooksPage(borrowedBooks);
        mainContentPanel.add(booksPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showFinancePage() {
        currentPage = "finance";
        mainContentPanel.removeAll();
        FinancePage financePage = new FinancePage(balance, transactions);
        mainContentPanel.add(financePage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showSettingsPage() {
        currentPage = "settings";
        mainContentPanel.removeAll();
        SettingsPage settingsPage = new SettingsPage();
        mainContentPanel.add(settingsPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LibraryCardMainFrame frame = new LibraryCardMainFrame();
            frame.setVisible(true);
        });
    }
}
