// Kieu du lieu khop voi DTO cua backend (xem cac file *Dtos.java)

export type VaiTro = 'ADMIN' | 'LE_TAN' | 'GIAO_VIEN' | 'HOC_VIEN' | 'CTV'

export interface LoginResponse {
  token: string
  hetHanSauPhut: number
  id: number
  tenDangNhap: string
  hoTen: string
  vaiTro: VaiTro
  refreshToken: string
  phaiDoiMatKhau: boolean
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
  dangApDung: boolean
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
  coTaiKhoan: boolean
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
  nguon: 'TRUC_TIEP' | 'TRUC_TUYEN' | 'CTV' | 'HOC_VIEN_GIOI_THIEU'
  ctvId?: number
  tenCtv?: string
  gioiThieuHocVienId?: number
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
  soGiayChungNhanGv?: string
  loaiGiangDay: 'LY_THUYET' | 'THUC_HANH' | 'CA_HAI'
  hoatDong: boolean
  tenDangNhap?: string
}

export interface Xe {
  id: number
  bienSo: string
  hang: string
  nhanHieu?: string
  namSanXuat?: number
  trangThai: 'SAN_SANG' | 'BAO_DUONG' | 'NGUNG'
  ngayBaoDuongTiep?: string
}

export type TrangThaiCtv = 'CHO_DUYET' | 'HOAT_DONG' | 'TAM_KHOA' | 'NGUNG'
export type HangCtv = 'THUONG' | 'BAC' | 'VANG'
export type LoaiCtv = 'HOC_VIEN_CU' | 'SINH_VIEN' | 'DOI_TAC'

export interface Ctv {
  id: number
  hoTen: string
  soDienThoai: string
  zalo?: string
  cccd?: string
  diaChi?: string
  diaBan?: string
  loai: LoaiCtv
  hang: HangCtv
  trangThai: TrangThaiCtv
  nganHang?: string
  soTaiKhoan?: string
  chuTaiKhoan?: string
  coCamKet: boolean
  ngayBatDau?: string
  ghiChu?: string
  hocVienId?: number
  tenDangNhap?: string
  nguoiTaoId?: number
  nguoiTao?: string
  createdAt?: string
  nguoiDuyet?: string
  ngayDuyet?: string
  soLead: number
  soHocVien: number
}

export type TrangThaiLead = 'MOI' | 'DA_LIEN_HE' | 'DA_CHOT' | 'KHONG_THANH'

export interface Lead {
  id: number
  hoTen: string
  soDienThoai: string
  diaChi?: string
  hangMuonHoc?: string
  ghiChu?: string
  nguon: 'CTV' | 'HOC_VIEN' | 'VAN_PHONG'
  ctvId?: number
  tenCtv?: string
  hocVienGioiThieuId?: number
  trangThai: TrangThaiLead
  dangKyId?: number
  maHoSo?: string
  nguoiNhap?: string
  createdAt: string
}

export type TrangThaiHoaHong = 'CHUA_DU_DIEU_KIEN' | 'DU_DIEU_KIEN' | 'DA_DUYET' | 'DA_CHI' | 'HUY'

export interface HoaHong {
  id: number
  dangKyId: number
  maHoSo: string
  hocVien: string
  maKhoa: string
  hang: string
  ctvId: number
  tenCtv: string
  kieu?: 'PHAN_TRAM' | 'CO_DINH'
  giaTri?: number
  coSo: number
  soTien: number
  trangThai: TrangThaiHoaHong
  ngayDuDieuKien?: string
  ky?: string
  ngayDuyet?: string
  soPhieuChi?: string
  ghiChu?: string
}

export interface TongHopKy {
  ctvId: number
  tenCtv: string
  soDienThoai: string
  nganHang?: string
  soTaiKhoan?: string
  chuTaiKhoan?: string
  soDon: number
  duDieuKien: number
  daDuyet: number
  daChi: number
}

export interface PhieuChi {
  id: number
  soPhieu: string
  ctvId: number
  tenCtv: string
  ky: string
  soTien: number
  hinhThuc: 'TIEN_MAT' | 'CHUYEN_KHOAN'
  ngayChi: string
  nguoiChi?: string
  ghiChu?: string
  soDon: number
}

export interface ChinhSach {
  id: number
  hangCtv: HangCtv
  hangGplx: string
  kieu: 'PHAN_TRAM' | 'CO_DINH'
  giaTri: number
  hieuLucTu: string
  dangApDung: boolean
  soDonDaDung: number
}

export interface BaoCaoCtv {
  ctvId: number
  tenCtv: string
  hang: HangCtv
  trangThai: TrangThaiCtv
  soLead: number
  soChot: number
  tyLeChot: number
  soHocVien: number
  doanhThu: number
  hoaHongPhatSinh: number
  daChi: number
  conPhaiTra: number
}

export interface CtvCuaToi {
  ctvId: number
  hoTen: string
  trangThai: TrangThaiCtv
  hang: HangCtv
  soLead: number
  soChot: number
  choDuyet: number
  choChi: number
  daNhan: number
}

export interface NguoiDung {
  id: number
  tenDangNhap: string
  hoTen: string
  soDienThoai?: string
  email?: string
  vaiTro: VaiTro
  hoatDong: boolean
  phaiDoiMatKhau: boolean
  dangKhoaTam: boolean
  lanDangNhapCuoi?: string
}

/** Mat khau tam chi tra ve mot lan khi cap / dat lai. */
export interface KetQuaCapMatKhau {
  nguoiDungId: number
  tenDangNhap: string
  matKhauTam: string
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

export interface HangCongKhai {
  ma: string
  ten: string
  gioLyThuyet: number
  gioThucHanh: number
  tuoiToiThieu: number
  soNgayKhoaToiDa: number
  canCuPhapLy?: string
}

export interface KetQuaTraCuu {
  maHoSo: string
  hoTen: string
  hang: string
  maKhoa: string
  ngayKhaiGiang: string
  ngayBeGiang: string
  trangThai: string
  ngayDangKy: string
}

export interface ThongBao {
  id: number
  loai: string
  tieuDe: string
  noiDung: string
  duongDan?: string
  daDoc: boolean
  thoiGian: string
}
