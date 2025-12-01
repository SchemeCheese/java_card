package applet;

/**
 * Constants cho Library Card Applet
 * Chứa tất cả các INS codes và độ dài dữ liệu
 */
public class AppletConstants {
    
    // AID mặc định
    public static final byte[] DEFAULT_AID = {
        (byte)0x4C, (byte)0x49, (byte)0x42, (byte)0x52,  // 'L','I','B','R'
        (byte)0x41, (byte)0x52, (byte)0x59              // 'A','R','Y'
    };
    
    // INS codes - PIN Management
    public static final byte INS_CREATE_PIN = (byte)0x10;
    public static final byte INS_VERIFY_PIN = (byte)0x20;
    public static final byte INS_CHANGE_PIN = (byte)0x30;
    public static final byte INS_GET_PIN_TRIES = (byte)0x90;
    public static final byte INS_RESET_PIN = (byte)0xA0;
    
    // INS codes - Card Info Management
    public static final byte INS_SET_CARD_INFO = (byte)0x40;
    public static final byte INS_GET_CARD_INFO = (byte)0x50;
    
    // INS codes - Book Management
    public static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    public static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    public static final byte INS_RETURN_BOOK = (byte)0x80;
    
    // PIN Configuration
    public static final byte PIN_TRY_LIMIT = (byte)3;
    public static final byte PIN_MAX_SIZE = (byte)8;
    public static final byte PIN_MIN_SIZE = (byte)4;
    
    // Card Info Configuration
    public static final byte CARD_ID_LENGTH = (byte)10;
    public static final byte NAME_MAX_LENGTH = (byte)50;
    public static final byte EXPIRY_DATE_LENGTH = (byte)8; // DDMMYYYY format
    
    // Book Management Configuration
    public static final byte MAX_BORROWED_BOOKS = (byte)10;
    public static final byte BOOK_ID_LENGTH = (byte)8;
    
    // Admin Key for PIN Reset
    public static final byte[] ADMIN_KEY = {
        (byte)0x41, (byte)0x44, (byte)0x4D, (byte)0x49  // "ADMI"
    };
    
    private AppletConstants() {
        // Prevent instantiation
    }
}

