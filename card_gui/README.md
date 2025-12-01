## Library Card JavaCard GUI – Cấu trúc dự án

### 1. Mục tiêu
- **Thẻ ảo JavaCard** chạy bằng JCardSim.
- **Giao diện quản lý** thẻ thư viện: PIN, thông tin thẻ, mượn/trả sách.

### 2. Cấu trúc thư mục chính
- **`src/applet/`** – Mã applet chạy trên thẻ ảo:
  - `AppletConstants.java` – Các hằng số chung (AID, INS, độ dài dữ liệu…).
  - `PinManager.java` – Xử lý PIN (tạo, kiểm tra, đổi, reset, đếm số lần sai).
  - `CardInfoManager.java` – Lưu / đọc thông tin thẻ (ID, tên, ngày hết hạn).
  - `BookManager.java` – Quản lý sách mượn / trả.
  - `LibraryCardApplet.java` – Applet chính, router INS → các manager ở trên.

- **`src/service/`**
  - `SimulatorService.java` – Khởi động JCardSim `Simulator`, nạp `LibraryCardApplet`, cung cấp hàm `connect()`, `sendCommand(...)`, xử lý PIN ở mức service cho GUI mới.

- **`src/pages/`, `src/ui/`, `src/components/`, `src/models/`**
  - Các lớp giao diện (Swing) và model dùng cho **GUI mới** `LibraryCardMainFrame` (giao diện đẹp, dạng dashboard).

- **`src/LibraryCardMainFrame.java`**
  - Cửa sổ chính của ứng dụng “Library Management System”.
  - Dùng `SimulatorService` để làm việc với thẻ ảo.

- **`src/LibraryCardSimulatorGUI.java`**
  - GUI demo độc lập, làm việc trực tiếp với `Simulator` và applet qua APDU thô.
  - Có các tab: Kết nối, Quản lý PIN, Thông tin thẻ, Mượn sách, Nhật ký.

### 3. Cách chạy nhanh
- Chạy GUI demo simulator: chạy `main` trong `LibraryCardSimulatorGUI`.
- Chạy GUI mới: chạy `main` trong `LibraryCardMainFrame`.


