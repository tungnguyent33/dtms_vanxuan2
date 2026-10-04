# DTMS Vạn Xuân – Hệ thống quản lý trung tâm đào tạo lái xe

Đồ án thực tập tốt nghiệp – Nguyễn Thanh Tùng (K23DTCN405), Khoa CNTT – PTIT.
Spring Boot 3 (Java 21) + React 18 (TypeScript, Ant Design) + MySQL 8.

## Cấu trúc

```
source/
├── backend/                 Spring Boot (Maven)
│   └── src/main/
│       ├── java/vn/vanxuan/dtms/
│       │   ├── common/      Lỗi, phân trang, cấp số tự tăng, đọc số thành chữ, nhật ký
│       │   ├── security/    JWT, Spring Security
│       │   ├── config/      Swagger
│       │   └── module/      auth, danhmuc, khoa, hocvien, hocphi, lichhoc, baocao
│       └── resources/
│           ├── application.yml
│           ├── db/migration/ V1__init.sql, V2__seed.sql, V3__bo_dem.sql (Flyway)
│           └── fonts/        DejaVu (in PDF tiếng Việt)
├── frontend/                React + Vite
│   └── src/ api/ auth/ components/ pages/ utils/
├── docker-compose.yml       Chạy cả hệ thống bằng Docker
└── api-test.http            Thử API bằng IntelliJ / VS Code REST Client
```

## Chạy khi phát triển

1. MySQL 8 đang chạy ở cổng 3306, tài khoản `root` (mật khẩu mặc định trong `application.yml` là `123456`,
   hoặc đặt biến môi trường `DB_PASSWORD`). CSDL `vanxuan_dtms` được tạo tự động.
2. Backend: mở thư mục `backend` bằng IntelliJ IDEA → chạy `DtmsApplication`
   (hoặc `mvn spring-boot:run`). Flyway tự tạo bảng và dữ liệu mẫu.
   Swagger: http://localhost:8080/swagger-ui.html
3. Frontend:
   ```
   cd frontend
   npm install
   npm run dev
   ```
   Mở http://localhost:5173

Tài khoản mẫu (mật khẩu `123456`): `admin`, `letan01`, `gv01`.
Học viên mẫu `025205000001`, `025307000002` (mật khẩu `123456`) bị buộc đổi mật khẩu ở lần đăng nhập đầu.
Tài khoản mới (nhân viên, giáo viên, học viên) được cấp **mật khẩu tạm** hiện một lần, người dùng phải đổi khi đăng nhập.

Biến môi trường thêm (đều có giá trị mặc định):

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `JWT_EXPIRATION_MINUTES` | 15 | Hạn access token |
| `JWT_REFRESH_DAYS` | 7 | Hạn refresh token (xoay vòng, lưu băm trong CSDL) |
| `CAPTCHA_BAT` | true | Captcha cho form đăng ký trực tuyến và tra cứu hồ sơ |
| `GIOI_HAN_TAN_SUAT` | true | Giới hạn số lần gọi theo IP: đăng nhập, API công khai |

## Kiểm thử

```
cd backend && mvn test          # unit test quy tắc nghiệp vụ
cd frontend && npm run lint && npm run build
```

## Triển khai bằng Docker

```
cp .env.example .env            # sửa mật khẩu và JWT_SECRET
docker compose up -d --build
```
Mở http://<địa-chỉ-máy-chủ>

## Đã có / việc tiếp theo

Đã có: đăng nhập JWT + phân quyền; danh mục; khóa đào tạo (≤ 10 ngày); tiếp nhận hồ sơ (kiểm tra CCCD,
tuổi, sĩ số), đăng ký trực tuyến, duyệt, hủy, chuyển khóa, tải ảnh; lịch học (chặn trùng lịch giáo viên, xe);
điểm danh; tiến độ; xét hoàn thành; phiếu thu, in PDF, hủy phiếu, công nợ, xuất Excel; dashboard.

V2 đã bổ sung:
- Cổng Khách: trang chủ công khai (khóa đang tuyển, hạng đào tạo, quy trình), đăng ký trực tuyến, tra cứu hồ sơ.
- FR-01: refresh token xoay vòng, đổi mật khẩu, bắt buộc đổi mật khẩu tạm, khóa tạm khi sai mật khẩu 5 lần.
- FR-03: quản lý người dùng, giáo viên (kèm cấp tài khoản), xe tập lái, cộng tác viên, thông số hạng GPLX (menu "Danh mục & người dùng").
- FR-15, FR-16, BR-12: thông báo trong hệ thống (chuông), nhắc lịch học, nhắc nợ, nhắc báo cáo Sở, xe đến hạn bảo dưỡng
  (tác vụ 7h sáng; quản trị viên có nút "Chạy nhắc việc ngay"); cổng học viên xem và tải phiếu thu.
- UC05 – 5b: khóa đủ sĩ số thì gợi ý khóa kế tiếp cùng hạng còn chỗ.
- Cộng tác viên: hồ sơ CTV có duyệt (người nhập ≠ người duyệt, bắt buộc CCCD và cam kết), hạng Thường/Bạc/Vàng,
  lead và chống trùng SĐT (ghi nhận người giới thiệu đầu tiên), chính sách hoa hồng theo hạng bằng và ngày hiệu lực,
  hoa hồng tự tính khi học viên đóng đủ % học phí, duyệt và chi theo kỳ tháng, báo cáo theo CTV,
  cổng CTV (vai trò `CTV`) và học viên giới thiệu bạn bè / xin làm CTV.
- Bảo mật: học viên chỉ xem được hồ sơ, phiếu thu, công nợ của mình; captcha và giới hạn tần suất cho API công khai.

Kịch bản kiểm thử V2: `huong-dan-kiem-thu-v2.md`.

Việc tiếp theo: module sát hạch (FR-12, lịch thi trong cổng học viên), in giấy xác nhận (FR-10),
xuất báo cáo gửi Sở Xây dựng (FR-14).
