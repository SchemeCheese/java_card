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

    // Key is no longer hardcoded. It is injected at runtime after secure exchange.
    private static byte[] aesKey;
    private static String systemMasterKey; // Plaintext System Master Key (RAM only)

    private static final String ALGORITHM = "AES/ECB/NoPadding";

    /**
     * Set Master Key (called after secure exchange with Server & Card)
     * @param masterKey Plaintext Master Key acquired from secure channel
     */
    public static void setMasterKey(String masterKey) {
        if (masterKey == null || masterKey.isEmpty()) {
            throw new IllegalArgumentException("Master Key cannot be empty");
        }
        systemMasterKey = masterKey;
        
        // For 'encryptBalance' legacy support (which used a fixed key derived from "Password..."):
        // We will now use the first 16 bytes of Master Key directly or derive a default key.
        // Assuming Master Key from server is the root key. 
        // Let's use it directly as the AES Key for balance encryption for now (as requested).
        // Or better: Derive a default 'session key' from it to match previous 'AES_KEY' usage pattern
        try {
            // Demo strategy: Use Master Key bytes directly (padded/truncated to 16 bytes)
            byte[] keyBytes = masterKey.getBytes("UTF-8");
            aesKey = new byte[16];
            System.arraycopy(keyBytes, 0, aesKey, 0, Math.min(keyBytes.length, 16));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get Master Key (for Key Derivation)
     */
    public static String getMasterKey() {
        if (systemMasterKey == null) {
            throw new IllegalStateException("System Master Key not initialized. Connect and authenticate with server first.");
        }
        return systemMasterKey;
    }
    
    /**
     * Check if key is initialized
     */
    public static boolean isKeyInitialized() {
        return aesKey != null && systemMasterKey != null;
    }

    /**
     * Helper to get current AES Key (internal)
     */
    private static byte[] getAESKey() {
        if (aesKey == null) {
            throw new IllegalStateException("AES Key not initialized. Connect to card and authenticate first.");
        }
        return aesKey;
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

        SecretKeySpec keySpec = new SecretKeySpec(getAESKey(), "AES");
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

        SecretKeySpec keySpec = new SecretKeySpec(getAESKey(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decrypted = cipher.doFinal(encryptedData);

        // Read first 8 bytes as long
        ByteBuffer buffer = ByteBuffer.wrap(decrypted);
        return buffer.getLong();
    }
}
