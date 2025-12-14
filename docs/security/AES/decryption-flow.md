# AES Encryption - Decryption Flow

## 📋 Tổng quan

Luồng giải mã dữ liệu khi đọc từ card.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Yêu cầu đọc thông tin thẻ
     ▼
┌─────────────────────────┐
│  CardInfoPage.java      │
│  - Gọi getCardInfo()    │
└────┬────────────────────┘
     │ 2. Gọi SimulatorService
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Gửi GET_CARD_INFO    │
└────┬────────────────────┘
     │ 3. Nhận dữ liệu từ card
     │    [ENCRYPTED_FLAG] [CARD_ID] [NAME] [EXPIRY]
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Đọc Card ID (plain)  │
│  - Derive AES key       │
│  - Decrypt Name         │
│  - Decrypt Expiry Date  │
└────┬────────────────────┘
     │ 4. Trả về CardInfo
     ▼
┌─────────────────────────┐
│  CardInfoPage.java      │
│  - Hiển thị thông tin   │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - CardInfoPage.java

```java
// Yêu cầu đọc thông tin thẻ
CardInfo cardInfo = simulatorService.getCardInfo();

// Hiển thị
nameField.setText(cardInfo.getHolderName());
studentIdField.setText(cardInfo.getStudentId());
```

### 2. Service Layer - SimulatorService.java

```java
public CardInfo getCardInfo() throws Exception {
    if (!isConnected) {
        throw new Exception("Chưa kết nối với thẻ");
    }
    
    try {
        // 1. Gửi GET_CARD_INFO command
        byte[] cmd = new byte[5];
        cmd[0] = (byte)0x00;
        cmd[1] = AppletConstants.INS_GET_CARD_INFO;
        cmd[2] = (byte)0x00;
        cmd[3] = (byte)0x00;
        cmd[4] = (byte)0x00;
        
        byte[] resp = sendCommand(cmd);
        if (getSW(resp) != 0x9000) {
            throw new Exception("Lỗi khi đọc thông tin thẻ");
        }
        
        // 2. Parse response
        byte[] data = new byte[resp.length - 2];
        System.arraycopy(resp, 0, data, 0, data.length);
        
        int offset = 0;
        boolean encrypted = (data[offset++] == (byte)0x01);
        
        // 3. Đọc Card ID (plaintext)
        byte[] cardIdPlain = new byte[AppletConstants.CARD_ID_LENGTH];
        System.arraycopy(data, offset, cardIdPlain, 0, cardIdPlain.length);
        offset += cardIdPlain.length;
        String cardIdStr = new String(cardIdPlain, StandardCharsets.UTF_8).trim();
        
        // 4. Đọc Name
        byte nameLen = data[offset++];
        byte[] nameData = new byte[nameLen];
        System.arraycopy(data, offset, nameData, 0, nameLen);
        offset += nameLen;
        
        // 5. Đọc Expiry Date
        byte[] expiryData = new byte[AppletConstants.EXPIRY_DATE_LENGTH];
        System.arraycopy(data, offset, expiryData, 0, expiryData.length);
        offset += expiryData.length;
        
        // 6. Đọc số sách đã mượn
        byte numBooks = data[offset];
        
        // 7. Tạo CardInfo object
        CardInfo cardInfo = new CardInfo();
        cardInfo.setBorrowedBooks(numBooks & 0xFF);
        cardInfo.setStudentId(cardIdStr); // Card ID luôn plaintext
        
        // 8. Decrypt nếu đã mã hóa
        if (encrypted) {
            // Derive AES key từ Card ID (plaintext)
            String masterKey = AESUtility.getMasterKey();
            javax.crypto.SecretKey aesKey = getOrDeriveAESKey(cardIdStr);
            
            try {
                // Decrypt Name
                if (nameData.length >= 16) {
                    byte[] decryptedName = AESUtility.decrypt(nameData, aesKey);
                    String nameStr = new String(decryptedName, StandardCharsets.UTF_8).trim();
                    cardInfo.setHolderName(nameStr);
                } else {
                    // Data quá ngắn, có thể bị truncate
                    System.err.println("Warning: Encrypted name data too short");
                    cardInfo.setHolderName(new String(nameData, StandardCharsets.UTF_8).trim());
                }
                
                // Decrypt Expiry Date (nếu đủ chỗ)
                // Note: Expiry chỉ có 8 bytes, không đủ cho IV (16 bytes)
                // Nên có thể không decrypt được
                // cardInfo.setExpiryDate(...);
                
            } catch (Exception e) {
                // Decryption failed - fallback to raw data
                System.err.println("Warning: Failed to decrypt, using raw data");
                cardInfo.setHolderName(new String(nameData, StandardCharsets.UTF_8).trim());
            }
        } else {
            // Plaintext data
            cardInfo.setHolderName(new String(nameData, StandardCharsets.UTF_8).trim());
        }
        
        return cardInfo;
        
    } catch (Exception e) {
        e.printStackTrace();
        throw new Exception("Lỗi khi đọc và giải mã thông tin thẻ: " + e.getMessage());
    }
}
```

