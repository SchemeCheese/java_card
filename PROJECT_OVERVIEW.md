## Library Management System – JavaCard & Desktop GUI

### 1. Mục tiêu dự án

- **Mô phỏng thẻ sinh viên/thư viện** trên JavaCard bằng JCardSim.
- Xây dựng **ứng dụng desktop** quản lý:
  - Xác thực PIN và phân quyền.
  - Thông tin bạn đọc.
  - Mượn / Trả sách (mock data nhưng thao tác được).
  - Tài chính: nạp tiền, trừ tiền phạt, lịch sử giao dịch.
- Phân tách rõ ràng **Admin** và **Sinh viên**.

---

### 2. Kiến trúc 3 lớp

#### 2.1. Applet Layer (`card_gui/src/applet/`)

- Chạy trên **JCardSim** (mô phỏng JavaCard).
- Các lớp chính:
  - `LibraryCardApplet`: router chính, nhận APDU và chuyển đến các manager.
  - `PinManager`: tạo / xác thực / đổi / reset PIN trên thẻ (PBKDF2 + Salt).
  - `CardInfoManager`: lưu trữ tối thiểu: **Mã thẻ (MSSV), Tên, Ngày (expiry)**.
  - `BookManager`: xử lý lệnh về sách (mức đơn giản).
- Chỉ lưu **dữ liệu tối thiểu** trên thẻ để giống hạn chế bộ nhớ của JavaCard.

#### 2.2. Service Layer (`SimulatorService`)

- Kết nối JCardSim:
  - `connect()` → cài đặt applet, chọn applet.
  - `sendCommand()` → gửi APDU, `getSW()` → đọc status word.
- Quản lý **phiên làm việc**:
  - `isConnected`, `isPinVerified`, `pinTriesRemaining`.
  - `currentStudentCode`, `currentRole` (`Admin` / `normal`).
- Quản lý **dữ liệu nghiệp vụ** bên ngoài thẻ:
  - `List<CardInfo> cardList`: mỗi sinh viên là một thẻ, gồm:
    - MSSV, Họ tên, Email, Khoa/Viện, Ngày sinh, Địa chỉ.
    - Trạng thái thẻ (Hoạt động / Khóa).
    - PIN riêng của sinh viên (mặc định `000000`).
    - Số sách đang mượn, số dư tài khoản.
  - Sách mượn theo MSSV:
    - `getBorrowedBooks(studentCode)`, `addBorrowedBook(...)`, `removeBorrowedBook(...)`.
  - Tài chính:
    - `deposit(studentCode, amount)` – nạp tiền.
    - `payFine(studentCode, amount)` – thanh toán tiền phạt.
    - `getTransactions(studentCode)` – lịch sử giao dịch.
    - `getBalance(studentCode)` – số dư hiện tại.
- API cho GUI:
  - PIN:
    - `verifyStudentPin(studentCode, pin)`, `changeStudentPin(...)`.
    - `verifyPin(char[])`, `changePin(...)` dùng cho Admin (PIN lưu trên JavaCard).
  - Thẻ:
    - `addCardToList(CardInfo)`, `getAllCards()`, `searchCards(keyword)`, `toggleCardStatus(studentId)`.
    - `getCardByStudentCode(studentCode)`, `isCardExists(studentCode)`.

#### 2.3. GUI Layer (Swing – `card_gui/src/pages` + `LibraryCardMainFrame`)

- `LibraryCardMainFrame`:
  - Khởi động `SimulatorService`, tạo PIN mặc định trên thẻ (000000).
  - Thanh tab phía trên:
    - **PIN & Bảo Mật**.
    - **Thông Tin Bạn Đọc**.
    - **Mượn / Trả Sách**.
    - **Tài Chính**.
    - **Hệ Thống**.
  - Điều hướng giữa các trang, kiểm tra `isPinVerified` và `currentRole`.

---

### 3. Các tab chức năng chính

#### 3.1. PIN & Bảo Mật (`PinPage`)

- Nhập **MSSV + PIN** để đăng nhập.
  - PIN mặc định cho sinh viên: **`000000`**.
  - MSSV đặc biệt `CT060132` → **Admin**.
- Sau khi xác thực:
  - Cập nhật trạng thái: Đã xác thực / Chưa xác thực / Bị khóa.
  - Hiện nút **Đăng Xuất**.
  - Cho phép **Đổi PIN**:
    - Nếu là **Admin**: đổi PIN trên JavaCard qua `verifyPin` + `changePin`.
    - Nếu là **Sinh viên**: đổi PIN riêng trong `CardInfo` qua `changeStudentPin`.
- Nếu **thẻ bị khóa** (sai PIN nhiều lần) → khóa giao diện và hiển thị thông báo liên hệ Admin.
- Nếu sinh viên đăng nhập bằng **PIN mặc định** → popup khuyến khích đổi PIN để bảo mật.

#### 3.2. Hệ Thống (`SettingsPage`) – Chỉ Admin

- Chỉ truy cập được khi:
  - Đã xác thực PIN.
  - `currentRole == "Admin"`.
- Chức năng:
  - **Tạo thẻ mới** cho sinh viên (MSSV, Họ tên, Email, Khoa/Viện, Ngày sinh, Địa chỉ).
  - Quản lý danh sách thẻ: tìm kiếm theo MSSV/Tên, xem trạng thái.
  - **Khóa / Mở khóa** thẻ.
  - Nhật ký hoạt động mô phỏng (log thao tác).

