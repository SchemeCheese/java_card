package applet;

/**
 * Constants cho Library Card Applet
 * Updated: Chứa đầy đủ hằng số cho cả Applet và GUI Simulator
 */
public class AppletConstants {

    // AID mặc định (Đổi tên thành APPLET_AID để khớp với SimulatorService cũ)
    public static final byte[] APPLET_AID = {
            (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78, (byte)0x90, (byte)0x00
    };

    // INS codes - PIN Management
    public static final byte INS_CREATE_PIN = (byte)0x10;
    public static final byte INS_VERIFY_PIN = (byte)0x20;
    public static final byte INS_CHANGE_PIN = (byte)0x30;
    public static final byte INS_GET_PIN_TRIES = (byte)0x90;
    public static final byte INS_RESET_PIN = (byte)0xA0;
    public static final byte INS_GET_SALT = (byte)0x22; // [NEW]

    // INS codes - Card Info Management
    public static final byte INS_SET_CARD_INFO = (byte)0x40;
    public static final byte INS_GET_CARD_INFO = (byte)0x50;

    // INS codes - Book Management
    public static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    public static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    public static final byte INS_RETURN_BOOK = (byte)0x80;

    // PIN Configuration
    public static final byte PIN_TRY_LIMIT = (byte)3;
    public static final byte PIN_MAX_SIZE = (byte)32; // [UPDATED] 32 bytes cho SHA-256
    public static final byte SALT_LENGTH = (byte)16;  // [NEW] 16 bytes Salt

    // Card Info Configuration
    public static final byte CARD_ID_LENGTH = (byte)10;
    public static final byte NAME_MAX_LENGTH = (byte)50;
    public static final byte EXPIRY_DATE_LENGTH = (byte)8;

    // Book Management Configuration
    public static final byte MAX_BORROWED_BOOKS = (byte)10;
    public static final byte BOOK_ID_LENGTH = (byte)8;

    // Default Values (Thêm vào để SimulatorService sử dụng)
    public static final String DEFAULT_PIN = "123456";
    public static final int DEFAULT_PIN_TRIES = 3;

    // Admin Key
    public static final byte[] ADMIN_KEY = {
            (byte)0x41, (byte)0x44, (byte)0x4D, (byte)0x49  // "ADMI"
    };

    private AppletConstants() {
        // Prevent instantiation
    }
}