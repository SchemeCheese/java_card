# RSA Authentication - Luồng Chi Tiết với Giải Thích Hàm

## 🔑 RSA KEY - MODULUS VÀ EXPONENT LÀ GÌ?

### RSA Key Structure

RSA key bao gồm 2 phần chính:

```
┌─────────────────────────────────────────────────────────┐
│                    RSA PUBLIC KEY                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. MODULUS (n)                                         │
│     - Là một số nguyên lớn (1024 bits = 128 bytes)      │
│     - Được tạo từ 2 số nguyên tố lớn: p × q = n        │
│     - Ví dụ: n = 12345678901234567890... (128 bytes)   │
│     - Đây là "khóa công khai" - ai cũng biết được       │
│                                                         │
│  2. PUBLIC EXPONENT (e)                                 │
│     - Thường là 65537 (0x010001)                        │
│     - Được lưu dưới dạng 3 bytes: [0x01, 0x00, 0x01]   │
│     - Đây cũng là "khóa công khai"                      │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   RSA PRIVATE KEY                       │
│              (CHỈ TỒN TẠI TRÊN CARD)                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. PRIVATE EXPONENT (d)                                │
│     - Được tính từ: d = e^(-1) mod φ(n)                │
│     - φ(n) = (p-1) × (q-1)                             │
│     - Đây là "khóa bí mật" - KHÔNG BAO GIỜ rời khỏi card│
│                                                         │
│  2. PRIME FACTORS (p, q)                                │
│     - Hai số nguyên tố: p và q                          │
│     - n = p × q                                         │
│     - Cũng là "khóa bí mật"                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Ví dụ đơn giản:

```
Giả sử:
- p = 61 (số nguyên tố)
- q = 53 (số nguyên tố)
- n = p × q = 61 × 53 = 3233 (MODULUS)
- e = 65537 (PUBLIC EXPONENT - cố định)
- d = 2753 (PRIVATE EXPONENT - được tính toán)

Public Key = (n=3233, e=65537)  → Ai cũng biết
Private Key = (n=3233, d=2753)   → Chỉ card biết
```

### Trong hệ thống của chúng ta:

```
┌─────────────────────────────────────────────────────────┐
│  CARD (Applet)                                          │
│  ┌───────────────────────────────────────────────────┐ │
│  │ Private Key:                                      │ │
│  │   - Modulus (n): 128 bytes                       │ │
│  │   - Private Exponent (d): 128 bytes              │ │
│  │   - Prime p: 64 bytes                            │ │
│  │   - Prime q: 64 bytes                            │ │
│  │   ❌ KHÔNG BAO GIỜ rời khỏi card                  │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │ Public Key (có thể gửi ra ngoài):                │ │
│  │   - Modulus (n): 128 bytes                       │ │
│  │   - Public Exponent (e): 3 bytes [0x01,0x00,0x01]│ │
│  │   ✅ Được gửi về client và lưu trên server        │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 BƯỚC 1.1: KIỂM TRA KEYPAIR CÓ TRÊN CARD KHÔNG?

### Hàm: `getRSAPublicKey()`

**File:** `card_gui/src/service/SimulatorService.java`

**Mục đích:** Kiểm tra xem card đã có RSA keypair chưa bằng cách lấy public key từ card.

**Đầu vào:**
- Không có tham số (hàm không nhận input)

**Quá trình bên trong:**