### 3. Utility Layer - AESUtility.java

```java
public static byte[] decrypt(byte[] encryptedData, SecretKey key) {
    try {
        // 1. Kiểm tra độ dài tối thiểu (phải có IV)
        if (encryptedData.length < 16) {
            throw new IllegalArgumentException("Encrypted data too short");
        }
        
        // 2. Extract IV (16 bytes đầu tiên)
        byte[] iv = new byte[16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        // 3. Extract encrypted data (phần còn lại)
        byte[] encrypted = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 16, encrypted, 0, encrypted.length);
        
        // 4. Tạo Cipher với AES-CBC-PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // 5. Initialize cipher với key và IV
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        
        // 6. Decrypt data
        return cipher.doFinal(encrypted);
        
    } catch (Exception e) {
        throw new RuntimeException("Error decrypting data", e);
    }
}
```

## 📊 Data Format

### Response từ Card

```
┌──────┬──────────────┬──────┬──────────────────┬──────────────────┬──────────┐
│ FLAG │ CARD_ID (10) │ N_LEN│ NAME_ENCRYPTED   │ EXPIRY_ENCRYPTED │ NUM_BOOKS│
│ 0x01 │ [plaintext]  │ 1byte│ [IV+encrypted]   │ [IV+encrypted]   │ 1 byte   │
└──────┴──────────────┴──────┴──────────────────┴──────────────────┴──────────┘
```

### Encrypted Data Format

```
┌──────────────────┬──────────────────────┐
│ IV (16 bytes)    │ Encrypted Data       │
│ [from encrypted] │ [ciphertext]         │
└──────────────────┴──────────────────────┘
```

## ⚠️ Fallback Logic

### Trường hợp Decryption Fail

1. **Data quá ngắn** (< 16 bytes)
   - Không có đủ IV → không decrypt được
   - Fallback: Dùng raw data (có thể là garbage)

2. **Data bị truncate**
   - Encrypted data > buffer size → bị cắt
   - Mất IV hoặc một phần data → decrypt fail
   - Fallback: Dùng raw data

3. **Key không đúng**
   - Master key thay đổi → key khác → decrypt fail
   - Fallback: Dùng raw data

### Code Example

```java
try {
    if (nameData.length >= 16) {
        byte[] decryptedName = AESUtility.decrypt(nameData, aesKey);
        String nameStr = new String(decryptedName, StandardCharsets.UTF_8).trim();
        cardInfo.setHolderName(nameStr);
    } else {
        // Data quá ngắn → fallback
        System.err.println("Warning: Encrypted name data too short");
        cardInfo.setHolderName(new String(nameData, StandardCharsets.UTF_8).trim());
    }
} catch (Exception e) {
    // Decryption failed → fallback
    System.err.println("Warning: Failed to decrypt, using raw data");
    cardInfo.setHolderName(new String(nameData, StandardCharsets.UTF_8).trim());
}
```

## 🔐 Security Features

### 1. IV Extraction
- IV được extract từ encrypted data
- IV phải đúng (16 bytes đầu tiên)
- Nếu IV sai → decrypt fail

### 2. Key Derivation
- Key được derive từ Card ID (plaintext)
- Đảm bảo dùng đúng key cho mỗi card

### 3. Error Handling
- Nếu decrypt fail → fallback to raw data
- Log warning để debug
- Không crash application

## ⚠️ Lưu ý

1. **Card ID phải đúng**
   - Card ID dùng để derive key
   - Nếu Card ID sai → key sai → decrypt fail

2. **IV phải có đủ**
   - Encrypted data phải có ít nhất 16 bytes (IV)
   - Nếu < 16 bytes → không decrypt được

3. **Truncation Issue**
   - Nếu data bị truncate → có thể mất IV
   - Fallback logic xử lý trường hợp này

4. **Expiry Date**
   - Expiry chỉ có 8 bytes → không đủ cho IV
   - Có thể không decrypt được
   - Nên lưu plaintext hoặc tăng buffer size

## 📚 Xem thêm

- [AES Overview](./overview.md)
- [Key Derivation](./key-derivation.md)
- [Encryption Flow](./encryption-flow.md)
- [Storage Format](./storage-format.md)

