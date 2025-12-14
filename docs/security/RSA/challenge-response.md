# RSA Authentication - Challenge-Response

## 📋 Tổng quan

Cơ chế challenge-response để xác thực thẻ chống giả mạo.

## 🔄 Luồng Challenge-Response

```
┌─────────┐
│  Client │
│  (GUI)  │
└────┬────┘
     │ 1. Generate random challenge (16 bytes)
     ▼
┌─────────────────────────┐
│  RSAUtility.java        │
│  generateChallenge()    │
│  - SecureRandom         │
│  - 16 bytes random      │
└────┬────────────────────┘
     │ 2. Gửi challenge lên card
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  RSAAuthenticationManager│
│  signChallenge()        │
│  - Hash challenge (SHA-1)│
│  - Pad (PKCS#1 v1.5)    │
│  - Sign với private key │
└────┬────────────────────┘
     │ 3. Nhận signature (128 bytes)
     ▼
┌─────────────────────────┐
│  Client (GUI)            │
│  RSAUtility.java        │
│  verifySignature()      │
│  - Decrypt signature    │
│  - Extract hash         │
│  - Compare với challenge│
└─────────────────────────┘
```

## 🔐 Challenge Generation

### RSAUtility.java

```java
public static byte[] generateChallenge() {
    SecureRandom random = new SecureRandom();
    byte[] challenge = new byte[16];
    random.nextBytes(challenge);
    return challenge;
}
```

### Properties
- **Size**: 16 bytes (128 bits)
- **Random**: SecureRandom (cryptographically secure)
- **Uniqueness**: Mỗi lần generate challenge khác nhau

## 📊 Signature Process trên Card

### Step 1: Hash Challenge

```java
// Hash challenge với SHA-1
MessageDigest sha1 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
byte[] challengeHash = new byte[20]; // SHA-1 produces 20 bytes
sha1.doFinal(challenge, 0, 16, challengeHash, 0);
```

### Step 2: PKCS#1 v1.5 Padding

```
┌─────────────────────────────────────────────────────────────┐
│  PKCS#1 v1.5 Padding Format (128 bytes)                    │
│                                                             │
│  ┌─────┬─────┬──────────────────┬─────┬──────────────┬────┐│
│  │ 0x00│ 0x01│ PS (0xFF...)     │ 0x00│ DigestInfo   │Hash││
│  │     │     │ (90 bytes)       │     │ (15 bytes)   │(20)││
│  └─────┴─────┴──────────────────┴─────┴──────────────┴────┘│
│                                                             │
│  Total: 1 + 1 + 90 + 1 + 15 + 20 = 128 bytes              │
└─────────────────────────────────────────────────────────────┘
```

### Step 3: DigestInfo for SHA-1

```
DigestInfo (15 bytes):
30 21 30 09 06 05 2B 0E 03 02 1A 05 00 04 14

Breakdown:
- 30 21: SEQUENCE, length 33
- 30 09: SEQUENCE, length 9 (AlgorithmIdentifier)
  - 06 05: OID, length 5
  - 2B 0E 03 02 1A: SHA-1 OID (1.3.14.3.2.26)
  - 05 00: NULL
- 04 14: OCTET STRING, length 20 (hash value)
```

### Step 4: RSA Sign

```java
// Method 1: Signature API (preferred)
Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
sig.init(privateKey, Signature.MODE_SIGN);
short signatureLen = sig.sign(challenge, 0, 16, buffer, 0);

// Method 2: Cipher with manual padding (fallback)
Cipher rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
rsaCipher.init(privateKey, Cipher.MODE_DECRYPT); // Sign = decrypt
short signatureLen = rsaCipher.doFinal(padded, 0, 128, buffer, 0);
```

## 🔍 Verification Process

### Step 1: Decrypt Signature

```java
// Decrypt signature với public key
Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
cipher.init(Cipher.DECRYPT_MODE, publicKey);
byte[] decrypted = cipher.doFinal(signature);
```

### Step 2: Verify Padding Format

```java
// Check PKCS#1 v1.5 format
if (decrypted[0] != 0x00 || decrypted[1] != 0x01) {
    return false; // Invalid padding header
}

// Find 0x00 separator after PS
int sepIndex = -1;
for (int i = 2; i < decrypted.length; i++) {
    if (decrypted[i] == 0x00) {
        sepIndex = i;
        break;
    } else if (decrypted[i] != (byte)0xFF) {
        return false; // Invalid PS padding
    }
}
```

### Step 3: Extract Hash

```java
// Extract DigestInfo and hash
int digestInfoStart = sepIndex + 1;
byte[] expectedDigestInfo = {
    0x30, 0x21, 0x30, 0x09, 0x06, 0x05,
    0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05,
    0x00, 0x04, 0x14
};

// Verify DigestInfo
for (int i = 0; i < expectedDigestInfo.length; i++) {
    if (decrypted[digestInfoStart + i] != expectedDigestInfo[i]) {
        return false;
    }
}

// Extract hash (20 bytes after DigestInfo)
int hashStart = digestInfoStart + expectedDigestInfo.length;
byte[] extractedHash = new byte[20];
System.arraycopy(decrypted, hashStart, extractedHash, 0, 20);
```

### Step 4: Compare Hashes

```java
// Hash challenge với SHA-1
MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
byte[] challengeHash = sha1.digest(challenge);

// Compare hashes
return Arrays.equals(challengeHash, extractedHash);
```

## 📊 Data Flow

### Complete Flow

```
Challenge (16 bytes)
    ↓
SHA-1 Hash → Hash (20 bytes)
    ↓
PKCS#1 v1.5 Padding → Padded (128 bytes)
    ↓
RSA Sign (private key) → Signature (128 bytes)
    ↓
RSA Decrypt (public key) → Padded (128 bytes)
    ↓
Extract Hash → Extracted Hash (20 bytes)
    ↓
Compare with Challenge Hash → Match?
```

## 🔐 Security Features

### 1. Random Challenge
- Mỗi lần authentication dùng challenge khác
- Chống replay attacks
- Challenge không được reuse

### 2. On-card Signing
- Private key không rời khỏi card
- Signature được tạo trên card
- Chống key extraction

### 3. PKCS#1 v1.5 Padding
- Standard padding scheme
- Chống padding oracle attacks
- DigestInfo đảm bảo hash algorithm

### 4. Hash Verification
- SHA-1 hash của challenge
- So sánh hash thay vì challenge trực tiếp
- Đảm bảo integrity

## ⚠️ Lưu ý

1. **Challenge phải random và unique**
   - Không reuse challenge
   - Mỗi lần authentication generate challenge mới

2. **Signature API vs Manual**
   - Signature API nhanh hơn và đơn giản hơn
   - Manual verification có fallback logic
   - Cả 2 methods đều được support

3. **Error Handling**
   - 6700: Challenge length sai hoặc keypair not ready
   - 6A00-6A2A: Various signing errors
   - Cần handle các error codes cụ thể

4. **SHA-1 vs SHA-256**
   - Hiện tại dùng SHA-1 (JavaCard limitation)
   - SHA-256 an toàn hơn nhưng không được support trên một số card
   - Có thể upgrade lên SHA-256 nếu card support

## 📚 Xem thêm

- [RSA Overview](./overview.md)
- [Key Generation](./key-generation.md)
- [Authentication Flow](./authentication-flow.md)

