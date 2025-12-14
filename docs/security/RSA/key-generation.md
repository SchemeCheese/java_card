# RSA Authentication - Key Generation

## 📋 Tổng quan

Luồng tạo cặp khóa RSA trên card khi khởi tạo thẻ mới.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Tạo thẻ mới
     ▼
┌─────────────────────────┐
│  PinPage.java           │
│  - Sau khi tạo PIN      │
│  - Gọi generateKeyPair()│
└────┬────────────────────┘
     │ 2. Gửi GENERATE_KEYPAIR command
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  RSAAuthenticationManager│
│  - Generate RSA keypair │
│  - 1024-bit RSA         │
│  - Trả về Public Key    │
└────┬────────────────────┘
     │ 3. Nhận Public Key
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Convert to PEM       │
│  - Lưu lên server       │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - PinPage.java

```java
// Sau khi tạo PIN thành công
if (pinCreated) {
    // Tạo RSA keypair
    try {
        byte[] publicKeyData = simulatorService.generateRSAKeyPair();
        
        // Convert to PEM và lưu lên server
        if (publicKeyData != null && publicKeyData.length > 0) {
            // Extract modulus and exponent
            byte[] modulus = new byte[128];
            byte[] exponent = new byte[3];
            System.arraycopy(publicKeyData, 0, modulus, 0, 128);
            System.arraycopy(publicKeyData, 128, exponent, 0, 3);
            
            // Convert to PEM
            String publicKeyPEM = RSAUtility.convertToPEM(modulus, exponent);
            
            // Lưu lên server
            if (apiManager.isServerAvailable()) {
                cardApi.updateRSAPublicKey(studentCode, publicKeyPEM);
            }
        }
    } catch (Exception e) {
        System.err.println("Error generating RSA keypair: " + e.getMessage());
    }
}
```

### 2. Service Layer - SimulatorService.java

```java
public byte[] generateRSAKeyPair() throws Exception {
    if (!isConnected) {
        throw new Exception("Chưa kết nối với thẻ");
    }
    
    // Gửi GENERATE_KEYPAIR command
    byte[] cmd = new byte[5];
    cmd[0] = (byte)0x00;
    cmd[1] = AppletConstants.INS_RSA_GENERATE_KEYPAIR; // 0xB0
    cmd[2] = (byte)0x00;
    cmd[3] = (byte)0x00;
    cmd[4] = (byte)0x00;
    
    byte[] resp = sendCommand(cmd);
    
    if (getSW(resp) != 0x9000) {
        throw new Exception("Failed to generate RSA keypair: " + 
            String.format("%04X", getSW(resp)));
    }
    
    // Response: [MODULUS (128 bytes)] [PUBLIC_EXPONENT (3 bytes)]
    byte[] publicKeyData = new byte[resp.length - 2];
    System.arraycopy(resp, 0, publicKeyData, 0, publicKeyData.length);
    
    return publicKeyData;
}
```

### 3. Applet Layer - RSAAuthenticationManager.java

```java
public void generateKeyPair(APDU apdu) {
    // Kiểm tra: Keypair chưa được tạo
    if (keyPairGenerated) {
        ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
    }
    
    try {
        // 1. Tạo cặp khóa RSA (1024-bit)
        KeyPair rsaKeyPair = new KeyPair(
            KeyPair.ALG_RSA, 
            AppletConstants.RSA_KEY_SIZE // 1024 bits
        );
        rsaKeyPair.genKeyPair();
        
        // 2. Lưu private và public key
        privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
        publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        
        keyPairGenerated = true;
        
        // 3. Gửi Public Key về client
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        // Modulus (128 bytes)
        short modulusLen = publicKey.getModulus(buffer, offset);
        offset += modulusLen;
        
        // Public Exponent (3 bytes: 0x01 0x00 0x01 = 65537)
        Util.arrayCopy(publicExponent, (short)0, buffer, offset, (short)3);
        offset += 3;
        
        // 4. Gửi response
        apdu.setOutgoingAndSend((short)0, offset);
        
    } catch (CryptoException e) {
        ISOException.throwIt(ISO7816.SW_UNKNOWN);
    }
}
```

## 📊 Data Format

### APDU Command

```
┌─────┬──────┬─────┬─────┬─────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │
│ 0x00│ 0xB0 │0x00 │0x00 │0x00 │
└─────┴──────┴─────┴─────┴─────┘
```

### Response

```
┌──────────────────────┬──────────────────┬─────┬─────┐
│ MODULUS (128 bytes)  │ EXPONENT (3)     │ SW1 │ SW2 │
│ [1024-bit modulus]   │ [0x01,0x00,0x01] │ 0x90│0x00 │
└──────────────────────┴──────────────────┴─────┴─────┘
```

## 🔐 Key Generation Process

### Step 1: Create KeyPair Object

```java
KeyPair rsaKeyPair = new KeyPair(
    KeyPair.ALG_RSA,      // RSA algorithm
    (short)1024           // Key size: 1024 bits
);
```

### Step 2: Generate Keys

```java
rsaKeyPair.genKeyPair();
```

### Step 3: Extract Keys

```java
privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
```

### Step 4: Get Public Key Components

```java
// Modulus (128 bytes for 1024-bit RSA)
byte[] modulus = new byte[128];
short modulusLen = publicKey.getModulus(modulus, (short)0);

// Public Exponent (usually 65537 = 0x010001)
byte[] exponent = {0x01, 0x00, 0x01};
```

## 🔑 Public Key Format

### JavaCard Format
- **Modulus**: 128 bytes (1024 bits)
- **Exponent**: 3 bytes (65537 = 0x010001)

### PEM Format (for Server)
```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
```

### Conversion Process

```java
// 1. Convert modulus and exponent to BigInteger
BigInteger n = new BigInteger(1, modulus);
BigInteger e = new BigInteger(1, exponent);

// 2. Create RSAPublicKeySpec
RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
KeyFactory keyFactory = KeyFactory.getInstance("RSA");
PublicKey publicKey = keyFactory.generatePublic(keySpec);

// 3. Encode to PEM
byte[] encoded = publicKey.getEncoded();
String pem = Base64.getEncoder().encodeToString(encoded);
```

## ⚠️ Lưu ý

1. **Keypair chỉ được tạo 1 lần**
   - Nếu keypair đã tồn tại, card trả về `SW_COMMAND_NOT_ALLOWED`
   - Để tạo lại, cần reset card hoặc cấp thẻ mới

2. **Private Key không rời khỏi card**
   - Private key chỉ tồn tại trong EEPROM
   - Không thể export private key
   - Chỉ có thể sign challenge trên card

3. **Public Key được lưu trên server**
   - Public key là công khai
   - Dùng để verify signature
   - Có thể lưu trong database

4. **Key Size: 1024-bit**
   - Đủ cho demo/testing
   - Production nên dùng 2048-bit hoặc cao hơn
   - 1024-bit đã bị coi là không an toàn cho production

## 📚 Xem thêm

- [RSA Overview](./overview.md)
- [Authentication Flow](./authentication-flow.md)
- [Challenge-Response](./challenge-response.md)

