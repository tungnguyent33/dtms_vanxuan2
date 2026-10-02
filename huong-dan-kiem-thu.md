# 🧪 Kịch Bản Kiểm Thử (Backtest) DTMS Vạn Xuân

Dưới đây là kịch bản chi tiết để bạn tự tay kiểm thử toàn bộ luồng nghiệp vụ của hệ thống trên trình duyệt tại **http://localhost:5173**. Hãy thực hiện theo thứ tự từ trên xuống dưới.

---

## 👤 Kịch bản 1: Học viên đăng ký trực tuyến (Không cần đăng nhập)

1. Mở một tab ẩn danh (hoặc tab mới) và truy cập: **http://localhost:5173/dang-ky**
2. **Thử lỗi (BR-02 Không đủ tuổi):**
   - Điền tên: `Học Viên Nhỏ Tuổi`
   - Ngày sinh: Chọn một ngày trong năm **2015** (chưa đủ 18 tuổi)
   - CCCD: `001002003004`
   - Chọn khóa học `A1-...`
   - Nhấn **Đăng ký**.
   - ❌ **Kỳ vọng:** Hệ thống báo lỗi "Học viên chưa đủ tuổi...".
3. **Đăng ký thành công:**
   - Sửa ngày sinh thành **2000-01-01** (đủ 18 tuổi).
   - CCCD: `001200300400`
   - Điền đầy đủ thông tin còn lại.
   - Chọn khóa học và hình thức lý thuyết.
   - Nhấn **Đăng ký**.
   - ✅ **Kỳ vọng:** Thông báo thành công và hiển thị **Mã hồ sơ** (VD: `HS-A1-0003`). Hồ sơ này sẽ ở trạng thái **Chờ duyệt**.

---

## 👩‍💼 Kịch bản 2: Lễ tân (Tiếp nhận & Thu ngân)

Truy cập **http://localhost:5173** và đăng nhập với tài khoản:
- **Tên đăng nhập:** `letan01`
- **Mật khẩu:** `123456`

### 2.1. Duyệt hồ sơ trực tuyến
1. Vào menu **Hồ sơ học viên** (hoặc Học viên).
2. Tìm kiếm hồ sơ vừa đăng ký ở Kịch bản 1 (trạng thái: *Chờ duyệt*).
3. Nhấn nút **Duyệt**.
4. ✅ **Kỳ vọng:** Trạng thái hồ sơ chuyển sang *Đã tiếp nhận* (hoặc *Đang học* nếu khóa đã khai giảng).

### 2.2. Tiếp nhận hồ sơ trực tiếp & Bắt lỗi
1. Tại trang **Hồ sơ học viên**, nhấn nút **Thêm mới / Đăng ký**.
2. **Thử lỗi (BR-01 Trùng CCCD):** Nhập CCCD `025205000001` (đã có trong dữ liệu mẫu) và nhấn kiểm tra.
   - ✅ **Kỳ vọng:** Hệ thống tự động điền thông tin của "Phạm Văn Mẫu".
3. **Thử lỗi (Đăng ký trùng khóa):** Chọn khóa `A1-2026-10-01` cho Phạm Văn Mẫu và lưu.
   - ❌ **Kỳ vọng:** Báo lỗi "Học viên đã có hồ sơ trong khóa này".
4. Đóng form.

### 2.3. Lập phiếu thu & In PDF
1. Chuyển sang menu **Công nợ**.
2. Tìm học viên "Phạm Văn Mẫu" (còn nợ 1.000.000đ).
3. Nhấn **Lập phiếu thu**.
4. Nhập số tiền: `1000000` (1 triệu), chọn Hình thức: *Tiền mặt*. Nhấn Lưu.
5. Nhấn vào biểu tượng **In PDF** trên phiếu thu vừa lập.
6. ✅ **Kỳ vọng:** Một tab mới mở ra chứa file PDF Phiếu thu có hỗ trợ tiếng Việt đầy đủ. Học viên này sẽ chuyển sang trạng thái "Hết nợ".

---

## 👨‍🏫 Kịch bản 3: Giáo viên (Tổng quan & Điểm danh)

