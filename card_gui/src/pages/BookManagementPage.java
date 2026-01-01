package pages;

import api.ApiServiceManager;
import api.BookInventoryApiService;
import api.BookInventoryApiService.BookInfo;
import com.google.gson.JsonObject;
import constants.AppConstants;
import service.SimulatorService;
import ui.RoundedBorder;
import ui.UIComponentFactory;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Quản Lý Kho Sách Page - Dành riêng cho Admin
 * Features: CRUD operations + Import từ Excel
 */
public class BookManagementPage extends JPanel {

    private SimulatorService simulatorService;
    private ApiServiceManager apiManager;
    private BookInventoryApiService inventoryApi;

    // UI Components
    private JPanel booksGridPanel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private JComboBox<String> statusFilter;
    private JLabel totalBooksLabel;
    
    // Pagination
    private int currentPage = 1;
    private int itemsPerPage = 9;
    private int totalPages = 1;
    private JLabel pageLabel;
    
    // Data
    private List<BookInfo> allBooks = new ArrayList<>();
    private List<BookInfo> filteredBooks = new ArrayList<>();

    // Colors
    private static final Color CARD_SHADOW = new Color(0, 0, 0, 20);
    private static final Color GRADIENT_START = new Color(79, 70, 229);
    private static final Color GRADIENT_END = new Color(147, 51, 234);

    public BookManagementPage(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
        this.apiManager = ApiServiceManager.getInstance();
        this.inventoryApi = apiManager.getBookInventoryApiService();

        setLayout(new BorderLayout());
        setBackground(AppConstants.BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createPaginationPanel(), BorderLayout.SOUTH);

        loadBooks();
    }

