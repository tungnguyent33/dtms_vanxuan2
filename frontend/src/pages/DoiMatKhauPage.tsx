import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Alert, App, Button, Card, Form, Input, Typography } from 'antd'
import { LockOutlined } from '@ant-design/icons'
import { api, loiApi } from '../api/client'
import type { LoginResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'

/** FR-01: doi mat khau. Bat buoc khi dang dung mat khau tam (moi cap hoac vua duoc dat lai). */
export default function DoiMatKhauPage() {
  const { message } = App.useApp()
  const { user, capNhatPhien, dangXuat } = useAuth()
  const navigate = useNavigate()
  const [loi, setLoi] = useState<string>()
  const [dangGui, setDangGui] = useState(false)

  if (!user) return <Navigate to="/dang-nhap" replace />
  const batBuoc = !!user.phaiDoiMatKhau

  const onFinish = async (v: { matKhauCu: string; matKhauMoi: string }) => {
    setDangGui(true)
    setLoi(undefined)
    try {
      const { data } = await api.put<LoginResponse>('/auth/doi-mat-khau', { matKhauCu: v.matKhauCu, matKhauMoi: v.matKhauMoi })
      capNhatPhien(data)
      message.success('Đã đổi mật khẩu. Các thiết bị khác đã được đăng xuất.')
      navigate('/', { replace: true })
    } catch (e) {
      setLoi(loiApi(e))
    } finally {
      setDangGui(false)
    }
  }

  return (
    <div className="login-wrap">
      <Card style={{ width: '100%', maxWidth: 400 }}>
        <Typography.Title level={4} style={{ textAlign: 'center', marginBottom: 4 }}>Đổi mật khẩu</Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center' }}>{user.hoTen} ({user.tenDangNhap})</Typography.Paragraph>
        {batBuoc && (
          <Alert type="warning" showIcon style={{ marginBottom: 16 }}
                 message="Bạn đang dùng mật khẩu tạm. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống." />
        )}
        {loi && <Alert type="error" message={loi} showIcon style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="matKhauCu" label={batBuoc ? 'Mật khẩu tạm được cấp' : 'Mật khẩu hiện tại'}
                     rules={[{ required: true, message: 'Nhập mật khẩu hiện tại' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" autoFocus />
          </Form.Item>
          <Form.Item name="matKhauMoi" label="Mật khẩu mới" hasFeedback
                     rules={[
                       { required: true, message: 'Nhập mật khẩu mới' },
                       { min: 8, message: 'Ít nhất 8 ký tự' },
                       { pattern: /(?=.*[A-Za-z])(?=.*[0-9])/, message: 'Cần có cả chữ và số' },
                     ]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="nhapLai" label="Nhập lại mật khẩu mới" dependencies={['matKhauMoi']} hasFeedback
                     rules={[
                       { required: true, message: 'Nhập lại mật khẩu mới' },
                       ({ getFieldValue }) => ({
                         validator: (_, v) => (!v || v === getFieldValue('matKhauMoi')
                           ? Promise.resolve() : Promise.reject(new Error('Mật khẩu nhập lại không khớp'))),
                       }),
                     ]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={dangGui}>Đổi mật khẩu</Button>
        </Form>
        <div style={{ marginTop: 12, textAlign: 'center' }}>
          {batBuoc
            ? <Button type="link" onClick={() => { dangXuat(); navigate('/dang-nhap') }}>Đăng xuất</Button>
            : <Button type="link" onClick={() => navigate(-1)}>Quay lại</Button>}
        </div>
      </Card>
    </div>
  )
}
