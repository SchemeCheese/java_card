# Triển khai Mã hóa AES và Xác thực RSA

## Tổng quan

Hệ thống đã được tích hợp:
- **AES Encryption**: Mã hóa thông tin thẻ (Card ID, Name, Expiry Date) trước khi lưu trên thẻ
- **RSA Authentication**: Xác thực thẻ để chống giả mạo

## Kiến trúc

### 1. JavaCard Applet Layer

#### RSAAuthenticationManager
- **Chức năng**: Tạo cặp khóa RSA và ký challenge
- **File**: `card_gui/src/applet/RSAAuthenticationManager.java`
- **Methods**:
  - `generateKeyPair()`: Tạo cặp khóa RSA (1024-bit)
  - `getPublicKey()`: Lấy public key từ thẻ
  - `signChallenge()`: Ký challenge với private key

#### AESEncryptionManager
- **Chức năng**: Quản lý AES key (client-side encryption)
- **File**: `card_gui/src/applet/AESEncryptionManager.java`
- **Lưu ý**: Do giới hạn JavaCard, mã hóa được thực hiện ở client-side

#### CardInfoManager (Updated)
- **Chức năng**: Lưu trữ thông tin thẻ (có thể đã mã hóa)
- **File**: `card_gui/src/applet/CardInfoManager.java`
- **Thay đổi**: Thêm flag `encrypted` để đánh dấu dữ liệu đã mã hóa

### 2. API Server Layer

#### Database Schema
- **File**: `server/database/schema.sql`
- **Thêm trường**:
  - `rsa_public_key`: TEXT - Lưu RSA public key (PEM format)
  - `rsa_key_created_at`: TIMESTAMP - Thời gian tạo khóa
  - `aes_master_key_hash`: VARCHAR(255) - Hash của AES master key

#### Card Controller
- **File**: `server/controllers/cardController.js`
- **Endpoints mới**:
  - `PUT /api/cards/:studentId/rsa-key`: Cập nhật RSA public key
  - `GET /api/cards/:studentId/rsa-key`: Lấy RSA public key

### 3. GUI Utility Layer

#### RSAUtility
- **File**: `card_gui/src/utils/RSAUtility.java`
- **Chức năng**:
  - Convert RSA key format (JavaCard → Java)
  - Verify RSA signature
  - Generate challenge

#### AESUtility
- **File**: `card_gui/src/utils/AESUtility.java`
- **Chức năng**:
  - Derive AES key từ master key + card ID
  - Encrypt/Decrypt dữ liệu với AES-128
  - Generate random AES key

#### SimulatorService (Updated)
- **File**: `card_gui/src/service/SimulatorService.java`
- **Methods mới**:
  - `generateRSAKeyPair()`: Tạo khóa RSA trên thẻ
  - `getRSAPublicKey()`: Lấy public key từ thẻ
  - `signRSAChallenge()`: Ký challenge
  - `authenticateCardWithRSA()`: Xác thực thẻ
  - `setAESKey()`: Thiết lập AES key trên thẻ

## Cách sử dụng

### 1. Tạo thẻ mới với RSA

```java
// 1. Tạo thẻ trên GUI
SimulatorService service = new SimulatorService();
service.connectToSimulator();

// 2. Tạo RSA keypair trên thẻ
byte[] publicKeyData = service.generateRSAKeyPair();

// 3. Extract modulus và exponent
byte[] modulus = new byte[128];
byte[] exponent = new byte[3];
System.arraycopy(publicKeyData, 0, modulus, 0, 128);
System.arraycopy(publicKeyData, 128, exponent, 0, 3);

// 4. Convert sang PEM và lưu lên server
String pemKey = RSAUtility.convertToPEM(modulus, exponent);
// Gọi API: PUT /api/cards/{studentId}/rsa-key
```

### 2. Xác thực thẻ với RSA

```java
// 1. Lấy public key từ server
// GET /api/cards/{studentId}/rsa-key
String publicKeyPEM = ...;

// 2. Xác thực thẻ
boolean authenticated = service.authenticateCardWithRSA(publicKeyPEM);
if (authenticated) {
    System.out.println("Thẻ hợp lệ!");
} else {
    System.out.println("Thẻ giả mạo!");
}
```

### 3. Mã hóa thông tin thẻ với AES

```java
// 1. Derive AES key từ master key và card ID
String masterKey = AESUtility.getMasterKey();
String cardId = "CT060132";
SecretKey aesKey = AESUtility.deriveKey(masterKey, cardId);

// 2. Mã hóa thông tin
String cardName = "Nguyễn Văn A";
byte[] encryptedName = AESUtility.encrypt(cardName.getBytes("UTF-8"), aesKey);

// 3. Gửi dữ liệu đã mã hóa lên thẻ
// (Thẻ sẽ lưu dữ liệu đã mã hóa, không giải mã)

// 4. Khi đọc, giải mã dữ liệu
byte[] decryptedName = AESUtility.decrypt(encryptedName, aesKey);
String name = new String(decryptedName, "UTF-8");
```

## APDU Commands

### RSA Commands

| INS Code | Command | Input | Output |
|----------|---------|-------|--------|
| 0xB0 | Generate KeyPair | - | Modulus (128) + Exponent (3) |
| 0xB1 | Get Public Key | - | Modulus (128) + Exponent (3) |
| 0xB2 | Sign Challenge | Challenge (16) | Signature (128) |

### AES Commands

| INS Code | Command | Input | Output |
|----------|---------|-------|--------|
| 0xC0 | Set AES Key | Key (16) | Status |
| 0xC1 | Encrypt | Data | Encrypted |
| 0xC2 | Decrypt | Encrypted | Data |

**Lưu ý**: AES encrypt/decrypt được thực hiện ở client-side do giới hạn JavaCard.

## Migration Database

Chạy migration script để thêm các trường mới:

```sql
-- File: server/database/migrations/add_rsa_aes_fields.sql
ALTER TABLE cards 
ADD COLUMN rsa_public_key TEXT,
ADD COLUMN rsa_key_created_at TIMESTAMP NULL,
ADD COLUMN aes_master_key_hash VARCHAR(255);
```

## Bảo mật

### RSA
- **Private Key**: Chỉ lưu trên thẻ, không bao giờ rời khỏi thẻ
- **Public Key**: Lưu trên server, công khai
- **Challenge-Response**: Mỗi lần xác thực dùng challenge ngẫu nhiên

### AES
- **Master Key**: Nên lưu trong secure storage (không hardcode)
- **Key Derivation**: Dùng PBKDF2 với 10000 iterations
- **IV**: Random IV cho mỗi lần mã hóa

## Lưu ý

1. **JavaCard Limitations**: 
   - AES encryption/decryption nên thực hiện ở client-side
   - Thẻ chỉ lưu dữ liệu đã mã hóa

2. **Master Key**:
   - Hiện tại dùng default key (KHÔNG AN TOÀN cho production!)
   - Cần lưu trong config file hoặc environment variable

3. **RSA Key Size**:
   - Hiện tại dùng 1024-bit (đủ cho demo)
   - Production nên dùng 2048-bit hoặc cao hơn

## Testing

1. Tạo thẻ mới và generate RSA keypair
2. Lưu public key lên server
3. Test xác thực thẻ với RSA
4. Test mã hóa/giải mã thông tin với AES