```java
public byte[] getRSAPublicKey() throws Exception {
    // Bước 1: Kiểm tra đã kết nối với card chưa
    if (!isConnected) {
        throw new Exception("Chưa kết nối với thẻ");
    }
    
    // Bước 2: Tạo APDU command để yêu cầu public key
    byte[] cmd = new byte[5];
    cmd[0] = (byte)0x00;  // CLA (Class) - luôn là 0x00
    cmd[1] = AppletConstants.INS_RSA_GET_PUBLIC_KEY;  // INS = 0xB1
    cmd[2] = (byte)0x00;  // P1 (Parameter 1)
    cmd[3] = (byte)0x00;  // P2 (Parameter 2)
    cmd[4] = (byte)0x00;  // Lc (Length of command data) = 0 (không gửi data)
    
    // Format APDU: [CLA] [INS] [P1] [P2] [Lc]
    // Ví dụ: 00 B1 00 00 00
    
    // Bước 3: Gửi command lên card và nhận response
    byte[] resp = sendCommand(cmd);
    
    // Bước 4: Kiểm tra Status Word (SW)
    // SW = 0x9000 → Thành công
    // SW ≠ 0x9000 → Lỗi (ví dụ: 0x6985 = keypair chưa được tạo)
    if (getSW(resp) != 0x9000) {
        throw new Exception("Thẻ chưa có khóa RSA: " + String.format("%04X", getSW(resp)));
    }
    
    // Bước 5: Trích xuất dữ liệu từ response
    // Response format: [MODULUS (128 bytes)] [EXPONENT (3 bytes)] [SW1] [SW2]
    // Chúng ta cần bỏ 2 bytes cuối (SW1, SW2)
    byte[] publicKeyData = new byte[resp.length - 2];
    System.arraycopy(resp, 0, publicKeyData, 0, publicKeyData.length);
    
    // publicKeyData = [MODULUS (128 bytes)] [EXPONENT (3 bytes)]
    return publicKeyData;
}
```

**Đầu ra:**
- `byte[] publicKeyData`: Mảng byte chứa:
  - Bytes 0-127: Modulus (128 bytes)
  - Bytes 128-130: Public Exponent (3 bytes)
- **Tổng cộng: 131 bytes**

**Ví dụ response:**
```
Response từ card:
┌─────────────────────────────────────────────────────────┐
│ Bytes 0-127:   Modulus (128 bytes)                      │
│                [A1 B2 C3 D4 E5 F6 ... 128 bytes ...]    │
├─────────────────────────────────────────────────────────┤
│ Bytes 128-130: Exponent (3 bytes)                       │
│                [01 00 01] = 65537                       │
├─────────────────────────────────────────────────────────┤
│ Bytes 131-132: Status Word                              │
│                [90 00] = Success                       │
└─────────────────────────────────────────────────────────┘
```

**Lỗi có thể xảy ra:**
- `0x6985` (SW_CONDITIONS_NOT_SATISFIED): Keypair chưa được tạo
- `0x6F00` (SW_UNKNOWN): Lỗi không xác định

---

## 📋 BƯỚC 1.2: TRÍCH XUẤT MODULUS VÀ EXPONENT

### Hàm: Trích xuất từ `publicKeyData`

**File:** `card_gui/src/service/SimulatorService.java` (trong `authenticateCardWithRSA`)

**Mục đích:** Tách `publicKeyData` thành 2 phần riêng biệt: Modulus và Exponent.

**Đầu vào:**
- `byte[] cardPublicKeyData`: Mảng 131 bytes từ bước 1.1
  - Bytes 0-127: Modulus
  - Bytes 128-130: Exponent

**Quá trình:**

```java
// Bước 1: Tạo mảng để chứa Modulus (128 bytes)
byte[] modulus = new byte[AppletConstants.RSA_MODULUS_SIZE];  // 128 bytes

// Bước 2: Tạo mảng để chứa Exponent (3 bytes)
byte[] exponent = new byte[3];

// Bước 3: Copy Modulus từ publicKeyData
// Copy từ vị trí 0, độ dài 128 bytes
System.arraycopy(cardPublicKeyData, 0, modulus, 0, modulus.length);
// modulus = cardPublicKeyData[0..127]

// Bước 4: Copy Exponent từ publicKeyData
// Copy từ vị trí 128 (sau Modulus), độ dài 3 bytes
System.arraycopy(cardPublicKeyData, modulus.length, exponent, 0, exponent.length);
// exponent = cardPublicKeyData[128..130]
```

**Đầu ra:**
- `byte[] modulus`: 128 bytes - Modulus của RSA key
- `byte[] exponent`: 3 bytes - Public Exponent (thường là [0x01, 0x00, 0x01] = 65537)

**Ví dụ:**
```
Input: cardPublicKeyData (131 bytes)
┌─────────────────────────────────────────────────────────┐
│ [0x12, 0x34, 0x56, ... 128 bytes Modulus ...]          │
│ [0x01, 0x00, 0x01]                                      │
└─────────────────────────────────────────────────────────┘

Output:
modulus = [0x12, 0x34, 0x56, ... 128 bytes ...]
exponent = [0x01, 0x00, 0x01]
```

