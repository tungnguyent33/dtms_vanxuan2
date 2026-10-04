-- Flyway V5 - FR-01: doi mat khau, refresh token, khoa tam tai khoan khi dang nhap sai nhieu lan.

ALTER TABLE nguoi_dung
  ADD COLUMN phai_doi_mat_khau TINYINT(1) NOT NULL DEFAULT 0,   -- mat khau tam: bat buoc doi o lan dang nhap ke tiep
  ADD COLUMN so_lan_sai        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  ADD COLUMN khoa_den          DATETIME NULL;                    -- tam khoa sau nhieu lan sai mat khau

-- Tai khoan hoc vien mau dang dung mat khau chung "123456" -> buoc doi khi dang nhap
UPDATE nguoi_dung SET phai_doi_mat_khau = 1 WHERE vai_tro_id = 4;

-- Chi luu BAM SHA-256 cua refresh token: lo CSDL cung khong dung lai duoc token.
CREATE TABLE refresh_token (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nguoi_dung_id  BIGINT UNSIGNED NOT NULL,
  token_hash     CHAR(64) NOT NULL UNIQUE,
  het_han        DATETIME NOT NULL,
  thu_hoi        TINYINT(1) NOT NULL DEFAULT 0,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rt_nd FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
);
CREATE INDEX idx_rt_nd ON refresh_token(nguoi_dung_id);
