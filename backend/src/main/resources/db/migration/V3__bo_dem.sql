-- Flyway V3 - Bo dem cap so tu tang (ma hoc vien, ma ho so, so phieu thu, so giay xac nhan).
-- Moi lan cap so, service khoa dong (SELECT ... FOR UPDATE) de hai nguoi dung
-- lap phieu cung luc khong bi trung so (xem UC11, luong re nhanh 4b).
CREATE TABLE bo_dem (
  ten       VARCHAR(30) PRIMARY KEY,   -- VD: HV2026, HS-A1, PT-202610, XN2026
  gia_tri   BIGINT UNSIGNED NOT NULL DEFAULT 0
);

-- Dong bo voi du lieu mau o V2
INSERT INTO bo_dem (ten, gia_tri) VALUES
 ('HV2026', 2),
 ('HS-A1', 2),
 ('PT-202610', 1);
