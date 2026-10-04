-- =====================================================================
-- Flyway V7 - Module cong tac vien (CTV): ho so CTV co duyet, lead, chinh sach hoa hong,
-- hoa hong tinh tu dong theo dang ky, chi tra theo ky thang.
-- Nguyen tac: nguoi nhap != nguoi duyet (le tan tao CTV/lead, admin duyet CTV va chi tien).
-- =====================================================================

INSERT INTO vai_tro (id, ma, ten) VALUES (5, 'CTV', 'Cộng tác viên');

-- ---------------------------------------------------------------------
-- 1. Ho so CTV
-- ---------------------------------------------------------------------
ALTER TABLE cong_tac_vien
  ADD COLUMN cccd            CHAR(12) NULL UNIQUE,                 -- bat buoc voi CTV moi (doi soat khi tra tien)
  ADD COLUMN zalo            VARCHAR(15) NULL,
  ADD COLUMN dia_chi         VARCHAR(255) NULL,
  ADD COLUMN loai            ENUM('HOC_VIEN_CU','SINH_VIEN','DOI_TAC') NOT NULL DEFAULT 'DOI_TAC',
  ADD COLUMN hang            ENUM('THUONG','BAC','VANG') NOT NULL DEFAULT 'THUONG',
  ADD COLUMN tinh_trang      ENUM('CHO_DUYET','HOAT_DONG','TAM_KHOA','NGUNG') NOT NULL DEFAULT 'CHO_DUYET',
  ADD COLUMN ngan_hang       VARCHAR(100) NULL,
  ADD COLUMN so_tai_khoan    VARCHAR(30) NULL,
  ADD COLUMN chu_tai_khoan   VARCHAR(100) NULL,
  ADD COLUMN cam_ket_url     VARCHAR(255) NULL,                    -- anh / scan cam ket da ky (thu muc uploads, khong public)
  ADD COLUMN ngay_bat_dau    DATE NULL,
  ADD COLUMN ghi_chu         VARCHAR(255) NULL,
  ADD COLUMN hoc_vien_id     BIGINT UNSIGNED NULL UNIQUE,          -- CTV la hoc vien cu / dang hoc
  ADD COLUMN nguoi_dung_id   BIGINT UNSIGNED NULL UNIQUE,          -- tai khoan dang nhap vai tro CTV
  ADD COLUMN nguoi_tao_id    BIGINT UNSIGNED NULL,
  ADD COLUMN created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN nguoi_duyet_id  BIGINT UNSIGNED NULL,
  ADD COLUMN ngay_duyet      DATETIME NULL,
  ADD CONSTRAINT fk_ctv_hv    FOREIGN KEY (hoc_vien_id) REFERENCES hoc_vien(id),
  ADD CONSTRAINT fk_ctv_nd    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
  ADD CONSTRAINT fk_ctv_tao   FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id),
  ADD CONSTRAINT fk_ctv_duyet FOREIGN KEY (nguoi_duyet_id) REFERENCES nguoi_dung(id);

-- Chuyen trang thai cu (1/0) sang 4 trang thai; muc hoa hong co dinh cu thay bang bang chinh sach
UPDATE cong_tac_vien SET tinh_trang = IF(trang_thai = 1, 'HOAT_DONG', 'NGUNG'), ngay_bat_dau = CURRENT_DATE;
ALTER TABLE cong_tac_vien DROP COLUMN trang_thai, DROP COLUMN muc_hoa_hong;
ALTER TABLE cong_tac_vien CHANGE COLUMN tinh_trang trang_thai
  ENUM('CHO_DUYET','HOAT_DONG','TAM_KHOA','NGUNG') NOT NULL DEFAULT 'CHO_DUYET';

-- ---------------------------------------------------------------------
-- 2. Chinh sach hoa hong theo hang CTV x hang GPLX x thoi diem hieu luc
-- ---------------------------------------------------------------------
CREATE TABLE chinh_sach_hoa_hong (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  hang_ctv      ENUM('THUONG','BAC','VANG') NOT NULL,
  hang_gplx     VARCHAR(5) NOT NULL,
  kieu          ENUM('PHAN_TRAM','CO_DINH') NOT NULL,
  gia_tri       DECIMAL(12,2) NOT NULL,                  -- % hoc phi hoac so tien VND
  hieu_luc_tu   DATE NOT NULL,
  nguoi_tao_id  BIGINT UNSIGNED NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cs_hang FOREIGN KEY (hang_gplx) REFERENCES hang_gplx(ma),
  CONSTRAINT fk_cs_nd   FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id),
  CONSTRAINT uq_cs UNIQUE (hang_ctv, hang_gplx, hieu_luc_tu),
  CONSTRAINT ck_cs_gia_tri CHECK (gia_tri >= 0 AND (kieu <> 'PHAN_TRAM' OR gia_tri <= 100))
);

