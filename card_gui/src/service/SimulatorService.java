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
 * [FINAL UPDATED] Logic Mở khóa sẽ Reset PIN về 000000
 */
public class SimulatorService {
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    private int pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;
    private String currentStudentCode = "";
    private String currentRole = "Sinh viên";  // Default role

    // In-memory storage for multiple cards (GUI display)
    private List<CardInfo> cardList = new ArrayList<>();

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

    // --- [NEW LOGIC] BẢO MẬT NÂNG CAO ---

    public boolean isChangePinRequired() {
        if ("Admin".equals(currentRole)) return false;

        CardInfo card = getCardByStudentCode(currentStudentCode);
        if (card != null) {
            return AppletConstants.DEFAULT_PIN.equals(card.getPin());
        }
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

    // --- QUẢN LÝ THẺ & PIN SINH VIÊN (MOCK DB) ---

    public boolean isCardExists(String studentCode) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) return true;
        }
        return false;
    }

    public CardInfo getCardByStudentCode(String studentCode) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equalsIgnoreCase(studentCode)) return card;
        }
        return null;
    }

    public boolean verifyStudentPin(String studentCode, String pin) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) {
            if ("Khóa".equals(card.getStatus())) {
                return false;
            }

            if (card.getPin().equals(pin)) {
                card.setPinRetryCount(3);
                return true;
            } else {
                int remaining = card.getPinRetryCount() - 1;
                card.setPinRetryCount(Math.max(0, remaining));

                if (remaining <= 0) {
                    card.setStatus("Khóa");
                }
                return false;
            }
        }
        return false;
    }

    public int getStudentPinTries(String studentCode) {
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getPinRetryCount() : 0;
    }

    public boolean changeStudentPin(String studentCode, String oldPin, String newPin) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null && card.getPin().equals(oldPin)) {
            card.setPin(newPin);
            return true;
        }
        return false;
    }

    /**
     * [UPDATED] Khóa/Mở khóa thẻ.
     * Nếu Mở khóa -> Reset PIN về 000000 và Reset số lần thử về 3.
     */
    public boolean toggleCardStatus(String studentId) {
        for (CardInfo card : cardList) {
            if (card.getStudentId().equals(studentId)) {
                String currentStatus = card.getStatus();
                String newStatus = currentStatus.equals("Hoạt động") ? "Khóa" : "Hoạt động";

                card.setStatus(newStatus);

                // [LOGIC MỚI] Reset khi mở khóa
                if ("Hoạt động".equals(newStatus)) {
                    card.setPinRetryCount(3);
                    card.setPin(AppletConstants.DEFAULT_PIN); // Reset về 000000
                }
                return true;
            }
        }
        return false;
    }

    public void addCardToList(CardInfo cardInfo) {
        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i).getStudentId().equals(cardInfo.getStudentId())) {
                cardList.set(i, cardInfo);
                return;
            }
        }
        cardList.add(cardInfo);
    }

    public List<CardInfo> getAllCards() {
        return new ArrayList<>(cardList);
    }

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

    // --- QUẢN LÝ SÁCH (MOCK DATA) ---
    private static final String[][] AVAILABLE_BOOKS = {
            {"NV001", "Nhà Giả Kim"}, {"DB002", "Đắc Nhân Tâm"},
            {"TH003", "Trên Đường Băng"}, {"CX004", "Cà Phê Cùng Tony"},
            {"HP005", "Harry Potter"}, {"LT006", "Lược Sử Thời Gian"},
            {"TN007", "Tuổi Trẻ Đáng Giá"}, {"NL008", "Nhà Lãnh Đạo"}
    };

    private java.util.Map<String, java.util.List<models.BorrowedBook>> studentBooks = new java.util.HashMap<>();

    public java.util.List<models.BorrowedBook> getBorrowedBooks(String studentCode) {
        return studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
    }

    public void addBorrowedBook(String studentCode, models.BorrowedBook book) {
        java.util.List<models.BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        books.add(book);
        studentBooks.put(studentCode, books);
        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) card.setBorrowedBooks(books.size());
    }

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
            if (card != null) card.setBorrowedBooks(books.size());
        }
    }

    public String borrowBook(String studentCode, String bookId) {
        String bookName = null;
        for (String[] book : AVAILABLE_BOOKS) {
            if (book[0].equalsIgnoreCase(bookId)) {
                bookName = book[1];
                break;
            }
        }
        if (bookName == null) return "Không tìm thấy sách với mã: " + bookId;

        java.util.List<models.BorrowedBook> books = studentBooks.getOrDefault(studentCode, new java.util.ArrayList<>());
        for (models.BorrowedBook b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) return "Bạn đã mượn sách này rồi!";
        }
        if (books.size() >= 5) return "Bạn đã mượn tối đa 5 cuốn sách!";

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate dueDate = today.plusDays(14);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        models.BorrowedBook newBook = new models.BorrowedBook(
                bookId.toUpperCase(), bookName, today.format(fmt), dueDate.format(fmt), "Đang mượn", 0
        );
        books.add(newBook);
        studentBooks.put(studentCode, books);

        CardInfo card = getCardByStudentCode(studentCode);
        if (card != null) card.setBorrowedBooks(books.size());
        return null;
    }

    public String returnBook(String studentCode, String bookId) {
        java.util.List<models.BorrowedBook> books = studentBooks.get(studentCode);
        if (books == null || books.isEmpty()) return "Bạn không có sách nào để trả!";

        models.BorrowedBook toRemove = null;
        int fine = 0;
        for (models.BorrowedBook b : books) {
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
    private java.util.Map<String, java.util.List<models.Transaction>> studentTransactions = new java.util.HashMap<>();

    public boolean deposit(String studentCode, long amount) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null) return false;
        card.setBalance(card.getBalance() + amount);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.util.List<models.Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new models.Transaction(today.format(fmt), "Nạp tiền", amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        return true;
    }

    public boolean payFine(String studentCode, long amount) {
        CardInfo card = getCardByStudentCode(studentCode);
        if (card == null || card.getBalance() < amount) return false;
        card.setBalance(card.getBalance() - amount);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.util.List<models.Transaction> transactions = studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
        transactions.add(0, new models.Transaction(today.format(fmt), "Thanh toán phạt", -amount, "Thành công"));
        studentTransactions.put(studentCode, transactions);
        return true;
    }

    public java.util.List<models.Transaction> getTransactions(String studentCode) {
        return studentTransactions.getOrDefault(studentCode, new java.util.ArrayList<>());
    }

    public long getBalance(String studentCode) {
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getBalance() : 0;
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

    public boolean setCardInfo(CardInfo cardInfo) throws Exception {
        return true;
    }

    public CardInfo getCardInfo() throws Exception {
        return new CardInfo();
    }
}