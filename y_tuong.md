# Ý tưởng chung – Hệ thống thẻ thư viện dùng Smart Card

## 1. Mô tả bài toán

Nhóm xây dựng hệ thống thẻ thư viện sử dụng smart card cho sinh viên. Mỗi sinh viên được cấp một thẻ thư viện (Java Card). Khi sinh viên đến mượn/trả sách hoặc thanh toán tiền phạt, cán bộ thư viện (hoặc chính sinh viên) sẽ cắm thẻ vào đầu đọc, nhập mã PIN để xác thực, sau đó thực hiện các chức năng trên phần mềm máy tính. [attached_file:3]

Dữ liệu quan trọng như thông tin cá nhân, tình trạng mượn sách, số dư/tiền phạt được lưu trực tiếp trên thẻ dưới dạng đã mã hóa và đồng thời được lưu/đồng bộ với cơ sở dữ liệu trên máy tính để có thể quản lý, thống kê và phục hồi khi cần. Hệ thống đảm bảo:
- Bảo mật mã PIN (không dùng trực tiếp làm khóa AES, tuân theo lưu ý của cô về hàm dẫn xuất khóa).
- Bảo mật dữ liệu trên thẻ bằng mã hóa đối xứng.
- Có thể mở rộng dùng mật mã khóa công khai để ký/xác thực giao dịch ở các mốc sau (AES + RSA đúng yêu cầu đề). [attached_file:3]

Hệ thống gồm:
- Thẻ thư viện (Java Card applet).
- Ứng dụng desktop trên máy tính để thao tác (Java).
- Cơ sở dữ liệu (CSDL) trên máy tính để lưu bản sao dữ liệu và lịch sử giao dịch. [attached_file:3]

## 2. Công nghệ, IDE và công cụ sử dụng

- IDE:
  - Eclipse IDE (phiên bản mới) + plugin Java Card để phát triển, build và debug applet (JCIDE cũ, ít được hỗ trợ nên không dùng).
- Ngôn ngữ:
  - Java SE cho ứng dụng desktop.
  - Java Card cho applet trên thẻ.
- Kết nối với thẻ:
  - Thư viện chuẩn javax.smartcardio để làm việc với đầu đọc, gửi/nhận APDU.
- Crypto & bảo mật (phía PC):
  - Java Cryptography Architecture/Extension (JCA/JCE).
  - Thư viện BouncyCastle (nếu cần) để hỗ trợ PBKDF2, AES, RSA thuận tiện hơn. [attached_file:3]
- Cơ sở dữ liệu:
  - MySQL hoặc PostgreSQL.
  - JDBC driver tương ứng (MySQL Connector/J hoặc PostgreSQL JDBC).
- Giao diện người dùng:
  - Java Swing, tổ chức layout theo nhiều tab chức năng:
    - Tab “PIN & Bảo mật”.
    - Tab “Thông tin bạn đọc”.
    - Tab “Mượn/Trả sách”.
    - Tab “Tài chính”.
    - Tab “Hệ thống”.

## 3. Cơ chế và thuật toán mã hóa

### 3.1 Dẫn xuất khóa từ PIN (KDF)

- Thuật toán: PBKDF2WithHmacSHA256.
- Đầu vào: PIN (do người dùng nhập) + salt ngẫu nhiên 16 byte.
- Tham số dự kiến:
  - Số vòng lặp: khoảng 100 000.
  - Độ dài khóa: 256 bit (32 byte).
- Kết quả: Khóa bí mật DEK (Data Encryption Key) dùng cho AES.

Lý do: Thay vì băm PIN (SHA-256) dùng trực tiếp cho AES – cách này cô đã cảnh báo là không an toàn – PBKDF2 giúp tăng chi phí brute-force và phù hợp với quy trình thực tế. [attached_file:3]

### 3.2 Mã hóa đối xứng dữ liệu trên thẻ

- Thuật toán: AES-256/CBC/PKCS5Padding.
- Khóa AES: lấy từ PBKDF2(PIN, salt).
- IV: 16 byte ngẫu nhiên, tạo mỗi lần mã hóa; lưu cùng ciphertext trên thẻ/CSDL.
- Dữ liệu mã hóa:
  - Thông tin cá nhân bạn đọc (private data).
  - Nhật ký mượn/trả được lưu trên thẻ.
  - Thông tin số dư, nợ phạt (nếu lưu trên thẻ).