---

## 📋 BƯỚC 1.3: CHUYỂN ĐỔI MODULUS + EXPONENT → JAVA PUBLICKEY

### Hàm: `RSAUtility.convertToPublicKey(byte[] modulus, byte[] exponent)`

**File:** `card_gui/src/utils/RSAUtility.java`

**Mục đích:** Chuyển đổi Modulus và Exponent từ định dạng byte array sang Java `PublicKey` object để có thể dùng verify signature.

**Đầu vào:**
- `byte[] modulus`: 128 bytes - Modulus từ card
- `byte[] exponent`: 3 bytes - Public Exponent từ card

**Quá trình bên trong:**

```java
public static PublicKey convertToPublicKey(byte[] modulus, byte[] exponent) {
    try {
        // Bước 1: Chuyển đổi Modulus từ byte[] → BigInteger
        // BigInteger(1, modulus):
        //   - 1 = signum (dương)
        //   - modulus = mảng byte cần chuyển đổi
        BigInteger n = new BigInteger(1, modulus);
        // n = số nguyên lớn đại diện cho Modulus
        // Ví dụ: n = 12345678901234567890... (số rất lớn)
        
        // Bước 2: Chuyển đổi Exponent từ byte[] → BigInteger
        BigInteger e = new BigInteger(1, exponent);
        // e = 65537 (từ [0x01, 0x00, 0x01])
        
        // Bước 3: Tạo RSAPublicKeySpec
        // RSAPublicKeySpec là một "specification" mô tả RSA public key
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
        // keySpec chứa: (modulus = n, exponent = e)
        
        // Bước 4: Tạo KeyFactory để "chế tạo" PublicKey
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // KeyFactory là "nhà máy" để tạo key từ specification
        
        // Bước 5: Tạo PublicKey object từ specification
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        // publicKey là Java PublicKey object, có thể dùng để verify signature
        
        return publicKey;
        
    } catch (Exception e) {
        throw new RuntimeException("Error converting RSA key", e);
    }
}
```

**Đầu ra:**
- `PublicKey publicKey`: Java PublicKey object
  - Có thể dùng với `Signature.verify()` hoặc `Cipher.decrypt()`
  - Chứa thông tin: Modulus (n) và Exponent (e)

**Ví dụ:**
```
Input:
  modulus = [0x12, 0x34, 0x56, ... 128 bytes]
  exponent = [0x01, 0x00, 0x01]

Quá trình:
  1. n = BigInteger(1, modulus) 
     → n = 12345678901234567890... (số nguyên lớn)
  2. e = BigInteger(1, exponent)
     → e = 65537
  3. keySpec = RSAPublicKeySpec(n=..., e=65537)
  4. publicKey = KeyFactory.generatePublic(keySpec)

Output:
  publicKey = Java PublicKey object
  - publicKey.getModulus() → trả về n
  - publicKey.getPublicExponent() → trả về e
```

---

## 📋 BƯỚC 1.4: TẠO CHALLENGE NGẪU NHIÊN

### Hàm: `RSAUtility.generateChallenge()`

**File:** `card_gui/src/utils/RSAUtility.java`

**Mục đích:** Tạo một số ngẫu nhiên 16 bytes để gửi cho card ký (challenge).

**Đầu vào:**
- Không có tham số

**Quá trình bên trong:**

```java
public static byte[] generateChallenge() {
    // Bước 1: Tạo SecureRandom object
    // SecureRandom là bộ tạo số ngẫu nhiên "an toàn" (cryptographically secure)
    // Khác với Random thông thường, SecureRandom dùng thuật toán mạnh hơn
    SecureRandom random = new SecureRandom();
    
    // Bước 2: Tạo mảng byte 16 bytes
    byte[] challenge = new byte[16];
    
    // Bước 3: Fill mảng với số ngẫu nhiên
    random.nextBytes(challenge);
    // challenge = [random byte, random byte, ... 16 bytes random]
    
    return challenge;
}
```

