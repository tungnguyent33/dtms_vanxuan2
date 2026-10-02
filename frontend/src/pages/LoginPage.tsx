import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useAuth } from '../auth/AuthContext'
import { loiApi } from '../api/client'

export default function LoginPage() {
  const { user, dangNhap } = useAuth()
  const navigate = useNavigate()
  const [loi, setLoi] = useState<string>()
  const [dangGui, setDangGui] = useState(false)

  if (user) return <Navigate to="/" replace />

  const onFinish = async (v: { tenDangNhap: string; matKhau: string }) => {
    setDangGui(true)
    setLoi(undefined)
    try {
      await dangNhap(v.tenDangNhap, v.matKhau)
      navigate('/', { replace: true })
    } catch (e) {
      setLoi(loiApi(e))
    } finally {
      setDangGui(false)
    }
  }

  return (
    <div className="login-wrap">
      <Card style={{ width: '100%', maxWidth: 380 }}>
        <Typography.Title level={4} style={{ textAlign: 'center', marginBottom: 4 }}>
          Trung tâm đào tạo lái xe Vạn Xuân
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center' }}>
          Đăng nhập hệ thống quản lý
        </Typography.Paragraph>
        {loi && <Alert type="error" message={loi} showIcon style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={onFinish} autoComplete="on">
          <Form.Item name="tenDangNhap" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
            <Input prefix={<UserOutlined />} autoFocus autoComplete="username" />
          </Form.Item>
          <Form.Item name="matKhau" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={dangGui}>
            Đăng nhập
          </Button>
        </Form>
        <Typography.Paragraph type="secondary" style={{ marginTop: 16, fontSize: 12, textAlign: 'center' }}>
          Học viên đăng ký khóa mới? <a href="/dang-ky">Đăng ký trực tuyến</a>
        </Typography.Paragraph>
      </Card>
    </div>
  )
}
