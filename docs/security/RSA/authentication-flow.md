# RSA Authentication - Authentication Flow

## 📋 Tổng quan

Luồng xác thực thẻ sử dụng RSA challenge-response để đảm bảo thẻ là thật.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  Server │
│  (GUI)  │
└────┬────┘
     │ 1. Lấy Public Key từ server
     ▼
┌─────────────────────────┐
│  PinPage.java           │
│  - Get public key       │
│  - Generate challenge   │
└────┬────────────────────┘
     │ 2. Gửi challenge lên card
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  RSAAuthenticationManager│
│  - Sign challenge       │
│  - Trả về signature     │
└────┬────────────────────┘
     │ 3. Nhận signature
     ▼
┌─────────────────────────┐
│  RSAUtility.java        │
│  - Verify signature     │
│  - So sánh với challenge│
└────┬────────────────────┘
     │ 4. Kết quả: Authenticated/Not
     ▼
┌─────────────────────────┐
│  PinPage.java           │
│  - Hiển thị kết quả     │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - PinPage.java

```java
// Sau khi verify PIN thành công
if (pinVerified) {
    // Xác thực RSA
    boolean rsaAuthenticated = authenticateCardWithRSA(studentCode);
    
    if (rsaAuthenticated) {
        System.out.println("✓ RSA Authentication successful");
        // Cho phép truy cập
    } else {
        System.out.println("✗ RSA Authentication failed");
        // Có thể cảnh báo hoặc yêu cầu tạo lại keypair
    }
}
```

### 2. Service Layer - SimulatorService.java

```java
public boolean authenticateCardWithRSA(String publicKeyPEM) {
    try {
        if (!isConnected) {
            return false;
        }
        
        // 1. Verify keypair exists on card
        try {
            byte[] testKey = getRSAPublicKey();
            if (testKey == null || testKey.length == 0) {
                System.out.println("RSA keypair not found on card");
                return false;
            }
        } catch (Exception e) {
            System.out.println("RSA keypair check failed: " + e.getMessage());
            return false;
        }
        
        // 2. Get public key from card
        byte[] cardPublicKeyData = getRSAPublicKey();
        
        // 3. Extract modulus and exponent
        byte[] modulus = new byte[128];
        byte[] exponent = new byte[3];
        System.arraycopy(cardPublicKeyData, 0, modulus, 0, 128);
        System.arraycopy(cardPublicKeyData, 128, exponent, 0, 3);
        
        // 4. Convert to Java PublicKey
        PublicKey cardPublicKey = RSAUtility.convertToPublicKey(modulus, exponent);
        
        // 5. Generate random challenge (16 bytes)
        byte[] challenge = RSAUtility.generateChallenge();
        
        // 6. Sign challenge on card
        byte[] signature;
        try {
            signature = signRSAChallenge(challenge);
        } catch (Exception e) {
            // 6700 error means keypair may not be ready
            if (e.getMessage().contains("6700")) {
                System.out.println("RSA keypair not ready for signing");
                return false;
            }
            throw e;
        }
        
        // 7. Verify signature
        return RSAUtility.verifySignature(cardPublicKey, challenge, signature);
        
    } catch (Exception e) {
        System.out.println("RSA authentication error: " + e.getMessage());
        return false;
    }
}
```

### 3. Challenge Signing - SimulatorService.java

```java
public byte[] signRSAChallenge(byte[] challenge) throws Exception {
    if (!isConnected) {
        throw new Exception("Chưa kết nối với thẻ");
    }
    
    if (challenge.length != AppletConstants.RSA_CHALLENGE_SIZE) {
        throw new Exception("Challenge phải có 16 bytes");
    }
    
    // Build APDU command
    byte[] cmd = new byte[5 + challenge.length];
    cmd[0] = (byte)0x00;
    cmd[1] = AppletConstants.INS_RSA_SIGN_CHALLENGE; // 0xB2
    cmd[2] = (byte)0x00;
    cmd[3] = (byte)0x00;
    cmd[4] = (byte)challenge.length; // 16
    System.arraycopy(challenge, 0, cmd, 5, challenge.length);
    
    // Gửi command lên card
    byte[] resp = sendCommand(cmd);
    
    if (getSW(resp) != 0x9000) {
        // Handle error codes
        throw new Exception("Failed to sign challenge: " + 
            String.format("%04X", getSW(resp)));
    }
    
    // Response: Signature (128 bytes)
    byte[] signature = new byte[resp.length - 2];
    System.arraycopy(resp, 0, signature, 0, signature.length);
    
    return signature;
}
```

### 4. Applet Layer - RSAAuthenticationManager.java

