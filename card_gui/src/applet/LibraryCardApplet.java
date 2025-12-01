package applet;

import javacard.framework.*;

/**
 * Applet Thẻ Thư Viện - Version Modular
 * Router chuyển các lệnh đến module tương ứng
 */
public class LibraryCardApplet extends Applet {

    private PinManager pinManager;
    private CardInfoManager cardInfoManager;
    private BookManager bookManager;

    protected LibraryCardApplet() {
        // Constructor rỗng
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        LibraryCardApplet applet = new LibraryCardApplet();

        applet.pinManager = new PinManager();
        applet.cardInfoManager = new CardInfoManager();
        applet.bookManager = new BookManager();

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
            case AppletConstants.INS_GET_SALT: // [NEW] Router cho lệnh lấy Salt
                pinManager.getSalt(apdu);
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