Đăng xuất tài khoản Lễ tân. Đăng nhập lại với tài khoản Giáo viên:
- **Tên đăng nhập:** `gv01`
- **Mật khẩu:** `123456`

1. Hệ thống tự động chuyển đến trang **Tổng quan**.
2. ✅ **Kỳ vọng:** Giáo viên sẽ thấy được Lịch dạy trong 7 ngày tới và Danh sách học viên mà mình đang phụ trách.
3. Ở thanh menu bên trái, chuyển sang trang **Điểm danh**.
4. Chọn khóa học `A1-2026-10-01` và chọn một buổi học bất kỳ (ví dụ: Lý thuyết ngày 05/10/2026).
5. Danh sách học viên của lớp hiện ra.
6. Tích chọn **Có mặt** cho một vài học viên, nhập số giờ học tương ứng.
7. Nhấn **Lưu điểm danh**.
8. ✅ **Kỳ vọng:** Hệ thống lưu thành công. (Giáo viên không nhìn thấy các menu như Công nợ hay Khóa học).

---

## 👨‍💻 Kịch bản 4: Quản trị viên (Quản lý chung)

Đăng xuất tài khoản Giáo viên. Đăng nhập lại với tài khoản Quản trị:
- **Tên đăng nhập:** `admin`
- **Mật khẩu:** `123456`

### 4.1. Xem Dashboard
1. Vào trang **Tổng quan**.
2. ✅ **Kỳ vọng:** Nhìn thấy các thẻ thống kê tổng quan (Học viên đang học, Khóa đang tuyển, Tổng công nợ, Doanh thu...).

### 4.2. Chuyển khóa / Hủy hồ sơ
1. Vào menu **Hồ sơ học viên**.
2. Chọn một học viên ở trạng thái *Đã tiếp nhận* nhưng CHƯA đóng tiền.
3. Thử tính năng **Chuyển khóa** sang một khóa khác cùng hạng.
4. Hoặc thử tính năng **Hủy hồ sơ** (Nhập lý do: "Học viên xin rút").
5. ✅ **Kỳ vọng:** Trạng thái hồ sơ thay đổi tương ứng. (Lưu ý: Nếu học viên đã có phiếu thu, hệ thống sẽ chặn không cho hủy cho đến khi hủy phiếu thu).

### 4.3. Quản lý Lịch học (Bắt lỗi trùng lịch)
1. Vào menu **Lịch học**.
2. Chọn khóa `A1-2026-10-01`.
3. Nhấn **Thêm buổi học**.
4. Chọn loại: *Thực hành*, Ngày: `2026-10-06`, Giờ: `07:30` đến `10:30`.
5. Chọn Giáo viên: `Trần Văn Giáo`, Xe: `19-TL 000.01` (Trùng hoàn toàn với dữ liệu mẫu).
6. Nhấn Lưu.
7. ❌ **Kỳ vọng:** Hệ thống báo lỗi trùng lịch giáo viên hoặc trùng lịch xe.

---

## 👨‍🎓 Kịch bản 5: Học viên (Theo dõi tiến độ học tập)

Đăng xuất tài khoản Quản trị. Đăng nhập với tài khoản của Học viên:
- **Tên đăng nhập:** `025205000001` (Đây là số CCCD của học viên mẫu Phạm Văn Mẫu)
- **Mật khẩu:** `123456`

1. Hệ thống tự động chuyển đến trang **Tiến độ học tập**.
2. ✅ **Kỳ vọng:** Bạn sẽ nhìn thấy ngay:
   - Hồ sơ đăng ký khóa học cùng với tình trạng đóng học phí (đã đóng / còn nợ).
   - Tiến độ tích lũy số giờ học (Lý thuyết / Thực hành) và thông báo Đạt / Chưa đạt điều kiện thi.
   - Thời khóa biểu các lớp (Lý thuyết / Thực hành) trong 30 ngày sắp tới.

---
**Chúc bạn kiểm thử thành công! 🚀** Nếu gặp bất kỳ lỗi gì ngoài dự kiến, hãy báo lại cho tôi.
