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
import PublicLayout from './components/PublicLayout'
import TrangChuKhachPage from './pages/khach/TrangChuKhachPage'
import TraCuuPage from './pages/khach/TraCuuPage'
import DoiMatKhauPage from './pages/DoiMatKhauPage'
import QuanTriPage from './pages/quantri/QuanTriPage'
import ThongBaoPage from './pages/ThongBaoPage'
import CtvPage from './pages/ctv/CtvPage'
import CtvCuaToiPage from './pages/ctv/CtvCuaToiPage'

/** Trang "/": khach thay trang chu cong khai; da dang nhap thi vao trang mac dinh theo vai tro. */
function TrangChu() {
  const { user } = useAuth()
  if (!user) return <PublicLayout><TrangChuKhachPage /></PublicLayout>
  if (user.phaiDoiMatKhau) return <Navigate to="/doi-mat-khau" replace />
  if (user.vaiTro === 'ADMIN') return <Navigate to="/tong-quan" replace />
  if (user.vaiTro === 'GIAO_VIEN') return <Navigate to="/tong-quan-gv" replace />
  if (user.vaiTro === 'HOC_VIEN') return <Navigate to="/hoc-tap" replace />
  if (user.vaiTro === 'CTV') return <Navigate to="/ctv-cua-toi" replace />
  return <Navigate to="/hoc-vien" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/doi-mat-khau" element={<DoiMatKhauPage />} />
      <Route path="/" element={<TrangChu />} />
      <Route element={<PublicLayout />}>
        <Route path="/dang-ky" element={<PublicDangKyPage />} />
        <Route path="/tra-cuu" element={<TraCuuPage />} />
      </Route>
      <Route element={<RequireRole><AppLayout /></RequireRole>}>
        <Route path="/tong-quan" element={<RequireRole roles={['ADMIN']}><DashboardPage /></RequireRole>} />
        <Route path="/tong-quan-gv" element={<RequireRole roles={['GIAO_VIEN']}><TongQuanGiaoVienPage /></RequireRole>} />
        <Route path="/hoc-tap" element={<RequireRole roles={['HOC_VIEN']}><HocTapPage /></RequireRole>} />
        <Route path="/hoc-vien" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><HocVienPage /></RequireRole>} />
        <Route path="/khoa" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><KhoaPage /></RequireRole>} />
        <Route path="/lich-hoc" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><LichHocPage /></RequireRole>} />
        <Route path="/diem-danh" element={<RequireRole roles={['ADMIN', 'GIAO_VIEN']}><DiemDanhPage /></RequireRole>} />
        <Route path="/thong-bao" element={<ThongBaoPage />} />
        <Route path="/ctv" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><CtvPage /></RequireRole>} />
        <Route path="/ctv-cua-toi" element={<RequireRole roles={['CTV', 'HOC_VIEN']}><CtvCuaToiPage /></RequireRole>} />
        <Route path="/quan-tri" element={<RequireRole roles={['ADMIN']}><QuanTriPage /></RequireRole>} />
        <Route path="/cong-no" element={<RequireRole roles={['ADMIN', 'LE_TAN']}><CongNoPage /></RequireRole>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
