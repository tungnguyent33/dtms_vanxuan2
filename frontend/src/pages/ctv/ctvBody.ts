import type { Dayjs } from 'dayjs'
import type { LoaiCtv } from '../../api/types'

export interface CtvFormValues {
  hoTen: string
  soDienThoai: string
  zalo?: string
  cccd: string
  diaChi?: string
  diaBan?: string
  loai: LoaiCtv
  nganHang?: string
  soTaiKhoan?: string
  chuTaiKhoan?: string
  ngayBatDau?: Dayjs
  ghiChu?: string
}

/** Doi gia tri form sang body API (ngay dang yyyy-MM-dd). */
export const ctvBody = (v: CtvFormValues) => ({ ...v, ngayBatDau: v.ngayBatDau?.format('YYYY-MM-DD') })