-- Muc minh hoa (khong phai so lieu that cua trung tam)
INSERT INTO chinh_sach_hoa_hong (hang_ctv, hang_gplx, kieu, gia_tri, hieu_luc_tu) VALUES
 ('THUONG','A1','CO_DINH',100000,'2026-01-01'), ('BAC','A1','CO_DINH',150000,'2026-01-01'), ('VANG','A1','CO_DINH',200000,'2026-01-01'),
 ('THUONG','A', 'CO_DINH',200000,'2026-01-01'), ('BAC','A', 'CO_DINH',300000,'2026-01-01'), ('VANG','A', 'CO_DINH',400000,'2026-01-01');

-- Tham so he thong dang khoa - gia tri (de admin chinh tren giao dien)
CREATE TABLE cau_hinh (
  khoa     VARCHAR(60) PRIMARY KEY,
  gia_tri  VARCHAR(255) NOT NULL,
  mo_ta    VARCHAR(255)
);
INSERT INTO cau_hinh (khoa, gia_tri, mo_ta) VALUES
 ('HOA_HONG_PHAN_TRAM_DONG', '100', 'Hoa hồng phát sinh khi học viên đã đóng ít nhất bấy nhiêu % học phí'),
 ('HOA_HONG_NGAY_CHI', '5', 'Ngày chi hoa hồng hằng tháng (cho kỳ tháng trước)');

-- ---------------------------------------------------------------------
-- 3. Lead: khach tiem nang do CTV / hoc vien / van phong gioi thieu
-- ---------------------------------------------------------------------
CREATE TABLE lead_khach (
  id                       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ho_ten                   VARCHAR(100) NOT NULL,
  so_dien_thoai            VARCHAR(15)  NOT NULL,
  dia_chi                  VARCHAR(255) NULL,
  hang_muon_hoc            VARCHAR(5)   NULL,
  ghi_chu                  VARCHAR(255) NULL,
  nguon                    ENUM('CTV','HOC_VIEN','VAN_PHONG') NOT NULL,
  ctv_id                   BIGINT UNSIGNED NULL,
  hoc_vien_gioi_thieu_id   BIGINT UNSIGNED NULL,
  trang_thai               ENUM('MOI','DA_LIEN_HE','DA_CHOT','KHONG_THANH') NOT NULL DEFAULT 'MOI',
  dang_ky_id               BIGINT UNSIGNED NULL UNIQUE,
  nguoi_nhap_id            BIGINT UNSIGNED NULL,
  created_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_lead_hang FOREIGN KEY (hang_muon_hoc) REFERENCES hang_gplx(ma),
  CONSTRAINT fk_lead_ctv  FOREIGN KEY (ctv_id) REFERENCES cong_tac_vien(id),
  CONSTRAINT fk_lead_hv   FOREIGN KEY (hoc_vien_gioi_thieu_id) REFERENCES hoc_vien(id),
  CONSTRAINT fk_lead_dk   FOREIGN KEY (dang_ky_id) REFERENCES dang_ky(id),
  CONSTRAINT fk_lead_nd   FOREIGN KEY (nguoi_nhap_id) REFERENCES nguoi_dung(id),
  CONSTRAINT ck_lead_nguon CHECK (nguon <> 'CTV' OR ctv_id IS NOT NULL)
);
CREATE INDEX idx_lead_sdt ON lead_khach(so_dien_thoai);

-- Nguon "hoc vien gioi thieu" cho dang ky
ALTER TABLE dang_ky
  MODIFY COLUMN nguon ENUM('TRUC_TIEP','TRUC_TUYEN','CTV','HOC_VIEN_GIOI_THIEU') NOT NULL DEFAULT 'TRUC_TIEP',
  ADD COLUMN gioi_thieu_hoc_vien_id BIGINT UNSIGNED NULL,
  ADD CONSTRAINT fk_dk_hv_gt FOREIGN KEY (gioi_thieu_hoc_vien_id) REFERENCES hoc_vien(id);

