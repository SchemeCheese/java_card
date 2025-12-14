# AES Encryption - Encryption Flow

## 📋 Tổng quan

Luồng mã hóa dữ liệu (Name, Expiry Date) trước khi lưu lên card.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Nhập thông tin thẻ
     ▼
┌─────────────────────────┐
│  SettingsPage.java      │
│  - Card ID (plaintext)   │
│  - Name (plaintext)      │
│  - Expiry Date (plaintext)│
└────┬────────────────────┘
     │ 2. Gọi setCardInfo()
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Derive AES key       │
│  - Encrypt Name         │
│  - Encrypt Expiry Date  │
└────┬────────────────────┘
     │ 3. Gửi lên card
     │    [ENCRYPTED_FLAG] [CARD_ID] [ENCRYPTED_NAME] [ENCRYPTED_EXPIRY]
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  CardInfoManager        │
│  - Lưu Card ID (plain)  │
│  - Lưu Name (encrypted) │
│  - Lưu Expiry (encrypted)│
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - SettingsPage.java

```java
// User nhập thông tin
CardInfo cardInfo = new CardInfo();
cardInfo.setStudentId("2021600001");  // Card ID
cardInfo.setHolderName("Nguyễn Văn A");  // Name

// Gọi SimulatorService để lưu
simulatorService.setCardInfo(cardInfo);
```

### 2. Service Layer - SimulatorService.java

```java
public boolean setCardInfo(CardInfo cardInfo) throws Exception {
    if (!isConnected || !isPinVerified) {
        throw new Exception("Chưa kết nối hoặc chưa xác thực PIN");
    }
    
    try {
        // 1. Lấy Card ID (plaintext - dùng để derive key)
        String cardId = cardInfo.getStudentId();
        
        // 2. Derive AES key từ master key và card ID
        String masterKey = AESUtility.getMasterKey();
        javax.crypto.SecretKey aesKey = getOrDeriveAESKey(cardId);
        
        // 3. Encrypt Name
        byte[] namePlain = cardInfo.getHolderName().getBytes(StandardCharsets.UTF_8);
        byte[] encryptedName = AESUtility.encrypt(namePlain, aesKey);
        
        // 4. Encrypt Expiry Date
        String expiryDate = java.time.LocalDate.now()
            .plusYears(5)
            .format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
        byte[] encryptedExpiry = AESUtility.encrypt(
            expiryDate.getBytes(StandardCharsets.UTF_8), aesKey);
        
        // 5. Prepare Card ID (plaintext - pad/truncate to 10 bytes)
        byte[] cardIdBytes = cardId.getBytes(StandardCharsets.UTF_8);
        byte[] cardIdData = new byte[AppletConstants.CARD_ID_LENGTH];
        System.arraycopy(cardIdBytes, 0, cardIdData, 0, 
            Math.min(cardIdData.length, cardIdBytes.length));
        
        // 6. Truncate encrypted data to fit applet constraints
        byte[] nameData = new byte[Math.min(encryptedName.length, 
            AppletConstants.NAME_MAX_LENGTH)];
        System.arraycopy(encryptedName, 0, nameData, 0, nameData.length);
        
        byte[] expiryData = new byte[Math.min(encryptedExpiry.length, 
            AppletConstants.EXPIRY_DATE_LENGTH)];
        System.arraycopy(encryptedExpiry, 0, expiryData, 0, 
            Math.min(expiryData.length, encryptedExpiry.length));
        
        // 7. Build APDU command
        // Format: [ENCRYPTED_FLAG (1)] [CARD_ID (10)] 
        //         [NAME_LEN (1)] [NAME_ENCRYPTED] [EXPIRY_ENCRYPTED (8)]
        int totalLength = 1 + cardIdData.length + 1 + nameData.length + expiryData.length;
        byte[] cmd = new byte[5 + totalLength];
        cmd[0] = (byte)0x00;
        cmd[1] = AppletConstants.INS_SET_CARD_INFO;
        cmd[2] = (byte)0x00;
        cmd[3] = (byte)0x00;
        cmd[4] = (byte)totalLength;
        
        int offset = 5;
        cmd[offset++] = (byte)0x01; // Encrypted flag
        System.arraycopy(cardIdData, 0, cmd, offset, cardIdData.length); // Card ID (plaintext)
        offset += cardIdData.length;
        cmd[offset++] = (byte)nameData.length; // Name length
        System.arraycopy(nameData, 0, cmd, offset, nameData.length); // Encrypted Name
        offset += nameData.length;
        System.arraycopy(expiryData, 0, cmd, offset, expiryData.length); // Encrypted Expiry
        
        // 8. Gửi command lên card
        byte[] resp = sendCommand(cmd);
        return getSW(resp) == 0x9000;
        
    } catch (Exception e) {
        e.printStackTrace();
        throw new Exception("Lỗi khi mã hóa và lưu thông tin thẻ: " + e.getMessage());
    }
}
```

