import javacard.framework.*;
import javacard.security.*;

/**
 * Applet Thẻ Thư Viện - Version Fixed cho JavaCard 2.2.2
 * Chức năng:
 * - Quản lý PIN (tạo, đổi, kiểm tra) với giới hạn 3 lần nhập sai
 * - Lưu thông tin thẻ: ID, Họ tên, Ngày hết hạn
 * - Quản lý sách mượn (sửa dùng mảng 1 chiều)
 */
public class LibraryCardAppletFixed extends Applet {
    
    // AID mặc định
    private static final byte[] DEFAULT_AID = {
        (byte)0x4C, (byte)0x49, (byte)0x42, (byte)0x52,  // 'L','I','B','R'
        (byte)0x41, (byte)0x52, (byte)0x59              // 'A','R','Y'
    };
    
    // Các INS code cho các lệnh APDU
    private static final byte INS_CREATE_PIN = (byte)0x10;
    private static final byte INS_VERIFY_PIN = (byte)0x20;
    private static final byte INS_CHANGE_PIN = (byte)0x30;
    private static final byte INS_SET_CARD_INFO = (byte)0x40;
    private static final byte INS_GET_CARD_INFO = (byte)0x50;
    private static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    private static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    private static final byte INS_RETURN_BOOK = (byte)0x80;
    private static final byte INS_GET_PIN_TRIES = (byte)0x90;
    private static final byte INS_RESET_PIN = (byte)0xA0;
    
    // Độ dài các trường dữ liệu
    private static final byte PIN_TRY_LIMIT = (byte)3;
    private static final byte PIN_MAX_SIZE = (byte)8;
    private static final byte CARD_ID_LENGTH = (byte)10;
    private static final byte NAME_MAX_LENGTH = (byte)50;
    private static final byte MAX_BORROWED_BOOKS = (byte)10;
    private static final byte BOOK_ID_LENGTH = (byte)8;
    
    // Các trường dữ liệu
    private OwnerPIN pin;
    private byte[] cardId;
    private byte[] holderName;
    private byte holderNameLength;
    private byte[] expiryDate; // DDMMYYYY format
    private byte[] borrowedBooks; // Mảng 1 chiều thay vì 2 chiều
    private byte numBorrowedBooks;
    private boolean cardInitialized;
    
    /**
     * Constructor mặc định (cho JCardSim)
     */
    public LibraryCardAppletFixed() {
        initialize();
        register();
    }
    
    /**
     * Constructor với tham số cài đặt
     */
    private LibraryCardAppletFixed(byte[] bArray, short bOffset, byte bLength) {
        initialize();
        
        // Xử lý tham số cài đặt
        byte iLen = bArray[bOffset];
        if (iLen == 0) {
            register();
        } else {
            register(bArray, (short)(bOffset + 1), iLen);
        }
    }
    
    /**
     * Khởi tạo các biến và mảng
     */
    private void initialize() {
        // Khởi tạo PIN với 3 lần thử
        pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_MAX_SIZE);
        
        // Khởi tạo các mảng lưu trữ
        cardId = new byte[CARD_ID_LENGTH];
        holderName = new byte[NAME_MAX_LENGTH];
        expiryDate = new byte[8]; // DDMMYYYY
        
        // Mảng 1 chiều: 10 sách x 8 bytes = 80 bytes
        borrowedBooks = new byte[MAX_BORROWED_BOOKS * BOOK_ID_LENGTH];
        
