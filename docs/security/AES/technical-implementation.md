# Chi Tiết Kỹ Thuật: Mã Hóa AES trong Hệ Thống Thẻ Thư Viện

Tài liệu này mô tả chi tiết cách thức mã hóa AES được cài đặt và vận hành trong hệ thống, bao gồm quy trình tạo khóa, mã hóa, truyền nhận và lưu trữ dữ liệu.

## 1. Tổng Quan Kiến Trúc

Khác với mô hình mã hóa truyền thống nơi Server hoặc Smartcard thực hiện mã hóa, hệ thống này áp dụng mô hình **Client-Side Encryption** (Mã hóa tại máy trạm).

*   **Client (Java Swing App)**: Đóng vai trò chính, thực hiện toàn bộ việc sinh khóa, trích xuất khóa, mã hóa và giải mã dữ liệu.
*   **JavaCard (Applet)**: Đóng vai trò "Kho lưu trữ an toàn" (Secure Storage). Thẻ lưu trữ dữ liệu dưới dạng byte đã mã hóa (ciphertext) mà không cần hiểu nội dung hoặc giữ khóa giải mã.

## 2. Thông Số Kỹ Thuật (Cryptographic Specifications)

### 2.1. Algorthims
*   **Mã hóa dữ liệu**: `AES/CBC/PKCS5Padding` (128-bit).
*   **Sinh khóa (Key Derivation)**: `PBKDF2WithHmacSHA256`.
*   **Tạo số ngẫu nhiên**: `SecureRandom` (Java Standard).

### 2.2. Key Parameters
*   **Key Size**: 128 bits (16 bytes).
*   **IV Size**: 16 bytes (Random cho mỗi lần mã hóa).
*   **PBKDF2 Iterations**: 10,000 vòng lặp.
*   **Salt**: Sử dụng chính `Student ID` (mã sinh viên) làm salt/discriminator.

## 3. Quy Trình Quản Lý Khóa (Key Management)

Hệ thống không lưu trữ khóa AES tĩnh. Khóa được tái tạo (derive) mỗi khi cần sử dụng dựa trên một "bí mật chung" (Master Key) và danh tính người dùng.

### 3.1. Master Key
*   **Định nghĩa**: Một chuỗi bí mật cấp hệ thống (`LibraryCardMasterKey2024!`).
*   **Code Reference**:
    *   **File**: `card_gui/src/utils/AESUtility.java`
    *   **Function**: `getMasterKey()`

### 3.2. Quy trình sinh khóa (Key Derivation)
Mỗi sinh viên có một khóa AES riêng biệt. Quy trình tái tạo khóa như sau:

*   **File**: `card_gui/src/utils/AESUtility.java`
*   **Function**: `deriveKey(String masterKey, String cardId)`
*   **Logic**:
    ```java
    SecretKey key = PBKDF2(
        Password: "LibraryCardMasterKey2024!", 
        Salt: cardId,           // Ví dụ: B19DCCN001
        Iterations: 10000,
        OutputLength: 128 bits
    );
    ```

## 4. Quy Trình Dữ Liệu (Data Flow)

### 4.1. Ghi và Mã Hóa (Write Flow)
Khi cập nhật thông tin thẻ (Ví dụ: Tên sinh viên).

**Bước 1: Client chuẩn bị và mã hóa dữ liệu**
*   **File**: `card_gui/src/service/SimulatorService.java`
*   **Function**: `setCardInfo(CardInfo cardInfo)`
*   **Chi tiết**:
    1.  Lấy `cardId` từ object `CardInfo`.
    2.  Gọi `AESUtility.deriveKey(...)` để tạo khóa.
    3.  Gọi `AESUtility.encrypt(data, key)` để mã hóa Tên và Ngày hết hạn.
    4.  Đóng gói lệnh APDU `INS_SET_CARD_INFO`.

**Bước 2: Client gửi lệnh APDU**
*   **File**: `card_gui/src/service/SimulatorService.java`
*   **Function**: `sendCommand(byte[] cmd)`

**Bước 3: Applet nhận và lưu dữ liệu**
*   **File**: `card_gui/src/applet/CardInfoManager.java`
*   **Function**: `setCardInfo(APDU apdu, PinManager pinManager)`
*   **Chi tiết**:
    1.  Nhận buffer từ APDU.
    2.  Copy dữ liệu đã mã hóa vào mảng byte `holderName` và `expiryDate` trong EEPROM.

### 4.2. Đọc và Giải Mã (Read Flow)
Khi đọc thẻ để hiển thị thông tin.

**Bước 1: Client gửi yêu cầu đọc**
*   **File**: `card_gui/src/service/SimulatorService.java`
*   **Function**: `getCardInfo()` (gửi lệnh `INS_GET_CARD_INFO`)

**Bước 2: Applet trả về dữ liệu thô**
*   **File**: `card_gui/src/applet/CardInfoManager.java`
*   **Function**: `getCardInfo(APDU apdu, byte numBorrowedBooks)`

**Bước 3: Client giải mã dữ liệu**
*   **File**: `card_gui/src/service/SimulatorService.java`
*   **Function**: `getCardInfo()`
*   **Chi tiết**:
    1.  Nhận phản hồi APDU chứa dữ liệu encrypted.
    2.  Tách lấy `cardId` (plaintext).
    3.  Tính lại khóa: `AESUtility.deriveKey(masterKey, cardId)`.
    4.  Giải mã: Gọi `AESUtility.decrypt(nameData, key)`.

## 5. Cấu Trúc APDU
Chi tiết gói tin sử dụng cho AES.

### Lệnh `INS_SET_CARD_INFO` (0x40)
*   **Constant File**: `card_gui/src/applet/AppletConstants.java`
*   **P1, P2**: 0x00.
*   **Data Field**:
    *   `Flag` (1 byte): `0x01` (Bật chế độ mã hóa).
    *   `Student Code` (10 bytes): Plaintext (ASCII).
    *   `Name Length` (1 byte): Độ dài gói tin mã hóa tên.
    *   `Encrypted Name` (Var): `IV` + `Ciphertext`.
    *   `Encrypted Expiry` (Var): `IV` + `Ciphertext`.

## 6. Các Hạn Chế Hiện Tại
*   **Kích thước dữ liệu**: Trường `holderName` trong `CardInfoManager.java` giới hạn `NAME_MAX_LENGTH` (50 bytes). Khi mã hóa, dữ liệu tăng kích thước (IV 16 bytes + Padding). Nếu tên quá dài, dữ liệu mã hóa sẽ bị cắt cụt (truncate) khi lưu vào thẻ, dẫn đến lỗi giải mã.
*   **Bảo mật Master Key**: Key tĩnh trong `AESUtility.getMasterKey()` là rủi ro nếu file `.jar` bị decompile.
