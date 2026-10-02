import { createContext, useContext, useState, type ReactNode } from 'react'
import { api, TOKEN_KEY, USER_KEY } from '../api/client'
import type { LoginResponse, VaiTro } from '../api/types'

export interface NguoiDungHienTai {
  id: number
  tenDangNhap: string
  hoTen: string
  vaiTro: VaiTro
}

interface AuthState {
  user: NguoiDungHienTai | null
  dangNhap: (tenDangNhap: string, matKhau: string) => Promise<NguoiDungHienTai>
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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<NguoiDungHienTai | null>(docUserDaLuu)

  const dangNhap = async (tenDangNhap: string, matKhau: string) => {
    const { data } = await api.post<LoginResponse>('/auth/login', { tenDangNhap, matKhau })
    const u: NguoiDungHienTai = { id: data.id, tenDangNhap: data.tenDangNhap, hoTen: data.hoTen, vaiTro: data.vaiTro }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
    return u
  }

  const dangXuat = () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }

  const coQuyen = (...roles: VaiTro[]) => !!user && roles.includes(user.vaiTro)

  return <AuthContext.Provider value={{ user, dangNhap, dangXuat, coQuyen }}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phai dung ben trong AuthProvider')
  return ctx
}
