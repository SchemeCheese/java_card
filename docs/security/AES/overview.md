# AES Encryption - Tổng quan

## 🎯 Mục đích

AES (Advanced Encryption Standard) được dùng để:
- Mã hóa thông tin nhạy cảm trên thẻ (Name, Expiry Date)
- Bảo vệ dữ liệu khỏi truy cập trái phép
- Đảm bảo tính bảo mật khi lưu trữ trên card

## 🔒 Nguyên tắc bảo mật

### ✅ Client-side Encryption

```
┌─────────────────────────────────────────┐
│  Encryption Flow                        │
│  ┌───────────────────────────────────┐ │
│  │  Client (Java GUI)                │ │
│  │  - Derive AES key                 │ │
│  │  - Encrypt data (Name, Expiry)    │ │
│  │  - Send encrypted data to card    │ │
│  └───────────────────────────────────┘ │
│                    ↓                    │
│  ┌───────────────────────────────────┐ │
│  │  Card (Applet)                    │ │
│  │  - Store encrypted data only      │ │
│  │  - Does NOT decrypt               │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### ❌ Tại sao không encrypt trên card?

- JavaCard có giới hạn về memory và processing power
- AES encryption trên card tốn nhiều tài nguyên
- Client-side encryption hiệu quả hơn

## 📊 Kiến trúc

### Components

1. **AESUtility (Client)**
   - File: `card_gui/src/utils/AESUtility.java`
   - Chức năng: Encrypt/decrypt dữ liệu
   - Algorithm: AES-128-CBC-PKCS5Padding

2. **SimulatorService (Client)**
   - File: `card_gui/src/service/SimulatorService.java`
   - Chức năng: Derive key và gửi dữ liệu đã mã hóa lên card

3. **CardInfoManager (Applet)**
   - File: `card_gui/src/applet/CardInfoManager.java`
   - Chức năng: Lưu trữ dữ liệu đã mã hóa

## 🔐 Encryption Configuration

### Algorithm
- **Algorithm**: AES (Advanced Encryption Standard)
- **Key Size**: 128 bits (16 bytes)
- **Mode**: CBC (Cipher Block Chaining)
- **Padding**: PKCS5Padding
- **IV**: Random 16 bytes (prepended to encrypted data)

### Key Derivation
- **Method**: PBKDF2 with HMAC-SHA256
- **Iterations**: 10000
- **Input**: Master Key + Card ID (as salt)
- **Output**: 128-bit AES key

## 📋 Data Storage Strategy

### Card ID: Plaintext
- **Lý do**: Cần Card ID để derive AES key
- **Vị trí**: Lưu plaintext trên card
- **Rủi ro**: Thấp (Card ID không nhạy cảm)

### Name & Expiry Date: Encrypted
- **Lý do**: Thông tin nhạy cảm
- **Vị trí**: Lưu encrypted trên card
- **Format**: `[IV (16 bytes)] + [Encrypted Data]`

## 📋 Các luồng chính

1. **[Key Derivation](./key-derivation.md)** - Cách tạo AES key từ master key
2. **[Encryption Flow](./encryption-flow.md)** - Luồng mã hóa khi lưu dữ liệu
3. **[Decryption Flow](./decryption-flow.md)** - Luồng giải mã khi đọc dữ liệu
4. **[Storage Format](./storage-format.md)** - Format lưu trữ trên card

## ⚠️ Limitations

### 1. Truncation Issue
- Card chỉ có 50 bytes cho Name
- Encrypted data = IV (16 bytes) + Encrypted content
- Nếu encrypted data > 34 bytes → bị truncate → mất IV → không decrypt được

### 2. Expiry Date
- Card chỉ có 8 bytes cho Expiry Date
- Encrypted data cần tối thiểu 16 bytes (IV)
- **Giải pháp**: Expiry Date không thể encrypt đúng với giới hạn hiện tại

### 3. Master Key
- Hiện tại hardcode: `"LibraryCardMasterKey2024!"`
- **Không an toàn cho production!**
- Nên lưu trong secure storage hoặc environment variable

## 🔑 Master Key

### Current Implementation
```java
// AESUtility.java
public static String getMasterKey() {
    return "LibraryCardMasterKey2024!"; // ⚠️ HARDCODED
}
```

### Security Concerns
- ⚠️ Hardcoded trong code (không an toàn)
- ⚠️ Dễ bị reverse engineer
- ✅ Nên dùng secure key management trong production

## 📚 Xem thêm

- [Key Derivation](./key-derivation.md)
- [Encryption Flow](./encryption-flow.md)
- [Decryption Flow](./decryption-flow.md)
- [Storage Format](./storage-format.md)

