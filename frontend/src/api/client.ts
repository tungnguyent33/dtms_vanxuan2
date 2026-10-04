import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiError, LoginResponse } from './types'

export const TOKEN_KEY = 'dtms_token'
export const REFRESH_KEY = 'dtms_refresh'
export const USER_KEY = 'dtms_user'

/** Axios dung chung: tu gan JWT, tu lam moi phien khi access token het han. */
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** Luu phien moi (dang nhap, lam moi, doi mat khau). */
export function luuPhien(data: LoginResponse) {
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(REFRESH_KEY, data.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify({
    id: data.id, tenDangNhap: data.tenDangNhap, hoTen: data.hoTen, vaiTro: data.vaiTro,
    phaiDoiMatKhau: data.phaiDoiMatKhau,
  }))
}

export function xoaPhien() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

// Nhieu request cung gap 401 thi chi goi /auth/refresh mot lan (refresh token xoay vong, dung lai se bi thu hoi)
let dangLamMoi: Promise<string | null> | null = null

function lamMoiPhien(): Promise<string | null> {
  if (!dangLamMoi) {
    const refreshToken = localStorage.getItem(REFRESH_KEY)
    dangLamMoi = (refreshToken
      ? axios.post<LoginResponse>('/api/auth/refresh', { refreshToken }, { timeout: 20000 })
          .then((res) => { luuPhien(res.data); return res.data.token })
          .catch(() => null)
      : Promise.resolve(null)
    ).finally(() => { setTimeout(() => { dangLamMoi = null }, 0) })
  }
  return dangLamMoi
}

function veDangNhap() {
  xoaPhien()
  const p = window.location.pathname
  // Trang cong khai (khach) khong can dang nhap: khong day sang trang dang nhap
  if (!p.startsWith('/dang-nhap') && !['/', '/dang-ky', '/tra-cuu'].includes(p)) window.location.href = '/dang-nhap'
}

api.interceptors.response.use(
  (res) => res,
  async (err: AxiosError<ApiError>) => {
    const config = err.config as (InternalAxiosRequestConfig & { _daThuLai?: boolean }) | undefined
    const url = config?.url ?? ''
    const laAuth = url.includes('/auth/login') || url.includes('/auth/refresh')
    if (err.response?.status === 401 && config && !laAuth && !config._daThuLai) {
      const tokenMoi = await lamMoiPhien()
      if (tokenMoi) {
        config._daThuLai = true
        config.headers.Authorization = `Bearer ${tokenMoi}`
        return api(config)
      }
      veDangNhap()
    } else if (err.response?.status === 403 && err.response.data?.code === 'PHAI_DOI_MAT_KHAU') {
      if (window.location.pathname !== '/doi-mat-khau') window.location.href = '/doi-mat-khau'
    }
    return Promise.reject(err)
  },
)

/** Lay thong bao loi tieng Viet tu backend (ApiError) de hien thi. */
export function loiApi(err: unknown): string {
  const e = err as AxiosError<ApiError>
  const data = e.response?.data
  // details cua loi nghiep vu (VD goi y khoa) la du lieu cho giao dien, chi ghep vao thong bao khi la loi nhap lieu
  if (data?.details && data.code === 'DU_LIEU_KHONG_HOP_LE') {
    const chiTiet = Object.values(data.details).join('; ')
    return `${data.message}: ${chiTiet}`
  }
  if (data?.message) return data.message
  if (e.code === 'ECONNABORTED') return 'Máy chủ phản hồi quá lâu'
  if (!e.response) return 'Không kết nối được máy chủ'
  return 'Có lỗi xảy ra'
}

/** Lay ma loi (ApiError.code) de xu ly rieng mot so truong hop. */
export function maLoi(err: unknown): string | undefined {
  return (err as AxiosError<ApiError>).response?.data?.code
}

/** UC05 - 5b: khoa da du si so -> backend goi y khoa cung hang con cho. */
export interface GoiYKhoa {
  khoaId: number
  maKhoa: string
  khaiGiang: string
  conCho: number
}

export function goiYKhoa(err: unknown): GoiYKhoa | undefined {
  const d = (err as AxiosError<ApiError>).response?.data
  if (d?.code !== 'KHOA_DU_SI_SO' || !d.details?.goiYKhoaId) return undefined
  return {
    khoaId: Number(d.details.goiYKhoaId), maKhoa: d.details.goiYMaKhoa,
    khaiGiang: d.details.goiYKhaiGiang, conCho: Number(d.details.goiYConCho),
  }
}

/** Tai tep co xac thuc (PDF, Excel, anh) roi mo hoac luu. */
export async function taiTep(url: string, tenTep?: string, moTabMoi = false) {
  const res = await api.get(url, { responseType: 'blob' })
  const blobUrl = URL.createObjectURL(res.data as Blob)
  if (moTabMoi) {
    window.open(blobUrl, '_blank')
  } else {
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = tenTep ?? 'tai-ve'
    a.click()
  }
  setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000)
}
