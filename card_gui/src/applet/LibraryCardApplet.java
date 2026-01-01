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
    private RSAAuthenticationManager rsaAuthManager;
    private AESEncryptionManager aesEncryptionManager;
    private byte[] encryptedBalance; // [NEW] 16 bytes for AES encrypted balance

    protected LibraryCardApplet() {
        // Constructor rỗng
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        LibraryCardApplet applet = new LibraryCardApplet();

        applet.pinManager = new PinManager();
        applet.cardInfoManager = new CardInfoManager();
        applet.bookManager = new BookManager();
        applet.rsaAuthManager = new RSAAuthenticationManager();
        applet.aesEncryptionManager = new AESEncryptionManager();
        applet.encryptedBalance = new byte[16]; // [NEW] Initialize balance storage

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
        byte p1 = buffer[ISO7816.OFFSET_P1];

        // Support legacy format: INS=0x31 with P1 to distinguish PIN commands
        // Format: 80 31 P1 00 Lc [Data]
        // P1: 0x01=VERIFY, 0x02=CHANGE, 0x03=CREATE, 0x04=RESET
        if (ins == (byte)0x31) {
            switch (p1) {
                case 0x01: // VERIFY_PIN
                    pinManager.verifyPin(apdu);
                    return;
                case 0x02: // CHANGE_PIN
                    pinManager.changePin(apdu);
                    return;
                case 0x03: // CREATE_PIN
                    pinManager.createPin(apdu);
                    return;
                case 0x04: // RESET_PIN
                    pinManager.resetPin(apdu);
                    return;
                default:
                    ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
            }
        }

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

            // RSA Authentication
            case AppletConstants.INS_RSA_GENERATE_KEYPAIR:
                try {
                    rsaAuthManager.generateKeyPair(apdu);
                } catch (Exception e) {
                    ISOException.throwIt(ISO7816.SW_UNKNOWN);
                }
                break;
            case AppletConstants.INS_RSA_GET_PUBLIC_KEY:
                try {
                    rsaAuthManager.getPublicKey(apdu);
                } catch (Exception e) {
                    ISOException.throwIt(ISO7816.SW_UNKNOWN);
                }
                break;
            case AppletConstants.INS_RSA_SIGN_CHALLENGE:
                // Check if rsaAuthManager is initialized
                if (rsaAuthManager == null) {
                    ISOException.throwIt((short)0x6A1B); // RSA manager not initialized
                    break;
                }
                try {
                    rsaAuthManager.signChallenge(apdu);
                } catch (ISOException e) {
                    // Re-throw ISOException (already has proper SW code)
                    throw e;
                } catch (Throwable t) {
                    // Catch any other exception/error and throw with specific code
                    // This includes RuntimeException, Error, etc.
                    ISOException.throwIt((short)0x6A1A); // Exception in router
                }
                break;
            case AppletConstants.INS_RSA_DECRYPT: // [NEW] Decrypt Server Key
                 // Check if rsaAuthManager is initialized
                if (rsaAuthManager == null) {
                    ISOException.throwIt((short)0x6A1B);
                    break;
                }
                try {
                    rsaAuthManager.decrypt(apdu);
                } catch (Exception e) {
                    ISOException.throwIt(ISO7816.SW_UNKNOWN);
                }
                break;
            // AES Encryption
            case AppletConstants.INS_AES_SET_KEY:
                aesEncryptionManager.setAESKey(apdu, pinManager);
                break;
            case AppletConstants.INS_AES_ENCRYPT:
                aesEncryptionManager.encrypt(apdu);
                break;
            case AppletConstants.INS_AES_DECRYPT:
                aesEncryptionManager.decrypt(apdu);
                break;

            // [NEW] Balance Management (Simple inline implementation)
            case AppletConstants.INS_GET_BALANCE:
                getBalance(apdu);
                break;
            case AppletConstants.INS_UPDATE_BALANCE:
                updateBalance(apdu);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // [NEW] Get encrypted balance (16 bytes)
    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(encryptedBalance, (short)0, buffer, (short)0, (short)16);
        apdu.setOutgoingAndSend((short)0, (short)16);
    }

    // [NEW] Update encrypted balance (requires PIN verification)
    private void updateBalance(APDU apdu) {
        if (!pinManager.isPinValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = (short)(buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc != 16) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        short bytesRead = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, encryptedBalance, (short)0, (short)16);
    }
}