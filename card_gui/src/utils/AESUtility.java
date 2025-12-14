package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.security.MessageDigest;

/**
 * Utility class để mã hóa/giải mã dữ liệu với AES-128
 * Client-side encryption: Mã hóa trước khi gửi lên thẻ, giải mã sau khi nhận từ thẻ
 */
public class AESUtility {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 128;  // AES-128
    private static final int IV_SIZE = 16;    // 16 bytes IV
    
    /**
     * Generate AES key từ master key và card ID
     * Sử dụng PBKDF2 để derive key
     * 
     * @param masterKey Master key (có thể lưu trong config)
     * @param cardId Card ID để tạo unique key cho mỗi thẻ
     * @return AES SecretKey
     */
    public static SecretKey deriveKey(String masterKey, String cardId) {
        try {
            // Sử dụng PBKDF2 để derive key
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                masterKey.toCharArray(),
                cardId.getBytes("UTF-8"),
                10000,  // iterations
                KEY_SIZE
            );
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Error deriving AES key", e);
        }
    }
    
    /**
     * Generate random AES key (dùng khi tạo thẻ mới)
     * 
     * @return Random AES SecretKey
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating AES key", e);
        }
    }
    
    /**
     * Encrypt data với AES
     * 
     * @param data Dữ liệu cần mã hóa
     * @param key AES key
     * @return Encrypted data (IV + encrypted data)
     */
    public static byte[] encrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(data);
            
            // Prepend IV to encrypted data
            byte[] result = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, result, IV_SIZE, encrypted.length);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }
    
    /**
     * Decrypt data với AES
     * 
     * @param encryptedData Encrypted data (IV + encrypted data)
     * @param key AES key
     * @return Decrypted data
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key) {
        try {
            if (encryptedData.length < IV_SIZE) {
                throw new IllegalArgumentException("Encrypted data too short");
            }
            
            // Extract IV
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Extract encrypted data
            byte[] encrypted = new byte[encryptedData.length - IV_SIZE];
            System.arraycopy(encryptedData, IV_SIZE, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
    
    /**
     * Encrypt string
     * 
     * @param plaintext Plain text
     * @param key AES key
     * @return Encrypted bytes (Base64 encoded)
     */
    public static String encryptString(String plaintext, SecretKey key) {
        try {
            byte[] encrypted = encrypt(plaintext.getBytes("UTF-8"), key);
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting string", e);
        }
    }
    
    /**
     * Decrypt string
     * 
     * @param encryptedBase64 Encrypted data (Base64 encoded)
     * @param key AES key
     * @return Decrypted plain text
     */
    public static String decryptString(String encryptedBase64, SecretKey key) {
        try {
            byte[] encrypted = java.util.Base64.getDecoder().decode(encryptedBase64);
            byte[] decrypted = decrypt(encrypted, key);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting string", e);
        }
    }
    
    /**
     * Get master key từ config hoặc environment
     * Trong production, nên lưu trong secure storage
     * 
     * @return Master key string
     */
    public static String getMasterKey() {
        // TODO: Lấy từ config file hoặc environment variable
        // Tạm thời dùng default (KHÔNG AN TOÀN cho production!)
        return "LibraryCardMasterKey2024!";
    }
}

