# Cập nhật Bảo mật Server và Database

## 📋 Tổng quan

Cập nhật server và database để tuân thủ nguyên tắc bảo mật:
- **PIN verification**: Chỉ trên thẻ (applet), không trên server
- **PIN hash trên server**: Chỉ dùng cho admin reset PIN khi mất thẻ

## 🔄 Thay đổi

### 1. Routes (`server/routes/pinRoutes.js`)

#### ✅ Thêm mới:
- `PUT /api/pin/admin/reset/:studentId` - Admin reset PIN (với admin key)

#### ❌ Đã xóa:
- `POST /api/pin/verify` - Verify PIN trên server (ĐÃ XÓA - KHÔNG AN TOÀN)
  - Đã xóa vì không an toàn
  - Phải dùng card-based verification (SimulatorService.verifyPin)

#### ✅ Giữ nguyên:
- `POST /api/pin/change` - Đổi PIN (cần cập nhật để sync với thẻ)
- `GET /api/pin/tries/:studentId` - Lấy số lần thử còn lại
- `POST /api/pin/reset-tries/:studentId` - Reset số lần thử

### 2. Controllers (`server/controllers/pinController.js`)

#### `verifyPin()` - ⚠️ DEPRECATED
```javascript
// ⚠️ SECURITY WARNING: Verify PIN trên server KHÔNG AN TOÀN
// ✅ Nên dùng: SimulatorService.verifyPin() - verify trên thẻ
```

#### `resetPin()` - ✅ ADMIN FUNCTION
```javascript
// ✅ ĐÚNG: Admin reset PIN khi mất thẻ
// Yêu cầu: adminKey trong request body
// PIN hash trên server được cập nhật để admin có thể reset lại
```

### 3. Database Schema

**KHÔNG CẦN THAY ĐỔI** - PIN hash vẫn cần lưu trên server để:
- Admin reset PIN khi mất thẻ
- Backup/restore
- Không dùng để verify PIN thông thường

## 🔐 Cấu hình

### Environment Variables

Thêm vào `.env`:

```env
# Admin secret key for PIN reset
ADMIN_SECRET_KEY=your_secure_admin_key_here_change_in_production
```

**Lưu ý**: 
- Thay đổi giá trị mặc định trong production
- Không commit `.env` vào git
- Dùng strong random key (ít nhất 32 ký tự)

## 📝 API Endpoints

### 1. Admin Reset PIN (MỚI)

```http
PUT /api/pin/admin/reset/:studentId
Content-Type: application/json

{
  "newPin": "123456",
  "adminKey": "your_admin_secret_key"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Reset PIN thành công",
  "data": {
    "studentId": "CT060132",
    "note": "User cần cập nhật PIN trên thẻ mới sau khi nhận thẻ..."
  }
}
```

### 2. Verify PIN (DEPRECATED)

```http
POST /api/pin/verify
Content-Type: application/json

{
  "studentId": "CT060132",
  "pin": "123456"
}
```

**⚠️ Warning**: Endpoint này sẽ log warning về bảo mật. Nên dùng card-based verification.

### 3. Change PIN

```http
POST /api/pin/change
Content-Type: application/json

{
  "studentId": "CT060132",
  "oldPin": "123456",
  "newPin": "654321"
}
```

**⚠️ Lưu ý**: Endpoint này chỉ cập nhật PIN trên server. Cần cập nhật PIN trên thẻ riêng.

## 🎯 Best Practices

### ✅ ĐÚNG: Verify PIN trên thẻ

```java
// GUI code
SimulatorService service = new SimulatorService();
service.connectToSimulator();
boolean verified = service.verifyPin(pin.toCharArray());
```

### ❌ SAI: Verify PIN trên server

```javascript
// KHÔNG NÊN DÙNG
POST /api/pin/verify
{
  "studentId": "...",
  "pin": "..."
}
```

### ✅ ĐÚNG: Admin reset PIN

```javascript
// Khi user mất thẻ, admin có thể reset PIN
PUT /api/pin/admin/reset/:studentId
{
  "newPin": "123456",
  "adminKey": "admin_secret_key"
}
```

## 📊 Luồng hoạt động

### Verify PIN (ĐÚNG)

```
User nhập PIN
    ↓
GUI → Thẻ: APDU INS_VERIFY_PIN
    ↓
Thẻ verify PIN (so sánh hash)
    ↓
Thẻ trả về: Success/Fail
    ↓
GUI nhận kết quả
```

### Admin Reset PIN (ĐÚNG)

```
Admin request reset PIN
    ↓
Server verify admin key
    ↓
Server update PIN hash trong database
    ↓
User nhận thẻ mới → Verify PIN trên thẻ
```

## 🔒 Bảo mật

### PIN Hash trên Server

| Mục đích | Có nên lưu? | Lý do |
|----------|------------|-------|
| **Admin reset PIN** | ✅ CÓ | Cần để admin reset khi mất thẻ |
| **Backup/Restore** | ✅ CÓ | Cần để khôi phục thẻ |
| **Verify PIN thông thường** | ❌ KHÔNG | Nên verify trên thẻ |

### Admin Key

- **Lưu trong**: Environment variable (`ADMIN_SECRET_KEY`)
- **Độ dài**: Ít nhất 32 ký tự
- **Bảo mật**: Không commit vào git
- **Rotation**: Định kỳ thay đổi

## ✅ Checklist

- [x] Thêm endpoint admin reset PIN
- [x] Thêm admin key verification
- [x] Thêm deprecation warning cho verify endpoint
- [x] Cập nhật documentation
- [ ] Thêm admin key vào `.env.example`
- [ ] Cập nhật GUI để không dùng server verify endpoint
- [ ] Test admin reset PIN flow

## 🚀 Migration

Không cần migration database vì:
- PIN hash vẫn cần lưu trên server (cho admin reset)
- Chỉ thay đổi cách sử dụng (không verify trên server)

## 📚 Tài liệu liên quan

- `PIN_SECURITY_GUIDE.md` - Hướng dẫn bảo mật PIN
- `PIN_STORAGE_SECURITY.md` - Bảo mật lưu trữ PIN

