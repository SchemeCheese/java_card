# Bảo mật Lưu trữ và Xác thực PIN

## ✅ ĐÚNG: Lưu và Verify PIN trên Thẻ (Applet)

### Cách hoạt động:

```
┌─────────────────────────────────────────────────────────┐
│  1. Tạo PIN                                             │
│     User nhập PIN → GUI                                 │
│     GUI hash PIN (PBKDF2) → Gửi hash lên thẻ            │
│     Thẻ lưu PIN hash trong EEPROM                       │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  2. Verify PIN                                          │
│     User nhập PIN → GUI                                 │
│     GUI lấy Salt từ thẻ                                 │
│     GUI hash PIN với Salt → Gửi hash lên thẻ           │
│     Thẻ so sánh hash với hash đã lưu                    │
│     Thẻ trả về: Success/Fail                            │
└─────────────────────────────────────────────────────────┘
```

### Ưu điểm:

1. **PIN hash không rời khỏi thẻ**
   - PIN hash chỉ tồn tại trong EEPROM của thẻ
   - Không thể đọc PIN hash từ bên ngoài

2. **Không thể verify PIN mà không có thẻ**
   - Attacker không thể verify PIN từ xa
   - Cần có thẻ vật lý mới verify được

3. **Chống tấn công từ xa**
   - Database bị hack → Không ảnh hưởng (PIN hash không ở đó)
   - Server bị hack → Không thể verify PIN

4. **Bảo vệ bằng phần cứng**
   - EEPROM có bảo vệ vật lý
   - Khó đọc dữ liệu trực tiếp từ chip

## ❌ SAI: Lưu và Verify PIN trên Server

### Cách hoạt động (KHÔNG AN TOÀN):

```
┌─────────────────────────────────────────────────────────┐
│  1. Tạo PIN                                             │
│     User nhập PIN → GUI                                 │
│     Server hash PIN → Lưu vào database                  │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  2. Verify PIN                                          │
│     User nhập PIN → GUI                                 │
│     GUI gửi PIN → Server                                │
│     Server hash PIN → So sánh với database              │
│     Server trả về: Success/Fail                         │
└─────────────────────────────────────────────────────────┘
```

### Rủi ro:

1. **Database bị hack**
   - Attacker có PIN hash
   - Có thể brute force PIN
   - Có thể verify PIN mà không cần thẻ

2. **Tấn công từ xa**
   - Không cần thẻ vật lý
   - Có thể tạo thẻ giả với PIN hash đã biết

3. **Không có bảo vệ phần cứng**
   - PIN hash lưu trong database (dễ truy cập)
   - Không có bảo vệ vật lý

## 📊 So sánh

| Tiêu chí | Verify trên Thẻ ✅ | Verify trên Server ❌ |
|----------|-------------------|----------------------|
| **PIN hash lưu ở đâu** | Thẻ (EEPROM) | Database |
| **Cần thẻ vật lý** | ✅ Có | ❌ Không |
| **Chống tấn công từ xa** | ✅ Có | ❌ Không |
| **Database bị hack** | ✅ An toàn | ❌ Nguy hiểm |
| **Bảo vệ phần cứng** | ✅ Có | ❌ Không |
| **Bảo mật** | ✅ Cao | ❌ Thấp |

## 🔐 Code Implementation

### ✅ ĐÚNG: Verify trên thẻ

```java
// SimulatorService.java
public boolean verifyPin(char[] pinChars) throws Exception {
    if (!isConnected) return false;
    
    // 1. Lấy Salt từ thẻ
    byte[] salt = getSaltFromCard();
    
    // 2. Hash PIN với Salt
    byte[] hash = hashPin(pinChars, salt);
    
    // 3. Gửi hash lên thẻ để verify
    byte[] resp = sendCommand(INS_VERIFY_PIN, hash);
    
    // 4. Thẻ verify và trả về kết quả
    return resp[0] == 0x01;  // Success
}
```

### ❌ SAI: Verify trên server

```java
// KHÔNG NÊN LÀM
public boolean verifyPin(String pin) {
    // Lấy PIN hash từ database
    String pinHash = card.getPinHash();
    
    // Hash PIN và so sánh
    String computedHash = hashPin(pin, card.getPinSalt());
    return pinHash.equals(computedHash);  // ❌ KHÔNG AN TOÀN
}
```

## 🎯 Kết luận

**Câu trả lời: ĐÚNG!**

✅ **An toàn nhất**: Lưu PIN hash trên thẻ và verify PIN trên thẻ

**Lý do:**
1. PIN hash không rời khỏi thẻ
2. Không thể verify PIN mà không có thẻ
3. Chống tấn công từ xa
4. Có bảo vệ phần cứng

**PIN hash trên server:**
- ⚠️ Chỉ nên dùng cho admin reset PIN (khi mất thẻ)
- ❌ KHÔNG nên dùng để verify PIN thông thường

