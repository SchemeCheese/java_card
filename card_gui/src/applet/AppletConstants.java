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

    // INS codes - Balance Management (Encrypted)
    public static final byte INS_GET_BALANCE = (byte)0xD0;
    public static final byte INS_UPDATE_BALANCE = (byte)0xD1;

    // INS codes - Book Management
    public static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    public static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    public static final byte INS_RETURN_BOOK = (byte)0x80;

    // INS codes - RSA Authentication
    public static final byte INS_RSA_GENERATE_KEYPAIR = (byte)0xB0;
    public static final byte INS_RSA_GET_PUBLIC_KEY = (byte)0xB1;
    public static final byte INS_RSA_SIGN_CHALLENGE = (byte)0xB2;
    public static final byte INS_RSA_DECRYPT = (byte)0xB3; // [NEW] Decrypt Server Key

    // INS codes - AES Encryption
    public static final byte INS_AES_SET_KEY = (byte)0xC0;
    public static final byte INS_AES_ENCRYPT = (byte)0xC1;
    public static final byte INS_AES_DECRYPT = (byte)0xC2;

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

    // RSA Configuration
    public static final short RSA_KEY_SIZE = (short)1024;  // 1024-bit RSA
    public static final short RSA_MODULUS_SIZE = (short)128;  // 1024 bits = 128 bytes
    public static final short RSA_EXPONENT_SIZE = (short)3;  // Usually 3 bytes (65537 = 0x010001)
    public static final short RSA_SIGNATURE_SIZE = (short)128;  // 1024-bit signature = 128 bytes
    public static final short RSA_CHALLENGE_SIZE = (short)16;  // 16 bytes challenge

    // AES Configuration
    public static final byte AES_KEY_SIZE = (byte)16;  // AES-128: 16 bytes key
    public static final byte AES_BLOCK_SIZE = (byte)16;  // AES block size: 16 bytes

    // Default Values (Thêm vào để SimulatorService sử dụng)
    public static final String DEFAULT_PIN = "000000";  // 6 số 0
    public static final int DEFAULT_PIN_TRIES = 3;

    // Admin Credentials (Fixed)
    public static final String ADMIN_STUDENT_CODE = "CT060132";  // MSSV của Admin
    
    // Admin Key (Legacy)
    public static final byte[] ADMIN_KEY = {
            (byte)0x41, (byte)0x44, (byte)0x4D, (byte)0x49  // "ADMI"
    };

    private AppletConstants() {
        // Prevent instantiation
    }
}