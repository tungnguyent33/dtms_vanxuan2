import { useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Button, Drawer, Grid, Menu } from 'antd'
import { LoginOutlined, MenuOutlined } from '@ant-design/icons'
import { useAuth } from '../auth/AuthContext'

const MENU = [
  { key: '/', label: 'Trang chủ' },
  { key: '/dang-ky', label: 'Đăng ký học' },
  { key: '/tra-cuu', label: 'Tra cứu hồ sơ' },
]

/** Khung trang cho tac nhan Khach (khong can dang nhap). */
export default function PublicLayout({ children }: { children?: ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const screens = Grid.useBreakpoint()
  const [moMenu, setMoMenu] = useState(false)
  const laMobile = !screens.md

  const menu = (
    <Menu
      mode={laMobile ? 'inline' : 'horizontal'}
      selectedKeys={[location.pathname]}
      items={MENU}
      onClick={(e) => { navigate(e.key); setMoMenu(false) }}
      style={{ borderBottom: 'none', flex: 1, justifyContent: 'flex-end', minWidth: 0 }}
    />
  )

  const nutDangNhap = (
    <Button type="primary" icon={<LoginOutlined />} onClick={() => navigate(user ? '/' : '/dang-nhap')}>
      {user ? 'Vào hệ thống' : 'Đăng nhập'}
    </Button>
  )

  return (
    <div className="pub">
      <header className="pub-header">
        <div className="pub-container pub-header-inner">
          <Link to="/" className="pub-brand">
            <span className="pub-logo">VX</span>
            <span>
              Lái xe Vạn Xuân
              <small>Đào tạo mô tô hạng A1, A</small>
            </span>
          </Link>
          {laMobile ? (
            <Button icon={<MenuOutlined />} onClick={() => setMoMenu(true)} aria-label="Mở menu" />
          ) : (
            <>
              {menu}
              {nutDangNhap}
            </>
          )}
        </div>
      </header>
      <Drawer open={laMobile && moMenu} onClose={() => setMoMenu(false)} placement="right" width={260} title="Menu">
        {menu}
        <div style={{ padding: 16 }}>{nutDangNhap}</div>
      </Drawer>

      <main>{children ?? <Outlet />}</main>

      <footer className="pub-footer">
        <div className="pub-container">
          <b>Trung tâm đào tạo lái xe Phú Thọ – chi nhánh Vạn Xuân</b>
          <div>Tam Nông, Phú Thọ · Đào tạo lái xe mô tô hạng A1, A</div>
          <div className="pub-footer-note">
            Chương trình đào tạo theo Thông tư 14/2025/TT-BXD, sửa đổi bởi Thông tư 17/2026/TT-BXD.
          </div>
        </div>
      </footer>
    </div>
  )
}
