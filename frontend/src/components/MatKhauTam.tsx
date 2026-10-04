import { Alert, Typography } from 'antd'
import type { useAppProps } from 'antd/es/app/context'
import type { KetQuaCapMatKhau } from '../api/types'

/**
 * Hien mat khau tam vua cap (chi xem duoc MOT lan - may chu khong luu dang ro).
 * Nguoi dung se bi buoc doi mat khau o lan dang nhap dau tien.
 */
export function hienMatKhauTam(modal: useAppProps['modal'], kq: KetQuaCapMatKhau, tieuDe = 'Đã cấp mật khẩu tạm') {
  modal.success({
    title: tieuDe,
    width: 440,
    content: (
      <div>
        <Alert type="warning" showIcon style={{ margin: '8px 0 12px' }}
               message="Mật khẩu chỉ hiện một lần. Hãy ghi lại hoặc sao chép để gửi cho người dùng." />
        <div>Tên đăng nhập: <Typography.Text strong copyable>{kq.tenDangNhap}</Typography.Text></div>
        <div style={{ marginTop: 4 }}>
          Mật khẩu tạm: <Typography.Text code strong copyable style={{ fontSize: 16 }}>{kq.matKhauTam}</Typography.Text>
        </div>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0, fontSize: 12 }}>
          Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.
        </Typography.Paragraph>
      </div>
    ),
  })
}