-- ---------------------------------------------------------------------
-- 4. Hoa hong (mot dong / mot dang ky co CTV) va phieu chi hoa hong
-- ---------------------------------------------------------------------
CREATE TABLE chi_hoa_hong (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  so_phieu      VARCHAR(20) NOT NULL UNIQUE,              -- PC-yyyyMM-nnnn
  ctv_id        BIGINT UNSIGNED NOT NULL,
  ky            CHAR(7) NOT NULL,                         -- yyyy-MM: ky hoa hong duoc chi
  so_tien       DECIMAL(12,0) NOT NULL,
  hinh_thuc     ENUM('TIEN_MAT','CHUYEN_KHOAN') NOT NULL,
  ngay_chi      DATE NOT NULL,
  nguoi_chi_id  BIGINT UNSIGNED NOT NULL,
  ghi_chu       VARCHAR(255) NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chi_ctv FOREIGN KEY (ctv_id) REFERENCES cong_tac_vien(id),
  CONSTRAINT fk_chi_nd  FOREIGN KEY (nguoi_chi_id) REFERENCES nguoi_dung(id),
  CONSTRAINT ck_chi_tien CHECK (so_tien > 0)
);

CREATE TABLE hoa_hong (
  id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  dang_ky_id         BIGINT UNSIGNED NOT NULL UNIQUE,
  ctv_id             BIGINT UNSIGNED NOT NULL,
  chinh_sach_id      BIGINT UNSIGNED NULL,
  kieu               ENUM('PHAN_TRAM','CO_DINH') NULL,     -- chup lai muc ap dung luc dang ky
  gia_tri            DECIMAL(12,2) NULL,
  co_so              DECIMAL(12,0) NOT NULL DEFAULT 0,      -- hoc phi phai dong luc tinh
  so_tien            DECIMAL(12,0) NOT NULL DEFAULT 0,
  trang_thai         ENUM('CHUA_DU_DIEU_KIEN','DU_DIEU_KIEN','DA_DUYET','DA_CHI','HUY') NOT NULL DEFAULT 'CHUA_DU_DIEU_KIEN',
  ngay_du_dieu_kien  DATE NULL,
  ky                 CHAR(7) NULL,                          -- yyyy-MM: thang du dieu kien
  nguoi_duyet_id     BIGINT UNSIGNED NULL,
  ngay_duyet         DATETIME NULL,
  chi_hoa_hong_id    BIGINT UNSIGNED NULL,
  ghi_chu            VARCHAR(255) NULL,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hh_dk    FOREIGN KEY (dang_ky_id) REFERENCES dang_ky(id),
  CONSTRAINT fk_hh_ctv   FOREIGN KEY (ctv_id) REFERENCES cong_tac_vien(id),
  CONSTRAINT fk_hh_cs    FOREIGN KEY (chinh_sach_id) REFERENCES chinh_sach_hoa_hong(id),
  CONSTRAINT fk_hh_duyet FOREIGN KEY (nguoi_duyet_id) REFERENCES nguoi_dung(id),
  CONSTRAINT fk_hh_chi   FOREIGN KEY (chi_hoa_hong_id) REFERENCES chi_hoa_hong(id)
);
CREATE INDEX idx_hh_ctv_tt ON hoa_hong(ctv_id, trang_thai);

-- Hoa hong cho cac dang ky co CTV da co tu truoc (CTV cu mac dinh hang THUONG); trang thai tinh lai trong ung dung
INSERT INTO hoa_hong (dang_ky_id, ctv_id, chinh_sach_id, kieu, gia_tri, co_so, so_tien, trang_thai)
SELECT dk.id, dk.ctv_id, cs.id, cs.kieu, cs.gia_tri, dk.hoc_phi - dk.giam_tru,
       CASE WHEN cs.kieu = 'CO_DINH' THEN cs.gia_tri
            ELSE ROUND((dk.hoc_phi - dk.giam_tru) * cs.gia_tri / 100 / 1000) * 1000 END,
       IF(dk.trang_thai = 'DA_HUY', 'HUY', 'CHUA_DU_DIEU_KIEN')
FROM dang_ky dk
JOIN khoa_dao_tao k ON k.id = dk.khoa_id
JOIN chinh_sach_hoa_hong cs ON cs.hang_ctv = 'THUONG' AND cs.hang_gplx = k.hang_ma AND cs.hieu_luc_tu = '2026-01-01'
WHERE dk.ctv_id IS NOT NULL;
