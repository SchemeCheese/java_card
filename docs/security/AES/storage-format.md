# AES Encryption - Storage Format

## 📋 Tổng quan

Format lưu trữ dữ liệu đã mã hóa trên card.

## 📊 Data Structure trên Card

### CardInfoManager.java

```java
// Card ID: Plaintext (10 bytes)
private byte[] cardId;

// Name: Có thể đã mã hóa (max 50 bytes)
private byte[] holderName;
private byte holderNameLength;

// Expiry Date: Có thể đã mã hóa (8 bytes)
private byte[] expiryDate;
```

### Storage Format

```
┌─────────────────────────────────────────────────────────────┐
│  Card Storage Layout                                        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Card ID (10 bytes) - PLAINTEXT                      │  │
│  │  "2021600001"                                        │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Name Length (1 byte)                               │  │
│  │  0x32 (50 bytes)                                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Name (max 50 bytes) - ENCRYPTED                    │  │
│  │  [IV (16)] + [Encrypted Data]                       │  │
│  │  ⚠️ Có thể bị truncate nếu > 50 bytes              │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Expiry Date (8 bytes) - ENCRYPTED                 │  │
│  │  [IV (16)] + [Encrypted Data]                      │  │
│  │  ⚠️ Không đủ chỗ cho IV (cần 16 bytes)            │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🔐 Encrypted Data Format

### Standard Format

```
┌──────────────────┬──────────────────────┐
│ IV (16 bytes)    │ Encrypted Data       │
│ [random bytes]   │ [ciphertext]         │
└──────────────────┴──────────────────────┘
Total: 16 + encrypted_length bytes
```

### Example: Encrypt "Nguyễn Văn A"

```
Plaintext: "Nguyễn Văn A" (UTF-8: 13 bytes)

After PKCS5Padding: 16 bytes (padded to block size)

Encrypted: 
  - IV: 16 bytes (random)
  - Encrypted: 16 bytes
  Total: 32 bytes ✅ Fits in 50 bytes buffer
```

### Example: Encrypt Long Name

```
Plaintext: "Nguyễn Văn A B C D E F G H I J K L M N O P" (50+ bytes)

After PKCS5Padding: 64 bytes (multiple of 16)

Encrypted:
  - IV: 16 bytes
  - Encrypted: 64 bytes
  Total: 80 bytes ❌ Exceeds 50 bytes buffer

Truncated to 50 bytes:
  - IV: 16 bytes
  - Encrypted: 34 bytes (truncated)
  ⚠️ Mất một phần encrypted data → Decrypt fail
```

## 📏 Buffer Size Constraints

### Card ID
- **Size**: 10 bytes (fixed)
- **Format**: Plaintext
- **Content**: Student ID (e.g., "2021600001")

### Name
- **Size**: Max 50 bytes (variable)
- **Format**: Encrypted (if flag = 1)
- **Content**: Holder name
- **Issue**: 
  - Encrypted format = IV (16) + Encrypted data
  - Nếu encrypted data > 34 bytes → bị truncate
  - Truncate → mất IV hoặc data → decrypt fail

### Expiry Date
- **Size**: 8 bytes (fixed)
- **Format**: Encrypted (if flag = 1)
- **Content**: DDMMYYYY format
- **Issue**:
  - Encrypted format cần tối thiểu 16 bytes (IV)
  - Buffer chỉ có 8 bytes → **Không thể encrypt đúng**

## ⚠️ Limitations và Workarounds

### 1. Name Truncation

**Vấn đề:**
- Name dài → encrypted data > 34 bytes → bị truncate

**Giải pháp:**
```java
// Option 1: Chỉ encrypt nếu đủ chỗ
if (encryptedName.length <= 34) {
    // Encrypt và lưu
} else {
    // Lưu plaintext (fallback)
}

// Option 2: Tăng buffer size (nếu có thể)
// NAME_MAX_LENGTH = 100 bytes (thay vì 50)
```

### 2. Expiry Date

**Vấn đề:**
- Buffer chỉ có 8 bytes
- Encrypted format cần 16+ bytes

**Giải pháp:**
```java
// Option 1: Lưu plaintext (không nhạy cảm)
// Expiry date không cần encrypt

// Option 2: Tăng buffer size
// EXPIRY_DATE_LENGTH = 32 bytes (thay vì 8)
```

### 3. Current Implementation

```java
// Card ID: Plaintext (cần để derive key)
cardId = "2021600001" (plaintext)

// Name: Encrypted (nếu đủ chỗ)
encryptedName = [IV (16)] + [Encrypted Data]
// Nếu > 50 bytes → truncate → có thể decrypt fail

// Expiry Date: Encrypted (nhưng không đủ chỗ)
encryptedExpiry = [IV (16)] + [Encrypted Data]
// Buffer chỉ 8 bytes → không thể encrypt đúng
// Hiện tại: Encrypt nhưng sẽ fail khi decrypt
```

## 📊 APDU Command Format

### SET_CARD_INFO Command

```
┌─────┬──────┬─────┬─────┬─────┬──────┬──────────────┬──────┬──────────────────┬──────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ FLAG │ CARD_ID (10) │ N_LEN│ NAME_ENCRYPTED   │ EXPIRY_ENCRYPTED │
│ 0x00│ 0x40 │0x00 │0x00 │ ... │ 0x01 │ [plaintext]  │ 1byte│ [IV+encrypted]   │ [IV+encrypted]   │
└─────┴──────┴─────┴─────┴─────┴──────┴──────────────┴──────┴──────────────────┴──────────────────┘
```

### GET_CARD_INFO Response

```
┌──────┬──────────────┬──────┬──────────────────┬──────────────────┬──────────┐
│ FLAG │ CARD_ID (10) │ N_LEN│ NAME_ENCRYPTED   │ EXPIRY_ENCRYPTED │ NUM_BOOKS│
│ 0x01 │ [plaintext]  │ 1byte│ [IV+encrypted]   │ [IV+encrypted]   │ 1 byte   │
└──────┴──────────────┴──────┴──────────────────┴──────────────────┴──────────┘
```

## 🔑 Key Points

1. **Card ID luôn plaintext**
   - Cần để derive AES key
   - Không nhạy cảm (public information)

2. **Name có thể encrypted**
   - Nếu đủ chỗ (≤ 34 bytes encrypted data)
   - Nếu không đủ → lưu plaintext hoặc truncate

3. **Expiry Date**
   - Buffer quá nhỏ (8 bytes)
   - Không thể encrypt đúng
   - Nên lưu plaintext

4. **Encrypted Flag**
   - Flag = 0x01: Name và Expiry đã encrypted
   - Flag = 0x00: Tất cả plaintext

## 📚 Xem thêm

- [AES Overview](./overview.md)
- [Key Derivation](./key-derivation.md)
- [Encryption Flow](./encryption-flow.md)
- [Decryption Flow](./decryption-flow.md)

