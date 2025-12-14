# PIN Management - Đổi PIN

## 📋 Tổng quan

Luồng đổi PIN khi user muốn thay đổi PIN hiện tại. Yêu cầu verify PIN cũ trước.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Nhập PIN cũ
     ▼
┌─────────────────┐
│  PinPage.java   │
│  - Verify PIN cũ│
└────┬────────────┘
     │ 2. Verify thành công
     ▼
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 3. Nhập PIN mới
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - Tạo Salt mới         │
│  - Hash PIN mới         │
│  - Gửi lên card         │
└────┬────────────────────┘
     │ 4. Gửi CHANGE_PIN command
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  PinManager.changePin() │
│  - Kiểm tra PIN verified│
│  - Cập nhật Salt mới    │
│  - Cập nhật Hash mới    │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - PinPage.java

```java
// Step 1: Verify PIN cũ
char[] oldPin = oldPinField.getPassword();
boolean verified = simulatorService.verifyPin(oldPin);

if (!verified) {
    showError("PIN cũ không đúng!");
    return;
}

// Step 2: Nhập PIN mới
char[] newPin = newPinField.getPassword();
char[] confirmPin = confirmPinField.getPassword();

// Step 3: Validate PIN mới
if (!Arrays.equals(newPin, confirmPin)) {
    showError("PIN mới không khớp!");
    return;
}

if (newPin.length != 6) {
    showError("PIN phải có 6 chữ số!");
    return;
}

// Step 4: Đổi PIN
boolean success = simulatorService.changePin(oldPin, newPin);

if (success) {
    showSuccess("Đổi PIN thành công!");
} else {
    showError("Đổi PIN thất bại!");
}
```

### 2. Service Layer - SimulatorService.java

```java
public boolean changePin(char[] oldPin, char[] newPin) throws Exception {
    if (!isConnected) return false;
    
    // Yêu cầu: PIN cũ phải được verify trước
    if (!isPinVerified) {
        throw new Exception("Chưa verify PIN cũ");
    }
    
    // 1. Tạo Salt mới (random)
    byte[] newSalt = generateSalt();
    
    // 2. Hash PIN mới với Salt mới
    byte[] newHash = hashPin(newPin, newSalt);
    
    // 3. Build APDU command
    // Format: [CLA] [INS_CHANGE_PIN] [P1] [P2] [LEN] [NEW_SALT (16)] [NEW_HASH (32)]
    int dataLength = newSalt.length + newHash.length; // 48 bytes
    byte[] cmd = new byte[5 + dataLength];
    cmd[0] = 0x00;
    cmd[1] = AppletConstants.INS_CHANGE_PIN; // 0x30
    cmd[2] = 0x00;
    cmd[3] = 0x00;
    cmd[4] = (byte)dataLength;
    
    // Copy Salt và Hash mới vào command
    System.arraycopy(newSalt, 0, cmd, 5, newSalt.length);
    System.arraycopy(newHash, 0, cmd, 5 + newSalt.length, newHash.length);
    
    // 4. Gửi command lên card
    byte[] resp = sendCommand(cmd);
    
    // 5. Kiểm tra response
    if (getSW(resp) == 0x9000) {
        // Reset PIN verified state sau khi đổi
        isPinVerified = false;
        return true;
    }
    return false;
}
```

### 3. Applet Layer - PinManager.java

```java
public void changePin(APDU apdu) {
    // Kiểm tra: PIN cũ phải được verify trước
    if (!pin.isValidated()) {
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }
    
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    short offset = ISO7816.OFFSET_CDATA;
    
    // 1. Đọc và cập nhật Salt mới (16 bytes đầu tiên)
    Util.arrayCopy(buffer, offset, pinSalt, (short)0, AppletConstants.SALT_LENGTH);
    offset += AppletConstants.SALT_LENGTH;
    
    // 2. Đọc và cập nhật Hash mới (32 bytes tiếp theo)
    pin.update(buffer, offset, AppletConstants.PIN_MAX_SIZE);
    
    // 3. Reset trạng thái xác thực sau khi đổi
    // User phải verify PIN mới để tiếp tục sử dụng
    pin.resetAndUnblock();
    
    // Response: Status Word 0x9000 (Success)
}
```

## 📊 Data Format

### APDU Command

```
┌─────┬──────┬─────┬─────┬─────┬──────────────────┬────────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ NEW_SALT (16)    │ NEW_HASH (32)     │
│ 0x00│ 0x30 │0x00 │0x00 │0x30 │ [random bytes]   │ [PBKDF2 output]   │
└─────┴──────┴─────┴─────┴─────┴──────────────────┴────────────────────┘
```

### Response

```
┌─────┬─────┐
│ SW1 │ SW2 │
│ 0x90│0x00 │  Success
└─────┴─────┘
```

## 🔐 Security Features

### 1. Yêu cầu Verify PIN cũ
- Phải verify PIN cũ trước khi đổi
- `pin.isValidated()` phải = true
- Chống đổi PIN trái phép

### 2. Salt mới cho mỗi lần đổi
- Mỗi lần đổi PIN, tạo Salt mới (random)
- Không reuse Salt cũ
- Tăng tính bảo mật

### 3. Reset PIN Verified State
- Sau khi đổi PIN, `isPinVerified` = false
- User phải verify PIN mới để tiếp tục
- Đảm bảo user biết PIN mới

## ⚠️ Lưu ý

1. **PIN cũ phải được verify trước**
   - Nếu chưa verify, card trả về `SW_SECURITY_STATUS_NOT_SATISFIED`
   - Phải gọi `verifyPin()` trước `changePin()`

2. **PIN mới phải khác PIN cũ**
   - Client nên validate trước khi gửi
   - Card không kiểm tra (client responsibility)

3. **Sau khi đổi, phải verify PIN mới**
   - `isPinVerified` được reset về false
   - User phải nhập PIN mới để tiếp tục sử dụng

4. **Salt mới được tạo mỗi lần**
   - Không reuse Salt cũ
   - Tăng tính bảo mật

## 📚 Xem thêm

- [PIN Overview](./overview.md)
- [Create PIN](./create-pin.md)
- [Verify PIN](./verify-pin.md)
- [Reset PIN](./reset-pin.md)

