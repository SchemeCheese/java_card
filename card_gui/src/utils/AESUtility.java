package utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Utility for AES Encryption/Decryption of Balance
 */
public class AESUtility {

    // Hardcoded Key for Demo (16 bytes)
    // In production, this should be derived or stored securely
    private static final byte[] AES_KEY = {
        (byte)0x50, (byte)0x61, (byte)0x73, (byte)0x73, // 'P','a','s','s'
        (byte)0x77, (byte)0x6F, (byte)0x72, (byte)0x64, // 'w','o','r','d'
        (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, // '1','2','3','4'
        (byte)0x41, (byte)0x42, (byte)0x43, (byte)0x44  // 'A','B','C','D'
    };

    private static final String ALGORITHM = "AES/ECB/NoPadding";
    private static final String MASTER_KEY = "MASTER_KEY_FOR_LIBRARY_CARD_SYSTEM"; // Demo Master Key

    /**
     * Get Master Key
     */
    public static String getMasterKey() {
        return MASTER_KEY;
    }

    /**
     * Derive AES Key from Master Key and Salt (Card ID)
     * using PBKDF2WithHmacSHA256
     */
    public static javax.crypto.SecretKey deriveKey(String masterKey, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt.getBytes(), 65536, 128);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Generic Encrypt using SecretKey
     */
    public static byte[] encrypt(byte[] data, javax.crypto.SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        // Pad data to multiple of 16 bytes (PKCS5Padding manual or ZeroPadding)
        // Since we use NoPadding, we must pad manually. JavaCard usually expects fixed blocks.
        // For simplicity/demo: Zero padding to next 16 bytes if needed
        int len = data.length;
        int padding = 16 - (len % 16);
        if (padding == 16 && len > 0) padding = 0; // Already multiple
        if (len == 0) padding = 16;
        
        byte[] input = new byte[len + padding];
        System.arraycopy(data, 0, input, 0, len);
        
        return cipher.doFinal(input);
    }

    /**
     * Generic Decrypt using SecretKey
     */
    public static byte[] decrypt(byte[] data, javax.crypto.SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Encrypt a long value (balance) into 16 bytes using static AES_KEY
     */
    public static byte[] encryptBalance(long balance) throws Exception {
        // Convert long to bytes (8 bytes)
        ByteBuffer buffer = ByteBuffer.allocate(16); // Allocate 16 bytes for AES block
        buffer.putLong(balance);
        // Padding remaining 8 bytes with 0
        while (buffer.hasRemaining()) {
            buffer.put((byte)0);
        }
        byte[] input = buffer.array();

        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        return cipher.doFinal(input);
    }

    /**
     * Decrypt 16 bytes into a long value (balance)
     */
    public static long decryptBalance(byte[] encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.length != 16) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }

        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decrypted = cipher.doFinal(encryptedData);

        // Read first 8 bytes as long
        ByteBuffer buffer = ByteBuffer.wrap(decrypted);
        return buffer.getLong();
    }
}
