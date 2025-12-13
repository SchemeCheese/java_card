package service;

import com.licel.jcardsim.base.Simulator;
import applet.AppletConstants;
import models.CardInfo;
import models.BorrowedBook;
import models.Transaction;
import api.ApiClient;
import api.CardApiService;
import api.BookApiService;
import api.TransactionApiService;
import javacard.framework.AID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for handling JavaCard simulator operations
 * [UPDATED] Tích hợp với Server API thật (MySQL)
 */
public class SimulatorService {
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    private int pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;
    private String currentStudentCode = "";
    private String currentRole = "Sinh viên";  // Default role

    // API Services - kết nối với Server thật
    private ApiClient apiClient;
    private CardApiService cardApiService;
    private BookApiService bookApiService;
    private TransactionApiService transactionApiService;
    private boolean useServerApi = false; // Flag để chuyển đổi giữa mock và API thật

    // In-memory storage for multiple cards (GUI display) - fallback khi không có server
    private List<CardInfo> cardList = new ArrayList<>();
    
    public SimulatorService() {
        // Khởi tạo API services
        apiClient = new ApiClient();
        cardApiService = new CardApiService();
        bookApiService = new BookApiService();
        transactionApiService = new TransactionApiService();
        
        // Kiểm tra server có sẵn không
        useServerApi = apiClient.isServerAvailable();
        if (useServerApi) {
            System.out.println("✅ Server API available - using real database");
        } else {
            System.out.println("⚠️ Server API not available - using mock data");
        }
    }

    // Cấu hình PBKDF2
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int HASH_BIT_LENGTH = 256;

