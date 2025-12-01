package applet;

import javacard.framework.*;

/**
 * Module quản lý PIN
 * Cải tiến: Lưu trữ Salt và xác thực qua Hash
 */
public class PinManager {

    private OwnerPIN pin;
    private byte[] pinSalt; // [NEW] Lưu trữ Salt

    /**
     * Khởi tạo PinManager
     */
    public PinManager() {
        pin = new OwnerPIN(AppletConstants.PIN_TRY_LIMIT, AppletConstants.PIN_MAX_SIZE);
        pinSalt = new byte[AppletConstants.SALT_LENGTH];
    }

    /**
     * Lấy đối tượng PIN
     */
    public OwnerPIN getPin() {
        return pin;
    }

    /**
     * [NEW] Gửi Salt về cho Client để Client tính Hash
     */
    public void getSalt(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        // Copy salt vào buffer để gửi đi
        Util.arrayCopy(pinSalt, (short)0, buffer, (short)0, AppletConstants.SALT_LENGTH);

        apdu.setOutgoingAndSend((short)0, AppletConstants.SALT_LENGTH);
    }

    /**
     * Tạo PIN mới
     * Format Input: [SALT (16 bytes)] [HASH (32 bytes)]
     */
    public void createPin(APDU apdu) {
        if (pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        // Dữ liệu bắt đầu từ OFFSET_CDATA
        short offset = ISO7816.OFFSET_CDATA;

        // 1. Lưu Salt (16 bytes đầu tiên)
        Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
        offset += AppletConstants.SALT_LENGTH;

        // 2. Lưu Hash vào OwnerPIN (32 bytes tiếp theo)
        // Lưu ý: PIN_MAX_SIZE đã được tăng lên 32 trong Constants
        pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);
    }

    /**
     * Xác thực PIN
     * Format Input: [HASH (32 bytes)]
     */
    public void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        // Client đã tính toán Hash(PIN, Salt) và gửi xuống
        // Thẻ chỉ việc so sánh Hash này với Hash đã lưu
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, AppletConstants.PIN_MAX_SIZE)) {
            // Hash khớp -> Đúng
            buffer[0] = (byte)0x01;
            apdu.setOutgoingAndSend((short)0, (short)1);
        } else {
            // Hash không khớp -> Sai -> OwnerPIN tự giảm bộ đếm
            buffer[0] = (byte)0x00;
            buffer[1] = pin.getTriesRemaining();
            apdu.setOutgoingAndSend((short)0, (short)2);
        }
    }

    /**
     * Thay đổi PIN (Yêu cầu đã verify PIN cũ trước đó)
     * Format Input: [NEW_SALT (16 bytes)] [NEW_HASH (32 bytes)]
     */
    public void changePin(APDU apdu) {
        // Kiểm tra xem đã xác thực PIN cũ chưa
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;

        // 1. Cập nhật Salt mới
        Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
        offset += AppletConstants.SALT_LENGTH;

        // 2. Cập nhật Hash mới vào OwnerPIN
        pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);

        // Reset trạng thái xác thực sau khi đổi
        pin.resetAndUnblock();
    }

    /**
     * Lấy số lần nhập PIN còn lại
     */
    public void getPinTries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = pin.getTriesRemaining();
        apdu.setOutgoingAndSend((short)0, (short)1);
    }

    /**
     * Reset PIN (Admin) - Mở khóa cứng
     * Format: [ADMIN_KEY] [NEW_SALT] [NEW_HASH]
     */
    public void resetPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short offset = ISO7816.OFFSET_CDATA;

        // 1. Xác thực admin key
        if (Util.arrayCompare(buffer, offset,
                AppletConstants.ADMIN_KEY, (short)0,
                (short)AppletConstants.ADMIN_KEY.length) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        offset += AppletConstants.ADMIN_KEY.length;

        // 2. Cập nhật Salt mới
        Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
        offset += AppletConstants.SALT_LENGTH;

        // 3. Cập nhật Hash mới
        pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);

        // Mở khóa và reset bộ đếm
        pin.resetAndUnblock();
    }

    public boolean isPinValidated() {
        return pin.isValidated();
    }
}