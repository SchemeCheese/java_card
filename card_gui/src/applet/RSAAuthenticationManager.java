package applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

/**
 * Module quản lý xác thực RSA
 * Tạo cặp khóa RSA và ký challenge để xác thực thẻ
 */
public class RSAAuthenticationManager {
    
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private boolean keyPairGenerated;
    
    // Lưu public exponent (thường là 65537 = 0x010001)
    private byte[] publicExponent;
    
    /**
     * Khởi tạo RSAAuthenticationManager
     */
    public RSAAuthenticationManager() {
        keyPairGenerated = false;
        publicExponent = new byte[3];
        publicExponent[0] = (byte)0x01;
        publicExponent[1] = (byte)0x00;
        publicExponent[2] = (byte)0x01;  // 65537
    }
    
    /**
     * Tạo cặp khóa RSA
     * Output: [MODULUS (128 bytes)] [PUBLIC_EXPONENT (3 bytes)]
     * 
     * @param apdu APDU command
     */
    public void generateKeyPair(APDU apdu) {
        if (keyPairGenerated) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }
        
        try {
            // Tạo cặp khóa RSA
            KeyPair rsaKeyPair = new KeyPair(KeyPair.ALG_RSA, AppletConstants.RSA_KEY_SIZE);
            rsaKeyPair.genKeyPair();
            
            privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
            publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
            
            keyPairGenerated = true;
            
            // Gửi Public Key về client
            byte[] buffer = apdu.getBuffer();
            short offset = 0;
            
            // Modulus (128 bytes)
            short modulusLen = publicKey.getModulus(buffer, offset);
            offset += modulusLen;
            
            // Public Exponent (3 bytes: 0x01 0x00 0x01 = 65537)
            Util.arrayCopy(publicExponent, (short)0, buffer, offset, (short)3);
            offset += 3;
            
            apdu.setOutgoingAndSend((short)0, offset);
            
        } catch (CryptoException e) {
            ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }
    }
    
    /**
     * Lấy Public Key (sau khi đã tạo)
     * Output: [MODULUS (128 bytes)] [PUBLIC_EXPONENT (3 bytes)]
     * 
     * @param apdu APDU command
     */
    public void getPublicKey(APDU apdu) {
        if (!keyPairGenerated) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        // Modulus
        short modulusLen = publicKey.getModulus(buffer, offset);
        offset += modulusLen;
        
        // Public Exponent (3 bytes)
        Util.arrayCopy(publicExponent, (short)0, buffer, offset, (short)3);
        offset += 3;
        
        apdu.setOutgoingAndSend((short)0, offset);
    }
    
    /**
     * Ký challenge với Private Key
     * Input: [CHALLENGE (16 bytes)]
     * Output: [SIGNATURE (128 bytes)]
     * 
     * @param apdu APDU command
     */
    public void signChallenge(APDU apdu) {
        try {
            // Step 0: Check keypair exists
            if (!keyPairGenerated) {
                ISOException.throwIt((short)0x6A00); // Keypair not generated
                return;
            }
            
            // Validate privateKey is not null
            if (privateKey == null) {
                ISOException.throwIt((short)0x6A0E); // Private key is null
                return;
            }
        } catch (ISOException e) {
            throw e;
        } catch (Throwable t) {
            ISOException.throwIt((short)0x6A20); // Exception in Step 0
            return;
        }
        
        byte[] buffer;
        try {
            buffer = apdu.getBuffer();
            if (buffer == null) {
                ISOException.throwIt((short)0x6A19); // Buffer is null
                return;
            }
        } catch (ISOException e) {
            throw e;
        } catch (Throwable t) {
            ISOException.throwIt((short)0x6A21); // Exception in getBuffer()
            return;
        }
        
        short incomingLength;
        try {
            // Step 1: Receive data first (JCardSim may require this before getIncomingLength)
            apdu.setIncomingAndReceive();
            
            // Step 2: Get incoming length AFTER setIncomingAndReceive
            incomingLength = apdu.getIncomingLength();
            
            // Step 3: Validate challenge length
            if (incomingLength != AppletConstants.RSA_CHALLENGE_SIZE) {
                ISOException.throwIt((short)0x6700); // Wrong length
                return;
            }
        } catch (ISOException e) {
            throw e;
        } catch (Exception e) {
            ISOException.throwIt((short)0x6A09); // Receive failed
            return;
        } catch (Throwable t) {
            ISOException.throwIt((short)0x6A22); // Exception in setIncomingAndReceive() or getIncomingLength()
            return;
        }
        
        short offset = ISO7816.OFFSET_CDATA;
        
        try {
            // Validate buffer and offsets before processing
            if (offset < 0 || offset >= buffer.length) {
                ISOException.throwIt((short)0x6A11); // Invalid offset
                return;
            }
            if (offset + AppletConstants.RSA_CHALLENGE_SIZE > buffer.length) {
                ISOException.throwIt((short)0x6A11); // Challenge exceeds buffer
                return;
            }
            if (buffer.length < AppletConstants.RSA_MODULUS_SIZE) {
                ISOException.throwIt((short)0x6A11); // Buffer too small for signature
                return;
            }
            
            // Step 4: Hash challenge with SHA-1
            MessageDigest sha1;
            try {
                sha1 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
            } catch (ISOException e) {
                throw e;
            } catch (CryptoException e) {
                ISOException.throwIt((short)0x6A0C); // SHA-1 not supported
                return;
            } catch (Throwable t) {
                ISOException.throwIt((short)0x6A24); // Exception in MessageDigest.getInstance()
                return;
            }
            
            byte[] challengeHash = new byte[20]; // SHA-1 produces 20 bytes
            try {
                sha1.doFinal(buffer, offset, AppletConstants.RSA_CHALLENGE_SIZE, challengeHash, (short)0);
            } catch (ISOException e) {
                throw e;
            } catch (CryptoException e) {
                ISOException.throwIt((short)0x6A0D); // Hash failed
                return;
            } catch (Throwable t) {
                ISOException.throwIt((short)0x6A25); // Exception in doFinal()
                return;
            }
            
            // Step 5: Pad with PKCS#1 v1.5 (EMSA-PKCS1-v1_5)
            // Format: 0x00 || 0x01 || PS || 0x00 || T
            // Where T = DigestInfo = SEQUENCE { AlgorithmIdentifier, digest }
            // For SHA-1: T = 30 21 30 09 06 05 2B 0E 03 02 1A 05 00 04 14 || hash
            // PS = 0xFF repeated (at least 8 bytes)
            // Total: 128 bytes (RSA_MODULUS_SIZE)
            
            byte[] padded = new byte[AppletConstants.RSA_MODULUS_SIZE];
            short pos = 0;
            
            padded[pos++] = (byte)0x00;
            padded[pos++] = (byte)0x01;
            
            // PS: Fill with 0xFF (at least 8 bytes)
            // PKCS#1 v1.5: k = 128, PS length = k - 3 - len(DigestInfo) - len(hash)
            // DigestInfo(SHA-1) length = 15, hash length = 20 => PS = 128 - 3 - 15 - 20 = 90 bytes
            short psLen = (short)(AppletConstants.RSA_MODULUS_SIZE - 3 - 15 - 20); // 90 bytes
            for (short i = 0; i < psLen; i++) {
                padded[pos++] = (byte)0xFF;
            }
            
            padded[pos++] = (byte)0x00;
            
            // DigestInfo for SHA-1: 30 21 30 09 06 05 2B 0E 03 02 1A 05 00 04 14
            byte[] digestInfo = {
                (byte)0x30, (byte)0x21, (byte)0x30, (byte)0x09, (byte)0x06, (byte)0x05,
                (byte)0x2B, (byte)0x0E, (byte)0x03, (byte)0x02, (byte)0x1A, (byte)0x05,
                (byte)0x00, (byte)0x04, (byte)0x14
            };
            Util.arrayCopyNonAtomic(digestInfo, (short)0, padded, pos, (short)digestInfo.length);
            pos += digestInfo.length;
            
            // Hash value
            Util.arrayCopyNonAtomic(challengeHash, (short)0, padded, pos, (short)challengeHash.length);
            pos += challengeHash.length;
            
            if (pos != AppletConstants.RSA_MODULUS_SIZE) {
                ISOException.throwIt((short)0x6A11); // Padding length mismatch
                return;
            }
            
            // Validate padded data: first byte must be 0x00 to ensure value < modulus
            if (padded[0] != (byte)0x00) {
                ISOException.throwIt((short)0x6A11); // Invalid padding format
                return;
            }
            
            // Step 6: Try Signature API first, fallback to Cipher NOPAD
            short signatureLen = 0;
            boolean signed = false;

            // Attempt 1: Signature API (ALG_RSA_SHA_PKCS1) with raw challenge
            try {
                Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
                sig.init(privateKey, Signature.MODE_SIGN);
                signatureLen = sig.sign(buffer, offset, AppletConstants.RSA_CHALLENGE_SIZE, buffer, (short)0);
                if (signatureLen != AppletConstants.RSA_MODULUS_SIZE) {
                    ISOException.throwIt((short)0x6A16);
                    return;
                }
                signed = true;
            } catch (CryptoException e) {
                // If algorithm not supported or other crypto issue, fall back
                short reason = e.getReason();
                if (reason == CryptoException.UNINITIALIZED_KEY) {
                    ISOException.throwIt((short)0x6A0E);
                    return;
                }
            } catch (Throwable t) {
                // fall back
            }

            // Attempt 2: ALG_RSA_NOPAD with manual padding, MODE_DECRYPT then MODE_ENCRYPT
            if (!signed) {
                Cipher rsaCipher;
                try {
                    rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
                } catch (CryptoException e) {
                    ISOException.throwIt((short)0x6A0C);
                    return;
                } catch (Throwable t) {
                    ISOException.throwIt((short)0x6A26);
                    return;
                }

                boolean initSuccess = false;
                try {
                    try {
                        rsaCipher.init(privateKey, Cipher.MODE_DECRYPT);
                        initSuccess = true;
                    } catch (CryptoException e) {
                        rsaCipher.init(privateKey, Cipher.MODE_ENCRYPT);
                        initSuccess = true;
                    }
                } catch (CryptoException e) {
                    short reason = e.getReason();
                    if (reason == CryptoException.UNINITIALIZED_KEY) {
                        ISOException.throwIt((short)0x6A0E);
                    } else {
                        ISOException.throwIt((short)0x6A0F);
                    }
                    return;
                } catch (Throwable t) {
                    ISOException.throwIt((short)0x6A27);
                    return;
                }

                if (!initSuccess) {
                    ISOException.throwIt((short)0x6A0F);
                    return;
                }

                try {
                    if (buffer.length < AppletConstants.RSA_MODULUS_SIZE) {
                        ISOException.throwIt((short)0x6A11);
                        return;
                    }

                    signatureLen = rsaCipher.doFinal(padded, (short)0, AppletConstants.RSA_MODULUS_SIZE, buffer, (short)0);
                    if (signatureLen != AppletConstants.RSA_MODULUS_SIZE) {
                        ISOException.throwIt((short)0x6A16);
                        return;
                    }
                } catch (CryptoException e) {
                    short reason = e.getReason();
                    if (reason == CryptoException.ILLEGAL_VALUE) {
                        ISOException.throwIt((short)0x6A2A);
                    } else if (reason == CryptoException.UNINITIALIZED_KEY) {
                        ISOException.throwIt((short)0x6A12);
                    } else {
                        ISOException.throwIt((short)0x6A13);
                    }
                    return;
                } catch (ArrayIndexOutOfBoundsException e) {
                    ISOException.throwIt((short)0x6A14);
                    return;
                } catch (NullPointerException e) {
                    ISOException.throwIt((short)0x6A17);
                    return;
                } catch (Exception e) {
                    ISOException.throwIt((short)0x6A14);
                    return;
                } catch (Throwable t) {
                    ISOException.throwIt((short)0x6A15);
                    return;
                }
            }

            if (!signed) {
                ISOException.throwIt((short)0x6A13); // Sign failed
                return;
            }
            
            // Step 7: Send response
            try {
                apdu.setOutgoingAndSend((short)0, signatureLen);
            } catch (ISOException e) {
                throw e;
            } catch (Exception e) {
                ISOException.throwIt((short)0x6A18); // Send failed
                return;
            } catch (Throwable t) {
                ISOException.throwIt((short)0x6A28); // Exception in setOutgoingAndSend()
                return;
            }
        } catch (ISOException e) {
            // Re-throw ISOException (already has proper SW code)
            throw e;
        } catch (Throwable t) {
            // Catch ANY exception/error that wasn't caught above
            ISOException.throwIt((short)0x6A1C); // Unexpected exception in signChallenge
            return;
        }
    }

    /**
     * Decrypt data using Private Key (for Master Key delivery)
     * Input: [ENCRYPTED_DATA (128 bytes for 1024-bit key)]
     * Output: [DECRYPTED_DATA (Variable length)]
     */
    public void decrypt(APDU apdu) {
        if (!keyPairGenerated || privateKey == null) {
            ISOException.throwIt((short)0x6A00); // Keypair not generated
        }

        byte[] buffer = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        
        // Input length validation (should match modulus size)
        if (len != AppletConstants.RSA_MODULUS_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        try {
            // Use RSA PKCS#1 padding for compatibility with standard crypto libraries (e.g., Node.js, Java)
            Cipher cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
            cipher.init(privateKey, Cipher.MODE_DECRYPT);
            
            // Decrypt directly in buffer
            // Input: buffer[OFFSET_CDATA] (128 bytes)
            // Output: buffer[0] (variable bytes)
            short decryptedLen = cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, len, buffer, (short)0);
            
            apdu.setOutgoingAndSend((short)0, decryptedLen);
            
        } catch (CryptoException e) {
             ISOException.throwIt((short)0x6A13); // Decrypt failed
        } catch (Exception e) {
             ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }
    }
    
    /**
     * Kiểm tra đã tạo khóa chưa
     */
    public boolean isKeyPairGenerated() {
        return keyPairGenerated;
    }
}
