import dayjs from 'dayjs'

export const tien = (v?: number | null) =>
  v == null ? '' : new Intl.NumberFormat('vi-VN').format(v) + ' đ'

export const ngay = (v?: string | null) => (v ? dayjs(v).format('DD/MM/YYYY') : '')

export const ngayGio = (v?: string | null) => (v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '')

export const gio = (v?: string | null) => (v ? v.slice(0, 5) : '')

export const NHAN_TRANG_THAI_DK: Record<string, { text: string; color: string }> = {
  CHO_DUYET: { text: 'Chờ duyệt', color: 'orange' },
  DA_TIEP_NHAN: { text: 'Đã tiếp nhận', color: 'blue' },
  DANG_HOC: { text: 'Đang học', color: 'processing' },
  CHUA_DAT: { text: 'Chưa đạt', color: 'red' },
  HOAN_THANH: { text: 'Hoàn thành', color: 'green' },
  SAT_HACH_TRUOT: { text: 'Trượt sát hạch', color: 'volcano' },
  DA_SAT_HACH_DAT: { text: 'Đạt sát hạch', color: 'success' },
  DA_HUY: { text: 'Đã hủy', color: 'default' },
}

export const NHAN_TRANG_THAI_KHOA: Record<string, { text: string; color: string }> = {
  DU_KIEN: { text: 'Dự kiến', color: 'default' },
  DANG_TUYEN: { text: 'Đang tuyển', color: 'blue' },
  DANG_DAO_TAO: { text: 'Đang đào tạo', color: 'processing' },
  DA_KET_THUC: { text: 'Đã kết thúc', color: 'green' },
  HUY: { text: 'Hủy', color: 'red' },
}

export const NHAN_HINH_THUC: Record<string, string> = {
  TU_HOC: 'Tự học LT',
  TAP_TRUNG: 'Học tập trung',
}

export const NHAN_NGUON: Record<string, string> = {
  TRUC_TIEP: 'Trực tiếp',
  TRUC_TUYEN: 'Trực tuyến',
  CTV: 'Cộng tác viên',
  HOC_VIEN_GIOI_THIEU: 'Học viên giới thiệu',
}

export const NHAN_TRANG_THAI_CTV: Record<string, { text: string; color: string }> = {
  CHO_DUYET: { text: 'Chờ duyệt', color: 'orange' },
  HOAT_DONG: { text: 'Hoạt động', color: 'green' },
  TAM_KHOA: { text: 'Tạm khóa', color: 'red' },
  NGUNG: { text: 'Ngừng', color: 'default' },
}

export const NHAN_HANG_CTV: Record<string, { text: string; color: string }> = {
  THUONG: { text: 'Thường', color: 'default' },
  BAC: { text: 'Bạc', color: 'geekblue' },
  VANG: { text: 'Vàng', color: 'gold' },
}

export const NHAN_LOAI_CTV: Record<string, string> = {
  HOC_VIEN_CU: 'Học viên cũ',
  SINH_VIEN: 'Sinh viên',
  DOI_TAC: 'Đối tác ngoài',
}

export const NHAN_TRANG_THAI_LEAD: Record<string, { text: string; color: string }> = {
  MOI: { text: 'Mới', color: 'blue' },
  DA_LIEN_HE: { text: 'Đã liên hệ', color: 'cyan' },
  DA_CHOT: { text: 'Đã chốt', color: 'green' },
  KHONG_THANH: { text: 'Không thành', color: 'default' },
}

export const NHAN_NGUON_LEAD: Record<string, string> = {
  CTV: 'CTV',
  HOC_VIEN: 'Học viên giới thiệu',
  VAN_PHONG: 'Văn phòng',
}

export const NHAN_TRANG_THAI_HOA_HONG: Record<string, { text: string; color: string }> = {
  CHUA_DU_DIEU_KIEN: { text: 'Chưa đủ điều kiện', color: 'default' },
  DU_DIEU_KIEN: { text: 'Đủ điều kiện', color: 'blue' },
  DA_DUYET: { text: 'Đã duyệt', color: 'purple' },
  DA_CHI: { text: 'Đã chi', color: 'green' },
  HUY: { text: 'Hủy', color: 'red' },
}

/** Muc hoa hong: 10% hoac 150.000 d */
export const mucHoaHong = (kieu?: string, giaTri?: number) =>
  kieu == null || giaTri == null ? '—' : kieu === 'PHAN_TRAM' ? `${giaTri}%` : tien(giaTri)

export const NHAN_LOAI_GIANG_DAY: Record<string, string> = {
  LY_THUYET: 'Lý thuyết',
  THUC_HANH: 'Thực hành',
  CA_HAI: 'Lý thuyết & thực hành',
}

export const NHAN_TRANG_THAI_XE: Record<string, { text: string; color: string }> = {
  SAN_SANG: { text: 'Sẵn sàng', color: 'green' },
  BAO_DUONG: { text: 'Bảo dưỡng', color: 'orange' },
  NGUNG: { text: 'Ngừng dùng', color: 'default' },
}

export const NHAN_VAI_TRO: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  LE_TAN: 'Lễ tân',
  GIAO_VIEN: 'Giáo viên',
  HOC_VIEN: 'Học viên',
  CTV: 'Cộng tác viên',
}
