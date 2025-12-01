package service;

import com.licel.jcardsim.base.Simulator;
import applet.AppletConstants;
import models.CardInfo;
import javacard.framework.AID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for handling JavaCard simulator operations
 * [FIXED] Sửa lỗi import và tên biến
 */
public class SimulatorService {
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    private int pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;
    private String currentStudentCode = "";
    private String currentRole = "Sinh viên";  // Default role

    // Cấu hình PBKDF2
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int HASH_BIT_LENGTH = 256; // 256 bits = 32 bytes

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isPinVerified() {
        return isPinVerified;
    }

    public void setPinVerified(boolean pinVerified) {
        isPinVerified = pinVerified;
    }

    public int getPinTriesRemaining() {
        return pinTriesRemaining;
    }

    public void setPinTriesRemaining(int tries) {
        this.pinTriesRemaining = tries;
    }

    public String getCurrentStudentCode() {
        return currentStudentCode;
    }

    public void setCurrentStudentCode(String studentCode) {
        this.currentStudentCode = studentCode;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String role) {
        this.currentRole = role;
    }

    /**
     * Kiểm tra thẻ sinh viên có tồn tại trong hệ thống không
     */
    public boolean isCardExists(String studentCode) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy thông tin thẻ theo MSSV
     */
    public CardInfo getCardByStudentCode(String studentCode) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) {
                return card;
            }
        }
        return null;
    }

    /**
     * Xác thực PIN của sinh viên (không phải Admin)
     */
    public boolean verifyStudentPin(String studentCode, String pin) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            return card.getPin().equals(pin);
        }
        return false;
    }

    /**
     * Đổi PIN cho sinh viên
     */
    public boolean changeStudentPin(String studentCode, String oldPin, String newPin) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null && card.getPin().equals(oldPin)) {
            card.setPin(newPin);
            return true;
        }
        return false;
    }

    // --- QUẢN LÝ SÁCH MƯỢN ---
    
    // Mock data cho sách có thể mượn
    private static final String[][] AVAILABLE_BOOKS = {
        {"NV001", "Nhà Giả Kim"},
        {"DB002", "Đắc Nhân Tâm"},
        {"TH003", "Trên Đường Băng"},
        {"CX004", "Cà Phê Cùng Tony"},
        {"HP005", "Harry Potter"},
        {"LT006", "Lược Sử Thời Gian"},
        {"TN007", "Tuổi Trẻ Đáng Giá Bao Nhiêu"},
        {"NL008", "Nhà Lãnh Đạo Không Chức Danh"}
    };
    
    // Lưu sách mượn theo studentCode
    private java.util.Map<String, java.util.List<models.BorrowedBook>> studentBooks = new java.util.HashMap<>();
    
    /**
     * Lấy danh sách sách đang mượn của sinh viên
     */
    public java.util.List<models.BorrowedBook> getBorrowedBooks(String studentCode) {
        return studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
    }

    /**
     * Thêm sách mượn (dùng cho mock UI BorrowedBooksPage)
     */
    public void addBorrowedBook(String studentCode, models.BorrowedBook book) {
        java.util.List<models.BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        books.add(book);
        studentBooks.put(studentCode, books);

        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            card.setBorrowedBooks(books.size());
        }
    }

    /**
     * Xóa sách mượn (dùng cho mock UI BorrowedBooksPage)
     */
    public void removeBorrowedBook(String studentCode, String bookId) {
        java.util.List<models.BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        models.BorrowedBook toRemove = null;
        for (models.BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) {
                toRemove = b;
                break;
            }
        }
        if (toRemove != null) {
            books.remove(toRemove);
            studentBooks.put(studentCode, books);

            CardInfo card = getCardByStudentCode(studentCode);
            if (card != null) {
                card.setBorrowedBooks(books.size());
            }
        }
    }
    
    /**
     * Mượn sách
     */
    public String borrowBook(String studentCode, String bookId) {
        // Tìm sách trong danh sách có sẵn
        String bookName = null;
        for (String[] book : AVAILABLE_BOOKS) {
            if (book[0].equalsIgnoreCase(bookId)) {
                bookName = book[1];
                break;
            }
        }
        
        if (bookName == null) {
            return "Không tìm thấy sách với mã: " + bookId;
        }
        
        // Kiểm tra đã mượn chưa
        java.util.List<models.BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        for (models.BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) {
                return "Bạn đã mượn sách này rồi!";
            }
        }
        
        // Kiểm tra giới hạn mượn
        if (books.size() >= 5) {
            return "Bạn đã mượn tối đa 5 cuốn sách!";
        }
        
        // Tạo ngày mượn và hạn trả
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate dueDate = today.plusDays(14);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        models.BorrowedBook newBook = new models.BorrowedBook(
            bookId.toUpperCase(), 
            bookName, 
            today.format(fmt), 
            dueDate.format(fmt), 
            "Đang mượn", 
            0
        );
        
        books.add(newBook);
        studentBooks.put(studentCode, books);
        
        // Cập nhật số sách trong CardInfo
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            card.setBorrowedBooks(books.size());
        }
        
        return null; // Success
    }
    
    /**
     * Trả sách
     */
    public String returnBook(String studentCode, String bookId) {
        java.util.List<models.BorrowedBook> books = studentBooks.get(studentCode);
        if (books == null || books.isEmpty()) {
            return "Bạn không có sách nào để trả!";
        }
        
        models.BorrowedBook toRemove = null;
        int fine = 0;
        for (models.BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) {
                toRemove = b;
                fine = b.getOverdueDays() * 5000; // 5000 VND per day
                break;
            }
        }
        
        if (toRemove == null) {
            return "Không tìm thấy sách " + bookId + " trong danh sách mượn!";
        }
        
        books.remove(toRemove);
        
        // Cập nhật số sách trong CardInfo
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            card.setBorrowedBooks(books.size());
            
            // Trừ tiền phạt nếu trễ hạn
            if (fine > 0) {
                long newBalance = card.getBalance() - fine;
                card.setBalance(Math.max(0, newBalance));
                return "FINE:" + fine; // Báo có tiền phạt
            }
        }
        
        return null; // Success
    }
    
    /**
     * Lấy tên sách theo mã
     */
    public String getBookName(String bookId) {
        for (String[] book : AVAILABLE_BOOKS) {
            if (book[0].equalsIgnoreCase(bookId)) {
                return book[1];
            }
        }
        return null;
    }

    // --- QUẢN LÝ TÀI CHÍNH ---
    
    // Lưu lịch sử giao dịch theo studentCode
    private java.util.Map<String, java.util.List<models.Transaction>> studentTransactions = new java.util.HashMap<>();
    
    /**
     * Nạp tiền vào thẻ
     */
    public boolean deposit(String studentCode, long amount) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null) return false;
        
        card.setBalance(card.getBalance() + amount);
        
        // Thêm giao dịch
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        java.util.List<models.Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new models.Transaction(today.format(fmt), "Nạp tiền", amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        
        return true;
    }
    
    /**
     * Thanh toán phạt
     */
    public boolean payFine(String studentCode, long amount) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null || card.getBalance() < amount) return false;
        
        card.setBalance(card.getBalance() - amount);
        
        // Thêm giao dịch
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        java.util.List<models.Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new models.Transaction(today.format(fmt), "Thanh toán phạt", -amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        
        return true;
    }
    
    /**
     * Lấy lịch sử giao dịch
     */
    public java.util.List<models.Transaction> getTransactions(String studentCode) {
        return studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
    }
    
    /**
     * Lấy số dư hiện tại
     */
    public long getBalance(String studentCode) {
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getBalance() : 0;
    }

    public void connect() throws Exception {
        simulator = new Simulator();
        // Sử dụng AppletConstants.APPLET_AID thay vì DEFAULT_AID
        AID aid = new AID(AppletConstants.APPLET_AID, (short)0, (byte)AppletConstants.APPLET_AID.length);

        String appletClassName = "applet.LibraryCardApplet";
        Class<?> appletClass = Class.forName(appletClassName);

        @SuppressWarnings("unchecked")
        Class<? extends javacard.framework.Applet> appletClassCasted =
                (Class<? extends javacard.framework.Applet>) appletClass;

        simulator.installApplet(aid, appletClassCasted);
        simulator.selectApplet(aid);

        isConnected = true;
    }

    public byte[] sendCommand(byte[] command) {
        if (simulator == null) {
            throw new RuntimeException("Simulator chưa được khởi động!");
        }
        return simulator.transmitCommand(command);
    }

    public int getSW(byte[] response) {
        if (response.length < 2) return 0;
        int sw1 = response[response.length - 2] & 0xFF;
        int sw2 = response[response.length - 1] & 0xFF;
        return (sw1 << 8) | sw2;
    }

    // --- CÁC HÀM TIỆN ÍCH BẢO MẬT ---

    /**
     * Sinh Salt ngẫu nhiên
     */
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        // Sử dụng AppletConstants
        byte[] salt = new byte[AppletConstants.SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Tạo Hash từ PIN và Salt sử dụng PBKDF2WithHmacSHA256
     */
    private byte[] hashPin(char[] pin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, HASH_BIT_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    // --- CÁC HÀM NGHIỆP VỤ PIN ---

    /**
     * Tạo PIN lần đầu (Khởi tạo thẻ)
     */
    public void createDemoPin() throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        String defaultPin = AppletConstants.DEFAULT_PIN;

        // 1. Sinh Salt ngẫu nhiên tại Client
        byte[] salt = generateSalt();

        // 2. Tính Hash PBKDF2
        byte[] hash = hashPin(defaultPin.toCharArray(), salt);

        // 3. Đóng gói APDU: [INS_CREATE] [Lc] [SALT (16)] [HASH (32)]
        int dataLength = salt.length + hash.length;
        byte[] cmd = new byte[5 + dataLength];

        cmd[0] = 0x00;
        cmd[1] = AppletConstants.INS_CREATE_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)dataLength;

        // Copy Salt
        System.arraycopy(salt, 0, cmd, 5, salt.length);
        // Copy Hash
        System.arraycopy(hash, 0, cmd, 5 + salt.length, hash.length);

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000) {
            throw new RuntimeException("Không thể tạo PIN demo. Lỗi SW: " + String.format("%04X", getSW(resp)));
        }
    }

    /**
     * Xác thực PIN
     */
    public boolean verifyPin(char[] pinChars) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        // Bước 1: Lấy Salt từ thẻ
        byte[] getSaltCmd = {0x00, AppletConstants.INS_GET_SALT, 0x00, 0x00, 0x00};
        byte[] saltResp = sendCommand(getSaltCmd);

        if (getSW(saltResp) != 0x9000 || saltResp.length < AppletConstants.SALT_LENGTH + 2) {
            throw new RuntimeException("Không thể lấy Salt từ thẻ. Hãy chắc chắn thẻ đã được tạo PIN.");
        }

        // Tách Salt từ phản hồi
        byte[] salt = Arrays.copyOf(saltResp, AppletConstants.SALT_LENGTH);

        // Bước 2: Tính Hash
        byte[] hash = hashPin(pinChars, salt);

        // Bước 3: Gửi Hash xuống thẻ
        byte[] verifyCmd = new byte[5 + hash.length];
        verifyCmd[0] = 0x00;
        verifyCmd[1] = AppletConstants.INS_VERIFY_PIN;
        verifyCmd[2] = 0x00;
        verifyCmd[3] = 0x00;
        verifyCmd[4] = (byte)hash.length;
        System.arraycopy(hash, 0, verifyCmd, 5, hash.length);

        byte[] resp = sendCommand(verifyCmd);

        if (getSW(resp) == 0x9000 && resp.length > 2) {
            if (resp[0] == 0x01) {
                isPinVerified = true;
                pinTriesRemaining = 3;
                return true;
            } else {
                pinTriesRemaining = resp[1] & 0xFF;
                return false;
            }
        } else if (getSW(resp) == 0x6983) { // SW_FILE_INVALID = PIN Blocked
            pinTriesRemaining = 0;
            throw new Exception("Thẻ đã bị khóa (Block) do nhập sai quá 3 lần!");
        }

        return false;
    }

    /**
     * Đổi PIN
     */
    public boolean changePin(char[] oldPin, char[] newPin) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        // 1. Sinh Salt MỚI
        byte[] newSalt = generateSalt();

        // 2. Tính Hash MỚI
        byte[] newHash = hashPin(newPin, newSalt);

        // 3. Gửi lệnh đổi
        int dataLength = newSalt.length + newHash.length;
        byte[] cmd = new byte[5 + dataLength];

        cmd[0] = 0x00;
        cmd[1] = AppletConstants.INS_CHANGE_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)dataLength;

        System.arraycopy(newSalt, 0, cmd, 5, newSalt.length);
        System.arraycopy(newHash, 0, cmd, 5 + newSalt.length, newHash.length);

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) == 0x9000) {
            isPinVerified = false;
            return true;
        }

        return false;
    }

    // --- CÁC HÀM NGHIỆP VỤ CARD INFO ---

    // In-memory storage for multiple cards (GUI display)
    private List<CardInfo> cardList = new ArrayList<>();

    /**
     * Lưu thông tin thẻ vào JavaCard
     * Applet format: [CARD_ID (10 bytes)][NAME_LENGTH][NAME][EXPIRY_DATE (8 bytes)]
     */
    public boolean setCardInfo(CardInfo cardInfo) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }
        if (!isPinVerified) {
            throw new RuntimeException("Vui lòng xác thực PIN trước!");
        }

        // Prepare card ID (pad to 10 bytes)
        byte[] cardIdBytes = new byte[AppletConstants.CARD_ID_LENGTH];
        byte[] studentIdBytes = cardInfo.getStudentId().getBytes(StandardCharsets.UTF_8);
        int copyLen = Math.min(studentIdBytes.length, AppletConstants.CARD_ID_LENGTH);
        System.arraycopy(studentIdBytes, 0, cardIdBytes, 0, copyLen);

        // Prepare name
        byte[] nameBytes = cardInfo.getHolderName().getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > AppletConstants.NAME_MAX_LENGTH) {
            nameBytes = Arrays.copyOf(nameBytes, AppletConstants.NAME_MAX_LENGTH);
        }

        // Prepare expiry date (use birthDate as expiry for now, pad to 8 bytes)
        byte[] expiryBytes = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
        String dateStr = cardInfo.getBirthDate().replace("/", "");
        byte[] dateBytes = dateStr.getBytes(StandardCharsets.UTF_8);
        int dateCopyLen = Math.min(dateBytes.length, AppletConstants.EXPIRY_DATE_LENGTH);
        System.arraycopy(dateBytes, 0, expiryBytes, 0, dateCopyLen);

        // Build APDU: [CARD_ID (10)][NAME_LEN (1)][NAME][EXPIRY (8)]
        int dataLength = AppletConstants.CARD_ID_LENGTH + 1 + nameBytes.length + AppletConstants.EXPIRY_DATE_LENGTH;
        byte[] cmd = new byte[5 + dataLength];
        cmd[0] = 0x00;
        cmd[1] = AppletConstants.INS_SET_CARD_INFO;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)dataLength;

        int offset = 5;
        System.arraycopy(cardIdBytes, 0, cmd, offset, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;

        cmd[offset++] = (byte)nameBytes.length;
        System.arraycopy(nameBytes, 0, cmd, offset, nameBytes.length);
        offset += nameBytes.length;

        System.arraycopy(expiryBytes, 0, cmd, offset, AppletConstants.EXPIRY_DATE_LENGTH);

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) == 0x9000) {
            System.out.println("Card info saved successfully!");
            return true;
        } else {
            System.err.println("Failed to save card info. SW: " + String.format("%04X", getSW(resp)));
            return false;
        }
    }

    /**
     * Đọc thông tin thẻ từ JavaCard
     * Response: [CARD_ID (10)][NAME_LEN][NAME][EXPIRY (8)][NUM_BOOKS]
     */
    public CardInfo getCardInfo() throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        byte[] cmd = {0x00, AppletConstants.INS_GET_CARD_INFO, 0x00, 0x00, 0x00};
        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000) {
            throw new RuntimeException("Không thể đọc thông tin thẻ. SW: " + String.format("%04X", getSW(resp)));
        }

        CardInfo cardInfo = new CardInfo();
        int offset = 0;

        // Card ID (10 bytes)
        if (resp.length > offset + AppletConstants.CARD_ID_LENGTH) {
            String cardId = new String(resp, offset, AppletConstants.CARD_ID_LENGTH, StandardCharsets.UTF_8).trim();
            cardInfo.setStudentId(cardId);
            offset += AppletConstants.CARD_ID_LENGTH;
        }

        // Name (length + data)
        if (offset < resp.length - 2) {
            int nameLen = resp[offset++] & 0xFF;
            if (nameLen > 0 && offset + nameLen <= resp.length - 2) {
                cardInfo.setHolderName(new String(resp, offset, nameLen, StandardCharsets.UTF_8));
                offset += nameLen;
            }
        }

        // Expiry Date (8 bytes)
        if (offset + AppletConstants.EXPIRY_DATE_LENGTH <= resp.length - 2) {
            String expiry = new String(resp, offset, AppletConstants.EXPIRY_DATE_LENGTH, StandardCharsets.UTF_8).trim();
            cardInfo.setBirthDate(expiry);
            offset += AppletConstants.EXPIRY_DATE_LENGTH;
        }

        // Number of borrowed books
        if (offset < resp.length - 2) {
            int numBooks = resp[offset] & 0xFF;
            cardInfo.setBorrowedBooks(numBooks);
        }

        return cardInfo;
    }

    /**
     * Thêm thẻ vào danh sách quản lý (in-memory)
     */
    public void addCardToList(CardInfo cardInfo) {
        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i).getStudentId().equals(cardInfo.getStudentId())) {
                cardList.set(i, cardInfo);
                return;
            }
        }
        cardList.add(cardInfo);
    }

    /**
     * Lấy danh sách tất cả thẻ
     */
    public List<CardInfo> getAllCards() {
        return new ArrayList<>(cardList);
    }

    /**
     * Tìm thẻ theo MSSV hoặc tên
     */
    public List<CardInfo> searchCards(String keyword) {
        List<CardInfo> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (CardInfo card : cardList) {
            if (card.getStudentId().toLowerCase().contains(lowerKeyword) ||
                card.getHolderName().toLowerCase().contains(lowerKeyword)) {
                results.add(card);
            }
        }
        return results;
    }

    /**
     * Cập nhật trạng thái thẻ (khóa/mở khóa)
     */
    public boolean toggleCardStatus(String studentId) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equals(studentId)) {
                String newStatus = card.getStatus().equals("Hoạt động") ? "Khóa" : "Hoạt động";
                card.setStatus(newStatus);
                return true;
            }
        }
        return false;
    }
}