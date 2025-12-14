# PIN Management - Xác thực PIN

## 📋 Tổng quan

Luồng xác thực PIN khi user sử dụng thẻ. PIN được verify trên card, không trên server.

## 🔄 Luồng hoạt động

```
┌─────────┐
│  User   │
│  (GUI)  │
└────┬────┘
     │ 1. Nhập PIN
     ▼
┌─────────────────┐
│  PinPage.java   │
│  - Nhận PIN     │
└────┬────────────┘
     │ 2. Lấy Salt từ card
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - getSalt()            │
│  - hashPin(PIN, salt)   │
│  - verifyPin(hash)      │
└────┬────────────────────┘
     │ 3. Gửi GET_SALT command
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  PinManager.getSalt()   │
│  - Trả về Salt          │
└────┬────────────────────┘
     │ 4. Nhận Salt
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - hashPin(PIN, salt)   │
│  - Gửi HASH lên card    │
└────┬────────────────────┘
     │ 5. Gửi VERIFY_PIN command
     ▼
┌─────────────────────────┐
│  Card (Applet)          │
│  PinManager.verifyPin() │
│  - So sánh hash         │
│  - Trả về kết quả       │
└────┬────────────────────┘
     │ 6. Response: Success/Fail
     ▼
┌─────────────────────────┐
│  SimulatorService.java  │
│  - isPinVerified = true │
└─────────────────────────┘
```

## 💻 Code Flow

### 1. GUI Layer - PinPage.java

```java
// User nhập PIN
char[] pinChars = pinField.getPassword();

// Gọi SimulatorService để verify
boolean success = simulatorService.verifyPin(pinChars);

if (success) {
    // PIN đúng → Cho phép truy cập
    showMainPage();
} else {
    // PIN sai → Hiển thị lỗi
    int triesRemaining = simulatorService.getPinTriesRemaining();
    showError("PIN sai! Còn " + triesRemaining + " lần thử");
}
```

### 2. Service Layer - SimulatorService.java

```java
public boolean verifyPin(char[] pinChars) throws Exception {
    if (!isConnected) return false;
    
    // Step 1: Lấy Salt từ card
    byte[] getSaltCmd = {
        0x00, 
        AppletConstants.INS_GET_SALT, // 0x22
        0x00, 
        0x00, 
        0x00
    };
    byte[] saltResp = sendCommand(getSaltCmd);
    
    if (getSW(saltResp) != 0x9000) {
        return false;
    }
    
    // Extract Salt (16 bytes đầu tiên)
    byte[] salt = Arrays.copyOf(saltResp, AppletConstants.SALT_LENGTH);
    
    // Step 2: Hash PIN với Salt
    byte[] hash = hashPin(pinChars, salt);
    
    // Step 3: Gửi hash lên card để verify
    byte[] verifyCmd = new byte[5 + hash.length];
    verifyCmd[0] = 0x00;
    verifyCmd[1] = AppletConstants.INS_VERIFY_PIN; // 0x20
    verifyCmd[2] = 0x00;
    verifyCmd[3] = 0x00;
    verifyCmd[4] = (byte)hash.length; // 32
    System.arraycopy(hash, 0, verifyCmd, 5, hash.length);
    
    byte[] resp = sendCommand(verifyCmd);
    
    // Step 4: Kiểm tra response
    if (getSW(resp) == 0x9000 && resp.length > 2) {
        if (resp[0] == 0x01) {
            // PIN đúng
            isPinVerified = true;
            pinTriesRemaining = 3;
            return true;
        } else {
            // PIN sai
            pinTriesRemaining = resp[1] & 0xFF;
            return false;
        }
    }
    return false;
}
```

### 3. Applet Layer - PinManager.java

```java
public void verifyPin(APDU apdu) {
    byte[] buffer = apdu.getBuffer();
    apdu.setIncomingAndReceive();
    
    // Client đã hash PIN với Salt và gửi hash lên
    // Card chỉ cần so sánh hash với hash đã lưu
    if (pin.check(buffer, ISO7816.OFFSET_CDATA, AppletConstants.PIN_MAX_SIZE)) {
        // Hash khớp → PIN đúng
        buffer[0] = (byte)0x01; // Success flag
        apdu.setOutgoingAndSend((short)0, (short)1);
    } else {
        // Hash không khớp → PIN sai
        // OwnerPIN tự động giảm tries remaining
        buffer[0] = (byte)0x00; // Fail flag
        buffer[1] = pin.getTriesRemaining(); // Số lần thử còn lại
        apdu.setOutgoingAndSend((short)0, (short)2);
    }
}
```

## 📊 Data Format

### Step 1: GET_SALT Command

```
┌─────┬──────┬─────┬─────┬─────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │
│ 0x00│ 0x22 │0x00 │0x00 │0x00 │
└─────┴──────┴─────┴─────┴─────┘
```

### Step 1: GET_SALT Response

```
┌──────────────────┬─────┬─────┐
│ SALT (16 bytes)  │ SW1 │ SW2 │
│ [stored on card] │ 0x90│0x00 │
└──────────────────┴─────┴─────┘
```

### Step 2: VERIFY_PIN Command

```
┌─────┬──────┬─────┬─────┬─────┬────────────────────┐
│ CLA │ INS  │ P1  │ P2  │ LEN │ HASH (32 bytes)    │
│ 0x00│ 0x20 │0x00 │0x00 │0x20 │ [PBKDF2 output]   │
└─────┴──────┴─────┴─────┴─────┴────────────────────┘
```

### Step 2: VERIFY_PIN Response (Success)

```
┌─────┬─────┬─────┐
│ FLAG│ SW1 │ SW2 │
│ 0x01│ 0x90│0x00 │  Success
└─────┴─────┴─────┘
```

### Step 2: VERIFY_PIN Response (Fail)

```
┌─────┬─────────────┬─────┬─────┐
│ FLAG│ TRIES LEFT  │ SW1 │ SW2 │
│ 0x00│   (1 byte)  │ 0x90│0x00 │  Fail
└─────┴─────────────┴─────┴─────┘
```

## 🔐 Security Features

### 1. Salt-based Hashing
- Mỗi lần verify, client phải lấy salt từ card
- Salt không được cache trên client
- Chống rainbow table attacks

### 2. Tries Limit
- Tối đa 3 lần thử
- Sau 3 lần sai, card bị lock
- Cần admin reset hoặc cấp thẻ mới

### 3. PIN Hash không rời khỏi card
- Client chỉ gửi hash, không gửi PIN
- Card verify hash, không gửi hash về client
- PIN plaintext chỉ tồn tại trong memory tạm thời

### 4. On-card Verification
- PIN được verify trên card, không trên server
- Không thể verify PIN mà không có thẻ vật lý
- Chống tấn công từ xa

## ⚠️ Lưu ý

1. **PIN phải được verify trước khi truy cập dữ liệu**
   - Các operations như `setCardInfo()`, `borrowBook()` yêu cầu PIN verified
   - `isPinVerified` flag được set sau khi verify thành công

2. **Tries Remaining**
   - Sau mỗi lần sai, tries giảm 1
   - Khi tries = 0, card bị lock
   - Cần admin reset hoặc cấp thẻ mới

3. **PIN không được lưu trên server**
   - Server không thể verify PIN
   - Tất cả verification đều trên card

## 📚 Xem thêm

- [PIN Overview](./overview.md)
- [Create PIN](./create-pin.md)
- [Change PIN](./change-pin.md)
- [Reset PIN](./reset-pin.md)

