# Hướng dẫn Bảo mật PIN

## ⚠️ Vấn đề hiện tại

Hiện tại hệ thống có **2 nơi verify PIN**:
1. ✅ **Trên thẻ (applet)** - AN TOÀN
2. ❌ **Trên server** - KHÔNG AN TOÀN

## 🔒 Nguyên tắc bảo mật

### ✅ ĐÚNG: Verify PIN trên thẻ

```
User nhập PIN
    ↓
GUI → Thẻ: APDU INS_VERIFY_PIN
    ↓
Thẻ verify PIN (so sánh hash)
    ↓
Thẻ trả về: Success/Fail
```

**Ưu điểm:**
- PIN hash không rời khỏi thẻ
- Không thể verify PIN mà không có thẻ
- Chống tấn công từ xa

### ❌ SAI: Verify PIN trên server

```
User nhập PIN
    ↓
GUI → Server: POST /api/pin/verify
    ↓
Server verify PIN (so sánh với pin_hash trong DB)
    ↓
Server trả về: Success/Fail
```

**Rủi ro:**
- Nếu database bị hack → Attacker có PIN hash
- Có thể verify PIN mà không cần thẻ
- Có thể brute force PIN hash
- Tạo thẻ giả với PIN hash đã biết

## 📋 Khuyến nghị

### 1. PIN Hash trên Server - CHỈ DÙNG CHO:

#### ✅ Admin Reset PIN (khi mất thẻ)
```javascript
// Admin có thể reset PIN khi user mất thẻ
PUT /api/cards/:studentId/reset-pin
{
  "newPin": "123456",
  "adminKey": "ADMIN_SECRET_KEY"
}
```

#### ✅ Backup/Restore
- Khi restore thẻ mới, cần PIN hash để khôi phục
- Chỉ dùng trong trường hợp khẩn cấp

#### ❌ KHÔNG DÙNG để verify PIN thông thường
- User phải có thẻ vật lý để verify PIN
- Verify PIN chỉ được thực hiện trên thẻ

### 2. Cải thiện Code

#### Option 1: Xóa endpoint verify PIN trên server (KHUYẾN NGHỊ)

```javascript
// ❌ XÓA endpoint này
// exports.verifyPin = async (req, res) => { ... }

// ✅ CHỈ GIỮ lại cho admin reset
exports.resetPin = async (req, res) => {
  // Verify admin key
  // Reset PIN hash trên server
  // User phải verify PIN trên thẻ sau khi reset
}
```

#### Option 2: Deprecate và thêm warning

```javascript
// ⚠️ DEPRECATED - Chỉ dùng cho legacy
// PIN verification nên được thực hiện trên thẻ
exports.verifyPin = async (req, res) => {
  console.warn('⚠️ WARNING: PIN verification on server is deprecated');
  // ... existing code
}
```

### 3. Cập nhật GUI

Đảm bảo GUI **LUÔN** verify PIN trên thẻ:

```java
// ✅ ĐÚNG: Verify trên thẻ
SimulatorService service = new SimulatorService();
service.connectToSimulator();
boolean verified = service.verifyPin(pin.toCharArray());

// ❌ SAI: Verify trên server
// CardApiService.verifyPin(studentId, pin); // KHÔNG DÙNG
```

## 🔐 Best Practices

### 1. PIN Hash Storage

| Nơi lưu | Mục đích | Bảo mật |
|---------|----------|---------|
| **Thẻ (applet)** | Verify PIN thông thường | ✅ Cao |
| **Server (DB)** | Admin reset, backup | ⚠️ Trung bình |

### 2. PIN Verification Flow

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │ Nhập PIN
       ▼
┌─────────────┐
│   GUI       │
└──────┬──────┘
       │ APDU: INS_VERIFY_PIN
       ▼
┌─────────────┐
│   Thẻ       │ ← Verify PIN ở đây
│  (Applet)   │
└──────┬──────┘
       │ Success/Fail
       ▼
┌─────────────┐
│   GUI       │
└─────────────┘
```

### 3. Admin Reset PIN Flow

```
┌─────────────┐
│   Admin     │
└──────┬──────┘
       │ Reset PIN request
       ▼
┌─────────────┐
│   Server    │ ← Verify admin key
│  (Database) │ ← Update PIN hash
└──────┬──────┘
       │ New PIN hash
       ▼
┌─────────────┐
│   Thẻ       │ ← User phải verify PIN mới trên thẻ
│  (Applet)   │
└─────────────┘
```

## 📝 Checklist

- [ ] Xóa hoặc deprecate endpoint `POST /api/pin/verify`
- [ ] Đảm bảo GUI chỉ verify PIN trên thẻ
- [ ] Thêm admin endpoint để reset PIN (với admin key)
- [ ] Document rõ ràng: PIN hash trên server chỉ dùng cho admin reset
- [ ] Thêm logging cho admin reset PIN operations
- [ ] Encrypt PIN hash trong database (optional, nhưng tốt hơn)

## 🎯 Kết luận

**PIN hash trên database:**
- ✅ **NÊN** lưu để admin có thể reset PIN khi mất thẻ
- ❌ **KHÔNG NÊN** dùng để verify PIN thông thường
- ✅ **NÊN** verify PIN trên thẻ (applet) để đảm bảo bảo mật

**Luồng đúng:**
1. User nhập PIN → GUI
2. GUI gửi PIN hash → Thẻ (APDU)
3. Thẻ verify PIN → Trả về kết quả
4. GUI nhận kết quả từ thẻ

**Luồng sai:**
1. User nhập PIN → GUI
2. GUI gửi PIN → Server
3. Server verify PIN → Trả về kết quả ❌

