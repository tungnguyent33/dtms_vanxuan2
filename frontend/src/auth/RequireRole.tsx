import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { Result } from 'antd'
import type { VaiTro } from '../api/types'
import { useAuth } from './AuthContext'

/** Chan trang theo vai tro. Backend van kiem tra lai quyen (@PreAuthorize) - day chi la lop giao dien. */
export default function RequireRole({ roles, children }: { roles?: VaiTro[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/dang-nhap" replace />
  if (roles && !roles.includes(user.vaiTro)) {
    return <Result status="403" title="Không có quyền" subTitle="Bạn không có quyền truy cập trang này." />
  }
  return <>{children}</>
}