**Đầu ra:**
- `byte[] challenge`: 16 bytes ngẫu nhiên
  - Mỗi lần gọi hàm sẽ tạo challenge khác nhau
  - Ví dụ: `[0x3A, 0x7F, 0x2B, 0x91, ... 16 bytes ...]`

**Ví dụ:**
```
Lần 1: challenge = [0x3A, 0x7F, 0x2B, 0x91, 0x45, 0xCD, ...]
Lần 2: challenge = [0x8E, 0x12, 0x67, 0xAB, 0x34, 0xEF, ...]
Lần 3: challenge = [0x1F, 0x9A, 0xBC, 0x23, 0x78, 0x56, ...]
```

**Tại sao cần random?**
- Chống replay attacks (tấn công lặp lại)
- Mỗi lần authentication dùng challenge khác nhau
- Kẻ tấn công không thể dùng lại signature cũ

---

## 📋 BƯỚC 2: GỬI CHALLENGE LÊN CARD ĐỂ KÝ

### Hàm: `signRSAChallenge(byte[] challenge)`

**File:** `card_gui/src/service/SimulatorService.java`

**Mục đích:** Gửi challenge lên card, card sẽ ký challenge bằng Private Key và trả về signature.

**Đầu vào:**
- `byte[] challenge`: 16 bytes - Challenge ngẫu nhiên từ bước 1.4

**Quá trình bên trong:**

```java
public byte[] signRSAChallenge(byte[] challenge) throws Exception {
    // Bước 1: Kiểm tra đã kết nối
    if (!isConnected) {
        throw new Exception("Chưa kết nối với thẻ");
    }
    
    // Bước 2: Validate challenge length
    if (challenge.length != AppletConstants.RSA_CHALLENGE_SIZE) {
        throw new Exception("Challenge phải có độ dài 16 bytes");
    }
    
    // Bước 3: Tạo APDU command
    // Format: [CLA] [INS] [P1] [P2] [Lc] [Data...]
    byte[] cmd = new byte[5 + challenge.length];
    
    cmd[0] = (byte)0x00;  // CLA
    cmd[1] = AppletConstants.INS_RSA_SIGN_CHALLENGE;  // INS = 0xB2
    cmd[2] = (byte)0x00;  // P1
    cmd[3] = (byte)0x00;  // P2
    cmd[4] = (byte)challenge.length;  // Lc = 16 (độ dài challenge)
    
    // Bước 4: Copy challenge vào command
    System.arraycopy(challenge, 0, cmd, 5, challenge.length);
    // cmd = [0x00, 0xB2, 0x00, 0x00, 0x10, challenge[0], challenge[1], ...]
    
    // Bước 5: Gửi command lên card
    byte[] resp = sendCommand(cmd);
    
    // Bước 6: Kiểm tra Status Word
    if (getSW(resp) != 0x9000) {
        throw new Exception("Failed to sign challenge: " + String.format("%04X", getSW(resp)));
    }
    
    // Bước 7: Trích xuất signature từ response
    // Response format: [SIGNATURE (128 bytes)] [SW1] [SW2]
    byte[] signature = new byte[resp.length - 2];
    System.arraycopy(resp, 0, signature, 0, signature.length);
    
    return signature;  // 128 bytes
}
```

**Đầu ra:**
- `byte[] signature`: 128 bytes - Signature từ card
  - Được tạo bằng cách: Hash challenge → Pad → Sign với Private Key

**Ví dụ APDU Command:**
```
Command gửi lên card:
┌─────┬──────┬─────┬─────┬─────┬──────────────────────────┐
│ CLA │ INS  │ P1  │ P2  │ Lc  │ CHALLENGE (16 bytes)     │
│0x00 │ 0xB2 │0x00 │0x00 │0x10 │ [3A 7F 2B 91 ...]       │
└─────┴──────┴─────┴─────┴─────┴──────────────────────────┘
```

**Response từ card:**
```
┌──────────────────────────────────────┬─────┬─────┐
│ SIGNATURE (128 bytes)                │ SW1 │ SW2 │
│ [A1 B2 C3 D4 ... 128 bytes ...]      │ 0x90│0x00 │
└──────────────────────────────────────┴─────┴─────┘
```

---

