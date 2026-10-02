import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

export const TOKEN_KEY = 'dtms_token'
export const USER_KEY = 'dtms_user'

/** Axios dung chung: tu gan JWT va xu ly phien het han. */
export const api = axios.create({ baseURL: '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err: AxiosError<ApiError>) => {
    const isLogin = err.config?.url?.includes('/auth/login')
    if (err.response?.status === 401 && !isLogin) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!window.location.pathname.startsWith('/dang-nhap')) window.location.href = '/dang-nhap'
    }
    return Promise.reject(err)
  },
)

/** Lay thong bao loi tieng Viet tu backend (ApiError) de hien thi. */
export function loiApi(err: unknown): string {
  const e = err as AxiosError<ApiError>
  const data = e.response?.data
  if (data?.details) {
    const chiTiet = Object.values(data.details).join('; ')
    return `${data.message}: ${chiTiet}`
  }
  if (data?.message) return data.message
  if (e.code === 'ECONNABORTED') return 'Máy chủ phản hồi quá lâu'
  if (!e.response) return 'Không kết nối được máy chủ'
  return 'Có lỗi xảy ra'
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
