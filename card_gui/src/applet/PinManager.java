package applet;

import javacard.framework.*;

/**
 * Module quản lý PIN
 * Xử lý tất cả các thao tác liên quan đến PIN: tạo, xác thực, đổi, reset
 */
public class PinManager {
    
    private OwnerPIN pin;
    
    /**
     * Khởi tạo PinManager
     */
    public PinManager() {
        pin = new OwnerPIN(AppletConstants.PIN_TRY_LIMIT, AppletConstants.PIN_MAX_SIZE);
    }
    
    /**
     * Lấy đối tượng PIN để kiểm tra trạng thái
     */
    public OwnerPIN getPin() {
        return pin;
    }
    
    /**
     * Tạo PIN mới (chỉ thực hiện được 1 lần)
     * Format: [PIN_LENGTH][PIN_DATA]
     * 
     * @param apdu APDU command
     */
    public void createPin(APDU apdu) {
        if (pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        byte pinLength = buffer[ISO7816.OFFSET_CDATA];
        
        if (pinLength < AppletConstants.PIN_MIN_SIZE || 
            pinLength > AppletConstants.PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        pin.update(buffer, (short)(ISO7816.OFFSET_CDATA + 1), pinLength);
    }
    
    /**
     * Xác thực PIN
     * Format: [PIN_LENGTH][PIN_DATA]
     * Response: [0x01] nếu đúng, [0x00][TRIES_REMAINING] nếu sai
     * 
     * @param apdu APDU command
     */
    public void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        byte pinLength = buffer[ISO7816.OFFSET_CDATA];
        
        if (pin.check(buffer, (short)(ISO7816.OFFSET_CDATA + 1), pinLength)) {
            // PIN đúng
            buffer[0] = (byte)0x01;
            apdu.setOutgoingAndSend((short)0, (short)1);
        } else {
            // PIN sai
            buffer[0] = (byte)0x00;
            buffer[1] = pin.getTriesRemaining();
            apdu.setOutgoingAndSend((short)0, (short)2);
        }
    }
    
    /**
     * Thay đổi PIN (yêu cầu xác thực PIN cũ trước)
     * Format: [OLD_PIN_LENGTH][OLD_PIN][NEW_PIN_LENGTH][NEW_PIN]
     * 
     * @param apdu APDU command
     */
    public void changePin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        
        short offset = ISO7816.OFFSET_CDATA;
        byte oldPinLength = buffer[offset++];
        
        // Xác thực PIN cũ
        if (!pin.check(buffer, offset, oldPinLength)) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        offset += oldPinLength;
        byte newPinLength = buffer[offset++];
        
        if (newPinLength < AppletConstants.PIN_MIN_SIZE || 
            newPinLength > AppletConstants.PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Cập nhật PIN mới
        pin.update(buffer, offset, newPinLength);
        pin.resetAndUnblock();
    }
    
    /**
     * Lấy số lần nhập PIN còn lại
     * Response: [TRIES_REMAINING]
     * 
     * @param apdu APDU command
     */
    public void getPinTries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = pin.getTriesRemaining();
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Reset PIN (chỉ admin mới được phép)
     * Format: [ADMIN_KEY][NEW_PIN_LENGTH][NEW_PIN]
     * 
     * @param apdu APDU command
     */
    public void resetPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        
        // Xác thực admin key
        if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, 
                             AppletConstants.ADMIN_KEY, (short)0, 
                             (short)AppletConstants.ADMIN_KEY.length) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        short offset = (short)(ISO7816.OFFSET_CDATA + AppletConstants.ADMIN_KEY.length);
        byte newPinLength = buffer[offset++];
        
        if (newPinLength < AppletConstants.PIN_MIN_SIZE || 
            newPinLength > AppletConstants.PIN_MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        pin.update(buffer, offset, newPinLength);
        pin.resetAndUnblock();
    }
    
    /**
     * Kiểm tra PIN đã được xác thực chưa
     * 
     * @return true nếu PIN đã được xác thực
     */
    public boolean isPinValidated() {
        return pin.isValidated();
    }
}

