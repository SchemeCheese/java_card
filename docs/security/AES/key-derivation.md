# AES Encryption - Key Derivation

## 📋 Tổng quan

Cách tạo AES key từ master key và card ID sử dụng PBKDF2.

## 🔄 Luồng hoạt động

```
┌─────────────────┐
│  Master Key     │  "LibraryCardMasterKey2024!"
│  (String)       │
└────┬────────────┘
     │
     │  + Card ID (as salt)
     ▼
┌─────────────────┐
│  Card ID        │  "2021600001"
│  (String)       │
└────┬────────────┘
     │
     ▼
┌─────────────────────────────────┐
│  PBKDF2-HMAC-SHA256             │
│  - Input: Master Key            │
│  - Salt: Card ID (UTF-8 bytes)  │
│  - Iterations: 10000            │
│  - Output Length: 128 bits      │
└────┬────────────────────────────┘
     │
     ▼
┌─────────────────┐
│  AES Key        │  16 bytes (128 bits)
│  (SecretKey)    │
└─────────────────┘
```

## 💻 Code Flow

### AESUtility.java

```java
public static SecretKey deriveKey(String masterKey, String cardId) {
    try {
        // 1. Tạo PBEKeySpec
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            masterKey.toCharArray(),        // Master key
            cardId.getBytes("UTF-8"),      // Card ID as salt
            10000,                         // PBKDF2 iterations
            128                            // Key size (bits)
        );
        
        // 2. Tạo SecretKeyFactory với PBKDF2
        javax.crypto.SecretKeyFactory factory = 
            javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        
        // 3. Generate key
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        
        // 4. Tạo SecretKey từ key bytes
        return new SecretKeySpec(keyBytes, "AES");
        
    } catch (Exception e) {
        throw new RuntimeException("Error deriving AES key", e);
    }
}
```

## 🔐 PBKDF2 Configuration

### Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| Algorithm | PBKDF2WithHmacSHA256 | Key derivation function |
| Master Key | "LibraryCardMasterKey2024!" | Secret master key |
| Salt | Card ID (UTF-8 bytes) | Unique per card |
| Iterations | 10000 | Number of iterations |
| Key Length | 128 bits (16 bytes) | AES-128 key size |
| Hash Function | SHA-256 | HMAC hash function |

### Why PBKDF2?

1. **Resistant to brute force**
   - 10000 iterations làm chậm brute force attacks
   - Mỗi key derivation mất ~10-50ms

2. **Unique key per card**
   - Card ID làm salt → mỗi card có key riêng
   - Nếu một card bị compromise, các card khác vẫn an toàn

3. **Industry standard**
   - PBKDF2 là standard cho key derivation
   - Được recommend bởi NIST

## 📊 Key Derivation Process

### Step 1: Prepare Inputs

```java
// Master Key
String masterKey = "LibraryCardMasterKey2024!";
char[] masterKeyChars = masterKey.toCharArray();

// Card ID as Salt
String cardId = "2021600001";
byte[] salt = cardId.getBytes("UTF-8");
```

### Step 2: Create PBEKeySpec

```java
PBEKeySpec spec = new PBEKeySpec(
    masterKeyChars,  // Password (master key)
    salt,            // Salt (card ID)
    10000,           // Iterations
    128              // Key length (bits)
);
```

### Step 3: Derive Key

```java
SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
SecretKey secretKey = factory.generateSecret(spec);
byte[] keyBytes = secretKey.getEncoded(); // 16 bytes
```

### Step 4: Create AES Key

```java
SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");
```

## 🔑 Key Caching

### SimulatorService.java

```java
// Cache để tránh derive key nhiều lần
private javax.crypto.SecretKey cachedAESKey;
private String cachedCardIdForKey;

private javax.crypto.SecretKey getOrDeriveAESKey(String cardId) {
    // Nếu đã cache và card ID giống → dùng cache
    if (cachedAESKey != null && 
        cardId != null && 
        cardId.equals(cachedCardIdForKey)) {
        return cachedAESKey;
    }
    
    // Derive key mới
    String masterKey = AESUtility.getMasterKey();
    cachedAESKey = AESUtility.deriveKey(masterKey, cardId);
    cachedCardIdForKey = cardId;
    
    return cachedAESKey;
}
```

### Benefits
- **Performance**: Tránh derive key nhiều lần (PBKDF2 tốn ~10-50ms)
- **Consistency**: Đảm bảo dùng cùng key trong một session

### Cache Invalidation
- Khi disconnect card → clear cache
- Khi reset auth state → clear cache

## ⚠️ Lưu ý

1. **Master Key Security**
   - Hiện tại hardcode (không an toàn)
   - Nên lưu trong secure storage hoặc environment variable
   - Không commit vào git

2. **Card ID as Salt**
   - Card ID phải unique
   - Nếu Card ID trùng → cùng key → không an toàn
   - Card ID nên là primary key

3. **Iterations**
   - 10000 iterations là hợp lý (balance giữa security và performance)
   - Có thể tăng lên 20000 nếu cần security cao hơn
   - Không nên giảm xuống < 10000

4. **Key Caching**
   - Cache chỉ trong memory (session)
   - Không lưu key vào disk
   - Clear cache khi disconnect

## 📚 Xem thêm

- [AES Overview](./overview.md)
- [Encryption Flow](./encryption-flow.md)
- [Decryption Flow](./decryption-flow.md)
- [Storage Format](./storage-format.md)