## 📋 BƯỚC 3: VERIFY SIGNATURE

### Hàm: `RSAUtility.verifySignature(PublicKey publicKey, byte[] challenge, byte[] signature)`

**File:** `card_gui/src/utils/RSAUtility.java`

**Mục đích:** Xác minh signature có đúng không bằng cách:
1. Decrypt signature với Public Key
2. Extract hash từ signature
3. Hash challenge với SHA-1
4. So sánh 2 hash

**Đầu vào:**
- `PublicKey publicKey`: Public Key từ bước 1.3
- `byte[] challenge`: 16 bytes - Challenge đã gửi cho card
- `byte[] signature`: 128 bytes - Signature nhận từ card

**Quá trình bên trong (Method 1 - Signature API):**

```java
public static boolean verifySignature(PublicKey publicKey, byte[] challenge, byte[] signature) {
    try {
        // METHOD 1: Dùng Signature API (nhanh và đơn giản)
        try {
            // Bước 1: Tạo Signature verifier với algorithm SHA1withRSA
            java.security.Signature verifier = 
                java.security.Signature.getInstance("SHA1withRSA");
            // SHA1withRSA = Hash challenge với SHA-1, sau đó verify với RSA
            
            // Bước 2: Khởi tạo verifier với Public Key
            verifier.initVerify(publicKey);
            // verifier biết dùng publicKey nào để verify
            
            // Bước 3: Update challenge vào verifier
            verifier.update(challenge);
            // verifier hash challenge với SHA-1
            
            // Bước 4: Verify signature
            boolean ok = verifier.verify(signature);
            // verifier tự động:
            //   - Decrypt signature với public key
            //   - Extract hash từ decrypted data
            //   - Hash challenge với SHA-1
            //   - So sánh 2 hash
            //   - Trả về true nếu match, false nếu không match
            
            if (ok) {
                return true;  // Signature hợp lệ
            }
        } catch (Exception e) {
            // Nếu Method 1 fail, dùng Method 2 (manual)
        }
        
        // METHOD 2: Manual verification (fallback)
        // ... (xem code chi tiết trong file)
        
    } catch (Exception e) {
        return false;
    }
}
```

**Quá trình bên trong (Method 2 - Manual):**

```java
// METHOD 2: Manual verification (chi tiết từng bước)

// Bước 1: Hash challenge với SHA-1
java.security.MessageDigest sha1 = 
    java.security.MessageDigest.getInstance("SHA-1");
byte[] challengeHash = sha1.digest(challenge);
// challengeHash = 20 bytes (SHA-1 output)

// Bước 2: Decrypt signature với Public Key
javax.crypto.Cipher cipher = 
    javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
cipher.init(javax.crypto.Cipher.DECRYPT_MODE, publicKey);
byte[] decrypted = cipher.doFinal(signature);
// decrypted = 128 bytes (PKCS#1 padded data)

// Bước 3: Verify PKCS#1 v1.5 padding format
// Format: 0x00 || 0x01 || PS (0xFF...) || 0x00 || DigestInfo || Hash
if (decrypted[0] != 0x00 || decrypted[1] != 0x01) {
    return false;  // Invalid padding
}

// Bước 4: Find separator (0x00 sau PS)
int sepIndex = -1;
for (int i = 2; i < decrypted.length; i++) {
    if (decrypted[i] == 0x00) {
        sepIndex = i;
        break;
    }
}

// Bước 5: Extract DigestInfo và Hash
int digestInfoStart = sepIndex + 1;
// Verify DigestInfo (15 bytes cho SHA-1)
byte[] expectedDigestInfo = {
    0x30, 0x21, 0x30, 0x09, 0x06, 0x05,
    0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05,
    0x00, 0x04, 0x14
};
// ... verify DigestInfo ...

// Bước 6: Extract hash (20 bytes sau DigestInfo)
int hashStart = digestInfoStart + expectedDigestInfo.length;
byte[] extractedHash = new byte[20];
System.arraycopy(decrypted, hashStart, extractedHash, 0, 20);

// Bước 7: Compare hashes
return java.util.Arrays.equals(challengeHash, extractedHash);
```

