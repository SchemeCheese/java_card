package applet;

import javacard.framework.*;

/**
 * Module quản lý thông tin thẻ
 * Xử lý lưu trữ và truy xuất thông tin thẻ: ID, tên, ngày hết hạn
 */
public class CardInfoManager {
    
    private byte[] cardId;
    private byte[] holderName;
    private byte holderNameLength;
    private byte[] expiryDate; // DDMMYYYY format
    
    /**
     * Khởi tạo CardInfoManager
     */
    public CardInfoManager() {
        cardId = new byte[AppletConstants.CARD_ID_LENGTH];
        holderName = new byte[AppletConstants.NAME_MAX_LENGTH];
        expiryDate = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
        holderNameLength = 0;
    }
    
    /**
     * Thiết lập thông tin thẻ (yêu cầu xác thực PIN)
     * Format: [CARD_ID][NAME_LENGTH][NAME][EXPIRY_DATE]
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
        
        // Đọc Card ID (10 bytes)
        Util.arrayCopy(buffer, offset, cardId, (short)0, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;
        
        // Đọc tên (độ dài biến đổi)
        holderNameLength = buffer[offset++];
        if (holderNameLength > AppletConstants.NAME_MAX_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopy(buffer, offset, holderName, (short)0, holderNameLength);
        offset += holderNameLength;
        
        // Đọc ngày hết hạn (8 bytes - DDMMYYYY)
        Util.arrayCopy(buffer, offset, expiryDate, (short)0, AppletConstants.EXPIRY_DATE_LENGTH);
    }
    
    /**
     * Lấy thông tin thẻ
     * Response: [CARD_ID][NAME_LENGTH][NAME][EXPIRY_DATE][NUM_BOOKS]
     * 
     * @param apdu APDU command
     * @param numBorrowedBooks Số lượng sách đang mượn (từ BookManager)
     */
    public void getCardInfo(APDU apdu, byte numBorrowedBooks) {
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        // Card ID
        Util.arrayCopy(cardId, (short)0, buffer, offset, AppletConstants.CARD_ID_LENGTH);
        offset += AppletConstants.CARD_ID_LENGTH;
        
        // Name
        buffer[offset++] = holderNameLength;
        Util.arrayCopy(holderName, (short)0, buffer, offset, holderNameLength);
        offset += holderNameLength;
        
        // Expiry Date
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