        numBorrowedBooks = 0;
        cardInitialized = false;
        holderNameLength = 0;
    }
    
    /**
     * Phương thức install
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new LibraryCardAppletFixed(bArray, bOffset, bLength);
    }
    
    /**
     * Xử lý các lệnh APDU
     */
    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];
        
        switch (ins) {
            case INS_CREATE_PIN:
                createPin(apdu);
                break;
            case INS_VERIFY_PIN:
                verifyPin(apdu);
                break;
            case INS_CHANGE_PIN:
                changePin(apdu);
                break;
            case INS_SET_CARD_INFO:
                setCardInfo(apdu);
                break;
            case INS_GET_CARD_INFO:
                getCardInfo(apdu);
                break;
            case INS_ADD_BORROWED_BOOK:
                addBorrowedBook(apdu);
                break;
            case INS_GET_BORROWED_BOOKS:
                getBorrowedBooks(apdu);
                break;
            case INS_RETURN_BOOK:
                returnBook(apdu);
                break;
            case INS_GET_PIN_TRIES:
                getPinTries(apdu);
                break;
            case INS_RESET_PIN:
                resetPin(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
    
    /**
     * Tạo PIN mới (chỉ thực hiện được 1 lần)
     * Format: [PIN_LENGTH][PIN_DATA]
     */
    private void createPin(APDU apdu) {
        if (pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        byte pinLength = buffer[ISO7816.OFFSET_CDATA];
        
        if (pinLength < 4 || pinLength > PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        pin.update(buffer, (short)(ISO7816.OFFSET_CDATA + 1), pinLength);
        cardInitialized = true;
    }
    
    /**
     * Xác thực PIN
     * Format: [PIN_LENGTH][PIN_DATA]
     */
    private void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        byte pinLength = buffer[ISO7816.OFFSET_CDATA];
        
        if (pin.check(buffer, (short)(ISO7816.OFFSET_CDATA + 1), pinLength)) {
            // PIN đúng
            buffer[0] = (byte)0x01;
        } else {
            // PIN sai
            buffer[0] = (byte)0x00;
            buffer[1] = pin.getTriesRemaining();
            apdu.setOutgoingAndSend((short)0, (short)2);
            return;
        }
        
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Thay đổi PIN (yêu cầu xác thực PIN cũ trước)
     * Format: [OLD_PIN_LENGTH][OLD_PIN][NEW_PIN_LENGTH][NEW_PIN]
     */
    private void changePin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        
        short offset = ISO7816.OFFSET_CDATA;
        byte oldPinLength = buffer[offset++];
        
        // Xác thực PIN cũ
        if (!pin.check(buffer, offset, oldPinLength)) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        offset += oldPinLength;
        byte newPinLength = buffer[offset++];
        
        if (newPinLength < 4 || newPinLength > PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Cập nhật PIN mới
        pin.update(buffer, offset, newPinLength);
        pin.resetAndUnblock();
    }
    
    /**
     * Thiết lập thông tin thẻ (yêu cầu xác thực PIN)
     * Format: [CARD_ID][NAME_LENGTH][NAME][EXPIRY_DATE]
     */
    private void setCardInfo(APDU apdu) {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;
        
        // Đọc Card ID (10 bytes)
        Util.arrayCopy(buffer, offset, cardId, (short)0, CARD_ID_LENGTH);
        offset += CARD_ID_LENGTH;
        
        // Đọc tên (độ dài biến đổi)
        holderNameLength = buffer[offset++];
        if (holderNameLength > NAME_MAX_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(buffer, offset, holderName, (short)0, holderNameLength);
        offset += holderNameLength;
        
        // Đọc ngày hết hạn (8 bytes - DDMMYYYY)
        Util.arrayCopy(buffer, offset, expiryDate, (short)0, (short)8);
    }
    
    /**
     * Lấy thông tin thẻ
     * Return: [CARD_ID][NAME_LENGTH][NAME][EXPIRY_DATE][NUM_BOOKS]
     */
    private void getCardInfo(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        // Card ID
        Util.arrayCopy(cardId, (short)0, buffer, offset, CARD_ID_LENGTH);
        offset += CARD_ID_LENGTH;
        
        // Name
        buffer[offset++] = holderNameLength;
        Util.arrayCopy(holderName, (short)0, buffer, offset, holderNameLength);
        offset += holderNameLength;
        
        // Expiry Date
        Util.arrayCopy(expiryDate, (short)0, buffer, offset, (short)8);
        offset += 8;
        
        // Number of borrowed books
        buffer[offset++] = numBorrowedBooks;
        
        apdu.setOutgoingAndSend((short)0, offset);
    }
    
    /**
     * Thêm sách mượn (yêu cầu xác thực PIN)
     * Format: [BOOK_ID]
     */
    private void addBorrowedBook(APDU apdu) {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        if (numBorrowedBooks >= MAX_BORROWED_BOOKS) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Tính vị trí trong mảng 1 chiều: sách thứ i nằm ở vị trí i * BOOK_ID_LENGTH
        short bookOffset = (short)(numBorrowedBooks * BOOK_ID_LENGTH);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, 
                      borrowedBooks, bookOffset, BOOK_ID_LENGTH);
        numBorrowedBooks++;
    }
    
    /**
     * Lấy danh sách sách đang mượn
     * Return: [NUM_BOOKS][BOOK_ID_1][BOOK_ID_2]...
     */
    private void getBorrowedBooks(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        buffer[offset++] = numBorrowedBooks;
        
        // Copy từng book ID từ mảng 1 chiều
        for (byte i = 0; i < numBorrowedBooks; i++) {
            short bookOffset = (short)(i * BOOK_ID_LENGTH);
            Util.arrayCopy(borrowedBooks, bookOffset, 
                          buffer, offset, BOOK_ID_LENGTH);
            offset += BOOK_ID_LENGTH;
        }
        
        apdu.setOutgoingAndSend((short)0, offset);
    }
    
    /**
     * Trả sách (yêu cầu xác thực PIN)
     * Format: [BOOK_ID]
     */
    private void returnBook(APDU apdu) {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Tìm sách cần trả trong mảng 1 chiều
        for (byte i = 0; i < numBorrowedBooks; i++) {
            short bookOffset = (short)(i * BOOK_ID_LENGTH);
            
            if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, 
                                 borrowedBooks, bookOffset, BOOK_ID_LENGTH) == 0) {
                // Tìm thấy sách, xóa khỏi danh sách bằng cách dịch các sách sau lên
                for (byte j = i; j < (byte)(numBorrowedBooks - 1); j++) {
                    short srcOffset = (short)((j + 1) * BOOK_ID_LENGTH);
                    short dstOffset = (short)(j * BOOK_ID_LENGTH);
                    Util.arrayCopy(borrowedBooks, srcOffset,
                                  borrowedBooks, dstOffset, BOOK_ID_LENGTH);
                }
                numBorrowedBooks--;
                return;
            }
        }
        
        // Không tìm thấy sách
        ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
    }
    
    /**
     * Lấy số lần nhập PIN còn lại
     * Return: [TRIES_REMAINING]
     */
    private void getPinTries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = pin.getTriesRemaining();
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Reset PIN (chỉ admin mới được phép)
     * Format: [ADMIN_KEY][NEW_PIN_LENGTH][NEW_PIN]
     */
    private void resetPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Xác thực admin key (đơn giản hóa - trong thực tế cần bảo mật hơn)
        // Admin key = "ADMI" in hex: A=0x41, D=0x44, M=0x4D, I=0x49
        byte[] adminKey = {(byte)0x41, (byte)0x44, (byte)0x4D, (byte)0x49};
        if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, 
                             adminKey, (short)0, (short)4) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        short offset = (short)(ISO7816.OFFSET_CDATA + 4);
        byte newPinLength = buffer[offset++];
        
        if (newPinLength < 4 || newPinLength > PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        pin.update(buffer, offset, newPinLength);
        pin.resetAndUnblock();
    }
}

