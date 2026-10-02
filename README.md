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

Tự làm tiếp (theo hướng dẫn): module sát hạch, cổng học viên (`/api/toi/...`), quản lý người dùng,
thông báo và nhắc nợ định kỳ, báo cáo gửi Sở Xây dựng.
