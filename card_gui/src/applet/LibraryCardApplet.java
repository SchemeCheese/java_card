package applet;

import javacard.framework.*;

/**
 * Applet Thẻ Thư Viện - Version Modular
 * Sử dụng các module riêng biệt để quản lý từng chức năng:
 * - PinManager: Quản lý PIN
 * - CardInfoManager: Quản lý thông tin thẻ
 * - BookManager: Quản lý sách mượn
 */
public class LibraryCardApplet extends Applet {
    
    // Các module quản lý
    private PinManager pinManager;
    private CardInfoManager cardInfoManager;
    private BookManager bookManager;
    
    /**
     * Constructor mặc định (cho JCardSim)
     * JCardSim sẽ tự động gọi install() method
     */
    protected LibraryCardApplet() {
        // Constructor rỗng - JCardSim sẽ gọi install() sau
    }
    
    /**
     * Phương thức install - JCardSim sẽ gọi method này
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        LibraryCardApplet applet = new LibraryCardApplet();
        
        // Khởi tạo các module
        applet.pinManager = new PinManager();
        applet.cardInfoManager = new CardInfoManager();
        applet.bookManager = new BookManager();
        
        // Xử lý tham số cài đặt và register
        if (bLength == 0 || bArray == null || bOffset >= bArray.length) {
            applet.register();
        } else {
            byte iLen = bArray[bOffset];
            if (iLen == 0) {
                applet.register();
            } else {
                applet.register(bArray, (short)(bOffset + 1), iLen);
            }
        }
    }
    
    /**
     * Xử lý các lệnh APDU
     * Router chuyển các lệnh đến module tương ứng
     */
    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];
        
        switch (ins) {
            // PIN Management
            case AppletConstants.INS_CREATE_PIN:
                pinManager.createPin(apdu);
                break;
            case AppletConstants.INS_VERIFY_PIN:
                pinManager.verifyPin(apdu);
                break;
            case AppletConstants.INS_CHANGE_PIN:
                pinManager.changePin(apdu);
                break;
            case AppletConstants.INS_GET_PIN_TRIES:
                pinManager.getPinTries(apdu);
                break;
            case AppletConstants.INS_RESET_PIN:
                pinManager.resetPin(apdu);
                break;
            
            // Card Info Management
            case AppletConstants.INS_SET_CARD_INFO:
                cardInfoManager.setCardInfo(apdu, pinManager);
                break;
            case AppletConstants.INS_GET_CARD_INFO:
                cardInfoManager.getCardInfo(apdu, bookManager.getNumBorrowedBooks());
                break;
            
            // Book Management
            case AppletConstants.INS_ADD_BORROWED_BOOK:
                bookManager.addBorrowedBook(apdu, pinManager);
                break;
            case AppletConstants.INS_GET_BORROWED_BOOKS:
                bookManager.getBorrowedBooks(apdu);
                break;
            case AppletConstants.INS_RETURN_BOOK:
                bookManager.returnBook(apdu, pinManager);
                break;
            
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}