### 3.3 Mật mã khóa công khai

- Thuật toán: RSA 2048-bit.
- Chức năng:
  - Ký các giao dịch quan trọng (nạp tiền, thanh toán phạt).
  - Có thể dùng để xác thực thẻ với server.
- Lưu trữ khóa:
  - Public key có thể lưu dạng rõ trong CSDL.
  - Private key luôn lưu ở dạng đã mã hóa (AES) trên thẻ hoặc trong CSDL, tuyệt đối không lưu plaintext, đúng lưu ý “private key phải mã hóa”.

### 3.4 Lưu trữ dữ liệu

- Private data:
  - Thông tin cá nhân, trạng thái mượn sách, số dư, công nợ… lưu trên thẻ ở dạng ciphertext AES.
  - Khi lưu vào CSDL, có thể lưu thêm cột ciphertext để so sánh, hoặc giải mã để dùng cho báo cáo tùy mức bảo mật.
- Public / ít nhạy cảm:
  - Mã thẻ, mã sinh viên, trạng thái thẻ… có thể lưu rõ để tìm kiếm, lọc, thống kê.

## 4. Cấu trúc tab và chức năng chi tiết

### 4.1 Tab “PIN & Bảo mật”

Mục đích: Quản lý mã PIN và trạng thái an toàn của thẻ.

Chức năng:
1. Tạo PIN ban đầu:
   - Khi phát hành thẻ, người dùng nhập PIN (6–8 chữ số).
   - Ứng dụng sinh salt ngẫu nhiên, chạy PBKDF2(PIN, salt, iterations) để sinh DEK.
   - Gửi salt và thông tin cần thiết xuống thẻ để thẻ lưu: hash/PBKDF2 của PIN, salt, bộ đếm số lần nhập sai.
2. Xác thực PIN:
   - Khi người dùng vào các tab nhạy cảm (Thông tin bạn đọc, Mượn/Trả, Tài chính), hệ thống yêu cầu nhập PIN.
   - Thẻ so sánh kết quả dẫn xuất với giá trị lưu, tăng bộ đếm nếu sai:
     - Sai 1 lần: cảnh báo còn 2 lần.
     - Sai 2 lần: cảnh báo còn 1 lần.
     - Sai 3 lần: khóa thẻ và hiển thị thông báo.
3. Đổi PIN:
   - Nhập PIN cũ + PIN mới.
   - Nếu PIN cũ đúng, tạo salt mới, chạy lại PBKDF2 để sinh khóa mới, cập nhật dữ liệu tương ứng trên thẻ.
4. Hiển thị trạng thái:
   - Số lần nhập sai còn lại.
   - Trạng thái thẻ (active/locked).
   - Thời điểm mở khóa nếu đang bị khóa tạm thời.

### 4.2 Tab “Thông tin bạn đọc”

Mục đích: Quản lý thông tin thành viên thư viện.

Dữ liệu lưu trong thẻ (AES):
- ID thẻ thư viện:
  - Sinh từ hàm hash có ý nghĩa, ví dụ: SHA-256(mã sinh viên + salt_thẻ).
  - Không cho phép sửa theo đúng yêu cầu “id của thẻ không được phép sửa”.
- Mã sinh viên.
- Họ tên.
- Ngày sinh.
- Khoa/lớp.
- Email, số điện thoại, địa chỉ.
- Ngày phát hành, ngày hết hạn thẻ.

Chức năng:
1. Xem thông tin:
   - Sau khi xác thực PIN, ứng dụng đọc ciphertext từ thẻ, giải mã bằng AES-256, hiển thị các trường lên form.
2. Cập nhật thông tin:
   - Cho phép sửa các trường mềm như email, số điện thoại, địa chỉ.
   - Không cho sửa ID thẻ, mã sinh viên.
   - Khi nhấn “Lưu”, ứng dụng gom dữ liệu, mã hóa AES, ghi xuống thẻ và đồng bộ CSDL.
