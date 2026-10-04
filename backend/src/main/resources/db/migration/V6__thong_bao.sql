-- Flyway V6 - FR-15, FR-16, BR-12: thong bao trong he thong va nhac viec dinh ky.

ALTER TABLE thong_bao
  ADD COLUMN loai        VARCHAR(30)  NOT NULL DEFAULT 'CHUNG',  -- HO_SO, HOC_PHI, LICH_HOC, NHAC_NO, BAO_CAO_SO, ...
  ADD COLUMN duong_dan   VARCHAR(255) NULL,                      -- trang can mo khi bam vao thong bao
  ADD COLUMN ma_su_kien  VARCHAR(100) NULL,                      -- chong gui trung khi tac vu dinh ky chay lai
  ADD CONSTRAINT uq_tb_su_kien UNIQUE (nguoi_nhan_id, ma_su_kien);

CREATE INDEX idx_tb_nguoi_nhan ON thong_bao(nguoi_nhan_id, da_doc, created_at);
