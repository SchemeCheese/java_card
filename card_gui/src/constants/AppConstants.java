package constants;

import java.awt.*;

/**
 * Application constants including colors, INS codes, and AID
 */
public class AppConstants {

    // Colors - Modern palette
    public static final Color PRIMARY_COLOR = new Color(37, 99, 235); // Blue
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94); // Green
    public static final Color DANGER_COLOR = new Color(239, 68, 68); // Red
    public static final Color WARNING_COLOR = new Color(251, 191, 36); // Yellow
    public static final Color BACKGROUND = new Color(249, 250, 251);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    public static final Color BORDER_COLOR = new Color(229, 231, 235);

    // AID của Applet
    public static final byte[] APPLET_AID = {
        (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78, (byte)0x90, (byte)0x00
    };

    // INS codes
    public static final byte INS_CREATE_PIN = (byte)0x10;
    public static final byte INS_VERIFY_PIN = (byte)0x20;
    public static final byte INS_CHANGE_PIN = (byte)0x30;
    public static final byte INS_SET_CARD_INFO = (byte)0x40;
    public static final byte INS_GET_CARD_INFO = (byte)0x50;
    public static final byte INS_ADD_BORROWED_BOOK = (byte)0x60;
    public static final byte INS_GET_BORROWED_BOOKS = (byte)0x70;
    public static final byte INS_RETURN_BOOK = (byte)0x80;

    // Default values
    public static final String DEFAULT_PIN = "123456";
    public static final int DEFAULT_PIN_TRIES = 3;

    private AppConstants() {
        // Prevent instantiation
    }
}
