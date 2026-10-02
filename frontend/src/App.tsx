import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import RequireRole from './auth/RequireRole'
import { useAuth } from './auth/AuthContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import TongQuanGiaoVienPage from './pages/TongQuanGiaoVienPage'
import HocTapPage from './pages/HocTapPage'
import HocVienPage from './pages/HocVienPage'
import KhoaPage from './pages/KhoaPage'
import LichHocPage from './pages/LichHocPage'
import DiemDanhPage from './pages/DiemDanhPage'
import CongNoPage from './pages/CongNoPage'
import PublicDangKyPage from './pages/PublicDangKyPage'

/** Trang mac dinh sau khi dang nhap, theo vai tro. */
function TrangChu() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/dang-nhap" replace />
  if (user.vaiTro === 'ADMIN') return <Navigate to="/tong-quan" replace />
  if (user.vaiTro === 'GIAO_VIEN') return <Navigate to="/tong-quan-gv" replace />
  if (user.vaiTro === 'HOC_VIEN') return <Navigate to="/hoc-tap" replace />
  return <Navigate to="/hoc-vien" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/dang-ky" element={<PublicDangKyPage />} />
      <Route element={<RequireRole><AppLayout /></RequireRole>}>
        <Route index element={<TrangChu />} />
        <Route path="/tong-quan" element={<RequireRole roles={['ADMIN']}><DashboardPage /></RequireRole>} />
        <Route path="/tong-quan-gv" element={<RequireRole roles={['GIAO_VIEN']}><TongQuanGiaoVienPage /></RequireRole>} />
        <Route path="/hoc-tap" element={<RequireRole roles={['HOC_VIEN']}><HocTapPage /></RequireRole>} />
        <Route path="/hoc-vien" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><HocVienPage /></RequireRole>} />
        <Route path="/khoa" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><KhoaPage /></RequireRole>} />
        <Route path="/lich-hoc" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><LichHocPage /></RequireRole>} />
        <Route path="/diem-danh" element={<RequireRole roles={['ADMIN', 'GIAO_VIEN']}><DiemDanhPage /></RequireRole>} />
        <Route path="/cong-no" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><CongNoPage /></RequireRole>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