```java
public void signChallenge(APDU apdu) {
    // 1. Kiểm tra keypair đã được tạo
    if (!keyPairGenerated || privateKey == null) {
        ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
    }
    
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    
    // 2. Validate challenge length (16 bytes)
    short incomingLength = apdu.getIncomingLength();
    if (incomingLength != AppletConstants.RSA_CHALLENGE_SIZE) {
        ISOException.throwIt((short)0x6700); // Wrong length
    }
    
    short offset = ISO7816.OFFSET_CDATA;
    
    // 3. Hash challenge với SHA-1
    MessageDigest sha1 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
    byte[] challengeHash = new byte[20]; // SHA-1 produces 20 bytes
    sha1.doFinal(buffer, offset, AppletConstants.RSA_CHALLENGE_SIZE, 
                 challengeHash, (short)0);
    
    // 4. Pad với PKCS#1 v1.5
    byte[] padded = new byte[128];
    // Format: 0x00 || 0x01 || PS (0xFF...) || 0x00 || DigestInfo || Hash
    // ... padding logic ...
    
    // 5. Sign với private key
    // Try Signature API first
    try {
        Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        sig.init(privateKey, Signature.MODE_SIGN);
        short signatureLen = sig.sign(buffer, offset, 
            AppletConstants.RSA_CHALLENGE_SIZE, buffer, (short)0);
        
        if (signatureLen == 128) {
            apdu.setOutgoingAndSend((short)0, signatureLen);
            return;
        }
    } catch (CryptoException e) {
        // Fallback to Cipher
    }
    
    // Fallback: Use Cipher with manual padding
    Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
    rsaCipher.init(privateKey, Cipher.MODE_DECRYPT); // Sign = decrypt with private key
    short signatureLen = rsaCipher.doFinal(padded, (short)0, 128, buffer, (short)0);
    
    // 6. Gửi signature (128 bytes)
    apdu.setOutgoingAndSend((short)0, signatureLen);
}
```

### 5. Verification - RSAUtility.java

```java
public static boolean verifySignature(PublicKey publicKey, byte[] challenge, byte[] signature) {
    try {
        // Method 1: Try Signature API (SHA1withRSA)
        try {
            java.security.Signature verifier = 
                java.security.Signature.getInstance("SHA1withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge);
            boolean ok = verifier.verify(signature);
            if (ok) {
                return true;
            }
        } catch (Exception e) {
            // Fallback to manual verification
        }
        
        // Method 2: Manual verification
        // 1. Hash challenge với SHA-1
        java.security.MessageDigest sha1 = 
            java.security.MessageDigest.getInstance("SHA-1");
        byte[] challengeHash = sha1.digest(challenge);
        
        // 2. Decrypt signature với public key
        javax.crypto.Cipher cipher = 
            javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, publicKey);
        byte[] decrypted = cipher.doFinal(signature);
        
        // 3. Verify PKCS#1 v1.5 padding
        // Format: 0x00 || 0x01 || PS (0xFF...) || 0x00 || DigestInfo || Hash
        
        // 4. Extract hash từ decrypted data
        // ... extract logic ...
        
        // 5. Compare hashes
        return java.util.Arrays.equals(challengeHash, extractedHash);
        
    } catch (Exception e) {
        System.out.println("RSA Verify Error: " + e.getMessage());
        return false;
    }
}
```

## 📊 Data Format

### SIGN_CHALLENGE Command

```
┌─────┬──────┬─────┬─────┬─────┬──────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ CHALLENGE (16)  │
│ 0x00│ 0xB2 │0x00 │0x00 │0x10 │ [random bytes]  │
└─────┴──────┴─────┴─────┴─────┴──────────────────┘
```

### SIGN_CHALLENGE Response

```
┌──────────────────────┬─────┬─────┐
│ SIGNATURE (128 bytes)│ SW1 │ SW2 │
│ [RSA signature]      │ 0x90│0x00 │
└──────────────────────┴─────┴─────┘
```

## 🔐 Signature Process

### Step 1: Hash Challenge

```
Challenge (16 bytes)
    ↓
SHA-1 Hash
    ↓
Hash (20 bytes)
```

### Step 2: PKCS#1 v1.5 Padding

```
┌─────┬─────┬──────────────────┬─────┬──────────────┬──────────┐
│ 0x00│ 0x01│ PS (0xFF...)     │ 0x00│ DigestInfo   │ Hash     │
│     │     │ (90 bytes)       │     │ (15 bytes)    │ (20 bytes)│
└─────┴─────┴──────────────────┴─────┴──────────────┴──────────┘
Total: 128 bytes (1024 bits)
```

### Step 3: RSA Sign

```
Padded Data (128 bytes)
    ↓
RSA Sign (decrypt with private key)
    ↓
Signature (128 bytes)
```

### Step 4: Verify

```
Signature (128 bytes)
    ↓
RSA Decrypt (with public key)
    ↓
Padded Data (128 bytes)
    ↓
Extract Hash
    ↓
Compare with Challenge Hash
```

## ⚠️ Lưu ý

1. **Challenge phải random**
   - Mỗi lần authentication dùng challenge khác
   - Chống replay attacks
   - Challenge được generate bởi client

2. **Keypair phải sẵn sàng**
   - Phải generate keypair trước khi authenticate
   - Nếu keypair chưa sẵn sàng, trả về error 6700

3. **Signature verification**
   - Có 2 methods: Signature API và manual
   - Signature API nhanh hơn
   - Manual verification có fallback logic

4. **Error Handling**
   - 6700: Wrong length hoặc keypair not ready
   - 6A00-6A2A: Various signing errors
   - Cần handle các error codes cụ thể

## 📚 Xem thêm

- [RSA Overview](./overview.md)
- [Key Generation](./key-generation.md)
- [Challenge-Response](./challenge-response.md)

