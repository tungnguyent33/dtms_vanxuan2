-- Flyway V2 - Dữ liệu mẫu (giả lập, không phải dữ liệu thật của học viên; học phí là số minh họa)

INSERT INTO vai_tro (id, ma, ten) VALUES
 (1,'ADMIN','Quản trị viên'), (2,'LE_TAN','Lễ tân / Tuyển sinh'),
 (3,'GIAO_VIEN','Giáo viên'), (4,'HOC_VIEN','Học viên');

-- Mật khẩu mẫu: "123456" (BCrypt) – đổi ngay khi triển khai
INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau_hash, ho_ten, so_dien_thoai, vai_tro_id) VALUES
 (1,'admin','$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2','Quản trị Vạn Xuân','0900000001',1),
 (2,'letan01','$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2','Nguyễn Thị Lễ Tân','0900000002',2),
 (3,'gv01','$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2','Trần Văn Giáo','0900000003',3);

-- Số giờ theo Thông tư 14/2025/TT-BXD sửa đổi bởi 17/2026/TT-BXD (hiệu lực 01/7/2026)
INSERT INTO hang_gplx (ma, ten, gio_ly_thuyet, gio_thuc_hanh, tuoi_toi_thieu, so_ngay_khoa_toi_da, hoc_phi_mac_dinh, can_cu_phap_ly) VALUES
 ('A1','Mô tô hai bánh dung tích đến 125 cm3 hoặc công suất đến 11 kW', 9, 3, 18, 10, 0,'TT 14/2025/TT-BXD, sửa đổi bởi TT 17/2026/TT-BXD'),
 ('A', 'Mô tô hai bánh dung tích trên 125 cm3 hoặc công suất trên 11 kW', 20, 12, 18, 10, 0,'TT 14/2025/TT-BXD, sửa đổi bởi TT 17/2026/TT-BXD');

INSERT INTO giao_vien (id, nguoi_dung_id, ho_ten, so_dien_thoai, so_giay_chung_nhan_gv, loai_giang_day) VALUES
 (1,3,'Trần Văn Giáo','0900000003','GV-PT-0001','CA_HAI');

INSERT INTO xe_tap_lai (id, bien_so, hang_ma, nhan_hieu, nam_san_xuat) VALUES
 (1,'19-TL 000.01','A1','Honda Wave',2023),
 (2,'19-TL 000.02','A','Honda CB150',2024);

INSERT INTO cong_tac_vien (id, ho_ten, so_dien_thoai, dia_ban, muc_hoa_hong) VALUES
 (1,'Lê Văn Cộng Tác','0911111111','Tam Nông',100000);

INSERT INTO khoa_dao_tao (id, ma_khoa, hang_ma, ngay_khai_giang, ngay_be_giang, si_so_toi_da, hoc_phi, trang_thai) VALUES
 (1,'A1-2026-10-01','A1','2026-10-05','2026-10-11',60,1500000,'DANG_TUYEN');

INSERT INTO hoc_vien (id, ma_hoc_vien, ho_ten, ngay_sinh, gioi_tinh, cccd, dia_chi, so_dien_thoai) VALUES
 (1,'HV2026000001','Phạm Văn Mẫu','2005-03-12','NAM','025205000001','Tam Nông, Phú Thọ','0922222221'),
 (2,'HV2026000002','Đỗ Thị Thử','2007-08-20','NU','025307000002','Tam Nông, Phú Thọ','0922222222');

INSERT INTO dang_ky (id, ma_ho_so, hoc_vien_id, khoa_id, hinh_thuc_ly_thuyet, nguon, ctv_id, hoc_phi, trang_thai, nguoi_tiep_nhan_id) VALUES
 (1,'HS-A1-0001',1,1,'TU_HOC','TRUC_TIEP',NULL,1500000,'DA_TIEP_NHAN',2),
 (2,'HS-A1-0002',2,1,'TAP_TRUNG','CTV',1,1500000,'DA_TIEP_NHAN',2);

INSERT INTO buoi_hoc (id, khoa_id, loai, ngay, gio_bat_dau, gio_ket_thuc, dia_diem, giao_vien_id, xe_id) VALUES
 (1,1,'LY_THUYET','2026-10-05','08:00','12:30','Phòng học VX-01',1,NULL),
 (2,1,'THUC_HANH','2026-10-06','07:30','10:30','Sân tập Vạn Xuân',1,1);

INSERT INTO diem_danh (buoi_hoc_id, dang_ky_id, co_mat, so_gio, nguoi_ghi_id) VALUES
 (1,2,1,4.5,3),
 (2,1,1,3,3),
 (2,2,1,3,3);

INSERT INTO phieu_thu (so_phieu, dang_ky_id, so_tien, hinh_thuc, noi_dung, ngay_thu, nguoi_thu_id) VALUES
 ('PT-202610-0001',1,500000,'TIEN_MAT','Học phí A1 – đợt 1','2026-10-05 09:00:00',2);
