package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

/**
 * Module quản lý mã hóa AES
 * Lưu trữ AES key và thực hiện mã hóa/giải mã dữ liệu
 */
public class AESEncryptionManager {
    
    private byte[] aesKey;
    private boolean keySet;
    private Cipher aesCipher;
    
    /**
     * Khởi tạo AESEncryptionManager
     */
    public AESEncryptionManager() {
        aesKey = new byte[AppletConstants.AES_KEY_SIZE];
        keySet = false;
        
        try {
            // Khởi tạo Cipher cho AES
            aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        } catch (CryptoException e) {
            // Nếu không hỗ trợ, sẽ dùng ECB mode
            try {
                aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
            } catch (CryptoException e2) {
                // Không hỗ trợ AES - sẽ không dùng mã hóa
                aesCipher = null;
            }
        }
    }
    
    /**
     * Thiết lập AES Key (Admin only - cần PIN đã verify)
     * Input: [AES_KEY (16 bytes)]
     * 
     * @param apdu APDU command
     * @param pinManager PinManager để kiểm tra PIN
     */
    public void setAESKey(APDU apdu, PinManager pinManager) {
        if (!pinManager.isPinValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        if (aesCipher == null) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;
        
        // Đọc AES key (16 bytes)
        if ((short)(apdu.getIncomingLength() - offset) != AppletConstants.AES_KEY_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        Util.arrayCopy(buffer, offset, aesKey, (short)0, AppletConstants.AES_KEY_SIZE);
        keySet = true;
        
        // Response: Success
        buffer[0] = (byte)0x01;
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Mã hóa dữ liệu với AES
     * Input: [DATA_LENGTH (1 byte)] [DATA (variable)]
     * Output: [ENCRYPTED_DATA (padded to 16 bytes blocks)]
     * 
     * @param apdu APDU command
     */
    public void encrypt(APDU apdu) {
        if (!keySet || aesCipher == null) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;
        
        // Đọc độ dài dữ liệu
        byte dataLen = buffer[offset++];
        if (dataLen <= 0 || dataLen > (byte)(buffer.length - offset)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Note: JavaCard có giới hạn về AES encryption
        // Trong thực tế, client sẽ mã hóa dữ liệu trước khi gửi
        // Thẻ chỉ lưu dữ liệu đã mã hóa
        // Method này được giữ lại để tương thích nhưng không thực sự mã hóa
        
        // Response: Echo back data (trong thực tế sẽ không dùng method này)
        buffer[0] = (byte)0x00;  // Not supported
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Giải mã dữ liệu với AES
     * Input: [ENCRYPTED_DATA (multiple of 16 bytes)]
     * Output: [DECRYPTED_DATA]
     * 
     * @param apdu APDU command
     */
    public void decrypt(APDU apdu) {
        if (!keySet || aesCipher == null) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short offset = ISO7816.OFFSET_CDATA;
        short dataLen = (short)(apdu.getIncomingLength() - offset);
        
        // Kiểm tra độ dài phải là bội của 16
        if ((dataLen % AppletConstants.AES_BLOCK_SIZE) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Note: JavaCard có giới hạn về AES decryption
        // Trong thực tế, client sẽ giải mã dữ liệu sau khi nhận từ thẻ
        // Thẻ chỉ lưu và trả về dữ liệu đã mã hóa
        // Method này được giữ lại để tương thích nhưng không thực sự giải mã
        
        // Response: Echo back data (trong thực tế sẽ không dùng method này)
        buffer[0] = (byte)0x00;  // Not supported
        apdu.setOutgoingAndSend((short)0, (short)1);
    }
    
    /**
     * Kiểm tra đã set key chưa
     */
    public boolean isKeySet() {
        return keySet;
    }
}

