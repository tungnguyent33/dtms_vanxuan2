// Kieu du lieu khop voi DTO cua backend (xem cac file *Dtos.java)

export type VaiTro = 'ADMIN' | 'LE_TAN' | 'GIAO_VIEN' | 'HOC_VIEN'

export interface LoginResponse {
  token: string
  hetHanSauPhut: number
  id: number
  tenDangNhap: string
  hoTen: string
  vaiTro: VaiTro
}

export interface ApiError {
  code: string
  message: string
  details?: Record<string, string>
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface HangGplx {
  ma: string
  ten: string
  gioLyThuyet: number
  gioThucHanh: number
  tuoiToiThieu: number
  soNgayKhoaToiDa: number
  hocPhiMacDinh: number
  canCuPhapLy?: string
}

export type TrangThaiKhoa = 'DU_KIEN' | 'DANG_TUYEN' | 'DANG_DAO_TAO' | 'DA_KET_THUC' | 'HUY'

export interface Khoa {
  id: number
  maKhoa: string
  hangMa: string
  ngayKhaiGiang: string
  ngayBeGiang: string
  soNgay: number
  siSoToiDa: number
  soDangKy: number
  hocPhi: number
  trangThai: TrangThaiKhoa
  ngayBaoCaoSo?: string
  ghiChu?: string
}

export interface HocVien {
  id: number
  maHocVien: string
  hoTen: string
  ngaySinh: string
  gioiTinh: 'NAM' | 'NU'
  cccd: string
  ngayCapCccd?: string
  diaChi: string
  soDienThoai: string
  email?: string
  coAnhChanDung: boolean
  coAnhCccd: boolean
}

export type TrangThaiDangKy =
  | 'CHO_DUYET' | 'DA_TIEP_NHAN' | 'DANG_HOC' | 'CHUA_DAT' | 'HOAN_THANH'
  | 'SAT_HACH_TRUOT' | 'DA_SAT_HACH_DAT' | 'DA_HUY'

export interface DangKy {
  id: number
  maHoSo: string
  hocVien: HocVien
  khoaId: number
  maKhoa: string
  hang: string
  hinhThucLyThuyet: 'TU_HOC' | 'TAP_TRUNG'
  nguon: 'TRUC_TIEP' | 'TRUC_TUYEN' | 'CTV'
  ctvId?: number
  hocPhi: number
  giamTru: number
  trangThai: TrangThaiDangKy
  ngayDangKy: string
  soGiayXacNhan?: string
  ngayHoanThanh?: string
  ghiChu?: string
}

export interface TienDo {
  dangKyId: number
  hang: string
  hinhThucLyThuyet: string
  gioLyThuyetDaHoc: number
  gioLyThuyetQuyDinh: number
  xetLyThuyet: boolean
  gioThucHanhDaHoc: number
  gioThucHanhQuyDinh: number
  duLyThuyet: boolean
  duThucHanh: boolean
}

export interface PhieuThu {
  id: number
  soPhieu: string
  dangKyId: number
  soTien: number
  hinhThuc: 'TIEN_MAT' | 'CHUYEN_KHOAN' | 'VNPAY'
  noiDung: string
  ngayThu: string
  nguoiThu: string
  trangThai: 'HIEU_LUC' | 'DA_HUY'
  lyDoHuy?: string
}

export interface CongNo {
  phaiDong: number
  daDong: number
  conNo: number
}

export interface DongCongNo {
  dangKyId: number
  maHoSo: string
  trangThai: string
  hoTen: string
  soDienThoai: string
  khoaId: number
  maKhoa: string
  phaiDong: number
  daDong: number
  conNo: number
}

export interface GiaoVien {
  id: number
  hoTen: string
  soDienThoai: string
  loaiGiangDay: string
}

export interface Xe {
  id: number
  bienSo: string
  hang: string
  nhanHieu?: string
  trangThai: string
}

export interface Ctv {
  id: number
  hoTen: string
  soDienThoai: string
  diaBan?: string
}

export interface BuoiHoc {
  id: number
  khoaId: number
  maKhoa: string
  loai: 'LY_THUYET' | 'THUC_HANH'
  ngay: string
  gioBatDau: string
  gioKetThuc: string
  thoiLuongGio: number
  diaDiem: string
  giaoVienId: number
  giaoVien: string
  xeId?: number
  bienSoXe?: string
  trangThai: 'KE_HOACH' | 'DA_DAY' | 'HUY'
}

export interface DongDiemDanh {
  dangKyId: number
  maHoSo: string
  hoTen: string
  soDienThoai: string
  hinhThucLyThuyet: string
  coMat: boolean | null
  soGio: number | null
  ghiChu: string | null
}

export interface DanhSachBuoi {
  buoi: BuoiHoc
  hocVien: DongDiemDanh[]
}

export interface KetQuaXet {
  dangKyId: number
  maHoSo: string
  hoTen: string
  hinhThucLyThuyet: string
  gioLyThuyet: number
  gioLyThuyetQuyDinh: number
  gioThucHanh: number
  gioThucHanhQuyDinh: number
  conNo: number
  dat: boolean
  lyDo: string[]
}

export interface TongQuan {
  hocVienDangHoc: number
  hoSoChoDuyet: number
  khoaDangTuyen: number
  hoanThanhTrongNam: number
  doanhThuThangNay: number
  tongCongNo: number
}

export interface DongDoanhThu {
  ky: string
  hang: string
  soPhieu: number
  doanhThu: number
}

export interface KhoaCongKhai {
  id: number
  maKhoa: string
  hang: string
  tenHang: string
  ngayKhaiGiang: string
  ngayBeGiang: string
  hocPhi: number
  conCho: number
}