    // ==========================================
    // HEADER SECTION
    // ==========================================
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, GRADIENT_END);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel icon = new JLabel("📚");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("QUẢN LÝ KHO SÁCH");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        totalBooksLabel = new JLabel("0 cuốn sách");
        totalBooksLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        totalBooksLabel.setForeground(new Color(255, 255, 255, 200));

        leftPanel.add(icon);
        leftPanel.add(title);
        leftPanel.add(Box.createHorizontalStrut(15));
        leftPanel.add(totalBooksLabel);

        // Right: Action buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JButton addBtn = createHeaderButton("➕ Thêm Sách", new Color(34, 197, 94));
        addBtn.addActionListener(e -> showAddBookDialog());

        JButton importBtn = createHeaderButton("📂 Import Excel", new Color(59, 130, 246));
        importBtn.addActionListener(e -> handleImportExcel());

        JButton refreshBtn = createHeaderButton("🔄 Làm Mới", new Color(107, 114, 128));
        refreshBtn.addActionListener(e -> loadBooks());

        rightPanel.add(addBtn);
        rightPanel.add(importBtn);
        rightPanel.add(refreshBtn);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppConstants.BACKGROUND);
        wrapper.setBorder(new EmptyBorder(0, 0, 15, 0));
        wrapper.add(header, BorderLayout.CENTER);

        return wrapper;
    }

    private JButton createHeaderButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(bgColor, 1, 8),
                new EmptyBorder(10, 18, 10, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });

        return btn;
    }

    // ==========================================
    // MAIN CONTENT SECTION
    // ==========================================
    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(AppConstants.BACKGROUND);

        // Filter bar
        content.add(createFilterBar(), BorderLayout.NORTH);

        // Books grid
        booksGridPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        booksGridPanel.setBackground(AppConstants.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(booksGridPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(AppConstants.BACKGROUND);
        scrollPane.getViewport().setBackground(AppConstants.BACKGROUND);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        content.add(scrollPane, BorderLayout.CENTER);

        return content;
    }

    private JPanel createFilterBar() {
        JPanel filterBar = new JPanel(new BorderLayout(15, 0));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 12),
                new EmptyBorder(15, 20, 15, 20)
        ));

        // Search field
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm theo tên sách, tác giả, mã sách...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 8),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchField.setPreferredSize(new Dimension(350, 40));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterBooks();
            }
        });

        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Filters
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        filtersPanel.setBackground(Color.WHITE);

        // Category filter
        JLabel categoryLabel = new JLabel("Thể loại:");
        categoryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryFilter = new JComboBox<>(new String[]{"Tất cả", "Văn học", "Khoa học", "Kỹ năng", "Lịch sử", "Công nghệ"});
        categoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryFilter.setPreferredSize(new Dimension(120, 35));
        categoryFilter.addActionListener(e -> filterBooks());

        // Status filter
        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusFilter = new JComboBox<>(new String[]{"Tất cả", "Có sẵn", "Hết sách"});
        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusFilter.setPreferredSize(new Dimension(120, 35));
        statusFilter.addActionListener(e -> filterBooks());

        filtersPanel.add(categoryLabel);
        filtersPanel.add(categoryFilter);
        filtersPanel.add(Box.createHorizontalStrut(10));
        filtersPanel.add(statusLabel);
        filtersPanel.add(statusFilter);

        filterBar.add(searchPanel, BorderLayout.WEST);
        filterBar.add(filtersPanel, BorderLayout.EAST);

        return filterBar;
    }

    // ==========================================
    // PAGINATION
    // ==========================================
    private JPanel createPaginationPanel() {
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        pagination.setBackground(AppConstants.BACKGROUND);

        JButton prevBtn = new JButton("◀ Trước");
        prevBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                displayBooks();
            }
        });

        pageLabel = new JLabel("Trang 1 / 1");
        pageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pageLabel.setForeground(AppConstants.TEXT_PRIMARY);

        JButton nextBtn = new JButton("Sau ▶");
        nextBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                displayBooks();
            }
        });

        pagination.add(prevBtn);
        pagination.add(pageLabel);
        pagination.add(nextBtn);

        return pagination;
    }

    // ==========================================
    // DATA LOADING & DISPLAY
    // ==========================================
    
    // Static storage để giữ dữ liệu local trong phiên làm việc
    private static List<BookInfo> localBooksCache = new ArrayList<>();
    private static boolean hasInitializedSampleData = false;
    
    private void loadBooks() {
        SwingWorker<List<BookInfo>, Void> worker = new SwingWorker<List<BookInfo>, Void>() {
            @Override
            protected List<BookInfo> doInBackground() throws Exception {
                if (apiManager.isServerAvailable()) {
                    try {
                        List<BookInfo> serverBooks = inventoryApi.getAllBooks(null, null, null, 1, 1000);
                        // Trả về list từ server (có thể empty)
                        return serverBooks != null ? serverBooks : new ArrayList<>();
                    } catch (Exception e) {
                        System.err.println("[BookManagement] Error loading from server: " + e.getMessage());
                    }
                }
                // Return null để biết là không kết nối được server
                return null;
            }

            @Override
            protected void done() {
                try {
                    List<BookInfo> serverBooks = get();
                    
                    if (serverBooks != null) {
                        // Load thành công từ server (kể cả empty list)
                        allBooks = serverBooks;
                        localBooksCache = new ArrayList<>(serverBooks);
                        System.out.println("[BookManagement] Loaded " + serverBooks.size() + " books from server");
                    } else {
                        // Không kết nối được server - dùng local cache
                        if (!localBooksCache.isEmpty()) {
                            allBooks = new ArrayList<>(localBooksCache);
                            System.out.println("[BookManagement] Using local cache: " + allBooks.size() + " books");
                        } else {
                            // Không có data - hiển thị empty
                            allBooks = new ArrayList<>();
                            System.out.println("[BookManagement] No data available (server offline, no cache)");
                        }
                    }
                    
                    totalBooksLabel.setText(allBooks.size() + " cuốn sách");
                    filterBooks();
                } catch (Exception e) {
                    e.printStackTrace();
                    // Fallback to local cache
                    if (!localBooksCache.isEmpty()) {
                        allBooks = new ArrayList<>(localBooksCache);
                    } else {
                        allBooks = new ArrayList<>();
                    }
                    totalBooksLabel.setText(allBooks.size() + " cuốn sách");
                    filterBooks();
                }
            }
        };
        worker.execute();
    }
    
    // Cập nhật local cache khi có thay đổi
    private void updateLocalCache() {
        localBooksCache = new ArrayList<>(allBooks);
    }
    
    private void addSampleBooks() {
        String[][] samples = {
            {"NV001", "Nhà Giả Kim", "Paulo Coelho", "Văn học", "5", "5"},
            {"DB002", "Đắc Nhân Tâm", "Dale Carnegie", "Kỹ năng", "3", "3"},
            {"TH003", "Trên Đường Băng", "Tony Buổi Sáng", "Kỹ năng", "4", "2"},
            {"CX004", "Cà Phê Cùng Tony", "Tony Buổi Sáng", "Kỹ năng", "3", "3"},
            {"HP005", "Harry Potter", "J.K. Rowling", "Văn học", "5", "1"},
            {"LT006", "Lược Sử Thời Gian", "Stephen Hawking", "Khoa học", "2", "2"},
            {"TN007", "Tuổi Trẻ Đáng Giá Bao Nhiêu", "Rosie Nguyễn", "Kỹ năng", "4", "4"},
            {"NL008", "Nhà Lãnh Đạo Không Chức Danh", "Robin Sharma", "Kỹ năng", "3", "0"}
        };
        
        for (String[] s : samples) {
            BookInfo book = new BookInfo();
            book.setBookId(s[0]);
            book.setTitle(s[1]);
            book.setAuthor(s[2]);
            book.setCategory(s[3]);
            book.setTotalCopies(Integer.parseInt(s[4]));
            book.setAvailableCopies(Integer.parseInt(s[5]));
            book.setStatus(Integer.parseInt(s[5]) > 0 ? "Có sẵn" : "Hết sách");
            allBooks.add(book);
        }
    }

    private void filterBooks() {
        String searchText = searchField.getText().toLowerCase().trim();
        String category = (String) categoryFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();

        filteredBooks.clear();

        for (BookInfo book : allBooks) {
            boolean matchSearch = searchText.isEmpty() ||
                    book.getTitle().toLowerCase().contains(searchText) ||
                    book.getAuthor().toLowerCase().contains(searchText) ||
                    book.getBookId().toLowerCase().contains(searchText);

            boolean matchCategory = "Tất cả".equals(category) ||
                    (book.getCategory() != null && book.getCategory().equals(category));

            boolean matchStatus = "Tất cả".equals(status) ||
                    ("Có sẵn".equals(status) && book.getAvailableCopies() > 0) ||
                    ("Hết sách".equals(status) && book.getAvailableCopies() == 0);

            if (matchSearch && matchCategory && matchStatus) {
                filteredBooks.add(book);
            }
        }

        currentPage = 1;
        totalPages = Math.max(1, (int) Math.ceil((double) filteredBooks.size() / itemsPerPage));
        displayBooks();
    }

    private void displayBooks() {
        booksGridPanel.removeAll();

        int start = (currentPage - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredBooks.size());

        for (int i = start; i < end; i++) {
            booksGridPanel.add(createBookCard(filteredBooks.get(i)));
        }

        // Fill empty slots
        int remaining = itemsPerPage - (end - start);
        for (int i = 0; i < remaining; i++) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            booksGridPanel.add(empty);
        }

        pageLabel.setText("Trang " + currentPage + " / " + totalPages);
        booksGridPanel.revalidate();
        booksGridPanel.repaint();
    }

    // ==========================================
    // BOOK CARD UI
    // ==========================================
    private JPanel createBookCard(BookInfo book) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow
                g2d.setColor(CARD_SHADOW);
                g2d.fillRoundRect(3, 3, getWidth() - 3, getHeight() - 3, 16, 16);
                
                // Card background
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 16, 16);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(320, 200));

        // Top: Icon + Category badge
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        // Book icon with color based on category
        JLabel bookIcon = new JLabel(getBookIcon(book.getCategory()));
        bookIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        bookIcon.setHorizontalAlignment(SwingConstants.CENTER);

        // Category badge
        JLabel categoryBadge = new JLabel(book.getCategory() != null ? book.getCategory() : "Khác");
        categoryBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        categoryBadge.setForeground(Color.WHITE);
        categoryBadge.setBackground(getCategoryColor(book.getCategory()));
        categoryBadge.setOpaque(true);
        categoryBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        topPanel.add(bookIcon, BorderLayout.WEST);
        topPanel.add(categoryBadge, BorderLayout.EAST);

        // Center: Book info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel titleLabel = new JLabel(book.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(AppConstants.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel authorLabel = new JLabel(book.getAuthor());
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        authorLabel.setForeground(AppConstants.TEXT_SECONDARY);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel idLabel = new JLabel("Mã: " + book.getBookId());
        idLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        idLabel.setForeground(AppConstants.TEXT_SECONDARY);
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Availability
        String availText = book.getAvailableCopies() + "/" + book.getTotalCopies() + " có sẵn";
        Color availColor = book.getAvailableCopies() > 0 ? new Color(34, 197, 94) : new Color(239, 68, 68);
        JLabel availLabel = new JLabel("● " + availText);
        availLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        availLabel.setForeground(availColor);
        availLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(authorLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(idLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(availLabel);

        // Bottom: Action buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton editBtn = createSmallButton("✏️ Sửa", new Color(59, 130, 246));
        editBtn.addActionListener(e -> showEditBookDialog(book));

        JButton deleteBtn = createSmallButton("🗑️ Xóa", new Color(239, 68, 68));
        deleteBtn.addActionListener(e -> handleDeleteBook(book));

        actionsPanel.add(editBtn);
        actionsPanel.add(deleteBtn);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(actionsPanel, BorderLayout.SOUTH);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(AppConstants.PRIMARY_COLOR, 2, 12),
                        new EmptyBorder(13, 13, 13, 13)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new EmptyBorder(15, 15, 15, 15));
            }
        });

        return card;
    }

    private JButton createSmallButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(color);
        btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(color, 1, 6),
                new EmptyBorder(5, 10, 5, 10)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String getBookIcon(String category) {
        if (category == null) return "📚";
        switch (category) {
            case "Văn học": return "📗";
            case "Khoa học": return "📘";
            case "Kỹ năng": return "📙";
            case "Lịch sử": return "📕";
            case "Công nghệ": return "💻";
            default: return "📚";
        }
    }

    private Color getCategoryColor(String category) {
        if (category == null) return new Color(107, 114, 128);
        switch (category) {
            case "Văn học": return new Color(34, 197, 94);
            case "Khoa học": return new Color(59, 130, 246);
            case "Kỹ năng": return new Color(249, 115, 22);
            case "Lịch sử": return new Color(239, 68, 68);
            case "Công nghệ": return new Color(139, 92, 246);
            default: return new Color(107, 114, 128);
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================
    private void showAddBookDialog() {
        showBookDialog(null, "Thêm Sách Mới");
    }

    private void showEditBookDialog(BookInfo book) {
        showBookDialog(book, "Sửa Thông Tin Sách");
    }

    private void showBookDialog(BookInfo book, String title) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Form fields
        JTextField bookIdField = createFormField(book != null ? book.getBookId() : "", book != null);
        JTextField titleField = createFormField(book != null ? book.getTitle() : "", false);
        JTextField authorField = createFormField(book != null ? book.getAuthor() : "", false);
        JTextField isbnField = createFormField(book != null ? book.getIsbn() : "", false);
        JTextField publisherField = createFormField(book != null ? book.getPublisher() : "", false);
        JTextField yearField = createFormField(book != null && book.getPublishYear() > 0 ? String.valueOf(book.getPublishYear()) : "", false);
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Văn học", "Khoa học", "Kỹ năng", "Lịch sử", "Công nghệ", "Khác"});
        if (book != null && book.getCategory() != null) categoryCombo.setSelectedItem(book.getCategory());
        JTextField totalField = createFormField(book != null ? String.valueOf(book.getTotalCopies()) : "1", false);
        JTextField locationField = createFormField(book != null ? book.getLocation() : "", false);

        // Add to form
        int row = 0;
        addFormRow(formPanel, gbc, row++, "Mã sách *:", bookIdField);
        addFormRow(formPanel, gbc, row++, "Tên sách *:", titleField);
        addFormRow(formPanel, gbc, row++, "Tác giả *:", authorField);
        addFormRow(formPanel, gbc, row++, "ISBN:", isbnField);
        addFormRow(formPanel, gbc, row++, "Nhà xuất bản:", publisherField);
        addFormRow(formPanel, gbc, row++, "Năm xuất bản:", yearField);
        addFormRowCombo(formPanel, gbc, row++, "Thể loại:", categoryCombo);
        addFormRow(formPanel, gbc, row++, "Số lượng *:", totalField);
        addFormRow(formPanel, gbc, row++, "Vị trí kệ:", locationField);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        btnPanel.setBackground(new Color(249, 250, 251));

        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = UIComponentFactory.createPrimaryButton(book == null ? "Thêm Sách" : "Lưu Thay Đổi");
        saveBtn.addActionListener(e -> {
            // Validate
            if (bookIdField.getText().trim().isEmpty() || 
                titleField.getText().trim().isEmpty() || 
                authorField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng điền đầy đủ các trường bắt buộc (*)", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int total = Integer.parseInt(totalField.getText().trim());
                if (total < 1) throw new NumberFormatException();

                // Create/Update book
                BookInfo newBook = book != null ? book : new BookInfo();
                newBook.setBookId(bookIdField.getText().trim());
                newBook.setTitle(titleField.getText().trim());
                newBook.setAuthor(authorField.getText().trim());
                newBook.setIsbn(isbnField.getText().trim());
                newBook.setPublisher(publisherField.getText().trim());
                newBook.setCategory((String) categoryCombo.getSelectedItem());
                newBook.setTotalCopies(total);
                newBook.setAvailableCopies(book == null ? total : book.getAvailableCopies());
                newBook.setLocation(locationField.getText().trim());
                
                if (!yearField.getText().trim().isEmpty()) {
                    newBook.setPublishYear(Integer.parseInt(yearField.getText().trim()));
                }

                // Try API first
                if (apiManager.isServerAvailable()) {
                    try {
                        JsonObject data = new JsonObject();
                        data.addProperty("bookId", newBook.getBookId());
                        data.addProperty("title", newBook.getTitle());
                        data.addProperty("author", newBook.getAuthor());
                        data.addProperty("isbn", newBook.getIsbn());
                        data.addProperty("publisher", newBook.getPublisher());
                        data.addProperty("publishYear", newBook.getPublishYear());
                        data.addProperty("category", newBook.getCategory());
                        data.addProperty("totalCopies", newBook.getTotalCopies());
                        data.addProperty("availableCopies", newBook.getAvailableCopies());
                        data.addProperty("location", newBook.getLocation());
                        
                        if (book == null) {
                            inventoryApi.createBook(data);
                        } else {
                            inventoryApi.updateBook(book.getBookId(), data);
                        }
                    } catch (Exception ex) {
                        System.err.println("API Error: " + ex.getMessage());
                    }
                }
                
                // Update local list
                if (book == null) {
                    allBooks.add(newBook);
                }
                
                // Cập nhật local cache
                updateLocalCache();
                
                totalBooksLabel.setText(allBooks.size() + " cuốn sách");
                filterBooks();
                dialog.dispose();
                
                JOptionPane.showMessageDialog(this, 
                    book == null ? "Thêm sách thành công!" : "Cập nhật thành công!", 
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Số lượng phải là số dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JTextField createFormField(String value, boolean readonly) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppConstants.BORDER_COLOR, 1, 6),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setEditable(!readonly);
        if (readonly) {
            field.setBackground(new Color(249, 250, 251));
        }
        return field;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private void addFormRowCombo(JPanel panel, GridBagConstraints gbc, int row, String label, JComboBox<?> combo) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(combo, gbc);
    }

    private void handleDeleteBook(BookInfo book) {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa sách:\n\"" + book.getTitle() + "\" (" + book.getBookId() + ")?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // Try API first
            if (apiManager.isServerAvailable()) {
                try {
                    inventoryApi.deleteBook(book.getBookId());
                } catch (Exception ex) {
                    System.err.println("API Error: " + ex.getMessage());
                }
            }
            
            allBooks.remove(book);
            updateLocalCache();
            totalBooksLabel.setText(allBooks.size() + " cuốn sách");
            filterBooks();
            JOptionPane.showMessageDialog(this, "Đã xóa sách thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==========================================
    // EXCEL IMPORT
    // ==========================================
    private void handleImportExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file Excel để import");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            SwingWorker<List<BookInfo>, Void> worker = new SwingWorker<List<BookInfo>, Void>() {
                @Override
                protected List<BookInfo> doInBackground() throws Exception {
                    return parseExcelFile(file);
                }

                @Override
                protected void done() {
                    try {
                        List<BookInfo> importedBooks = get();
                        if (importedBooks.isEmpty()) {
                            JOptionPane.showMessageDialog(BookManagementPage.this,
                                    "Không tìm thấy dữ liệu hợp lệ trong file Excel!", 
                                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        showImportPreviewDialog(importedBooks);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(BookManagementPage.this,
                                "Lỗi đọc file Excel: " + e.getMessage(), 
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private List<BookInfo> parseExcelFile(File file) throws Exception {
        List<BookInfo> books = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            if (file.getName().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;
            
            for (Row row : sheet) {
                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                
                // Parse row
                BookInfo book = new BookInfo();
                book.setBookId(getCellString(row.getCell(0)));
                book.setTitle(getCellString(row.getCell(1)));
                book.setAuthor(getCellString(row.getCell(2)));
                book.setIsbn(getCellString(row.getCell(3)));
                book.setPublisher(getCellString(row.getCell(4)));
                book.setPublishYear(getCellInt(row.getCell(5)));
                book.setCategory(getCellString(row.getCell(6)));
                book.setDescription(getCellString(row.getCell(7)));
                book.setTotalCopies(Math.max(1, getCellInt(row.getCell(8))));
                book.setAvailableCopies(book.getTotalCopies());
                book.setLocation(getCellString(row.getCell(9)));
                book.setStatus("Có sẵn");
                
                // Validate required fields
                if (!book.getBookId().isEmpty() && !book.getTitle().isEmpty()) {
                    books.add(book);
                }
            }
            
            workbook.close();
        }
        
        return books;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private int getCellInt(Cell cell) {
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    private void showImportPreviewDialog(List<BookInfo> books) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Preview Import", true);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(249, 250, 251));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("📥 Preview dữ liệu import (Chọn sách muốn import)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(titleLabel, BorderLayout.WEST);
        
        // Select all / Deselect all buttons
        JPanel selectBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        selectBtns.setOpaque(false);
        JButton selectAllBtn = new JButton("Chọn tất cả");
        JButton deselectAllBtn = new JButton("Bỏ chọn tất cả");
        selectBtns.add(selectAllBtn);
        selectBtns.add(deselectAllBtn);
        header.add(selectBtns, BorderLayout.EAST);

        // Table with checkbox
        String[] columns = {"Chọn", "Mã sách", "Tên sách", "Tác giả", "Thể loại", "Số lượng", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only checkbox is editable
            }
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
        };

        List<String> existingIds = new ArrayList<>();
        for (BookInfo b : allBooks) existingIds.add(b.getBookId());

        for (BookInfo book : books) {
            boolean isDuplicate = existingIds.contains(book.getBookId());
            String status = isDuplicate ? "⚠️ Trùng mã" : "✅ Sẵn sàng";
            model.addRow(new Object[]{
                    !isDuplicate, // Pre-select non-duplicates
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCategory(),
                    book.getTotalCopies(),
                    status
            });
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getColumnModel().getColumn(0).setMaxWidth(50); // Checkbox column width
        
        // Select all / Deselect all actions
        selectAllBtn.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(true, i, 0);
            }
        });
        deselectAllBtn.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(false, i, 0);
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        btnPanel.setBackground(new Color(249, 250, 251));

        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton importBtn = UIComponentFactory.createSuccessButton("Import sách đã chọn");
        importBtn.addActionListener(e -> {
            int added = 0;
            int skipped = 0;
            
            for (int i = 0; i < model.getRowCount(); i++) {
                boolean selected = (Boolean) model.getValueAt(i, 0);
                if (!selected) continue; // Skip unselected
                
                BookInfo book = books.get(i);
                
                if (existingIds.contains(book.getBookId())) {
                    skipped++;
                    continue;
                }
                
                // Try API
                if (apiManager.isServerAvailable()) {
                    try {
                        JsonObject data = new JsonObject();
                        data.addProperty("bookId", book.getBookId());
                        data.addProperty("title", book.getTitle());
                        data.addProperty("author", book.getAuthor());
                        data.addProperty("isbn", book.getIsbn());
                        data.addProperty("publisher", book.getPublisher());
                        data.addProperty("publishYear", book.getPublishYear());
                        data.addProperty("category", book.getCategory());
                        data.addProperty("description", book.getDescription());
                        data.addProperty("totalCopies", book.getTotalCopies());
                        data.addProperty("availableCopies", book.getAvailableCopies());
                        data.addProperty("location", book.getLocation());
                        inventoryApi.createBook(data);
                    } catch (Exception ex) {
                        System.err.println("API Error importing book: " + ex.getMessage());
                    }
                }
                
                allBooks.add(book);
                existingIds.add(book.getBookId());
                added++;
            }
            
            dialog.dispose();
            updateLocalCache();
            totalBooksLabel.setText(allBooks.size() + " cuốn sách");
            filterBooks();
            
            String msg = "Import hoàn tất!\n✅ Thêm thành công: " + added + " sách";
            if (skipped > 0) {
                msg += "\n⚠️ Bỏ qua (trùng mã): " + skipped + " sách";
            }
            JOptionPane.showMessageDialog(this, msg, "Kết quả Import", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(importBtn);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
