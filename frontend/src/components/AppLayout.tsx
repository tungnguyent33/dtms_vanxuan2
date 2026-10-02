import { useState, type ReactNode } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Button, Drawer, Dropdown, Grid, Layout, Menu, Typography } from 'antd'
import {
  BarChartOutlined, CalendarOutlined, CheckSquareOutlined, DollarOutlined, LogoutOutlined,
  MenuOutlined, ReadOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import type { VaiTro } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { NHAN_VAI_TRO } from '../utils/format'

const { Header, Sider, Content } = Layout

type MucMenu = { key: string; icon: ReactNode; label: string; roles: VaiTro[] }

const MENU: MucMenu[] = [
  { key: '/tong-quan', icon: <BarChartOutlined />, label: 'Tổng quan', roles: ['ADMIN'] },
  { key: '/tong-quan-gv', icon: <BarChartOutlined />, label: 'Tổng quan', roles: ['GIAO_VIEN'] },
  { key: '/hoc-tap', icon: <ReadOutlined />, label: 'Tiến độ học tập', roles: ['HOC_VIEN'] },
  { key: '/hoc-vien', icon: <TeamOutlined />, label: 'Học viên & hồ sơ', roles: ['ADMIN', 'LE_TAN'] },
  { key: '/khoa', icon: <ReadOutlined />, label: 'Khóa đào tạo', roles: ['ADMIN', 'LE_TAN'] },
  { key: '/lich-hoc', icon: <CalendarOutlined />, label: 'Lịch học', roles: ['ADMIN', 'LE_TAN'] },
  { key: '/diem-danh', icon: <CheckSquareOutlined />, label: 'Điểm danh', roles: ['ADMIN', 'GIAO_VIEN'] },
  { key: '/cong-no', icon: <DollarOutlined />, label: 'Công nợ học phí', roles: ['ADMIN', 'LE_TAN'] },
]

export default function AppLayout() {
  const { user, dangXuat } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const screens = Grid.useBreakpoint()
  const [moMenu, setMoMenu] = useState(false)
  const laMobile = !screens.md

  const items: MenuProps['items'] = MENU.filter((m) => user && m.roles.includes(user.vaiTro)).map((m) => ({
    key: m.key,
    icon: m.icon,
    label: m.label,
  }))

  const menu = (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      items={items}
      onClick={(e) => {
        navigate(e.key)
        setMoMenu(false)
      }}
    />
  )

  const tieuDe = (
    <div style={{ padding: '16px 20px', fontWeight: 700, color: '#1f4e79', lineHeight: 1.3 }}>
      TTĐT lái xe Vạn Xuân
      <div style={{ fontSize: 12, fontWeight: 400, color: '#888' }}>Hệ thống quản lý đào tạo</div>
    </div>
  )

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {!laMobile && (
        <Sider width={230} theme="light" style={{ borderRight: '1px solid #eee' }}>
          {tieuDe}
          {menu}
        </Sider>
      )}
      <Drawer open={laMobile && moMenu} onClose={() => setMoMenu(false)} placement="left" width={260}
              styles={{ body: { padding: 0 } }} title={null} closable={false}>
        {tieuDe}
        {menu}
      </Drawer>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 16px', display: 'flex', alignItems: 'center',
                         justifyContent: 'space-between', borderBottom: '1px solid #eee' }}>
          <div>{laMobile && <Button icon={<MenuOutlined />} onClick={() => setMoMenu(true)} />}</div>
          <Dropdown
            menu={{ items: [{ key: 'out', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: () => { dangXuat(); navigate('/dang-nhap') } }] }}
          >
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar icon={<UserOutlined />} style={{ background: '#1f4e79' }} />
              <div style={{ lineHeight: 1.2 }}>
                <Typography.Text strong>{user?.hoTen}</Typography.Text>
                <div style={{ fontSize: 12, color: '#888' }}>{user ? NHAN_VAI_TRO[user.vaiTro] : ''}</div>
              </div>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ padding: laMobile ? 12 : 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
