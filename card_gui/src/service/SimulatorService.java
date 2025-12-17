package service;

import com.licel.jcardsim.base.Simulator;
import applet.AppletConstants;
import models.CardInfo;
import javacard.framework.AID;
import utils.RSAUtility;
import utils.AESUtility;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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

    /**
     * Verify student PIN - AN TOÀN: Verify trên thẻ (applet)
     * ⚠️ KHÔNG verify bằng cách so sánh plain text PIN trong memory
     * ✅ ĐÚNG: Gọi thẻ để verify PIN
     * 
     * @param studentCode Student code
     * @param pin PIN to verify
     * @return true if PIN is correct
     */
    public boolean verifyStudentPin(String studentCode, String pin) {
        // ⚠️ SECURITY: Verify PIN trên thẻ, không phải trong memory
        if (!isConnected) {
            // Fallback: Nếu chưa kết nối thẻ, không thể verify
            return false;
        }
        
        try {
            // ✅ ĐÚNG: Verify PIN trên thẻ (applet)
            boolean verified = verifyPin(pin.toCharArray());
            
            if (verified) {
                // Update card status in memory
                CardInfo card = getCardByStudentCode(studentCode);
                if (card != null) {
                    card.setPinRetryCount(3);
                }
            } else {
                // Update retry count from card
                CardInfo card = getCardByStudentCode(studentCode);
                if (card != null) {
                    int remaining = getPinTriesRemaining();
                    card.setPinRetryCount(Math.max(0, remaining));
                    
                    if (remaining <= 0) {
                        card.setStatus("Khóa");
                    }
                }
            }
            
            return verified;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getStudentPinTries(String studentCode) {
        CardInfo card = getCardByStudentCode(studentCode);
        return card != null ? card.getPinRetryCount() : 0;
    }

    public boolean changeStudentPin(String studentCode, String oldPin, String newPin) {
        // ⚠️ SECURITY: PIN được lưu trên thẻ (applet), không phải trong memory
        // Cần đổi PIN trên thẻ trước, sau đó mới update memory
        
        if (!isConnected) {
            // Fallback: Nếu chưa kết nối thẻ, chỉ update memory (demo mode)
            CardInfo card = getCardByStudentCode(studentCode);
            if (card != null && card.getPin().equals(oldPin)) {
                card.setPin(newPin);
                return true;
            }
            return false;
        }
        
        try {
            // Step 1: Verify PIN cũ trên thẻ
            boolean verified = verifyPin(oldPin.toCharArray());
            if (!verified) {
                return false; // PIN cũ không đúng
            }
            
            // Step 2: Đổi PIN trên thẻ (applet)
            boolean success = changePin(oldPin.toCharArray(), newPin.toCharArray());
            if (!success) {
                return false;
            }
            
            // Step 3: Update memory and save to cardList
            CardInfo card = getCardByStudentCode(studentCode);
            if (card != null) {
                card.setPin(newPin);
                // Ensure the updated card is saved in cardList
                addCardToList(card);
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error changing student PIN: " + e.getMessage());
            return false;
        }
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
    
    /**
     * Disconnect from card and reset all state
     * Should be called when logging out
     */
    public void disconnect() {
        // Reset connection state
        isConnected = false;
        isPinVerified = false;
        pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;
        currentStudentCode = "";
        currentRole = "Sinh viên";
        
        // Clear simulator reference (but keep cardList for display)
        // Note: We don't clear cardList as it's used for display purposes
        // If you want to clear it, uncomment the line below:
        // cardList.clear();
        
        simulator = null;
    }
    
    /**
     * Reset all authentication state (but keep connection)
     * Use this when you want to keep the card connected but reset auth state
     */
    public void resetAuthState() {
        isPinVerified = false;
        pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;
        currentStudentCode = "";
        currentRole = "Sinh viên";
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
     * Set card info with AES encryption
     * Card ID is stored as plaintext (used for key derivation)
     * Only Name and Expiry Date are encrypted
     */
    public boolean setCardInfo(CardInfo cardInfo) throws Exception {
        if (!isConnected || !isPinVerified) {
            throw new Exception("Chưa kết nối hoặc chưa xác thực PIN");
        }
        
        try {
            // Card ID is stored as plaintext (needed for key derivation)
            String cardId = cardInfo.getStudentId();
            
            // Derive AES key from master key and card ID
            String masterKey = AESUtility.getMasterKey();
            javax.crypto.SecretKey aesKey = AESUtility.deriveKey(masterKey, cardId);
            
            // Encrypt Name and Expiry Date (Card ID is plaintext)
            byte[] encryptedName = AESUtility.encrypt(
                cardInfo.getHolderName().getBytes(StandardCharsets.UTF_8), aesKey);
            
            // Format expiry date (DDMMYYYY) - use current date + 5 years
            String expiryDate = java.time.LocalDate.now()
                .plusYears(5)
                .format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
            byte[] encryptedExpiry = AESUtility.encrypt(
                expiryDate.getBytes(StandardCharsets.UTF_8), aesKey);
            
            // Prepare Card ID as plaintext (pad/truncate to 10 bytes)
            byte[] cardIdBytes = cardId.getBytes(StandardCharsets.UTF_8);
            byte[] cardIdData = new byte[AppletConstants.CARD_ID_LENGTH];
            System.arraycopy(cardIdBytes, 0, cardIdData, 0, 
                Math.min(cardIdData.length, cardIdBytes.length));
            
            // Truncate encrypted Name to fit applet constraints (max 50 bytes)
            // Note: Encrypted data format is [IV (16 bytes)] + [Encrypted data]
            // If truncated, IV will be lost and decryption will fail
            // This is a limitation - we can only store partial encrypted data
            byte[] nameData = new byte[Math.min(encryptedName.length, AppletConstants.NAME_MAX_LENGTH)];
            System.arraycopy(encryptedName, 0, nameData, 0, nameData.length);
            
            // Truncate encrypted Expiry to fit applet constraints (8 bytes)
            // Note: This will lose IV, making decryption impossible
            // For expiry date, we might need to store it plaintext or use a different approach
            byte[] expiryData = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
            System.arraycopy(encryptedExpiry, 0, expiryData, 0, 
                Math.min(expiryData.length, encryptedExpiry.length));
            
            // Build command
            // Format: [ENCRYPTED_FLAG (1)] [CARD_ID_PLAINTEXT (10)] [NAME_LEN] [NAME_ENCRYPTED] [EXPIRY_ENCRYPTED]
            int totalLength = 1 + cardIdData.length + 1 + nameData.length + expiryData.length;
            byte[] cmd = new byte[5 + totalLength];
            cmd[0] = (byte)0x00;
            cmd[1] = AppletConstants.INS_SET_CARD_INFO;
            cmd[2] = (byte)0x00;
            cmd[3] = (byte)0x00;
            cmd[4] = (byte)totalLength;
            
            int offset = 5;
            // Encrypted flag (1 = Name and Expiry are encrypted, Card ID is plaintext)
            cmd[offset++] = (byte)0x01;
            // Card ID (PLAINTEXT)
            System.arraycopy(cardIdData, 0, cmd, offset, cardIdData.length);
            offset += cardIdData.length;
            // Name length
            cmd[offset++] = (byte)nameData.length;
            // Name (ENCRYPTED, may be truncated)
            System.arraycopy(nameData, 0, cmd, offset, nameData.length);
            offset += nameData.length;
            // Expiry date (ENCRYPTED, may be truncated)
            System.arraycopy(expiryData, 0, cmd, offset, expiryData.length);
            
            byte[] resp = sendCommand(cmd);
            return getSW(resp) == 0x9000;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Lỗi khi mã hóa và lưu thông tin thẻ: " + e.getMessage());
        }
    }

    /**
     * Get card info with AES decryption
     * Decrypts card data after receiving from card
     */
    public CardInfo getCardInfo() throws Exception {
        if (!isConnected) {
            throw new Exception("Chưa kết nối với thẻ");
        }
        
        try {
            // Send GET_CARD_INFO command
            byte[] cmd = new byte[5];
            cmd[0] = (byte)0x00;
            cmd[1] = AppletConstants.INS_GET_CARD_INFO;
            cmd[2] = (byte)0x00;
            cmd[3] = (byte)0x00;
            cmd[4] = (byte)0x00;
            
            byte[] resp = sendCommand(cmd);
            if (getSW(resp) != 0x9000) {
                throw new Exception("Lỗi khi đọc thông tin thẻ: " + String.format("%04X", getSW(resp)));
            }
            
            // Response format: [ENCRYPTED_FLAG (1)] [CARD_ID] [NAME_LEN] [NAME] [EXPIRY] [NUM_BOOKS]
            // If ENCRYPTED_FLAG = 1: Card ID is plaintext, Name and Expiry are encrypted
            byte[] data = new byte[resp.length - 2];
            System.arraycopy(resp, 0, data, 0, data.length);
            
            int offset = 0;
            boolean encrypted = (data[offset++] == (byte)0x01);
            
            // Read Card ID (PLAINTEXT if encrypted flag is set, otherwise may be plaintext)
            byte[] cardIdBytes = new byte[AppletConstants.CARD_ID_LENGTH];
            System.arraycopy(data, offset, cardIdBytes, 0, cardIdBytes.length);
            offset += cardIdBytes.length;
            String cardId = new String(cardIdBytes, StandardCharsets.UTF_8).trim();
            
            // Read Name
            byte nameLen = data[offset++];
            byte[] nameData = new byte[nameLen];
            System.arraycopy(data, offset, nameData, 0, nameLen);
            offset += nameLen;
            
            // Read Expiry
            byte[] expiryData = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
            System.arraycopy(data, offset, expiryData, 0, expiryData.length);
            offset += expiryData.length;
            
            // Read num books
            byte numBooks = data[offset];
            
            CardInfo cardInfo = new CardInfo();
            cardInfo.setBorrowedBooks(numBooks & 0xFF);
            cardInfo.setStudentId(cardId); // Card ID is always plaintext
            
            if (encrypted) {
                // Name and Expiry are encrypted, Card ID is plaintext
                // Use Card ID to derive key
                String masterKey = AESUtility.getMasterKey();
                javax.crypto.SecretKey aesKey = AESUtility.deriveKey(masterKey, cardId);
                
                // Decrypt Name
                try {
                    // Note: Encrypted data may be truncated (missing IV or partial data)
                    // If data is too short (< 16 bytes), it's likely truncated and can't be decrypted
                    if (nameData.length >= 16) {
                        byte[] decryptedName = AESUtility.decrypt(nameData, aesKey);
                        String nameStr = new String(decryptedName, StandardCharsets.UTF_8).trim();
                        cardInfo.setHolderName(nameStr);
                    } else {
                        // Data too short, likely truncated - treat as plaintext or use fallback
                        System.err.println("Warning: Encrypted name data too short, may be truncated");
                        String nameStr = new String(nameData, StandardCharsets.UTF_8).trim();
                        cardInfo.setHolderName(nameStr);
                    }
                } catch (Exception e) {
                    // Decryption failed - data may be truncated or corrupted
                    // Fallback to plaintext (may be garbage if actually encrypted)
                    System.err.println("Warning: Failed to decrypt name, using as plaintext: " + e.getMessage());
                    String nameStr = new String(nameData, StandardCharsets.UTF_8).trim();
                    cardInfo.setHolderName(nameStr);
                }
                
                // Expiry date decryption (not used in CardInfo model currently)
                // Note: Expiry is only 8 bytes, encrypted data needs 16+ bytes (IV + encrypted)
                // So expiry cannot be properly encrypted with current constraints
                // For now, we'll skip expiry decryption
            } else {
                // All data is plaintext
                String nameStr = new String(nameData, StandardCharsets.UTF_8).trim();
                cardInfo.setHolderName(nameStr);
            }
            
            return cardInfo;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Lỗi khi đọc và giải mã thông tin thẻ: " + e.getMessage());
        }
    }

    // ========== RSA Authentication Methods ==========
    
    /**
     * Generate RSA keypair on card
     * @return Public key (modulus + exponent) or null if error
     */
    public byte[] generateRSAKeyPair() throws Exception {
        if (!isConnected) {
            throw new Exception("Chưa kết nối với thẻ");
        }
        
        byte[] cmd = new byte[5];
        cmd[0] = (byte)0x00;  // CLA
        cmd[1] = AppletConstants.INS_RSA_GENERATE_KEYPAIR;
        cmd[2] = (byte)0x00;
        cmd[3] = (byte)0x00;
        cmd[4] = (byte)0x00;
        
        byte[] resp = sendCommand(cmd);
        if (getSW(resp) != 0x9000) {
            throw new Exception("Lỗi khi tạo khóa RSA: " + String.format("%04X", getSW(resp)));
        }
        
        // Response: [MODULUS (128 bytes)] [EXPONENT (3 bytes)]
        byte[] publicKeyData = new byte[resp.length - 2];
        System.arraycopy(resp, 0, publicKeyData, 0, publicKeyData.length);
        return publicKeyData;
    }
    
    /**
     * Get RSA public key from card
     * @return Public key (modulus + exponent) or null if not generated
     */
    public byte[] getRSAPublicKey() throws Exception {
        if (!isConnected) {
            throw new Exception("Chưa kết nối với thẻ");
        }
        
        byte[] cmd = new byte[5];
        cmd[0] = (byte)0x00;
        cmd[1] = AppletConstants.INS_RSA_GET_PUBLIC_KEY;
        cmd[2] = (byte)0x00;
        cmd[3] = (byte)0x00;
        cmd[4] = (byte)0x00;
        
        byte[] resp = sendCommand(cmd);
        if (getSW(resp) != 0x9000) {
            throw new Exception("Thẻ chưa có khóa RSA: " + String.format("%04X", getSW(resp)));
        }
        
        byte[] publicKeyData = new byte[resp.length - 2];
        System.arraycopy(resp, 0, publicKeyData, 0, publicKeyData.length);
        return publicKeyData;
    }
    
    /**
     * Sign challenge with RSA private key on card
     * @param challenge Challenge bytes (16 bytes)
     * @return Signature (128 bytes) or null if error
     */
    public byte[] signRSAChallenge(byte[] challenge) throws Exception {
        if (!isConnected) {
            throw new Exception("Chưa kết nối với thẻ");
        }
        
        if (challenge.length != AppletConstants.RSA_CHALLENGE_SIZE) {
            throw new Exception("Challenge phải có độ dài 16 bytes");
        }
        
        // Verify keypair exists before signing
        try {
            byte[] testKey = getRSAPublicKey();
            if (testKey == null || testKey.length == 0) {
                throw new Exception("RSA keypair chưa được tạo trên thẻ");
            }
            System.out.println("[DEBUG] RSA keypair exists on card, public key length: " + testKey.length);
        } catch (Exception e) {
            throw new Exception("RSA keypair không tồn tại hoặc chưa sẵn sàng: " + e.getMessage());
        }
        
        // Verify challenge length
        if (challenge.length != AppletConstants.RSA_CHALLENGE_SIZE) {
            throw new Exception("Challenge length mismatch: expected " + AppletConstants.RSA_CHALLENGE_SIZE + 
                               ", got " + challenge.length);
        }
        
        // Build APDU command: [CLA] [INS] [P1] [P2] [Lc] [Data...]
        byte[] cmd = new byte[5 + challenge.length];
        cmd[0] = (byte)0x00;  // CLA
        cmd[1] = AppletConstants.INS_RSA_SIGN_CHALLENGE;  // INS
        cmd[2] = (byte)0x00;  // P1
        cmd[3] = (byte)0x00;  // P2
        cmd[4] = (byte)challenge.length;  // Lc (length of data)
        System.arraycopy(challenge, 0, cmd, 5, challenge.length);
        
        System.out.println("[DEBUG] Sending RSA sign challenge command:");
        System.out.println("  CLA: 0x" + String.format("%02X", cmd[0]));
        System.out.println("  INS: 0x" + String.format("%02X", cmd[1]));
        System.out.println("  P1: 0x" + String.format("%02X", cmd[2]));
        System.out.println("  P2: 0x" + String.format("%02X", cmd[3]));
        System.out.println("  Lc: " + (cmd[4] & 0xFF) + " (expected: " + AppletConstants.RSA_CHALLENGE_SIZE + ")");
        System.out.println("  Challenge length: " + challenge.length);
        
        byte[] resp = sendCommand(cmd);
        int sw = getSW(resp);
        if (sw != 0x9000) {
            // Detailed error codes for debugging - MAPPED TO SPECIFIC STEPS
            String errorMsg = "Lỗi khi ký challenge: " + String.format("%04X", sw);
            String step = "Unknown";
            
            switch (sw) {
                case 0x6A00:
                    step = "Step 0: Keypair not generated";
                    errorMsg += " (Keypair chưa được tạo)";
                    break;
                case 0x6700:
                    step = "Step 2: Challenge length validation";
                    errorMsg += " (Challenge length không đúng - expected 16, got different)";
                    break;
                case 0x6A09:
                    step = "Step 3: Receive data failed";
                    errorMsg += " (Không thể nhận dữ liệu từ APDU)";
                    break;
                case 0x6A0C:
                    step = "Step 4: Algorithm not supported";
                    errorMsg += " (Signature.ALG_RSA_SHA_PKCS1 không được hỗ trợ)";
                    break;
                case 0x6A0D:
                    step = "Step 4: Signature.getInstance failed";
                    errorMsg += " (Không thể tạo Signature instance)";
                    break;
                case 0x6A0E:
                    step = "Step 5: Key not initialized";
                    errorMsg += " (Private key chưa được khởi tạo)";
                    break;
                case 0x6A0F:
                    step = "Step 5: Illegal value in init";
                    errorMsg += " (Giá trị không hợp lệ khi init Signature)";
                    break;
                case 0x6A10:
                    step = "Step 5: Signature.init failed";
                    errorMsg += " (Không thể khởi tạo Signature với private key)";
                    break;
                case 0x6A11:
                    step = "Step 6: Illegal value in sign";
                    errorMsg += " (Giá trị không hợp lệ khi sign - có thể buffer/index issue)";
                    break;
                case 0x6A12:
                    step = "Step 6: Key uninitialized in sign";
                    errorMsg += " (Key chưa được init khi sign)";
                    break;
                case 0x6A13:
                    step = "Step 6: Sign failed (CryptoException)";
                    errorMsg += " (RSA sign failed với CryptoException)";
                    break;
                case 0x6A14:
                    step = "Step 6: Sign failed (Exception)";
                    errorMsg += " (RSA sign failed với Exception - có thể là ArrayIndexOutOfBoundsException)";
                    break;
                case 0x6A15:
                    step = "Step 6: Sign failed (Error/Throwable)";
                    errorMsg += " (RSA sign failed với Error/Throwable)";
                    break;
                case 0x6A16:
                    step = "Step 6: Unexpected signature length";
                    errorMsg += " (Signature length không đúng - expected 128 bytes)";
                    break;
                case 0x6A17:
                    step = "Step 6: NullPointerException";
                    errorMsg += " (NullPointerException trong sign - có thể buffer/key null)";
                    break;
                case 0x6A18:
                    step = "Step 7: Send response failed";
                    errorMsg += " (Không thể gửi signature)";
                    break;
                case 0x6A19:
                    step = "Step 0-1: Buffer is null";
                    errorMsg += " (APDU buffer is null)";
                    break;
                case 0x6A1C:
                    step = "Unexpected exception in signChallenge";
                    errorMsg += " (Exception xảy ra ngoài các try-catch blocks - có thể từ getBuffer(), getIncomingLength(), etc.)";
                    break;
                case 0x6A20:
                    step = "Step 0: Exception in keypair check";
                    errorMsg += " (Exception khi kiểm tra keypair hoặc privateKey)";
                    break;
                case 0x6A21:
                    step = "Step 1: Exception in getBuffer()";
                    errorMsg += " (Exception khi gọi apdu.getBuffer())";
                    break;
                case 0x6A22:
                    step = "Step 1-2: Exception in getIncomingLength() or validation";
                    errorMsg += " (Exception khi gọi apdu.getIncomingLength() hoặc validate length)";
                    break;
                case 0x6A23:
                    step = "Step 3: Exception in setIncomingAndReceive()";
                    errorMsg += " (Exception khi gọi apdu.setIncomingAndReceive())";
                    break;
                case 0x6A24:
                    step = "Step 4: Exception in MessageDigest.getInstance()";
                    errorMsg += " (Exception khi tạo MessageDigest instance)";
                    break;
                case 0x6A25:
                    step = "Step 4: Exception in doFinal()";
                    errorMsg += " (Exception khi hash challenge với SHA-1)";
                    break;
                case 0x6A26:
                    step = "Step 6: Exception in Cipher.getInstance()";
                    errorMsg += " (Exception khi tạo Cipher instance)";
                    break;
                case 0x6A27:
                    step = "Step 6: Exception in cipher.init()";
                    errorMsg += " (Exception khi khởi tạo Cipher với private key)";
                    break;
                case 0x6A28:
                    step = "Step 7: Exception in setOutgoingAndSend()";
                    errorMsg += " (Exception khi gửi signature)";
                    break;
                case 0x6A29:
                    step = "Step 6: Illegal value in PKCS1 doFinal";
                    errorMsg += " (Giá trị không hợp lệ khi sign với PKCS1 - có thể DigestInfo+Hash format sai)";
                    break;
                case 0x6A2A:
                    step = "Step 6: Illegal value in NOPAD doFinal";
                    errorMsg += " (Giá trị không hợp lệ khi sign với NOPAD - có thể padded data >= modulus)";
                    break;
                case 0x6A1A:
                    step = "Router: Exception in LibraryCardApplet";
                    errorMsg += " (Exception xảy ra trong router khi gọi signChallenge)";
                    break;
                case 0x6A1B:
                    step = "Router: RSA manager not initialized";
                    errorMsg += " (rsaAuthManager is null - applet initialization problem)";
                    break;
                case 0x0001:
                    step = "UNKNOWN - Exception outside all try-catch";
                    errorMsg += " (SW_UNKNOWN - Exception xảy ra ngoài tất cả try-catch blocks, có thể là JCardSim issue)";
                    break;
                default:
                    step = "Unknown step";
                    errorMsg += " (Unknown error code: 0x" + String.format("%04X", sw) + ")";
            }
            
            System.out.println("[DEBUG] RSA Sign Error Details:");
            System.out.println("  Status Word: 0x" + String.format("%04X", sw));
            System.out.println("  Failed at: " + step);
            System.out.println("  Description: " + errorMsg);
            throw new Exception(errorMsg);
        }
        
        byte[] signature = new byte[resp.length - 2];
        System.arraycopy(resp, 0, signature, 0, signature.length);
        return signature;
    }
    
    /**
     * Authenticate card using RSA challenge-response
     * @param publicKey Public key from server (PEM format)
     * @return true if card is authenticated
     */
    public boolean authenticateCardWithRSA(String publicKeyPEM) {
        try {
            // Verify keypair exists and is ready before attempting authentication
            if (!isConnected) {
                return false;
            }
            
            // First, verify keypair exists on card
            try {
                byte[] testKey = getRSAPublicKey();
                if (testKey == null || testKey.length == 0) {
                    System.out.println("RSA keypair not found on card for authentication");
                    return false;
                }
            } catch (Exception keyCheckEx) {
                System.out.println("RSA keypair check failed: " + keyCheckEx.getMessage());
                return false;
            }
            
            // Get public key from card
            byte[] cardPublicKeyData = getRSAPublicKey();
            
            // Extract modulus and exponent
            byte[] modulus = new byte[AppletConstants.RSA_MODULUS_SIZE];
            byte[] exponent = new byte[3];
            System.arraycopy(cardPublicKeyData, 0, modulus, 0, modulus.length);
            System.arraycopy(cardPublicKeyData, modulus.length, exponent, 0, exponent.length);
            
            // Convert to Java PublicKey
            PublicKey cardPublicKey = RSAUtility.convertToPublicKey(modulus, exponent);
            
            // Generate challenge
            byte[] challenge = RSAUtility.generateChallenge();
            
            // Sign challenge on card - catch 6700 error specifically
            byte[] signature;
            try {
                signature = signRSAChallenge(challenge);
            } catch (Exception signEx) {
                // 6700 error means keypair may not be ready
                String errorMsg = signEx.getMessage();
                if (errorMsg != null && errorMsg.contains("6700")) {
                    System.out.println("RSA keypair not ready for signing (6700) - skipping authentication");
                    return false;
                }
                // Re-throw other errors
                throw signEx;
            }
            
            // Verify signature
            return RSAUtility.verifySignature(cardPublicKey, challenge, signature);
        } catch (Exception e) {
            // Don't print stack trace for expected errors (e.g., 6700)
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("6700")) {
                System.out.println("RSA authentication failed: Keypair not ready (6700)");
            } else {
                System.out.println("RSA authentication error: " + errorMsg);
            }
            return false;
        }
    }
    
    // ========== AES Encryption Methods ==========
    
    /**
     * Set AES key on card (requires PIN verification)
     * @param aesKey AES key (16 bytes)
     * @return true if successful
     */
    public boolean setAESKey(byte[] aesKey) throws Exception {
        if (!isConnected) {
            throw new Exception("Chưa kết nối với thẻ");
        }
        
        if (aesKey.length != AppletConstants.AES_KEY_SIZE) {
            throw new Exception("AES key phải có độ dài 16 bytes");
        }
        
        if (!isPinVerified) {
            throw new Exception("Cần xác thực PIN trước");
        }
        
        byte[] cmd = new byte[5 + aesKey.length];
        cmd[0] = (byte)0x00;
        cmd[1] = AppletConstants.INS_AES_SET_KEY;
        cmd[2] = (byte)0x00;
        cmd[3] = (byte)0x00;
        cmd[4] = (byte)aesKey.length;
        System.arraycopy(aesKey, 0, cmd, 5, aesKey.length);
        
        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
    }
}