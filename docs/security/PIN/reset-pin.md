# PIN Management - Reset PIN (Admin)

## 📋 Tổng quan

Luồng reset PIN khi user quên PIN hoặc thẻ bị lock. Chỉ admin mới có quyền reset.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  Admin  │
│  (GUI)  │
└────┬────┘
     │ 1. Nhập Admin Key
     │    + PIN mới
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Tạo Salt mới         │
│  - Hash PIN mới         │
│  - Gửi lên card         │
└────┬────────────────────┘
     │ 2. Gửi RESET_PIN command
     │    [ADMIN_KEY] [NEW_SALT] [NEW_HASH]
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  PinManager.resetPin()  │
│  - Verify Admin Key     │
│  - Cập nhật Salt mới    │
│  - Cập nhật Hash mới    │
│  - Unblock card         │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - SettingsPage.java (Admin)

```java
// Admin nhập thông tin
String adminKey = adminKeyField.getText();
char[] newPin = newPinField.getPassword();

// Validate
if (!adminKey.equals(AppletConstants.ADMIN_KEY_STRING)) {
    showError("Admin key không đúng!");
    return;
}

if (newPin.length != 6) {
    showError("PIN phải có 6 chữ số!");
    return;
}

// Reset PIN
boolean success = simulatorService.resetPin(adminKey, newPin);

if (success) {
    showSuccess("Reset PIN thành công!");
} else {
    showError("Reset PIN thất bại!");
}
```

### 2. Service Layer - SimulatorService.java

```java
public boolean resetPin(String adminKey, char[] newPin) throws Exception {
    if (!isConnected) return false;
    
    // 1. Convert admin key string to bytes
    byte[] adminKeyBytes = adminKey.getBytes(StandardCharsets.UTF_8);
    
    // 2. Tạo Salt mới (random)
    byte[] newSalt = generateSalt();
    
    // 3. Hash PIN mới với Salt mới
    byte[] newHash = hashPin(newPin, newSalt);
    
    // 4. Build APDU command
    // Format: [CLA] [INS_RESET_PIN] [P1] [P2] [LEN] 
    //         [ADMIN_KEY (4)] [NEW_SALT (16)] [NEW_HASH (32)]
    int dataLength = adminKeyBytes.length + newSalt.length + newHash.length;
    byte[] cmd = new byte[5 + dataLength];
    cmd[0] = 0x00;
    cmd[1] = AppletConstants.INS_RESET_PIN; // 0xA0
    cmd[2] = 0x00;
    cmd[3] = 0x00;
    cmd[4] = (byte)dataLength;
    
    int offset = 5;
    // Copy Admin Key
    System.arraycopy(adminKeyBytes, 0, cmd, offset, adminKeyBytes.length);
    offset += adminKeyBytes.length;
    // Copy Salt mới
    System.arraycopy(newSalt, 0, cmd, offset, newSalt.length);
    offset += newSalt.length;
    // Copy Hash mới
    System.arraycopy(newHash, 0, cmd, offset, newHash.length);
    
    // 5. Gửi command lên card
    byte[] resp = sendCommand(cmd);
    
    // 6. Kiểm tra response
    if (getSW(resp) == 0x9000) {
        // Reset PIN verified state
        isPinVerified = false;
        pinTriesRemaining = 3;
        return true;
    }
    return false;
}
```

### 3. Applet Layer - PinManager.java

```java
public void resetPin(APDU apdu) {
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    short offset = ISO7816.OFFSET_CDATA;
    
    // 1. Verify Admin Key
    if (Util.arrayCompare(buffer, offset,
            AppletConstants.ADMIN_KEY, (short)0,
            (short)AppletConstants.ADMIN_KEY.length) != 0) {
        // Admin key không đúng
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }
    offset += AppletConstants.ADMIN_KEY.length;
    
    // 2. Đọc và cập nhật Salt mới (16 bytes)
    Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
    offset += AppletConstants.SALT_LENGTH;
    
    // 3. Đọc và cập nhật Hash mới (32 bytes)
    pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);
    
    // 4. Unblock card và reset tries
    pin.resetAndUnblock();
    
    // Response: Status Word 0x9000 (Success)
}
```

## 📊 Data Format

### APDU Command

```
┌─────┬──────┬─────┬─────┬─────┬──────────────┬──────────────────┬────────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ ADMIN_KEY(4) │ NEW_SALT (16)    │ NEW_HASH (32)     │
│ 0x00│ 0xA0 │0x00 │0x00 │0x34 │ [0x41,0x44, │ [random bytes]   │ [PBKDF2 output]   │
│     │      │     │     │     │  0x4D,0x49] │                  │                   │
└─────┴──────┴─────┴─────┴─────┴──────────────┴──────────────────┴────────────────────┘
```

### Response

```
┌─────┬─────┐
│ SW1 │ SW2 │
│ 0x90│0x00 │  Success
└─────┴─────┘
```

## 🔐 Security Features

### 1. Admin Key Protection
- Admin key được hardcode trong applet: `{0x41, 0x44, 0x4D, 0x49}` ("ADMI")
- Chỉ admin biết key mới reset được
- Chống reset PIN trái phép

### 2. Unblock Card
- Sau khi reset, card được unblock
- Tries remaining được reset về 3
- User có thể sử dụng PIN mới ngay

### 3. Salt mới
- Mỗi lần reset, tạo Salt mới (random)
- Không reuse Salt cũ
- Tăng tính bảo mật

## ⚠️ Lưu ý

1. **Admin Key phải bảo mật**
   - Hiện tại hardcode trong applet (không an toàn cho production)
   - Nên dùng secure key management
   - Admin key không được lộ ra ngoài

2. **Reset PIN không cần verify PIN cũ**
   - Khác với `changePin()`, reset không cần PIN cũ
   - Dùng khi user quên PIN hoặc thẻ bị lock

3. **Sau khi reset, phải verify PIN mới**
   - `isPinVerified` được reset về false
   - User phải nhập PIN mới để tiếp tục

4. **Admin Key trong Production**
   - Không nên hardcode
   - Nên dùng secure key storage
   - Có thể dùng key derivation từ master key

## 🔑 Admin Key

### Current Implementation
```java
// AppletConstants.java
public static final byte[] ADMIN_KEY = {
    (byte)0x41, // 'A'
    (byte)0x44, // 'D'
    (byte)0x4D, // 'M'
    (byte)0x49  // 'I'
};
```

### Security Concerns
- ⚠️ Hardcoded trong code (không an toàn)
- ⚠️ Dễ bị reverse engineer
- ✅ Nên dùng secure key management trong production

## 📚 Xem thêm

- [PIN Overview](./overview.md)
- [Create PIN](./create-pin.md)
- [Verify PIN](./verify-pin.md)
- [Change PIN](./change-pin.md)

