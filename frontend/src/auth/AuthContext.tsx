import { createContext, useContext, useState, type ReactNode } from 'react'
import { api, luuPhien, REFRESH_KEY, TOKEN_KEY, USER_KEY, xoaPhien } from '../api/client'
import type { LoginResponse, VaiTro } from '../api/types'

export interface NguoiDungHienTai {
  id: number
  tenDangNhap: string
  hoTen: string
  vaiTro: VaiTro
  phaiDoiMatKhau?: boolean
}

interface AuthState {
  user: NguoiDungHienTai | null
  dangNhap: (tenDangNhap: string, matKhau: string) => Promise<NguoiDungHienTai>
  /** Cap nhat phien sau khi doi mat khau (backend cap token moi). */
  capNhatPhien: (data: LoginResponse) => void
  dangXuat: () => void
  coQuyen: (...roles: VaiTro[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

function docUserDaLuu(): NguoiDungHienTai | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw && localStorage.getItem(TOKEN_KEY) ? (JSON.parse(raw) as NguoiDungHienTai) : null
  } catch {
    return null
  }
}

const tuPhien = (data: LoginResponse): NguoiDungHienTai => ({
  id: data.id, tenDangNhap: data.tenDangNhap, hoTen: data.hoTen, vaiTro: data.vaiTro,
  phaiDoiMatKhau: data.phaiDoiMatKhau,
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<NguoiDungHienTai | null>(docUserDaLuu)

  const capNhatPhien = (data: LoginResponse) => {
    luuPhien(data)
    setUser(tuPhien(data))
  }

  const dangNhap = async (tenDangNhap: string, matKhau: string) => {
    const { data } = await api.post<LoginResponse>('/auth/login', { tenDangNhap, matKhau })
    capNhatPhien(data)
    return tuPhien(data)
  }

  const dangXuat = () => {
    const refreshToken = localStorage.getItem(REFRESH_KEY)
    // Thu hoi refresh token tren may chu; loi mang cung khong chan viec dang xuat
    if (refreshToken) api.post('/auth/dang-xuat', { refreshToken }).catch(() => undefined)
    xoaPhien()
    setUser(null)
  }

  const coQuyen = (...roles: VaiTro[]) => !!user && roles.includes(user.vaiTro)

  return (
    <AuthContext.Provider value={{ user, dangNhap, capNhatPhien, dangXuat, coQuyen }}>{children}</AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phai dung ben trong AuthProvider')
  return ctx
}