**Đầu ra:**
- `boolean`: 
  - `true` → Signature hợp lệ, challenge được ký đúng
  - `false` → Signature không hợp lệ, có thể thẻ giả hoặc lỗi

**Ví dụ:**
```
Input:
  publicKey = PublicKey object (từ bước 1.3)
  challenge = [0x3A, 0x7F, 0x2B, ... 16 bytes]
  signature = [0xA1, 0xB2, 0xC3, ... 128 bytes]

Quá trình:
  1. Hash challenge: SHA-1(challenge) → hash1 (20 bytes)
  2. Decrypt signature: RSA_decrypt(signature, publicKey) → padded (128 bytes)
  3. Extract hash từ padded: hash2 (20 bytes)
  4. Compare: hash1 == hash2?

Output:
  true → Signature hợp lệ ✅
  false → Signature không hợp lệ ❌
```

---

## 📊 TÓM TẮT LUỒNG HOÀN CHỈNH

```
BƯỚC 1.1: getRSAPublicKey()
  Input:  Không
  Output: byte[131] = [Modulus(128)] + [Exponent(3)]
  Mục đích: Kiểm tra keypair có trên card không

BƯỚC 1.2: Trích xuất Modulus và Exponent
  Input:  byte[131] từ bước 1.1
  Output: modulus[128], exponent[3]
  Mục đích: Tách thành 2 phần riêng biệt

BƯỚC 1.3: convertToPublicKey(modulus, exponent)
  Input:  modulus[128], exponent[3]
  Output: PublicKey object
  Mục đích: Chuyển đổi sang Java PublicKey để verify

BƯỚC 1.4: generateChallenge()
  Input:  Không
  Output: challenge[16] (random)
  Mục đích: Tạo challenge ngẫu nhiên

BƯỚC 2: signRSAChallenge(challenge)
  Input:  challenge[16]
  Output: signature[128]
  Mục đích: Card ký challenge, trả về signature

BƯỚC 3: verifySignature(publicKey, challenge, signature)
  Input:  publicKey, challenge[16], signature[128]
  Output: boolean (true/false)
  Mục đích: Verify signature có đúng không
```

---

## 🔍 GIẢI THÍCH THÊM VỀ MODULUS VÀ EXPONENT

### Modulus (n) là gì?

```
Modulus = p × q

Trong đó:
- p và q là 2 số nguyên tố lớn (mỗi số ~512 bits)
- n = p × q = 1024 bits = 128 bytes

Ví dụ (đơn giản):
  p = 61
  q = 53
  n = 61 × 53 = 3233

Trong thực tế:
  p = số nguyên tố rất lớn (~512 bits)
  q = số nguyên tố rất lớn (~512 bits)
  n = p × q = số rất rất lớn (1024 bits)
```

### Public Exponent (e) là gì?

```
Public Exponent thường là 65537 (0x010001)

Tại sao 65537?
- Là số nguyên tố
- Chỉ có 2 bit 1 (0x010001 = 10000000000000001 binary)
- Tính toán nhanh (exponentiation nhanh)
- An toàn

Lưu trữ: 3 bytes [0x01, 0x00, 0x01]
```

### Private Exponent (d) là gì?

```
Private Exponent được tính từ:
  d = e^(-1) mod φ(n)

Trong đó:
  φ(n) = (p-1) × (q-1)
  e = 65537 (public exponent)
  d = private exponent (bí mật, chỉ card biết)

Ví dụ (đơn giản):
  p = 61, q = 53
  n = 3233
  φ(n) = (61-1) × (53-1) = 60 × 52 = 3120
  e = 65537
  d = 65537^(-1) mod 3120 = 2753
```

### RSA Encryption/Signing:

```
Encrypt (với Public Key):
  ciphertext = plaintext^e mod n

Decrypt (với Private Key):
  plaintext = ciphertext^d mod n

Sign (với Private Key):
  signature = hash^d mod n

Verify (với Public Key):
  hash' = signature^e mod n
  So sánh: hash' == hash?
```

---

## 📚 Xem thêm

- [RSA Overview](./overview.md)
- [Key Generation](./key-generation.md)
- [Authentication Flow](./authentication-flow.md)
- [Challenge-Response](./challenge-response.md)