#### 3.3. Thông Tin Bạn Đọc (`CardInfoPage`)

- Lấy `CardInfo` từ `SimulatorService` theo MSSV đang đăng nhập.
- Hiển thị:
  - MSSV, Họ tên.
  - Ngày sinh, Email, Khoa/Viện.
  - Địa chỉ.
  - Trạng thái thẻ (Hoạt động / Khóa).
  - **Số sách đang mượn**.
  - **Số dư tài khoản hiện tại**.
- Là trang “tổng quan” thông tin cá nhân cho sinh viên.

#### 3.4. Mượn / Trả Sách (`BorrowedBooksPage`)

- Sử dụng **mock data sách** trong `BOOK_CATALOG` (NV001, DB002, TH003, …).
- Hiển thị bảng **danh sách sách đang mượn** của sinh viên:
  - Mã sách, Tên sách, Ngày mượn, Hạn trả, Trạng thái, Số ngày trễ.
- Chức năng **Mượn sách**:
  - Nhập mã sách (VD: `NV001`).
  - Kiểm tra:
    - Mã sách có trong catalog.
    - Chưa mượn trùng.
    - Chưa vượt giới hạn 5 cuốn.
  - Tạo bản ghi `BorrowedBook` mới, thêm vào danh sách và cập nhật `CardInfo.borrowedBooks`.
  - Hiện message thành công.
- Chức năng **Trả sách**:
  - Nhập mã sách cần trả.
  - Nếu sách quá hạn:
    - Tính tiền phạt: `overdueDays * 5000 VND`.
    - Hỏi xác nhận, sau đó gọi `simulatorService.payFine(...)`.
    - Nếu số dư không đủ → báo lỗi, không cho trả.
  - Xóa sách khỏi danh sách mượn, cập nhật `CardInfo.borrowedBooks`.

#### 3.5. Tài Chính (`FinancePage`)

- Lấy dữ liệu thực từ `SimulatorService`:
  - `balance = simulatorService.getBalance(MSSV hiện tại)`.
  - `transactions = simulatorService.getTransactions(MSSV hiện tại)`.
- Gồm 3 phần:
  - **Số dư hiện tại**:
    - Hiển thị số dư dạng `1.000.000 VND`.
    - Tự động cập nhật sau khi nạp tiền hoặc thanh toán phạt.
  - **Nạp tiền**:
    - Ô nhập số tiền (tự lọc ký tự số).
    - Nút **"Nạp Tiền"** gọi `deposit(...)` → tăng số dư + ghi transaction mới.
    - Các nút **nạp nhanh**: 50.000 / 100.000 / 200.000 / 500.000.
  - **Lịch sử giao dịch**:
    - Bảng hiển thị ngày, loại giao dịch (Nạp tiền / Thanh toán phạt), số tiền (+ / -), trạng thái.
    - Mỗi lần nạp tiền hoặc thanh toán phạt → lịch sử được cập nhật lại.

---

### 4. Quy trình demo cho giảng viên

1. **Đăng nhập Admin**
   - Vào tab **PIN & Bảo Mật**, nhập `CT060132` + `000000`.
   - Sau khi xác thực, chọn tab **Hệ Thống**.
2. **Tạo thẻ sinh viên**
   - Nhập thông tin sinh viên (MSSV, Họ tên, …), bấm **Tạo Thẻ Mới**.
3. **Đăng nhập sinh viên**
   - Đăng xuất Admin, vào lại tab **PIN & Bảo Mật**.
   - Nhập MSSV sinh viên vừa tạo + PIN mặc định `000000`.
   - Hệ thống hiển thị popup khuyến khích đổi PIN.
4. **Xem thông tin bạn đọc**
   - Vào tab **Thông Tin Bạn Đọc** để xem đầy đủ thông tin sinh viên + số dư + số sách đang mượn.
5. **Thao tác mượn / trả sách**
   - Vào tab **Mượn / Trả Sách**, thử:
     - Mượn 1–2 cuốn bằng mã trong danh sách.
     - Trả sách (nếu quá hạn → minh họa cơ chế tính và trừ tiền phạt).
6. **Quản lý tài chính**
   - Vào tab **Tài Chính**:
     - Thấy số dư ban đầu.
     - Nạp thêm tiền (VD: 100.000 VND), số dư và lịch sử giao dịch cập nhật ngay.
     - Nếu trước đó có phạt do trả sách trễ, cho thấy số dư đã bị trừ.

---

### 5. Điểm nhấn khi trình bày

- Dự án kết hợp **mô phỏng JavaCard** (bảo mật PIN, APDU) với **ứng dụng desktop** hiện đại (Swing + FlatLaf).
- Áp dụng **phân lớp rõ ràng**: Applet (thẻ), Service (nghiệp vụ & state), GUI (trình bày & tương tác).
- Thể hiện được:
  - Quản lý người dùng (Admin / Sinh viên).
  - Quản lý thẻ thư viện, PIN, trạng thái khóa/mở.
  - Quy trình mượn / trả sách và xử lý tiền phạt.
  - Nạp tiền và đồng bộ số dư ở tất cả các màn hình liên quan.

