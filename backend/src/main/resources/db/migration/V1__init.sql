-- =====================================================================
--  Hệ thống quản lý Trung tâm đào tạo lái xe Vạn Xuân (hạng A1, A)
--  Flyway V1 - cau truc CSDL (MySQL 8.0, utf8mb4)
--  Căn cứ nghiệp vụ: Thông tư 14/2025/TT-BXD, sửa đổi bởi 17/2026/TT-BXD
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Người dùng & phân quyền
-- ---------------------------------------------------------------------
CREATE TABLE vai_tro (
  id   TINYINT UNSIGNED PRIMARY KEY,
  ma   VARCHAR(20) NOT NULL UNIQUE,          -- ADMIN, LE_TAN, GIAO_VIEN, HOC_VIEN
  ten  VARCHAR(50) NOT NULL
);

CREATE TABLE nguoi_dung (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ten_dang_nhap       VARCHAR(50)  NOT NULL UNIQUE,
  mat_khau_hash       VARCHAR(100) NOT NULL,   -- BCrypt
  ho_ten              VARCHAR(100) NOT NULL,
  so_dien_thoai       VARCHAR(15),
  email               VARCHAR(100),
  vai_tro_id          TINYINT UNSIGNED NOT NULL,
  trang_thai          TINYINT(1) NOT NULL DEFAULT 1,   -- 1: hoạt động, 0: khóa
  lan_dang_nhap_cuoi  DATETIME,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_nd_vaitro FOREIGN KEY (vai_tro_id) REFERENCES vai_tro(id)
);