    // --- GETTERS / SETTERS ---
    public boolean isConnected() { return isConnected; }
    public boolean isPinVerified() { return isPinVerified; }
    public void setPinVerified(boolean pinVerified) { isPinVerified = pinVerified; }
    public int getPinTriesRemaining() { return pinTriesRemaining; }
    public void setPinTriesRemaining(int tries) { this.pinTriesRemaining = tries; }
    public String getCurrentStudentCode() { return currentStudentCode; }
    public void setCurrentStudentCode(String studentCode) { this.currentStudentCode = studentCode; }
    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String role) { this.currentRole = role; }

    // --- BẢO MẬT: Kiểm tra PIN mặc định ---

    public boolean isChangePinRequired() {
        // Admin không cần đổi PIN
        if ("Admin".equals(currentRole)) return false;
        if (currentStudentCode.isEmpty()) return false;

        // Gọi API kiểm tra PIN có phải mặc định không
        if (useServerApi) {
            try {
                ApiClient.ApiResponse response = apiClient.get("/pin/is-default/" + currentStudentCode);
                if (response.isSuccess() && response.getData() != null) {
                    boolean isDefault = response.getData().get("isDefaultPin").getAsBoolean();
                    System.out.println("[DEBUG] isChangePinRequired for " + currentStudentCode + ": " + isDefault);
                    return isDefault;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Fallback: không yêu cầu đổi nếu không check được
        return false;
    }

    public boolean unlockCardByAdminPin(String studentId, String adminPinInput) {
        try {
            boolean isAdminCorrect = verifyPin(adminPinInput.toCharArray());
            if (isAdminCorrect) {
                return toggleCardStatus(studentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- QUẢN LÝ THẺ & PIN SINH VIÊN ---

    public boolean isCardExists(String studentCode) {
        if (useServerApi) {
            try {
                CardInfo card = cardApiService.getCard(studentCode);
                return card != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback to local
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) return true;
        }
        return false;
    }

    public CardInfo getCardByStudentCode(String studentCode) {
        if (useServerApi) {
            try {
                return cardApiService.getCard(studentCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback to local
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) return card;
        }
        return null;
    }

    public boolean verifyStudentPin(String studentCode, String pin) {
        if (useServerApi) {
            try {
                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("studentId", studentCode);
                body.addProperty("pin", pin);
                ApiClient.ApiResponse response = apiClient.post("/pin/verify", body);
                
                if (response.isSuccess()) {
                    com.google.gson.JsonObject data = response.getData();
                    if (data.has("triesRemaining")) {
                        pinTriesRemaining = data.get("triesRemaining").getAsInt();
                    }
                    return true;
                } else {
                    if (response.getData() != null && response.getData().has("triesRemaining")) {
                        pinTriesRemaining = response.getData().get("triesRemaining").getAsInt();
                    }
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback to local mock
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            if ("Khóa".equals(card.getStatus())) return false;
            if (card.getPin().equals(pin)) {
                card.setPinRetryCount(3);
                return true;
            } else {
                int remaining = card.getPinRetryCount() - 1;
                card.setPinRetryCount(Math.max(0, remaining));
                if (remaining <= 0) card.setStatus("Khóa");
                return false;
            }
        }
        return false;
    }

    public int getStudentPinTries(String studentCode) {
        if (useServerApi) {
            return pinTriesRemaining;
        }
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getPinRetryCount() : 0;
    }

    public boolean changeStudentPin(String studentCode, String oldPin, String newPin) {
        if (useServerApi) {
            try {
                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("studentId", studentCode);
                body.addProperty("oldPin", oldPin);
                body.addProperty("newPin", newPin);
                System.out.println("[DEBUG] Changing PIN for: " + studentCode + ", oldPin length: " + oldPin.length() + ", newPin length: " + newPin.length());
                ApiClient.ApiResponse response = apiClient.post("/pin/change", body);
                System.out.println("[DEBUG] Change PIN response: success=" + response.isSuccess() + ", message=" + response.getMessage());
                if (response.isSuccess()) {
                    return true;
                }
                // Log lỗi chi tiết
                if (response.getData() != null) {
                    System.out.println("[DEBUG] Error data: " + response.getData().toString());
                }
                return false;
            } catch (Exception e) {
                System.out.println("[DEBUG] Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // Fallback
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null && card.getPin().equals(oldPin)) {
            card.setPin(newPin);
            return true;
        }
        return false;
    }

    /**
     * Khóa/Mở khóa thẻ
     */
    public boolean toggleCardStatus(String studentId) {
        if (useServerApi) {
            try {
                CardInfo card = cardApiService.getCard(studentId);
                if (card != null) {
                    String newStatus = "Hoạt động".equals(card.getStatus()) ? "Khóa" : "Hoạt động";
                    com.google.gson.JsonObject updates = new com.google.gson.JsonObject();
                    updates.addProperty("status", newStatus);
                    cardApiService.updateCard(studentId, updates);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback
        for (CardInfo card : cardList) {
            if (card.getStudentId().equals(studentId)) {
                String newStatus = "Hoạt động".equals(card.getStatus()) ? "Khóa" : "Hoạt động";
                card.setStatus(newStatus);
                if ("Hoạt động".equals(newStatus)) {
                    card.setPinRetryCount(3);
                    card.setPin(AppletConstants.DEFAULT_PIN);
                }
                return true;
            }
        }
        return false;
    }

    public void addCardToList(CardInfo cardInfo) {
        if (useServerApi) {
            try {
                cardApiService.createCard(
                    cardInfo.getStudentId(),
                    cardInfo.getHolderName(),
                    cardInfo.getEmail(),
                    cardInfo.getDepartment(),
                    cardInfo.getBirthDate(),
                    cardInfo.getAddress(),
                    cardInfo.getPin()
                );
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback
        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i).getStudentId().equals(cardInfo.getStudentId())) {
                cardList.set(i, cardInfo);
                return;
            }
        }
        cardList.add(cardInfo);
    }

    public List<CardInfo> getAllCards() {
        if (useServerApi) {
            try {
                return cardApiService.getAllCards(1, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(cardList);
    }

    public List<CardInfo> searchCards(String keyword) {
        List<CardInfo> allCards = getAllCards();
        List<CardInfo> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (CardInfo card : allCards) {
            if (card.getStudentId().toLowerCase().contains(lowerKeyword) ||
                    card.getHolderName().toLowerCase().contains(lowerKeyword)) {
                results.add(card);
            }
        }
        return results;
    }

    // --- QUẢN LÝ SÁCH ---
    private static final String[][] AVAILABLE_BOOKS = {
            {"NV001", "Nhà Giả Kim"}, {"DB002", "Đắc Nhân Tâm"},
            {"TH003", "Trên Đường Băng"}, {"CX004", "Cà Phê Cùng Tony"},
            {"HP005", "Harry Potter"}, {"LT006", "Lược Sử Thời Gian"},
            {"TN007", "Tuổi Trẻ Đáng Giá"}, {"NL008", "Nhà Lãnh Đạo"}
    };

    private java.util.Map<String, java.util.List<BorrowedBook>> studentBooks = new java.util.HashMap<>();

    public java.util.List<BorrowedBook> getBorrowedBooks(String studentCode) {
        if (useServerApi) {
            try {
                return bookApiService.getBorrowedBooksByStudent(studentCode, null, 1, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
    }

    public void addBorrowedBook(String studentCode, BorrowedBook book) {
        java.util.List<BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        books.add(book);
        studentBooks.put(studentCode, books);
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) card.setBorrowedBooks(books.size());
    }

    public void removeBorrowedBook(String studentCode, String bookId) {
        java.util.List<BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        BorrowedBook toRemove = null;
        for (BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) {
                toRemove = b;
                break;
            }
        }
        if (toRemove != null) {
            books.remove(toRemove);
            studentBooks.put(studentCode, books);
            CardInfo card = getCardByStudentCode(studentCode);
            if (card != null) card.setBorrowedBooks(books.size());
        }
    }

    public String borrowBook(String studentCode, String bookId) {
        if (useServerApi) {
            try {
                // Lấy tên sách từ server
                java.util.Date dueDate = java.util.Date.from(
                    java.time.LocalDate.now().plusDays(14)
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                );
                bookApiService.borrowBook(studentCode, bookId, "Sách " + bookId, dueDate);
                return null; // Success
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        
        // Fallback to mock
        String bookName = getBookName(bookId);
        if (bookName == null) return "Không tìm thấy sách với mã: " + bookId;

        java.util.List<BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        for (BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) return "Bạn đã mượn sách này rồi!";
        }
        if (books.size() >= 5) return "Bạn đã mượn tối đa 5 cuốn sách!";

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate dueDate = today.plusDays(14);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        BorrowedBook newBook = new BorrowedBook(
                bookId.toUpperCase(), bookName, today.format(fmt), dueDate.format(fmt), "Đang mượn", 0
        );
        books.add(newBook);
        studentBooks.put(studentCode, books);

        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) card.setBorrowedBooks(books.size());
        return null;
    }

    public String returnBook(String studentCode, String bookId) {
        // Note: Server API cần borrowId, không phải bookId
        // Fallback to mock for now
        java.util.List<BorrowedBook> books = studentBooks.get(studentCode);
        if (books == null || books.isEmpty()) return "Bạn không có sách nào để trả!";

        BorrowedBook toRemove = null;
        int fine = 0;
        for (BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) {
                toRemove = b;
                fine = b.getOverdueDays() * 5000;
                break;
            }
        }
        if (toRemove == null) return "Không tìm thấy sách trong danh sách mượn!";

        books.remove(toRemove);
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            card.setBorrowedBooks(books.size());
            if (fine > 0) {
                long newBalance = card.getBalance() - fine;
                card.setBalance(Math.max(0, newBalance));
                return "FINE:" + fine;
            }
        }
        return null;
    }

    public String getBookName(String bookId) {
        for (String[] book : AVAILABLE_BOOKS) {
            if (book[0].equalsIgnoreCase(bookId)) return book[1];
        }
        return null;
    }

    // --- QUẢN LÝ TÀI CHÍNH ---
    private java.util.Map<String, java.util.List<Transaction>> studentTransactions = new java.util.HashMap<>();

    public boolean deposit(String studentCode, long amount) {
        if (useServerApi) {
            try {
                CardInfo card = cardApiService.getCard(studentCode);
                if (card == null) return false;
                long balanceBefore = card.getBalance();
                long balanceAfter = balanceBefore + amount;
                
                // Tạo transaction
                transactionApiService.createTransaction(
                    studentCode, "Nạp tiền", amount, balanceBefore, balanceAfter, "Nạp tiền qua GUI"
                );
                
                // Cập nhật balance
                cardApiService.updateBalance(studentCode, amount);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Fallback
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null) return false;
        card.setBalance(card.getBalance() + amount);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.util.List<Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new Transaction(today.format(fmt), "Nạp tiền", amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        return true;
    }

    public boolean payFine(String studentCode, long amount) {
        if (useServerApi) {
            try {
                CardInfo card = cardApiService.getCard(studentCode);
                if (card == null || card.getBalance() < amount) return false;
                long balanceBefore = card.getBalance();
                long balanceAfter = balanceBefore - amount;
                
                transactionApiService.createTransaction(
                    studentCode, "Trả phạt", amount, balanceBefore, balanceAfter, "Thanh toán tiền phạt"
                );
                
                cardApiService.updateBalance(studentCode, -amount);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Fallback
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null || card.getBalance() < amount) return false;
        card.setBalance(card.getBalance() - amount);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.util.List<Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new Transaction(today.format(fmt), "Thanh toán phạt", -amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        return true;
    }

    public java.util.List<Transaction> getTransactions(String studentCode) {
        if (useServerApi) {
            try {
                return transactionApiService.getTransactionsByStudent(studentCode, null, null, null, null, 1, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
    }

    public long getBalance(String studentCode) {
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getBalance() : 0;
    }
    
    // Getter để kiểm tra trạng thái kết nối server
    public boolean isUsingServerApi() {
        return useServerApi;
    }

    // --- JAVACARD SIMULATOR & SECURITY (ADMIN) ---

    public void connect() throws Exception {
        simulator = new Simulator();
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
        if (simulator == null) throw new RuntimeException("Simulator chưa được khởi động!");
        return simulator.transmitCommand(command);
    }

    public int getSW(byte[] response) {
        if (response.length < 2) return 0;
        return ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    }

    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[AppletConstants.SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private byte[] hashPin(char[] pin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, HASH_BIT_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    public void createDemoPin() throws Exception {
        if (!isConnected) return;
        byte[] salt = generateSalt();
        byte[] hash = hashPin(AppletConstants.DEFAULT_PIN.toCharArray(), salt);
        int dataLength = salt.length + hash.length;
        byte[] cmd = new byte[5 + dataLength];
        cmd[0] = 0x00; cmd[1] = AppletConstants.INS_CREATE_PIN; cmd[4] = (byte)dataLength;
        System.arraycopy(salt, 0, cmd, 5, salt.length);
        System.arraycopy(hash, 0, cmd, 5 + salt.length, hash.length);
        sendCommand(cmd);
    }

    public boolean verifyPin(char[] pinChars) throws Exception {
        if (!isConnected) return false;
        byte[] getSaltCmd = {0x00, AppletConstants.INS_GET_SALT, 0x00, 0x00, 0x00};
        byte[] saltResp = sendCommand(getSaltCmd);
        if (getSW(saltResp) != 0x9000) return false;
        byte[] salt = Arrays.copyOf(saltResp, AppletConstants.SALT_LENGTH);
        byte[] hash = hashPin(pinChars, salt);
        byte[] verifyCmd = new byte[5 + hash.length];
        verifyCmd[0] = 0x00; verifyCmd[1] = AppletConstants.INS_VERIFY_PIN; verifyCmd[4] = (byte)hash.length;
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
        }
        return false;
    }

    public boolean changePin(char[] oldPin, char[] newPin) throws Exception {
        if (!isConnected) return false;
        byte[] newSalt = generateSalt();
        byte[] newHash = hashPin(newPin, newSalt);
        int dataLength = newSalt.length + newHash.length;
        byte[] cmd = new byte[5 + dataLength];
        cmd[0] = 0x00; cmd[1] = AppletConstants.INS_CHANGE_PIN; cmd[4] = (byte)dataLength;
        System.arraycopy(newSalt, 0, cmd, 5, newSalt.length);
        System.arraycopy(newHash, 0, cmd, 5 + newSalt.length, newHash.length);
        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
    }

    /**
     * Lưu thông tin thẻ xuống JavaCard qua APDU
     * Format: [CARD_ID (10 bytes)][NAME_LENGTH (1 byte)][NAME][EXPIRY_DATE (8 bytes)]
     */
    public boolean setCardInfo(CardInfo cardInfo) throws Exception {
        if (!isConnected) return false;

        String studentId = cardInfo.getStudentId();
        String holderName = cardInfo.getHolderName();
        
        // Chuẩn bị Card ID (10 bytes, padding với space nếu cần)
        byte[] cardIdBytes = new byte[AppletConstants.CARD_ID_LENGTH];
        byte[] studentIdBytes = studentId.getBytes("UTF-8");
        int copyLen = Math.min(studentIdBytes.length, AppletConstants.CARD_ID_LENGTH);
        System.arraycopy(studentIdBytes, 0, cardIdBytes, 0, copyLen);
        // Padding với space (0x20) nếu studentId ngắn hơn 10 bytes
        for (int i = copyLen; i < AppletConstants.CARD_ID_LENGTH; i++) {
            cardIdBytes[i] = 0x20;
        }

        // Chuẩn bị Name (max 50 bytes)
        byte[] nameBytes = holderName.getBytes("UTF-8");
        if (nameBytes.length > AppletConstants.NAME_MAX_LENGTH) {
            nameBytes = java.util.Arrays.copyOf(nameBytes, AppletConstants.NAME_MAX_LENGTH);
        }

        // Chuẩn bị Expiry Date (8 bytes - DDMMYYYY)
        // Mặc định: 4 năm từ ngày hiện tại
        java.time.LocalDate expiryDate = java.time.LocalDate.now().plusYears(4);
        String expiryStr = String.format("%02d%02d%04d", 
            expiryDate.getDayOfMonth(), expiryDate.getMonthValue(), expiryDate.getYear());
        byte[] expiryBytes = expiryStr.getBytes("UTF-8");

        // Tính tổng độ dài data: CardID(10) + NameLength(1) + Name + Expiry(8)
        int dataLength = AppletConstants.CARD_ID_LENGTH + 1 + nameBytes.length + AppletConstants.EXPIRY_DATE_LENGTH;

        // Tạo APDU command
        byte[] cmd = new byte[5 + dataLength];
        int offset = 0;
        cmd[offset++] = 0x00;                              // CLA
        cmd[offset++] = AppletConstants.INS_SET_CARD_INFO; // INS
        cmd[offset++] = 0x00;                              // P1
        cmd[offset++] = 0x00;                              // P2
        cmd[offset++] = (byte) dataLength;                 // Lc

        // Card ID (10 bytes)
        System.arraycopy(cardIdBytes, 0, cmd, offset, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;

        // Name Length (1 byte) + Name
        cmd[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, cmd, offset, nameBytes.length);
        offset += nameBytes.length;

        // Expiry Date (8 bytes)
        System.arraycopy(expiryBytes, 0, cmd, offset, AppletConstants.EXPIRY_DATE_LENGTH);

        // Gửi APDU
        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
    }

    /**
     * Đọc thông tin thẻ từ JavaCard qua APDU
     * Response: [CARD_ID (10 bytes)][NAME_LENGTH (1 byte)][NAME][EXPIRY_DATE (8 bytes)][NUM_BOOKS (1 byte)]
     */
    public CardInfo getCardInfo() throws Exception {
        if (!isConnected) return new CardInfo();

        // Gửi APDU GET_CARD_INFO
        byte[] cmd = {0x00, AppletConstants.INS_GET_CARD_INFO, 0x00, 0x00, 0x00};
        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000 || resp.length <= 2) {
            return new CardInfo();
        }

        // Parse response
        CardInfo cardInfo = new CardInfo();
        int offset = 0;

        // Card ID (10 bytes)
        String cardId = new String(resp, offset, AppletConstants.CARD_ID_LENGTH, "UTF-8").trim();
        cardInfo.setStudentId(cardId);
        offset += AppletConstants.CARD_ID_LENGTH;

        // Name Length + Name
        int nameLen = resp[offset++] & 0xFF;
        if (nameLen > 0 && offset + nameLen <= resp.length - 2) {
            String name = new String(resp, offset, nameLen, "UTF-8");
            cardInfo.setHolderName(name);
            offset += nameLen;
        }

        // Expiry Date (8 bytes - DDMMYYYY)
        if (offset + AppletConstants.EXPIRY_DATE_LENGTH <= resp.length - 2) {
            String expiryRaw = new String(resp, offset, AppletConstants.EXPIRY_DATE_LENGTH, "UTF-8");
            // Convert DDMMYYYY to DD/MM/YYYY for display
            if (expiryRaw.length() == 8) {
                String expiryFormatted = expiryRaw.substring(0, 2) + "/" + 
                                         expiryRaw.substring(2, 4) + "/" + 
                                         expiryRaw.substring(4, 8);
                cardInfo.setBirthDate(expiryFormatted); // Tạm dùng birthDate để lưu expiry
            }
            offset += AppletConstants.EXPIRY_DATE_LENGTH;
        }

        // Number of borrowed books (1 byte)
        if (offset < resp.length - 2) {
            int numBooks = resp[offset] & 0xFF;
            cardInfo.setBorrowedBooks(numBooks);
        }

        return cardInfo;
    }

    // ==================== BOOK MANAGEMENT - APDU ====================

    /**
     * Thêm sách mượn lên thẻ JavaCard qua APDU
     * Format: [BOOK_ID (8 bytes)]
     */
    public boolean addBorrowedBookToCard(String bookId) throws Exception {
        if (!isConnected || !isPinVerified) return false;

        // Chuẩn bị Book ID (8 bytes, padding với space nếu cần)
        byte[] bookIdBytes = new byte[AppletConstants.BOOK_ID_LENGTH];
        byte[] inputBytes = bookId.getBytes("UTF-8");
        int copyLen = Math.min(inputBytes.length, AppletConstants.BOOK_ID_LENGTH);
        System.arraycopy(inputBytes, 0, bookIdBytes, 0, copyLen);
        for (int i = copyLen; i < AppletConstants.BOOK_ID_LENGTH; i++) {
            bookIdBytes[i] = 0x20; // Padding với space
        }

        // Tạo APDU command
        byte[] cmd = new byte[5 + AppletConstants.BOOK_ID_LENGTH];
        cmd[0] = 0x00;                                    // CLA
        cmd[1] = AppletConstants.INS_ADD_BORROWED_BOOK;   // INS
        cmd[2] = 0x00;                                    // P1
        cmd[3] = 0x00;                                    // P2
        cmd[4] = AppletConstants.BOOK_ID_LENGTH;          // Lc
        System.arraycopy(bookIdBytes, 0, cmd, 5, AppletConstants.BOOK_ID_LENGTH);

        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
    }

    /**
     * Đọc danh sách sách đang mượn từ thẻ JavaCard qua APDU
     * Response: [NUM_BOOKS (1 byte)][BOOK_ID_1 (8 bytes)][BOOK_ID_2]...
     */
    public java.util.List<String> getBorrowedBooksFromCard() throws Exception {
        java.util.List<String> bookIds = new java.util.ArrayList<>();
        if (!isConnected) return bookIds;

        byte[] cmd = {0x00, AppletConstants.INS_GET_BORROWED_BOOKS, 0x00, 0x00, 0x00};
        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000 || resp.length <= 2) {
            return bookIds;
        }

        int numBooks = resp[0] & 0xFF;
        int offset = 1;

        for (int i = 0; i < numBooks && offset + AppletConstants.BOOK_ID_LENGTH <= resp.length - 2; i++) {
            String bookId = new String(resp, offset, AppletConstants.BOOK_ID_LENGTH, "UTF-8").trim();
            bookIds.add(bookId);
            offset += AppletConstants.BOOK_ID_LENGTH;
        }

        return bookIds;
    }

    /**
     * Trả sách - xóa khỏi thẻ JavaCard qua APDU
     * Format: [BOOK_ID (8 bytes)]
     */
    public boolean returnBookToCard(String bookId) throws Exception {
        if (!isConnected || !isPinVerified) return false;

        // Chuẩn bị Book ID (8 bytes)
        byte[] bookIdBytes = new byte[AppletConstants.BOOK_ID_LENGTH];
        byte[] inputBytes = bookId.getBytes("UTF-8");
        int copyLen = Math.min(inputBytes.length, AppletConstants.BOOK_ID_LENGTH);
        System.arraycopy(inputBytes, 0, bookIdBytes, 0, copyLen);
        for (int i = copyLen; i < AppletConstants.BOOK_ID_LENGTH; i++) {
            bookIdBytes[i] = 0x20;
        }

        byte[] cmd = new byte[5 + AppletConstants.BOOK_ID_LENGTH];
        cmd[0] = 0x00;                                // CLA
        cmd[1] = AppletConstants.INS_RETURN_BOOK;     // INS
        cmd[2] = 0x00;                                // P1
        cmd[3] = 0x00;                                // P2
        cmd[4] = AppletConstants.BOOK_ID_LENGTH;      // Lc
        System.arraycopy(bookIdBytes, 0, cmd, 5, AppletConstants.BOOK_ID_LENGTH);

        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
    }
}