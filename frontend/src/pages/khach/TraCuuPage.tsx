import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Descriptions, Form, Input, Tag, Typography } from 'antd'
import { FileSearchOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import type { KetQuaTraCuu } from '../../api/types'
import { NHAN_TRANG_THAI_DK, ngay, ngayGio } from '../../utils/format'
import CaptchaField from '../../components/CaptchaField'

/** Huong dan buoc tiep theo cho khach theo trang thai ho so. */
const HUONG_DAN: Record<string, { type: 'info' | 'success' | 'warning' | 'error'; text: string }> = {
  CHO_DUYET: { type: 'info', text: 'Trung tâm đã nhận đăng ký và sẽ gọi điện xác nhận. Khi đến nộp hồ sơ, bạn mang theo CCCD.' },
  DA_TIEP_NHAN: { type: 'success', text: 'Hồ sơ đã được tiếp nhận. Vui lòng có mặt đúng ngày khai giảng của khóa.' },
  DANG_HOC: { type: 'success', text: 'Bạn đang theo học. Đăng nhập cổng học viên để xem lịch học, tiến độ và học phí.' },
  CHUA_DAT: { type: 'warning', text: 'Bạn chưa đủ điều kiện hoàn thành khóa. Vui lòng liên hệ trung tâm để học bù hoặc chuyển khóa.' },
  HOAN_THANH: { type: 'success', text: 'Bạn đã hoàn thành khóa đào tạo và đủ điều kiện dự sát hạch.' },
  SAT_HACH_TRUOT: { type: 'warning', text: 'Kết quả sát hạch chưa đạt. Liên hệ trung tâm để đăng ký thi lại.' },
  DA_SAT_HACH_DAT: { type: 'success', text: 'Chúc mừng bạn đã đạt sát hạch.' },
  DA_HUY: { type: 'error', text: 'Hồ sơ đã bị hủy. Liên hệ trung tâm nếu cần đăng ký lại.' },
}

/** Khach tra cuu tinh trang ho so bang ma ho so + CCCD (khong can dang nhap). */
export default function TraCuuPage() {
  const [params] = useSearchParams()
  const [phienCaptcha, setPhienCaptcha] = useState(0)
  const traCuu = useMutation({
    mutationFn: async (v: { maHoSo: string; cccd: string; captchaId?: string; captcha?: string }) =>
      (await api.post<KetQuaTraCuu>('/public/tra-cuu', {
        maHoSo: v.maHoSo.trim(), cccd: v.cccd.trim(), captchaId: v.captchaId, captcha: v.captcha,
      })).data,
    // Ma xac nhan chi dung mot lan: moi lan tra cuu (dung hay sai) deu lay ma moi
    onSettled: () => setPhienCaptcha((n) => n + 1),
  })
  const kq = traCuu.data
  const hd = kq ? HUONG_DAN[kq.trangThai] : undefined

  return (
    <div className="public-wrap">
      <Typography.Title level={3}>Tra cứu hồ sơ</Typography.Title>
      <Typography.Paragraph type="secondary">
        Nhập mã hồ sơ (nhận được khi đăng ký, ví dụ HS-A1-0003) và số CCCD đã khai.
      </Typography.Paragraph>
      <Card>
        <Form layout="vertical" onFinish={(v) => traCuu.mutate(v)} initialValues={{ maHoSo: params.get('ma') ?? '' }}>
          <Form.Item name="maHoSo" label="Mã hồ sơ" rules={[{ required: true, message: 'Nhập mã hồ sơ' }]}>
            <Input placeholder="HS-A1-0003" style={{ textTransform: 'uppercase' }} />
          </Form.Item>
          <Form.Item name="cccd" label="Số CCCD" rules={[{ required: true, pattern: /^[0-9]{12}$/, message: 'CCCD gồm 12 chữ số' }]}>
            <Input maxLength={12} inputMode="numeric" />
          </Form.Item>
          <CaptchaField phienBan={phienCaptcha} />
          <Button type="primary" htmlType="submit" icon={<FileSearchOutlined />} block size="large" loading={traCuu.isPending}>
            Tra cứu
          </Button>
        </Form>
      </Card>

      {traCuu.isError && <Alert type="error" showIcon message={loiApi(traCuu.error)} style={{ marginTop: 16 }} />}

      {kq && (
        <Card style={{ marginTop: 16 }} title={`Hồ sơ ${kq.maHoSo}`}
              extra={<Tag color={NHAN_TRANG_THAI_DK[kq.trangThai]?.color}>{NHAN_TRANG_THAI_DK[kq.trangThai]?.text ?? kq.trangThai}</Tag>}>
          {hd && <Alert type={hd.type} showIcon message={hd.text} style={{ marginBottom: 16 }} />}
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Họ tên">{kq.hoTen}</Descriptions.Item>
            <Descriptions.Item label="Khóa học">{kq.maKhoa} (hạng {kq.hang})</Descriptions.Item>
            <Descriptions.Item label="Thời gian khóa">{ngay(kq.ngayKhaiGiang)} – {ngay(kq.ngayBeGiang)}</Descriptions.Item>
            <Descriptions.Item label="Ngày đăng ký">{ngayGio(kq.ngayDangKy)}</Descriptions.Item>
          </Descriptions>
        </Card>
      )}
    </div>
  )
}