-- ---------------------------------------------------------------------
-- 2. Danh mục
-- ---------------------------------------------------------------------
-- Số giờ để dạng cấu hình vì quy định thay đổi theo thông tư
CREATE TABLE hang_gplx (
  ma                   VARCHAR(5)   PRIMARY KEY,           -- A1, A
  ten                  VARCHAR(150) NOT NULL,
  gio_ly_thuyet        DECIMAL(5,1) NOT NULL,
  gio_thuc_hanh        DECIMAL(5,1) NOT NULL,
  tuoi_toi_thieu       TINYINT UNSIGNED NOT NULL DEFAULT 18,
  so_ngay_khoa_toi_da  TINYINT UNSIGNED NOT NULL DEFAULT 10,
  hoc_phi_mac_dinh     DECIMAL(12,0) NOT NULL DEFAULT 0,
  can_cu_phap_ly       VARCHAR(200),
  dang_ap_dung         TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE giao_vien (
  id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nguoi_dung_id          BIGINT UNSIGNED UNIQUE,
  ho_ten                 VARCHAR(100) NOT NULL,
  so_dien_thoai          VARCHAR(15)  NOT NULL,
  so_giay_chung_nhan_gv  VARCHAR(50),
  loai_giang_day         ENUM('LY_THUYET','THUC_HANH','CA_HAI') NOT NULL DEFAULT 'CA_HAI',
  trang_thai             TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_gv_nd FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
);

CREATE TABLE xe_tap_lai (
  id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  bien_so               VARCHAR(20) NOT NULL UNIQUE,
  hang_ma               VARCHAR(5)  NOT NULL,
  nhan_hieu             VARCHAR(50),
  nam_san_xuat          SMALLINT UNSIGNED,
  trang_thai            ENUM('SAN_SANG','BAO_DUONG','NGUNG') NOT NULL DEFAULT 'SAN_SANG',
  ngay_bao_duong_tiep   DATE,
  CONSTRAINT fk_xe_hang FOREIGN KEY (hang_ma) REFERENCES hang_gplx(ma)
);

CREATE TABLE cong_tac_vien (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ho_ten          VARCHAR(100) NOT NULL,
  so_dien_thoai   VARCHAR(15)  NOT NULL UNIQUE,
  dia_ban         VARCHAR(100),
  muc_hoa_hong    DECIMAL(12,0) NOT NULL DEFAULT 0,   -- VNĐ / học viên
  trang_thai      TINYINT(1) NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------
-- 3. Khóa đào tạo & học viên
-- ---------------------------------------------------------------------
CREATE TABLE khoa_dao_tao (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ma_khoa           VARCHAR(30) NOT NULL UNIQUE,            -- VD: A1-2026-10-01
  hang_ma           VARCHAR(5)  NOT NULL,
  ngay_khai_giang   DATE NOT NULL,
  ngay_be_giang     DATE NOT NULL,
  si_so_toi_da      SMALLINT UNSIGNED NOT NULL DEFAULT 50,
  hoc_phi           DECIMAL(12,0) NOT NULL,
  trang_thai        ENUM('DU_KIEN','DANG_TUYEN','DANG_DAO_TAO','DA_KET_THUC','HUY')
                    NOT NULL DEFAULT 'DU_KIEN',
  ngay_bao_cao_so   DATE,                                   -- ngày gửi báo cáo đăng ký khóa
  ghi_chu           VARCHAR(255),
  CONSTRAINT fk_khoa_hang FOREIGN KEY (hang_ma) REFERENCES hang_gplx(ma),
  CONSTRAINT ck_khoa_ngay CHECK (ngay_be_giang >= ngay_khai_giang)
);

CREATE TABLE hoc_vien (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ma_hoc_vien         VARCHAR(20)  NOT NULL UNIQUE,         -- VD: HV2026000123
  ho_ten              VARCHAR(100) NOT NULL,
  ngay_sinh           DATE NOT NULL,
  gioi_tinh           ENUM('NAM','NU') NOT NULL,
  cccd                CHAR(12) NOT NULL UNIQUE,
  ngay_cap_cccd       DATE,
  dia_chi             VARCHAR(255) NOT NULL,
  so_dien_thoai       VARCHAR(15)  NOT NULL,
  email               VARCHAR(100),
  anh_chan_dung_url   VARCHAR(255),
  anh_cccd_truoc_url  VARCHAR(255),
  anh_cccd_sau_url    VARCHAR(255),
  nguoi_dung_id       BIGINT UNSIGNED UNIQUE,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_hv_nd FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
  CONSTRAINT ck_hv_cccd CHECK (cccd REGEXP '^[0-9]{12}$')
);
CREATE INDEX idx_hv_sdt ON hoc_vien(so_dien_thoai);
CREATE INDEX idx_hv_ten ON hoc_vien(ho_ten);

-- Một học viên có thể đăng ký nhiều khóa (VD: học A1 rồi học A, hoặc học lại)
CREATE TABLE dang_ky (
  id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ma_ho_so             VARCHAR(30) NOT NULL UNIQUE,
  hoc_vien_id          BIGINT UNSIGNED NOT NULL,
  khoa_id              BIGINT UNSIGNED NOT NULL,
  hinh_thuc_ly_thuyet  ENUM('TU_HOC','TAP_TRUNG') NOT NULL,
  nguon                ENUM('TRUC_TIEP','TRUC_TUYEN','CTV') NOT NULL DEFAULT 'TRUC_TIEP',
  ctv_id               BIGINT UNSIGNED,
  hoc_phi              DECIMAL(12,0) NOT NULL,
  giam_tru             DECIMAL(12,0) NOT NULL DEFAULT 0,
  ly_do_giam_tru       VARCHAR(255),
  trang_thai           ENUM('CHO_DUYET','DA_TIEP_NHAN','DANG_HOC','CHUA_DAT','HOAN_THANH',
                            'SAT_HACH_TRUOT','DA_SAT_HACH_DAT','DA_HUY')
                       NOT NULL DEFAULT 'DA_TIEP_NHAN',
  ngay_dang_ky         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  nguoi_tiep_nhan_id   BIGINT UNSIGNED,           -- NULL khi đăng ký trực tuyến chưa duyệt
  so_giay_xac_nhan     VARCHAR(30) UNIQUE,        -- giấy xác nhận hoàn thành khóa đào tạo
  ngay_hoan_thanh      DATE,
  ghi_chu              VARCHAR(255),
  CONSTRAINT fk_dk_hv   FOREIGN KEY (hoc_vien_id) REFERENCES hoc_vien(id),
  CONSTRAINT fk_dk_khoa FOREIGN KEY (khoa_id) REFERENCES khoa_dao_tao(id),
  CONSTRAINT fk_dk_ctv  FOREIGN KEY (ctv_id) REFERENCES cong_tac_vien(id),
  CONSTRAINT fk_dk_nd   FOREIGN KEY (nguoi_tiep_nhan_id) REFERENCES nguoi_dung(id),
  CONSTRAINT uq_dk_hv_khoa UNIQUE (hoc_vien_id, khoa_id),
  CONSTRAINT ck_dk_tien CHECK (giam_tru >= 0 AND giam_tru <= hoc_phi),
  CONSTRAINT ck_dk_ctv CHECK (nguon <> 'CTV' OR ctv_id IS NOT NULL)
);
CREATE INDEX idx_dk_trangthai ON dang_ky(trang_thai);

-- ---------------------------------------------------------------------
-- 4. Lịch học & điểm danh
-- ---------------------------------------------------------------------
CREATE TABLE buoi_hoc (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  khoa_id        BIGINT UNSIGNED NOT NULL,
  loai           ENUM('LY_THUYET','THUC_HANH') NOT NULL,
  ngay           DATE NOT NULL,
  gio_bat_dau    TIME NOT NULL,
  gio_ket_thuc   TIME NOT NULL,
  dia_diem       VARCHAR(150) NOT NULL,
  giao_vien_id   BIGINT UNSIGNED NOT NULL,
  xe_id          BIGINT UNSIGNED,
  trang_thai     ENUM('KE_HOACH','DA_DAY','HUY') NOT NULL DEFAULT 'KE_HOACH',
  CONSTRAINT fk_bh_khoa FOREIGN KEY (khoa_id) REFERENCES khoa_dao_tao(id),
  CONSTRAINT fk_bh_gv   FOREIGN KEY (giao_vien_id) REFERENCES giao_vien(id),
  CONSTRAINT fk_bh_xe   FOREIGN KEY (xe_id) REFERENCES xe_tap_lai(id),
  CONSTRAINT ck_bh_gio  CHECK (gio_ket_thuc > gio_bat_dau)
);
CREATE INDEX idx_bh_gv_ngay ON buoi_hoc(giao_vien_id, ngay);
CREATE INDEX idx_bh_xe_ngay ON buoi_hoc(xe_id, ngay);

CREATE TABLE diem_danh (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  buoi_hoc_id   BIGINT UNSIGNED NOT NULL,
  dang_ky_id    BIGINT UNSIGNED NOT NULL,
  co_mat        TINYINT(1) NOT NULL,
  so_gio        DECIMAL(4,2) NOT NULL DEFAULT 0,
  ghi_chu       VARCHAR(255),
  nguoi_ghi_id  BIGINT UNSIGNED NOT NULL,
  ghi_luc       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_dd_bh FOREIGN KEY (buoi_hoc_id) REFERENCES buoi_hoc(id) ON DELETE CASCADE,
  CONSTRAINT fk_dd_dk FOREIGN KEY (dang_ky_id) REFERENCES dang_ky(id),
  CONSTRAINT fk_dd_nd FOREIGN KEY (nguoi_ghi_id) REFERENCES nguoi_dung(id),
  CONSTRAINT uq_dd UNIQUE (buoi_hoc_id, dang_ky_id),
  CONSTRAINT ck_dd_gio CHECK (so_gio >= 0 AND (co_mat = 1 OR so_gio = 0))
);

-- ---------------------------------------------------------------------
-- 5. Học phí
-- ---------------------------------------------------------------------
CREATE TABLE phieu_thu (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  so_phieu      VARCHAR(20) NOT NULL UNIQUE,           -- PT-202610-0001
  dang_ky_id    BIGINT UNSIGNED NOT NULL,
  so_tien       DECIMAL(12,0) NOT NULL,
  hinh_thuc     ENUM('TIEN_MAT','CHUYEN_KHOAN','VNPAY') NOT NULL,
  noi_dung      VARCHAR(255),
  ngay_thu      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  nguoi_thu_id  BIGINT UNSIGNED NOT NULL,
  trang_thai    ENUM('HIEU_LUC','DA_HUY') NOT NULL DEFAULT 'HIEU_LUC',
  ly_do_huy     VARCHAR(255),
  nguoi_huy_id  BIGINT UNSIGNED,
  CONSTRAINT fk_pt_dk  FOREIGN KEY (dang_ky_id) REFERENCES dang_ky(id),
  CONSTRAINT fk_pt_nd  FOREIGN KEY (nguoi_thu_id) REFERENCES nguoi_dung(id),
  CONSTRAINT fk_pt_huy FOREIGN KEY (nguoi_huy_id) REFERENCES nguoi_dung(id),
  CONSTRAINT ck_pt_tien CHECK (so_tien > 0),
  CONSTRAINT ck_pt_huy CHECK (trang_thai = 'HIEU_LUC' OR ly_do_huy IS NOT NULL)
);
CREATE INDEX idx_pt_ngay ON phieu_thu(ngay_thu);

-- ---------------------------------------------------------------------
-- 6. Sát hạch
-- ---------------------------------------------------------------------
CREATE TABLE ky_sat_hach (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  hang_ma         VARCHAR(5) NOT NULL,
  ngay_sat_hach   DATE NOT NULL,
  dia_diem        VARCHAR(200) NOT NULL,
  ghi_chu         VARCHAR(255),
  CONSTRAINT fk_ksh_hang FOREIGN KEY (hang_ma) REFERENCES hang_gplx(ma)
);

CREATE TABLE ket_qua_sat_hach (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ky_sat_hach_id  BIGINT UNSIGNED NOT NULL,
  dang_ky_id      BIGINT UNSIGNED NOT NULL,
  lan_thi         TINYINT UNSIGNED NOT NULL DEFAULT 1,
  diem_ly_thuyet  TINYINT UNSIGNED,
  dat_ly_thuyet   TINYINT(1),
  dat_thuc_hanh   TINYINT(1),
  ket_qua         ENUM('CHUA_THI','DAT','TRUOT','VANG') NOT NULL DEFAULT 'CHUA_THI',
  CONSTRAINT fk_kq_ksh FOREIGN KEY (ky_sat_hach_id) REFERENCES ky_sat_hach(id),
  CONSTRAINT fk_kq_dk  FOREIGN KEY (dang_ky_id) REFERENCES dang_ky(id),
  CONSTRAINT uq_kq UNIQUE (ky_sat_hach_id, dang_ky_id)
);

-- ---------------------------------------------------------------------
-- 7. Thông báo & nhật ký
-- ---------------------------------------------------------------------
CREATE TABLE thong_bao (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nguoi_nhan_id  BIGINT UNSIGNED NOT NULL,
  tieu_de        VARCHAR(150) NOT NULL,
  noi_dung       TEXT NOT NULL,
  kenh           ENUM('WEB','ZALO','SMS') NOT NULL DEFAULT 'WEB',
  da_doc         TINYINT(1) NOT NULL DEFAULT 0,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tb_nd FOREIGN KEY (nguoi_nhan_id) REFERENCES nguoi_dung(id)
);

CREATE TABLE nhat_ky (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nguoi_dung_id   BIGINT UNSIGNED,
  hanh_dong       VARCHAR(30)  NOT NULL,     -- CREATE, UPDATE, DELETE, HUY_PHIEU...
  doi_tuong       VARCHAR(50)  NOT NULL,     -- dang_ky, phieu_thu...
  doi_tuong_id    BIGINT UNSIGNED,
  du_lieu         JSON,
  thoi_gian       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_nk_nd FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
);

-- ---------------------------------------------------------------------
-- 8. View hỗ trợ báo cáo
-- ---------------------------------------------------------------------
CREATE VIEW v_cong_no AS
SELECT dk.id AS dang_ky_id, dk.ma_ho_so, dk.trang_thai, hv.ho_ten, hv.so_dien_thoai, k.id AS khoa_id, k.ma_khoa,
       dk.hoc_phi - dk.giam_tru                           AS phai_dong,
       COALESCE(SUM(CASE WHEN pt.trang_thai = 'HIEU_LUC' THEN pt.so_tien END), 0) AS da_dong,
       dk.hoc_phi - dk.giam_tru
         - COALESCE(SUM(CASE WHEN pt.trang_thai = 'HIEU_LUC' THEN pt.so_tien END), 0) AS con_no
FROM dang_ky dk
JOIN hoc_vien hv     ON hv.id = dk.hoc_vien_id
JOIN khoa_dao_tao k  ON k.id  = dk.khoa_id
LEFT JOIN phieu_thu pt ON pt.dang_ky_id = dk.id
WHERE dk.trang_thai <> 'DA_HUY'
GROUP BY dk.id, dk.ma_ho_so, dk.trang_thai, hv.ho_ten, hv.so_dien_thoai, k.id, k.ma_khoa, dk.hoc_phi, dk.giam_tru;

CREATE VIEW v_tien_do AS
SELECT dk.id AS dang_ky_id, dk.ma_ho_so, dk.hinh_thuc_ly_thuyet, h.ma AS hang,
       COALESCE(SUM(CASE WHEN bh.loai = 'LY_THUYET' THEN dd.so_gio END), 0) AS gio_lt_da_hoc,
       h.gio_ly_thuyet                                                     AS gio_lt_quy_dinh,
       COALESCE(SUM(CASE WHEN bh.loai = 'THUC_HANH' THEN dd.so_gio END), 0) AS gio_th_da_hoc,
       h.gio_thuc_hanh                                                     AS gio_th_quy_dinh
FROM dang_ky dk
JOIN khoa_dao_tao k ON k.id = dk.khoa_id
JOIN hang_gplx h    ON h.ma = k.hang_ma
LEFT JOIN diem_danh dd ON dd.dang_ky_id = dk.id AND dd.co_mat = 1
LEFT JOIN buoi_hoc bh  ON bh.id = dd.buoi_hoc_id
GROUP BY dk.id, dk.ma_ho_so, dk.hinh_thuc_ly_thuyet, h.ma, h.gio_ly_thuyet, h.gio_thuc_hanh;

CREATE VIEW v_doanh_thu_thang AS
SELECT DATE_FORMAT(pt.ngay_thu, '%Y-%m') AS thang, k.hang_ma,
       COUNT(*) AS so_phieu, SUM(pt.so_tien) AS doanh_thu
FROM phieu_thu pt
JOIN dang_ky dk     ON dk.id = pt.dang_ky_id
JOIN khoa_dao_tao k ON k.id  = dk.khoa_id
WHERE pt.trang_thai = 'HIEU_LUC'
GROUP BY DATE_FORMAT(pt.ngay_thu, '%Y-%m'), k.hang_ma;
