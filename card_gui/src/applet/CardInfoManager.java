package applet;

import javacard.framework.*;

    /**
     * Module quản lý thông tin thẻ
     * Xử lý lưu trữ và truy xuất thông tin thẻ: ID, tên, ngày hết hạn
     * Hỗ trợ AES: Card ID lưu plaintext (dùng cho key derivation), Name và Expiry có thể được mã hóa
     */
    public class CardInfoManager {
    
    // Card ID: LUÔN LÀ PLAINTEXT (cần để derive AES key)
    private byte[] cardId;
    // Name và Expiry: CÓ THỂ ĐÃ MÃ HÓA (client mã hóa trước khi gửi)
    private byte[] holderName;
    private byte holderNameLength;
    private byte[] expiryDate; // DDMMYYYY format
    
    // Flag để biết Name và Expiry có được mã hóa không
    // Card ID luôn là plaintext
    private boolean encrypted;
    
    /**
     * Khởi tạo CardInfoManager
     */
    public CardInfoManager() {
        cardId = new byte[AppletConstants.CARD_ID_LENGTH];
        holderName = new byte[AppletConstants.NAME_MAX_LENGTH];
        expiryDate = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
        holderNameLength = 0;
        encrypted = false;
    }
    
    /**
     * Thiết lập thông tin thẻ (yêu cầu xác thực PIN)
     * Format: [ENCRYPTED_FLAG (1 byte)] [CARD_ID_PLAINTEXT (10)] [NAME_LENGTH] [NAME] [EXPIRY_DATE]
     * Nếu ENCRYPTED_FLAG = 1: Card ID là plaintext, Name và Expiry đã được mã hóa AES từ client
     * Nếu ENCRYPTED_FLAG = 0: Tất cả dữ liệu là plaintext
     * 
     * @param apdu APDU command
     * @param pinManager PinManager để kiểm tra PIN đã xác thực
     */
    public void setCardInfo(APDU apdu, PinManager pinManager) {
        if (!pinManager.isPinValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;
        
        // Đọc flag mã hóa (1 byte)
        encrypted = (buffer[offset++] == (byte)0x01);
        
        // Đọc Card ID (10 bytes) - LUÔN LÀ PLAINTEXT
        Util.arrayCopy(buffer, offset, cardId, (short)0, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;
        
        // Đọc tên (độ dài biến đổi) - CÓ THỂ ĐÃ MÃ HÓA nếu encrypted = true
        holderNameLength = buffer[offset++];
        if (holderNameLength > AppletConstants.NAME_MAX_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(buffer, offset, holderName, (short)0, holderNameLength);
        offset += holderNameLength;
        
        // Đọc ngày hết hạn (8 bytes - DDMMYYYY) - CÓ THỂ ĐÃ MÃ HÓA nếu encrypted = true
        Util.arrayCopy(buffer, offset, expiryDate, (short)0, AppletConstants.EXPIRY_DATE_LENGTH);
    }
    
    /**
     * Lấy thông tin thẻ
     * Response: [ENCRYPTED_FLAG (1 byte)] [CARD_ID_PLAINTEXT][NAME_LENGTH][NAME][EXPIRY_DATE][NUM_BOOKS]
     * Nếu ENCRYPTED_FLAG = 1: Card ID là plaintext, Name và Expiry đã được mã hóa
     * 
     * @param apdu APDU command
     * @param numBorrowedBooks Số lượng sách đang mượn (từ BookManager)
     */
    public void getCardInfo(APDU apdu, byte numBorrowedBooks) {
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        // Encrypted flag
        buffer[offset++] = encrypted ? (byte)0x01 : (byte)0x00;
        
        // Card ID (LUÔN LÀ PLAINTEXT)
        Util.arrayCopy(cardId, (short)0, buffer, offset, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;
        
        // Name (CÓ THỂ ĐÃ MÃ HÓA nếu encrypted = true)
        buffer[offset++] = holderNameLength;
        Util.arrayCopy(holderName, (short)0, buffer, offset, holderNameLength);
        offset += holderNameLength;
        
        // Expiry Date (CÓ THỂ ĐÃ MÃ HÓA nếu encrypted = true)
        Util.arrayCopy(expiryDate, (short)0, buffer, offset, AppletConstants.EXPIRY_DATE_LENGTH);
        offset += AppletConstants.EXPIRY_DATE_LENGTH;
        
        // Number of borrowed books
        buffer[offset++] = numBorrowedBooks;
        
        apdu.setOutgoingAndSend((short)0, offset);
    }
    
    /**
     * Kiểm tra thẻ đã được khởi tạo chưa
     * 
     * @return true nếu thẻ đã có thông tin
     */
    public boolean isCardInitialized() {
        return holderNameLength > 0;
    }
}