### 3. Utility Layer - AESUtility.java

```java
public static byte[] encrypt(byte[] data, SecretKey key) {
    try {
        // 1. Tạo Cipher với AES-CBC-PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // 2. Generate random IV (16 bytes)
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        // 3. Initialize cipher với key và IV
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        
        // 4. Encrypt data
        byte[] encrypted = cipher.doFinal(data);
        
        // 5. Prepend IV to encrypted data
        // Format: [IV (16 bytes)] + [Encrypted Data]
        byte[] result = new byte[16 + encrypted.length];
        System.arraycopy(iv, 0, result, 0, 16);
        System.arraycopy(encrypted, 0, result, 16, encrypted.length);
        
        return result;
        
    } catch (Exception e) {
        throw new RuntimeException("Error encrypting data", e);
    }
}
```

### 4. Applet Layer - CardInfoManager.java

```java
public void setCardInfo(APDU apdu, PinManager pinManager) {
    // Kiểm tra PIN đã verify
    if (!pinManager.isPinValidated()) {
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }
    
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    short offset = ISO7816.OFFSET_CDATA;
    
    // 1. Đọc encrypted flag
    boolean encrypted = (buffer[offset++] == (byte)0x01);
    
    // 2. Đọc Card ID (plaintext - 10 bytes)
    Util.arrayCopy(buffer, offset, cardId, (short)0, AppletConstants.CARD_ID_LENGTH);
    offset += AppletConstants.CARD_ID_LENGTH;
    
    // 3. Đọc Name length và Name (có thể đã mã hóa)
    holderNameLength = buffer[offset++];
    Util.arrayCopy(buffer, offset, holderName, (short)0, holderNameLength);
    offset += holderNameLength;
    
    // 4. Đọc Expiry Date (có thể đã mã hóa - 8 bytes)
    Util.arrayCopy(buffer, offset, expiryDate, (short)0, AppletConstants.EXPIRY_DATE_LENGTH);
    
    // Card chỉ lưu dữ liệu, không giải mã
    // Decryption sẽ được thực hiện ở client khi đọc
}
```

## 📊 Data Format

### Encrypted Data Format

```
┌──────────────────┬──────────────────────┐
│ IV (16 bytes)    │ Encrypted Data       │
│ [random bytes]   │ [ciphertext]         │
└──────────────────┴──────────────────────┘
```

### APDU Command Format

```
┌─────┬──────┬─────┬─────┬─────┬──────┬──────────────┬──────┬──────────────────┬──────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ FLAG │ CARD_ID (10) │ N_LEN│ NAME_ENCRYPTED   │ EXPIRY_ENCRYPTED │
│ 0x00│ 0x40 │0x00 │0x00 │ ... │ 0x01 │ [plaintext]  │ 1byte│ [IV+encrypted]   │ [IV+encrypted]   │
└─────┴──────┴─────┴─────┴─────┴──────┴──────────────┴──────┴──────────────────┴──────────────────┘
```

## ⚠️ Limitations

### 1. Name Truncation
- Card chỉ có 50 bytes cho Name
- Encrypted format: `[IV (16)] + [Encrypted Data]`
- Nếu encrypted data > 34 bytes → bị truncate → mất IV → không decrypt được

### 2. Expiry Date
- Card chỉ có 8 bytes cho Expiry Date
- Encrypted data cần tối thiểu 16 bytes (IV)
- **Không thể encrypt đúng với giới hạn hiện tại**

### 3. Workaround
- Name: Chỉ encrypt nếu đủ chỗ, hoặc tăng buffer size
- Expiry Date: Lưu plaintext (không nhạy cảm)

## 🔐 Security Features

### 1. Random IV
- Mỗi lần encrypt, tạo IV mới (random)
- IV được prepend vào encrypted data
- Chống pattern attacks

### 2. CBC Mode
- Cipher Block Chaining mode
- Mỗi block phụ thuộc vào block trước
- Tăng tính bảo mật

### 3. PKCS5Padding
- Padding để đảm bảo data length là bội của 16 bytes
- Standard padding scheme

## 📚 Xem thêm

- [AES Overview](./overview.md)
- [Key Derivation](./key-derivation.md)
- [Decryption Flow](./decryption-flow.md)
- [Storage Format](./storage-format.md)

