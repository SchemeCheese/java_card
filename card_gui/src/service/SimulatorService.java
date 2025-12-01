package service;

import com.licel.jcardsim.base.Simulator;
import applet.AppletConstants; // [FIXED] Import đúng file từ package applet
import javacard.framework.AID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Service class for handling JavaCard simulator operations
 * [FIXED] Sửa lỗi import và tên biến
 */
public class SimulatorService {
    private Simulator simulator;
    private boolean isConnected = false;
    private boolean isPinVerified = false;
    private int pinTriesRemaining = AppletConstants.DEFAULT_PIN_TRIES;

    // Cấu hình PBKDF2
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int HASH_BIT_LENGTH = 256; // 256 bits = 32 bytes

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isPinVerified() {
        return isPinVerified;
    }

    public void setPinVerified(boolean pinVerified) {
        isPinVerified = pinVerified;
    }

    public int getPinTriesRemaining() {
        return pinTriesRemaining;
    }

    public void setPinTriesRemaining(int tries) {
        this.pinTriesRemaining = tries;
    }

    public void connect() throws Exception {
        simulator = new Simulator();
        // Sử dụng AppletConstants.APPLET_AID thay vì DEFAULT_AID
        AID aid = new AID(AppletConstants.APPLET_AID, (short)0, (byte)AppletConstants.APPLET_AID.length);

        String appletClassName = "applet.LibraryCardApplet";
        Class<?> appletClass = Class.forName(appletClassName);

        @SuppressWarnings("unchecked")
        Class<? extends javacard.framework.Applet> appletClassCasted =
                (Class<? extends javacard.framework.Applet>) appletClass;

        simulator.installApplet(aid, appletClassCasted);
        simulator.selectApplet(aid);

        isConnected = true;
    }

    public byte[] sendCommand(byte[] command) {
        if (simulator == null) {
            throw new RuntimeException("Simulator chưa được khởi động!");
        }
        return simulator.transmitCommand(command);
    }

    public int getSW(byte[] response) {
        if (response.length < 2) return 0;
        int sw1 = response[response.length - 2] & 0xFF;
        int sw2 = response[response.length - 1] & 0xFF;
        return (sw1 << 8) | sw2;
    }

    // --- CÁC HÀM TIỆN ÍCH BẢO MẬT ---

    /**
     * Sinh Salt ngẫu nhiên
     */
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        // Sử dụng AppletConstants
        byte[] salt = new byte[AppletConstants.SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Tạo Hash từ PIN và Salt sử dụng PBKDF2WithHmacSHA256
     */
    private byte[] hashPin(char[] pin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, HASH_BIT_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    // --- CÁC HÀM NGHIỆP VỤ PIN ---

    /**
     * Tạo PIN lần đầu (Khởi tạo thẻ)
     */
    public void createDemoPin() throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        String defaultPin = AppletConstants.DEFAULT_PIN;

        // 1. Sinh Salt ngẫu nhiên tại Client
        byte[] salt = generateSalt();

        // 2. Tính Hash PBKDF2
        byte[] hash = hashPin(defaultPin.toCharArray(), salt);

        // 3. Đóng gói APDU: [INS_CREATE] [Lc] [SALT (16)] [HASH (32)]
        int dataLength = salt.length + hash.length;
        byte[] cmd = new byte[5 + dataLength];

        cmd[0] = 0x00;
        cmd[1] = AppletConstants.INS_CREATE_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)dataLength;

        // Copy Salt
        System.arraycopy(salt, 0, cmd, 5, salt.length);
        // Copy Hash
        System.arraycopy(hash, 0, cmd, 5 + salt.length, hash.length);

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) != 0x9000) {
            throw new RuntimeException("Không thể tạo PIN demo. Lỗi SW: " + String.format("%04X", getSW(resp)));
        }
    }

    /**
     * Xác thực PIN
     */
    public boolean verifyPin(char[] pinChars) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        // Bước 1: Lấy Salt từ thẻ
        byte[] getSaltCmd = {0x00, AppletConstants.INS_GET_SALT, 0x00, 0x00, 0x00};
        byte[] saltResp = sendCommand(getSaltCmd);

        if (getSW(saltResp) != 0x9000 || saltResp.length < AppletConstants.SALT_LENGTH + 2) {
            throw new RuntimeException("Không thể lấy Salt từ thẻ. Hãy chắc chắn thẻ đã được tạo PIN.");
        }

        // Tách Salt từ phản hồi
        byte[] salt = Arrays.copyOf(saltResp, AppletConstants.SALT_LENGTH);

        // Bước 2: Tính Hash
        byte[] hash = hashPin(pinChars, salt);

        // Bước 3: Gửi Hash xuống thẻ
        byte[] verifyCmd = new byte[5 + hash.length];
        verifyCmd[0] = 0x00;
        verifyCmd[1] = AppletConstants.INS_VERIFY_PIN;
        verifyCmd[2] = 0x00;
        verifyCmd[3] = 0x00;
        verifyCmd[4] = (byte)hash.length;
        System.arraycopy(hash, 0, verifyCmd, 5, hash.length);

        byte[] resp = sendCommand(verifyCmd);

        if (getSW(resp) == 0x9000 && resp.length > 2) {
            if (resp[0] == 0x01) {
                isPinVerified = true;
                pinTriesRemaining = 3;
                return true;
            } else {
                pinTriesRemaining = resp[1] & 0xFF;
                return false;
            }
        } else if (getSW(resp) == 0x6983) { // SW_FILE_INVALID = PIN Blocked
            pinTriesRemaining = 0;
            throw new Exception("Thẻ đã bị khóa (Block) do nhập sai quá 3 lần!");
        }

        return false;
    }

    /**
     * Đổi PIN
     */
    public boolean changePin(char[] oldPin, char[] newPin) throws Exception {
        if (!isConnected) {
            throw new RuntimeException("Chưa kết nối simulator!");
        }

        // 1. Sinh Salt MỚI
        byte[] newSalt = generateSalt();

        // 2. Tính Hash MỚI
        byte[] newHash = hashPin(newPin, newSalt);

        // 3. Gửi lệnh đổi
        int dataLength = newSalt.length + newHash.length;
        byte[] cmd = new byte[5 + dataLength];

        cmd[0] = 0x00;
        cmd[1] = AppletConstants.INS_CHANGE_PIN;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)dataLength;

        System.arraycopy(newSalt, 0, cmd, 5, newSalt.length);
        System.arraycopy(newHash, 0, cmd, 5 + newSalt.length, newHash.length);

        byte[] resp = sendCommand(cmd);

        if (getSW(resp) == 0x9000) {
            isPinVerified = false;
            return true;
        }

        return false;
    }
}