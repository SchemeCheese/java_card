# RSA Authentication - Tổng quan

## 🎯 Mục đích

RSA (Rivest-Shamir-Adleman) được dùng để:
- Xác thực thẻ chống giả mạo
- Đảm bảo thẻ là thật (không phải clone)
- Challenge-response authentication

## 🔒 Nguyên tắc bảo mật

### ✅ Private Key chỉ trên Card

```
┌─────────────────────────────────────────┐
│  RSA Key Storage                        │
│  ┌───────────────────────────────────┐ │
│  │  Card (Applet)                    │ │
│  │  - Private Key (1024-bit)         │ │
│  │  - Public Key (1024-bit)          │ │
│  │  - ❌ Private Key KHÔNG rời khỏi   │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  Server (Database)                │ │
│  │  - Public Key (PEM format)        │ │
│  │  - ✅ Public Key là công khai      │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Challenge-Response Mechanism

```
Server → Card: Challenge (random 16 bytes)
Card → Server: Signature (RSA sign challenge)
Server: Verify signature với Public Key
```

## 📊 Kiến trúc

### Components

1. **RSAAuthenticationManager (Applet)**
   - File: `card_gui/src/applet/RSAAuthenticationManager.java`
   - Chức năng: Generate key pair, sign challenge
   - Storage: EEPROM của card

2. **RSAUtility (Client)**
   - File: `card_gui/src/utils/RSAUtility.java`
   - Chức năng: Convert key format, verify signature
   - Algorithm: SHA1withRSA

3. **SimulatorService (Client)**
   - File: `card_gui/src/service/SimulatorService.java`
   - Chức năng: Generate challenge, verify signature

4. **Server (Database)**
   - File: `server/models/Card.js`
   - Chức năng: Lưu public key (PEM format)

## 🔐 RSA Configuration

### Key Size
- **Key Size**: 1024 bits
- **Modulus Size**: 128 bytes
- **Public Exponent**: 65537 (0x010001)
- **Signature Size**: 128 bytes

### Signature Algorithm
- **Hash**: SHA-1
- **Padding**: PKCS#1 v1.5
- **Format**: EMSA-PKCS1-v1_5

## 📋 Các luồng chính

1. **[Key Generation](./key-generation.md)** - Tạo cặp khóa RSA trên card
2. **[Authentication Flow](./authentication-flow.md)** - Luồng xác thực thẻ
3. **[Challenge-Response](./challenge-response.md)** - Cơ chế challenge-response

## 🔑 APDU Commands

| INS Code | Command | Input | Output |
|----------|---------|-------|--------|
| 0xB0 | GENERATE_KEYPAIR | - | Modulus (128) + Exponent (3) |
| 0xB1 | GET_PUBLIC_KEY | - | Modulus (128) + Exponent (3) |
| 0xB2 | SIGN_CHALLENGE | Challenge (16) | Signature (128) |

## ⚠️ Lưu ý

1. **Private Key không bao giờ rời khỏi card**
   - Private key chỉ tồn tại trong EEPROM của card
   - Không thể export private key
   - Chỉ có thể sign challenge trên card

2. **Public Key được lưu trên server**
   - Public key là công khai
   - Dùng để verify signature
   - Có thể lưu trong database

3. **Challenge phải random**
   - Mỗi lần authentication dùng challenge khác
   - Chống replay attacks
   - Challenge được generate bởi client

4. **Key Size: 1024-bit**
   - Đủ cho demo/testing
   - Production nên dùng 2048-bit hoặc cao hơn

## 📚 Xem thêm

- [Key Generation](./key-generation.md)
- [Authentication Flow](./authentication-flow.md)
- [Challenge-Response](./challenge-response.md)

