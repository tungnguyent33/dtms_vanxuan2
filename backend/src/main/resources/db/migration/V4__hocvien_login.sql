INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau_hash, ho_ten, so_dien_thoai, vai_tro_id, trang_thai) VALUES
 (4,'025205000001','$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2','Phạm Văn Mẫu','0922222221',4, 1),
 (5,'025307000002','$2a$10$BT8/E6B2jJgsawzdvcvuzuc0FJ/vr5Qx4pr.RjBpzJo69mUvEJrP2','Đỗ Thị Thử','0922222222',4, 1);

UPDATE hoc_vien SET nguoi_dung_id = 4 WHERE id = 1;
UPDATE hoc_vien SET nguoi_dung_id = 5 WHERE id = 2;
