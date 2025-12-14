# PIN Management - Tạo PIN

## 📋 Tổng quan

Luồng tạo PIN mới khi khởi tạo thẻ hoặc reset thẻ.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Nhập PIN mới
     ▼
┌─────────────────┐
│  PinPage.java   │
│  - Nhận PIN     │
│  - Validate     │
└────┬────────────┘
     │ 2. Tạo Salt (random 16 bytes)
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - generateSalt()       │
│  - hashPin(PIN, salt)   │
│  - PBKDF2-SHA256        │
└────┬────────────────────┘
     │ 3. Gửi [SALT + HASH] lên card
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  PinManager.createPin() │
│  - Lưu Salt             │
│  - Lưu Hash             │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - PinPage.java

```java
// User nhập PIN
char[] pinChars = pinField.getPassword();

// Gọi SimulatorService để tạo PIN
simulatorService.createPin(pinChars);
```

### 2. Service Layer - SimulatorService.java

```java
public void createPin(char[] pinChars) throws Exception {
    if (!isConnected) return;
    
    // 1. Tạo Salt ngẫu nhiên (16 bytes)
    byte[] salt = generateSalt();
    
    // 2. Hash PIN với Salt
    byte[] hash = hashPin(pinChars, salt);
    
    // 3. Build APDU command
    // Format: [CLA] [INS_CREATE_PIN] [P1] [P2] [LEN] [SALT (16)] [HASH (32)]
    int dataLength = salt.length + hash.length; // 48 bytes
    byte[] cmd = new byte[5 + dataLength];
    cmd[0] = 0x00;
    cmd[1] = AppletConstants.INS_CREATE_PIN; // 0x10
    cmd[2] = 0x00;
    cmd[3] = 0x00;
    cmd[4] = (byte)dataLength;
    
    // Copy Salt và Hash vào command
    System.arraycopy(salt, 0, cmd, 5, salt.length);
    System.arraycopy(hash, 0, cmd, 5 + salt.length, hash.length);
    
    // 4. Gửi command lên card
    byte[] resp = sendCommand(cmd);
    
    // 5. Kiểm tra response
    if (getSW(resp) != 0x9000) {
        throw new Exception("Failed to create PIN");
    }
}
```

### 3. Applet Layer - PinManager.java

```java
public void createPin(APDU apdu) {
    // Kiểm tra: PIN chưa được tạo
    if (pin.isValidated()) {
        ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
    }
    
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    short offset = ISO7816.OFFSET_CDATA;
    
    // 1. Đọc và lưu Salt (16 bytes đầu tiên)
    Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
    offset += AppletConstants.SALT_LENGTH;
    
    // 2. Đọc và lưu Hash (32 bytes tiếp theo)
    pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);
    
    // Response: Status Word 0x9000 (Success)
}
```

## 📊 Data Format

### APDU Command

```
┌─────┬──────┬─────┬─────┬─────┬──────────────────┬────────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ SALT (16 bytes)  │ HASH (32 bytes)   │
│ 0x00│ 0x10 │0x00 │0x00 │0x30 │ [random bytes]   │ [PBKDF2 output]   │
└─────┴──────┴─────┴─────┴─────┴──────────────────┴────────────────────┘
```

### Response

```
┌─────┬─────┐
│ SW1 │ SW2 │
│ 0x90│0x00 │  Success
└─────┴─────┘
```

## 🔐 Hashing Process

### Input
- PIN: `char[]` (ví dụ: "123456")
- Salt: `byte[16]` (random)

### Process
```java
KeySpec spec = new PBEKeySpec(
    pin,           // PIN plaintext
    salt,          // 16 bytes random salt
    10000,         // PBKDF2 iterations
    256            // Hash length (bits) = 32 bytes
);

SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
byte[] hash = factory.generateSecret(spec).getEncoded();
```

### Output
- Hash: `byte[32]` (256-bit SHA-256 hash)

## ⚠️ Lưu ý

1. **PIN chỉ được tạo 1 lần**
   - Nếu PIN đã tồn tại, card trả về `SW_COMMAND_NOT_ALLOWED`
   - Để tạo lại, cần reset PIN (Admin)

2. **Salt phải random**
   - Mỗi PIN có salt riêng
   - Không được reuse salt

3. **PIN không được gửi plaintext**
   - Chỉ gửi hash lên card
   - PIN plaintext chỉ tồn tại trong memory tạm thời

4. **Default PIN**
   - Khi tạo thẻ mới, có thể tạo PIN mặc định "000000"
   - User nên đổi PIN ngay sau khi nhận thẻ

## 📚 Xem thêm

- [PIN Overview](./overview.md)
- [Verify PIN](./verify-pin.md)
- [Change PIN](./change-pin.md)
- [Reset PIN](./reset-pin.md)