3. Cảnh báo:
   - Thẻ sắp hết hạn.
   - Thẻ đang ở trạng thái bị khóa.

### 4.3 Tab “Mượn / Trả sách”

Mục đích: Quản lý mượn/trả sách thông qua thẻ thư viện.

Chức năng:
1. Mượn sách:
   - Nhập hoặc quét mã sách sau khi thẻ đã xác thực PIN.
   - Kiểm tra điều kiện:
     - Thẻ còn hạn, không bị khóa.
     - Số sách đang mượn chưa đạt giới hạn.
     - Không nợ tiền phạt vượt ngưỡng.
   - Ghi “phiếu mượn” vào thẻ (mã sách, ngày mượn, hạn trả, trạng thái…), mã hóa AES; đồng thời ghi một bản vào CSDL (có thể lưu rõ để báo cáo).
2. Trả sách:
   - Nhập hoặc quét mã sách trả.
   - Tính số ngày trễ (nếu có), tính tiền phạt tương ứng.
   - Cập nhật trạng thái phiếu mượn thành “đã trả” trên thẻ và CSDL.
   - Nếu phát sinh tiền phạt, tạo bản ghi công nợ chuyển sang tab Tài chính.
3. Xem danh sách sách đang mượn:
   - Hiển thị danh sách sách chưa trả, hạn trả, số ngày còn lại / số ngày trễ.
4. Cảnh báo:
   - Sách sắp quá hạn.
   - Sách đã quá hạn và đang sinh tiền phạt.

### 4.4 Tab “Tài chính / Tiền phạt”

Mục đích: Quản lý tiền phạt và số dư liên quan đến thẻ thư viện.

Dữ liệu:
- Số dư tài khoản thư viện (nếu áp dụng mô hình ví).
- Tổng tiền phạt chưa thanh toán.
- Lịch sử giao dịch: nạp tiền, thanh toán phạt, hoàn tiền (nếu có).

Chức năng:
1. Nạp tiền:
   - Sau khi xác thực PIN, người dùng nhập số tiền cần nạp.
   - Cập nhật số dư trên thẻ (đã mã hóa AES) và trong CSDL.
   - (Ở mốc dùng RSA) Tạo chữ ký RSA cho giao dịch để đảm bảo không bị sửa đổi; lưu chữ ký trong CSDL.
2. Thanh toán tiền phạt:
   - Hiển thị các khoản phạt hiện có.
   - Cho phép chọn khoản để thanh toán từ số dư hoặc thanh toán trực tiếp.
   - Mỗi lần thanh toán yêu cầu nhập lại PIN để tránh thao tác nhầm.
3. Xem lịch sử giao dịch:
   - Danh sách các lần nạp tiền, trừ tiền phạt, thời gian, số tiền, trạng thái (thành công/thất bại).

Mã hóa và khóa:
- Số dư và công nợ trên thẻ lưu dạng mã hóa AES.
- Các giao dịch quan trọng có thể được ký bằng RSA với private key lưu trên thẻ ở dạng mã hóa AES.

### 4.5 Tab “Hệ thống / Cài đặt”

Mục đích: Chứa các chức năng cấu hình cơ bản, không cần chia role phức tạp.

Chức năng:
1. Khởi tạo thẻ mới:
   - Nhập mã sinh viên và thông tin cơ bản.
   - Sinh ID thẻ (hash có ý nghĩa, không sửa).
   - Gán PIN ban đầu, sinh salt và DEK bằng PBKDF2, mã hóa dữ liệu ban đầu và ghi xuống thẻ.
   - Tạo record tương ứng trong CSDL.
2. Khóa / mở khóa thẻ:
   - Khóa thẻ khi bị mất, vi phạm.
   - Mở khóa lại khi đã xử lý xong (có quy trình xác minh).
3. Đồng bộ dữ liệu:
   - Nút “Đồng bộ” để đọc dữ liệu từ thẻ, giải mã, so sánh và cập nhật với CSDL.
4. Nhật ký hệ thống:
   - Lưu log các sự kiện: đăng nhập, đổi PIN, phát hành thẻ, khóa/mở khóa, mượn/trả, nạp tiền, thanh toán phạt, lỗi nhập sai PIN nhiều lần.

