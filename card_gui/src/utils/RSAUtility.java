package utils;

import java.security.*;
import java.security.spec.*;
import java.math.BigInteger;

/**
 * Utility class để xử lý RSA authentication
 * Chuyển đổi RSA key format và verify signature
 */
public class RSAUtility {
    
    /**
     * Convert RSA public key từ JavaCard format (modulus + exponent) sang Java PublicKey
     * 
     * @param modulus Modulus từ JavaCard (128 bytes cho 1024-bit RSA)
     * @param exponent Public exponent (thường là 3 bytes: 0x01 0x00 0x01)
     * @return Java PublicKey object
     */
    public static PublicKey convertToPublicKey(byte[] modulus, byte[] exponent) {
        try {
            BigInteger n = new BigInteger(1, modulus);  // Modulus
            BigInteger e = new BigInteger(1, exponent);  // Public exponent
            
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error converting RSA key", e);
        }
    }
    
    /**
     * Convert RSA public key sang PEM format
     * 
     * @param modulus Modulus từ JavaCard
     * @param exponent Public exponent từ JavaCard
     * @return PEM format string
     */
    public static String convertToPEM(byte[] modulus, byte[] exponent) {
        try {
            PublicKey publicKey = convertToPublicKey(modulus, exponent);
            return java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Error converting to PEM", e);
        }
    }
    
    /**
     * Verify RSA signature
     * 
     * @param publicKey Public key để verify
     * @param challenge Challenge đã gửi cho thẻ
     * @param signature Signature từ thẻ
     * @return true nếu signature hợp lệ
     */
    public static boolean verifySignature(PublicKey publicKey, byte[] challenge, byte[] signature) {
        try {
            // First try Signature API (SHA1withRSA) to match card's Signature path
            try {
                java.security.Signature verifier = java.security.Signature.getInstance("SHA1withRSA");
                verifier.initVerify(publicKey);
                verifier.update(challenge);
                boolean ok = verifier.verify(signature);
                if (ok) {
                    System.out.println("[RSA Verify] Signature verification SUCCESS (Signature API)");
                    return true;
                } else {
                    System.out.println("[RSA Verify] Signature API verification failed, fallback to manual");
                }
            } catch (Exception e) {
                // Fallback to manual
            }

            // Fallback: manual decrypt + padding check
            // Card signs: SHA-1 hash challenge -> PKCS#1 v1.5 padding -> RSA encrypt (sign)
            // So we verify by: RSA decrypt signature -> Unpad -> Extract hash -> Compare with SHA-1 hash of challenge
            
            // Step 1: Hash challenge with SHA-1 (same as card)
            java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
            byte[] challengeHash = sha1.digest(challenge);
            
            // Step 2: Decrypt signature with public key (RSA decrypt)
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, publicKey);
            byte[] decrypted = cipher.doFinal(signature);
            
            // Step 3: Verify PKCS#1 v1.5 padding format
            // Format: 0x00 || 0x01 || PS (0xFF...) || 0x00 || DigestInfo || Hash
            if (decrypted.length < 35) { // Minimum: 1 + 1 + 8 + 1 + 15 + 20 = 45, but we have 128
                System.out.println("[RSA Verify] Decrypted data too short: " + decrypted.length);
                return false;
            }
            
            if (decrypted[0] != 0x00 || decrypted[1] != 0x01) {
                System.out.println("[RSA Verify] Invalid PKCS#1 padding header");
                return false;
            }
            
            // Find 0x00 separator after PS
            int sepIndex = -1;
            for (int i = 2; i < decrypted.length; i++) {
                if (decrypted[i] == 0x00) {
                    sepIndex = i;
                    break;
                } else if (decrypted[i] != (byte)0xFF) {
                    System.out.println("[RSA Verify] Invalid PS padding");
                    return false;
                }
            }
            
            if (sepIndex == -1 || sepIndex < 10) { // At least 8 bytes of PS
                System.out.println("[RSA Verify] PS padding too short or separator not found");
                return false;
            }
            
            // Step 4: Extract DigestInfo and hash
            int digestInfoStart = sepIndex + 1;
            if (digestInfoStart + 15 + 20 > decrypted.length) {
                System.out.println("[RSA Verify] DigestInfo + hash exceeds decrypted data");
                return false;
            }
            
            // DigestInfo for SHA-1: 30 21 30 09 06 05 2B 0E 03 02 1A 05 00 04 14 (15 bytes)
            byte[] expectedDigestInfo = {
                0x30, 0x21, 0x30, 0x09, 0x06, 0x05,
                0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05,
                0x00, 0x04, 0x14
            };
            
            for (int i = 0; i < expectedDigestInfo.length; i++) {
                if (decrypted[digestInfoStart + i] != expectedDigestInfo[i]) {
                    System.out.println("[RSA Verify] Invalid DigestInfo at index " + i);
                    return false;
                }
            }
            
            // Extract hash (20 bytes after DigestInfo)
            int hashStart = digestInfoStart + expectedDigestInfo.length;
            byte[] extractedHash = new byte[20];
            System.arraycopy(decrypted, hashStart, extractedHash, 0, 20);
            
            // Step 5: Compare hashes
            boolean result = java.util.Arrays.equals(challengeHash, extractedHash);
            
            if (!result) {
                System.out.println("[RSA Verify] Signature verification FAILED");
                System.out.println("  Challenge: " + bytesToHex(challenge));
                System.out.println("  Challenge Hash (SHA-1): " + bytesToHex(challengeHash));
                System.out.println("  Extracted Hash: " + bytesToHex(extractedHash));
                System.out.println("  Signature: " + bytesToHex(signature));
            } else {
                System.out.println("[RSA Verify] Signature verification SUCCESS (manual)");
            }
            
            return result;
        } catch (Exception e) {
            System.out.println("[RSA Verify] Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    
    /**
     * Generate random challenge (16 bytes)
     * 
     * @return Random challenge bytes
     */
    public static byte[] generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[16];
        random.nextBytes(challenge);
        return challenge;
    }
    
    /**
     * Convert PEM string to PublicKey
     * Handles both full PEM format (with headers) and Base64-only format
     * 
     * @param pemString PEM format string or Base64 string
     * @return PublicKey object
     */
    public static PublicKey pemToPublicKey(String pemString) {
        try {
            // Remove PEM headers/footers if present
            String base64Key = pemString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // Remove all whitespace (newlines, spaces)
            
            byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing PEM key: " + e.getMessage(), e);
        }
    }
}
