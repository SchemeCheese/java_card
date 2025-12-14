# PIN Management - Tổng quan

## 🎯 Mục đích

PIN (Personal Identification Number) được dùng để:
- Xác thực người dùng khi sử dụng thẻ
- Bảo vệ dữ liệu trên thẻ (chỉ người có PIN mới truy cập được)
- Chống giả mạo và truy cập trái phép

## 🔒 Nguyên tắc bảo mật

### ✅ ĐÚNG: PIN hash chỉ lưu trên card

```
┌─────────────────────────────────────────┐
│  PIN Hash Storage                       │
│  ┌───────────────────────────────────┐ │
│  │  Card (Applet)                    │ │
│  │  - PIN Hash (32 bytes)            │ │
│  │  - Salt (16 bytes)                │ │
│  │  - Tries Remaining (3)            │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  Server (Database)                │ │
│  │  - ❌ KHÔNG LƯU PIN HASH          │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### ❌ SAI: PIN hash trên server

- Database bị hack → Attacker có PIN hash
- Có thể verify PIN mà không cần thẻ
- Có thể brute force PIN hash

## 📊 Kiến trúc

### Components

1. **PinManager (Applet)**
   - File: `card_gui/src/applet/PinManager.java`
   - Chức năng: Lưu trữ và verify PIN hash trên card
   - Storage: EEPROM của card

2. **SimulatorService (Client)**
   - File: `card_gui/src/service/SimulatorService.java`
   - Chức năng: Hash PIN và gửi lên card để verify
   - Algorithm: PBKDF2-SHA256

3. **PinPage (GUI)**
   - File: `card_gui/src/pages/PinPage.java`
   - Chức năng: UI để user nhập PIN

## 🔐 Hashing Algorithm

### PBKDF2 với SHA-256

```java
// Configuration
PBKDF2_ITERATIONS = 10000
HASH_BIT_LENGTH = 256 (32 bytes)
SALT_LENGTH = 16 bytes
```

### Process

```
PIN (plaintext)
    ↓
+ Salt (16 bytes, random)
    ↓
PBKDF2-SHA256 (10000 iterations)
    ↓
PIN Hash (32 bytes)
```

## 📋 Các luồng chính

1. **[Tạo PIN](./create-pin.md)** - Tạo PIN mới khi khởi tạo thẻ
2. **[Xác thực PIN](./verify-pin.md)** - Verify PIN khi sử dụng thẻ
3. **[Đổi PIN](./change-pin.md)** - Thay đổi PIN (cần verify PIN cũ)
4. **[Reset PIN](./reset-pin.md)** - Reset PIN khi quên (Admin only)

## 🔑 APDU Commands

| INS Code | Command | Input | Output |
|----------|---------|-------|--------|
| 0x10 | CREATE_PIN | [SALT (16)] [HASH (32)] | Status |
| 0x20 | VERIFY_PIN | [HASH (32)] | Success (0x01) / Fail (0x00 + tries) |
| 0x30 | CHANGE_PIN | [NEW_SALT (16)] [NEW_HASH (32)] | Status |
| 0x22 | GET_SALT | - | Salt (16 bytes) |
| 0x90 | GET_PIN_TRIES | - | Tries remaining (1 byte) |
| 0xA0 | RESET_PIN | [ADMIN_KEY (4)] [NEW_SALT (16)] [NEW_HASH (32)] | Status |

## ⚠️ Lưu ý

1. **PIN hash không bao giờ rời khỏi card**
   - Client chỉ gửi hash, không gửi PIN plaintext
   - Card verify hash, không gửi hash về client

2. **Salt được lưu trên card**
   - Mỗi PIN có salt riêng (random 16 bytes)
   - Salt được gửi về client khi cần hash PIN

3. **Tries limit: 3 lần**
   - Sau 3 lần sai, card bị lock
   - Cần admin reset hoặc cấp thẻ mới

4. **Server không lưu PIN hash**
   - PIN chỉ tồn tại trên card
   - Server không thể verify PIN

## 📚 Xem thêm

- [Create PIN Flow](./create-pin.md)
- [Verify PIN Flow](./verify-pin.md)
- [Change PIN Flow](./change-pin.md)
- [Reset PIN Flow](./reset-pin.md)

