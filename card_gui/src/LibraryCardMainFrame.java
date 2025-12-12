import com.formdev.flatlaf.FlatLightLaf;
import applet.AppletConstants;
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
 * [FINAL FULL VERSION] Tích hợp chặn tab khi chưa đổi PIN
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

        // Khởi động Simulator ngay khi mở ứng dụng
        initSimulator();

        initSampleData();
        initializeUI();
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initSimulator() {
        try {
            simulatorService.connect();
            System.out.println("Simulator connected successfully.");

            // Tự động tạo PIN mặc định (000000) nếu thẻ chưa có PIN
            try {
                simulatorService.createDemoPin();
                System.out.println("Default PIN (000000) created.");
            } catch (Exception e) {
                System.out.println("PIN already exists or error creating PIN: " + e.getMessage());
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi khởi động Simulator: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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

        // Định nghĩa các tab và hành động click
        // Lưu ý: Các tab nhạy cảm sẽ kiểm tra isPinVerified trước khi hiển thị
        tabLabels.put("pin", createTab("", "PIN & Bảo Mật", true, this::showPinPage));

        tabLabels.put("cardInfo", createTab("", "Thông Tin Bạn Đọc", false, () -> {
            if (checkPinStatus()) showCardInfoPage();
        }));

        tabLabels.put("books", createTab("", "Mượn / Trả Sách", false, () -> {
            if (checkPinStatus()) showBorrowedBooksPage();
        }));

        tabLabels.put("finance", createTab("", "Tài Chính", false, () -> {
            if (checkPinStatus()) showFinancePage();
        }));

        tabLabels.put("settings", createTab("", "Hệ Thống", false, () -> {
            checkAdminAccess(); // Chỉ hiện dialog, không chuyển tab
        }));

        tabLabels.values().forEach(tabsPanel::add);

        return tabsPanel;
    }

    // Hàm kiểm tra trạng thái PIN - QUAN TRỌNG
    private boolean checkPinStatus() {
        // 1. Kiểm tra đã đăng nhập chưa
        if (!simulatorService.isPinVerified()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng đăng nhập để truy cập chức năng này!",
                    "Yêu cầu xác thực", JOptionPane.WARNING_MESSAGE);
            showPinPage();
            updateTabHighlights(tabLabels.get("pin"));
            return false;
        }

        // 2. [NEW] Kiểm tra bắt buộc đổi PIN (nếu là Sinh viên & PIN mặc định)
        if (simulatorService.isChangePinRequired()) {
            JOptionPane.showMessageDialog(this,
                    "BẢO MẬT: Bạn phải đổi mã PIN mặc định trước khi sử dụng các chức năng khác!",
                    "Yêu Cầu Đổi PIN", JOptionPane.WARNING_MESSAGE);

            // Ép quay lại trang PIN
            showPinPage();
            updateTabHighlights(tabLabels.get("pin"));
            return false;
        }

        return true;
    }

    // Hàm kiểm tra quyền Admin cho tab Hệ Thống
    private void checkAdminAccess() {
        if (!simulatorService.isPinVerified()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng đăng nhập để truy cập chức năng này!",
                    "Yêu cầu xác thực", JOptionPane.WARNING_MESSAGE);
            showPinPage();
            updateTabHighlights(tabLabels.get("pin"));
            return;
        }

        // Kiểm tra role là Admin
        if (!"Admin".equals(simulatorService.getCurrentRole())) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ Admin mới có quyền truy cập Hệ Thống!",
                    "Không có quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Nếu là Admin, chuyển đến trang Settings
        showSettingsPage();
        updateTabHighlights(tabLabels.get("settings"));
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
                // Thực hiện action (đã bao gồm check PIN)
                onClick.run();

                // Nếu đang ở trang PIN và chưa verify, không highlight tab khác
                if (!simulatorService.isPinVerified() && !text.equals("PIN & Bảo Mật")) {
                    return;
                }

                // Nếu tab Hệ Thống mà không phải Admin, không highlight
                if (text.equals("Hệ Thống") && !"Admin".equals(simulatorService.getCurrentRole())) {
                    return;
                }

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
        // Truyền simulatorService vào PinPage để xử lý logic verify
        PinPage pinPage = new PinPage(simulatorService);
        mainContentPanel.add(pinPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showCardInfoPage() {
        currentPage = "cardInfo";
        mainContentPanel.removeAll();
        CardInfoPage cardInfoPage = new CardInfoPage(simulatorService);
        mainContentPanel.add(cardInfoPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showBorrowedBooksPage() {
        currentPage = "books";
        mainContentPanel.removeAll();
        BorrowedBooksPage booksPage = new BorrowedBooksPage(simulatorService);
        mainContentPanel.add(booksPage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showFinancePage() {
        currentPage = "finance";
        mainContentPanel.removeAll();
        FinancePage financePage = new FinancePage(simulatorService);
        mainContentPanel.add(financePage);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void showSettingsPage() {
        currentPage = "settings";
        mainContentPanel.removeAll();
        SettingsPage settingsPage = new SettingsPage(simulatorService);
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