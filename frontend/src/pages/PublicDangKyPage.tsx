import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Col, DatePicker, Form, Input, Radio, Result, Row, Select, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { api, goiYKhoa, loiApi } from '../api/client'
import type { KhoaCongKhai } from '../api/types'
import CaptchaField from '../components/CaptchaField'
import { ngay, tien } from '../utils/format'

/** Trang cong khai: khach tu dang ky khoa hoc (UC13). Khong can dang nhap. */
export default function PublicDangKyPage() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const khoaChon = Number(params.get('khoaId')) || undefined
  const [maHoSo, setMaHoSo] = useState<string>()
  const [phienCaptcha, setPhienCaptcha] = useState(0)
  const khoa = useQuery({
    queryKey: ['khoa-cong-khai'],
    queryFn: async () => (await api.get<KhoaCongKhai[]>('/public/khoa-dang-tuyen')).data,
  })

  const gui = useMutation({
    mutationFn: async (v: { hoTen: string; ngaySinh: Dayjs; gioiTinh: string; cccd: string; diaChi: string; soDienThoai: string; khoaId: number; hinhThucLyThuyet: string; captchaId?: string; captcha?: string }) =>
      (await api.post<{ maHoSo: string }>('/public/dang-ky', {
        hocVien: {
          hoTen: v.hoTen, ngaySinh: v.ngaySinh.format('YYYY-MM-DD'), gioiTinh: v.gioiTinh, cccd: v.cccd,
          diaChi: v.diaChi, soDienThoai: v.soDienThoai,
        },
        khoaId: v.khoaId, hinhThucLyThuyet: v.hinhThucLyThuyet, captchaId: v.captchaId, captcha: v.captcha,
      })).data,
    onSuccess: (r) => setMaHoSo(r.maHoSo),
    // Ma xac nhan chi dung mot lan: gui loi thi lay ma moi
    onError: () => setPhienCaptcha((n) => n + 1),
  })

  if (maHoSo) {
    return (
      <div className="public-wrap">
        <Result status="success" title="Đăng ký thành công"
                subTitle={`Mã hồ sơ của bạn: ${maHoSo}. Hãy ghi lại mã này để tra cứu. Trung tâm sẽ gọi điện xác nhận và hướng dẫn nộp hồ sơ, học phí.`}
                extra={[
                  <Button key="tc" type="primary" onClick={() => navigate(`/tra-cuu?ma=${maHoSo}`)}>Tra cứu hồ sơ</Button>,
                  <Button key="home" onClick={() => navigate('/')}>Về trang chủ</Button>,
                ]} />
      </div>
    )
  }

  return (
    <div className="public-wrap">
      <Typography.Title level={3}>Đăng ký học lái xe mô tô</Typography.Title>
      <Typography.Paragraph type="secondary">Trung tâm đào tạo lái xe Phú Thọ – chi nhánh Vạn Xuân (hạng A1, A)</Typography.Paragraph>
      <Card>
        {gui.isError && (() => {
          const g = goiYKhoa(gui.error)
          return (
            <Alert type={g ? 'warning' : 'error'} showIcon style={{ marginBottom: 16 }}
                   message={g ? 'Khóa bạn chọn vừa đủ chỗ' : loiApi(gui.error)}
                   description={g && `Khóa ${g.maKhoa} khai giảng ${ngay(g.khaiGiang)} còn ${g.conCho} chỗ.`}
                   action={g && (
                     <Button size="small" type="primary"
                             onClick={() => { form.setFieldValue('khoaId', g.khoaId); gui.reset(); khoa.refetch() }}>
                       Chọn khóa này
                     </Button>
                   )} />
          )
        })()}
        <Form form={form} layout="vertical" onFinish={(v) => gui.mutate(v)} initialValues={{ gioiTinh: 'NAM', hinhThucLyThuyet: 'TU_HOC', khoaId: khoaChon }}>
          <Form.Item name="khoaId" label="Chọn khóa học" rules={[{ required: true, message: 'Chọn khóa' }]}>
            <Select loading={khoa.isLoading} placeholder={khoa.data?.length === 0 ? 'Hiện chưa có khóa mở tuyển' : 'Chọn khóa'}
                    options={khoa.data?.map((k) => ({
                      value: k.id, disabled: k.conCho === 0,
                      label: `Hạng ${k.hang} · khai giảng ${ngay(k.ngayKhaiGiang)} · ${tien(k.hocPhi)} · còn ${k.conCho} chỗ`,
                    }))} />
          </Form.Item>
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item name="hoTen" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
            </Col>
            <Col xs={12} md={6}>
              <Form.Item name="ngaySinh" label="Ngày sinh" rules={[{ required: true, message: 'Chọn ngày sinh' }]}>
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} disabledDate={(d) => d.isAfter(dayjs())} />
              </Form.Item>
            </Col>
            <Col xs={12} md={6}>
              <Form.Item name="gioiTinh" label="Giới tính">
                <Select options={[{ value: 'NAM', label: 'Nam' }, { value: 'NU', label: 'Nữ' }]} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="cccd" label="Số CCCD" rules={[{ required: true, pattern: /^[0-9]{12}$/, message: 'CCCD gồm 12 chữ số' }]}>
                <Input maxLength={12} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="soDienThoai" label="Số điện thoại" rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
                <Input maxLength={10} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="diaChi" label="Địa chỉ" rules={[{ required: true, message: 'Nhập địa chỉ' }]}><Input /></Form.Item>
          <Form.Item name="hinhThucLyThuyet" label="Học lý thuyết">
            <Radio.Group options={[{ value: 'TU_HOC', label: 'Tự học' }, { value: 'TAP_TRUNG', label: 'Học tập trung tại trung tâm' }]} />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
            Thông tin chỉ dùng để lập hồ sơ học lái xe tại trung tâm.
          </Typography.Paragraph>
          <CaptchaField phienBan={phienCaptcha} />
          <Button type="primary" htmlType="submit" size="large" block loading={gui.isPending}>Gửi đăng ký</Button>
        </Form>
      </Card>
    </div>
  )
}
