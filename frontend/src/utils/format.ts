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
}

export const NHAN_VAI_TRO: Record<string, string> = {
  ADMIN: 'Quản trị viên',
  LE_TAN: 'Lễ tân',
  GIAO_VIEN: 'Giáo viên',
  HOC_VIEN: 'Học viên',
}
