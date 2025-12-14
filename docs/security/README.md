# Tài liệu Bảo mật - Security Documentation

Tài liệu chi tiết về các luồng bảo mật trong hệ thống Library Card Management.

## 📚 Cấu trúc tài liệu

### 🔐 PIN Management
- [Tổng quan PIN](./PIN/overview.md) - Kiến trúc và nguyên tắc
- [Tạo PIN](./PIN/create-pin.md) - Luồng tạo PIN mới
- [Xác thực PIN](./PIN/verify-pin.md) - Luồng verify PIN
- [Đổi PIN](./PIN/change-pin.md) - Luồng đổi PIN
- [Reset PIN](./PIN/reset-pin.md) - Luồng reset PIN (Admin)

### 🔒 AES Encryption
- [Tổng quan AES](./AES/overview.md) - Kiến trúc và nguyên tắc
- [Key Derivation](./AES/key-derivation.md) - Cách tạo AES key
- [Encryption Flow](./AES/encryption-flow.md) - Luồng mã hóa dữ liệu
- [Decryption Flow](./AES/decryption-flow.md) - Luồng giải mã dữ liệu
- [Storage Format](./AES/storage-format.md) - Format lưu trữ trên card

### 🔑 RSA Authentication
- [Tổng quan RSA](./RSA/overview.md) - Kiến trúc và nguyên tắc
- [Key Generation](./RSA/key-generation.md) - Luồng tạo cặp khóa RSA
- [Authentication Flow](./RSA/authentication-flow.md) - Luồng xác thực thẻ
- [Challenge-Response](./RSA/challenge-response.md) - Cơ chế challenge-response

## 🎯 Nguyên tắc bảo mật

1. **PIN**: Chỉ lưu và verify trên card, không rời khỏi card
2. **AES**: Client-side encryption, card chỉ lưu dữ liệu đã mã hóa
3. **RSA**: Private key chỉ trên card, public key trên server

## 📂 File liên quan

### Applet (JavaCard)
- `card_gui/src/applet/PinManager.java` - Quản lý PIN
- `card_gui/src/applet/AESEncryptionManager.java` - Quản lý AES (placeholder)
- `card_gui/src/applet/RSAAuthenticationManager.java` - Quản lý RSA
- `card_gui/src/applet/CardInfoManager.java` - Lưu trữ thông tin thẻ

### Client (Java GUI)
- `card_gui/src/service/SimulatorService.java` - Bridge giữa GUI và applet
- `card_gui/src/utils/AESUtility.java` - AES encryption/decryption
- `card_gui/src/utils/RSAUtility.java` - RSA key conversion và verification

### Server
- `server/controllers/pinController.js` - PIN endpoints (deprecated)
- `server/models/Card.js` - Card model (không lưu PIN)

## 🔗 Xem thêm

- [API Documentation](../server/README.md)
- [Integration Guide](../server/INTEGRATION_GUIDE.md)
- [Setup Guide](../server/SETUP.md)

