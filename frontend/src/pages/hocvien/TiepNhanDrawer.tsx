import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Alert, App, Button, Col, DatePicker, Drawer, Form, Input, Radio, Row, Select, Space } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../../api/client'
import { useCtv, useKhoa } from '../../api/hooks'
import type { DangKy, HocVien } from '../../api/types'
import { ngay, tien } from '../../utils/format'
import SoTienInput from '../../components/SoTienInput'

interface Props {
  open: boolean
  onClose: () => void
  onDone: (dangKyId: number) => void
}

interface FormValues {
  cccd: string
  hoTen: string
  ngaySinh: Dayjs
  gioiTinh: 'NAM' | 'NU'
  ngayCapCccd?: Dayjs
  diaChi: string
  soDienThoai: string
  email?: string
  khoaId: number
  hinhThucLyThuyet: 'TU_HOC' | 'TAP_TRUNG'
  nguon: 'TRUC_TIEP' | 'CTV'
  ctvId?: number
  giamTru?: number
  lyDoGiamTru?: string
  ghiChu?: string
}

/** UC05 - Tiep nhan ho so hoc vien. Buoc 1: nhap CCCD de dung lai ho so cu. */
export default function TiepNhanDrawer({ open, onClose, onDone }: Props) {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [hvCu, setHvCu] = useState<HocVien | null>(null)
  const [traCuu, setTraCuu] = useState(false)
  const { data: khoa } = useKhoa()
  const { data: ctv } = useCtv()
  const nguon = Form.useWatch('nguon', form)
  const giamTru = Form.useWatch('giamTru', form)
  const khoaId = Form.useWatch('khoaId', form)
  const khoaChon = khoa?.find((k) => k.id === khoaId)
  const khoaNhan = khoa?.filter((k) => k.trangThai === 'DANG_TUYEN' || k.trangThai === 'DANG_DAO_TAO')

  const traCccd = async () => {
    const cccd = form.getFieldValue('cccd')
    if (!/^[0-9]{12}$/.test(cccd ?? '')) {
      message.warning('CCCD phải gồm 12 chữ số')
      return
    }
    setTraCuu(true)
    try {
      const { data } = await api.get<HocVien>(`/hoc-vien/tra-cccd/${cccd}`)
      setHvCu(data)
      form.setFieldsValue({
        hoTen: data.hoTen, ngaySinh: dayjs(data.ngaySinh), gioiTinh: data.gioiTinh,
        ngayCapCccd: data.ngayCapCccd ? dayjs(data.ngayCapCccd) : undefined,
        diaChi: data.diaChi, soDienThoai: data.soDienThoai, email: data.email,
      })
      message.info(`Đã có hồ sơ ${data.maHocVien} – thông tin đã được điền sẵn`)
    } catch {
      setHvCu(null)
      message.info('Học viên mới – nhập đầy đủ thông tin')
    } finally {
      setTraCuu(false)
    }
  }

  const tao = useMutation({
    mutationFn: async (v: FormValues) => {
      const body = {
        hocVien: {
          hoTen: v.hoTen, ngaySinh: v.ngaySinh.format('YYYY-MM-DD'), gioiTinh: v.gioiTinh, cccd: v.cccd,
          ngayCapCccd: v.ngayCapCccd?.format('YYYY-MM-DD'), diaChi: v.diaChi, soDienThoai: v.soDienThoai,
          email: v.email || undefined,
        },
        khoaId: v.khoaId, hinhThucLyThuyet: v.hinhThucLyThuyet, nguon: v.nguon,
        ctvId: v.nguon === 'CTV' ? v.ctvId : undefined,
        giamTru: v.giamTru ?? 0, lyDoGiamTru: v.lyDoGiamTru, ghiChu: v.ghiChu,
      }
      return (await api.post<DangKy>('/dang-ky', body)).data
    },
    onSuccess: (dk) => {
      message.success(`Đã tạo hồ sơ ${dk.maHoSo}`)
      form.resetFields()
      setHvCu(null)
      onDone(dk.id)
    },
    onError: (e) => message.error(loiApi(e)),
  })

  return (
    <Drawer title="Tiếp nhận hồ sơ học viên" open={open} onClose={onClose} width={640} destroyOnClose
            extra={<Button type="primary" loading={tao.isPending} onClick={() => form.submit()}>Lưu hồ sơ</Button>}>
      <Form form={form} layout="vertical" onFinish={(v) => tao.mutate(v)}
            initialValues={{ gioiTinh: 'NAM', hinhThucLyThuyet: 'TU_HOC', nguon: 'TRUC_TIEP', giamTru: 0 }}>
        <Form.Item label="Số CCCD" required>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item name="cccd" noStyle rules={[{ required: true, pattern: /^[0-9]{12}$/, message: 'CCCD gồm 12 chữ số' }]}>
              <Input placeholder="12 chữ số" maxLength={12} onPressEnter={(e) => { e.preventDefault(); traCccd() }} />
            </Form.Item>
            <Button icon={<SearchOutlined />} loading={traCuu} onClick={traCccd}>Kiểm tra</Button>
          </Space.Compact>
        </Form.Item>
        {hvCu && (
          <Alert type="info" showIcon style={{ marginBottom: 16 }}
                 message={`Hồ sơ cũ: ${hvCu.maHocVien} – ${hvCu.hoTen}, sinh ${ngay(hvCu.ngaySinh)}`} />
        )}
        <Row gutter={12}>
          <Col xs={24} md={14}>
            <Form.Item name="hoTen" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ tên' }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col xs={12} md={5}>
            <Form.Item name="ngaySinh" label="Ngày sinh" rules={[{ required: true, message: 'Chọn ngày sinh' }]}>
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} disabledDate={(d) => d.isAfter(dayjs())} />
            </Form.Item>
          </Col>
          <Col xs={12} md={5}>
            <Form.Item name="gioiTinh" label="Giới tính">
              <Select options={[{ value: 'NAM', label: 'Nam' }, { value: 'NU', label: 'Nữ' }]} />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item name="soDienThoai" label="Số điện thoại"
                       rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
              <Input maxLength={10} />
            </Form.Item>
          </Col>
          <Col xs={12} md={8}>
            <Form.Item name="ngayCapCccd" label="Ngày cấp CCCD">
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={24} md={8}>
            <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="diaChi" label="Địa chỉ" rules={[{ required: true, message: 'Nhập địa chỉ' }]}>
              <Input />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="khoaId" label="Khóa đào tạo" rules={[{ required: true, message: 'Chọn khóa' }]}
                   extra={khoaChon && `Hạng ${khoaChon.hangMa} · ${ngay(khoaChon.ngayKhaiGiang)} – ${ngay(khoaChon.ngayBeGiang)} · `
                     + `${khoaChon.soDangKy}/${khoaChon.siSoToiDa} học viên · học phí ${tien(khoaChon.hocPhi)}`}>
          <Select placeholder="Chọn khóa đang tuyển"
                  options={khoaNhan?.map((k) => ({ value: k.id, label: `${k.maKhoa} – hạng ${k.hangMa}` }))} />
        </Form.Item>
        <Form.Item name="hinhThucLyThuyet" label="Hình thức học lý thuyết">
          <Radio.Group options={[{ value: 'TU_HOC', label: 'Tự học' }, { value: 'TAP_TRUNG', label: 'Học tập trung tại trung tâm' }]} />
        </Form.Item>
        <Row gutter={12}>
          <Col xs={24} md={12}>
            <Form.Item name="nguon" label="Nguồn">
              <Radio.Group options={[{ value: 'TRUC_TIEP', label: 'Trực tiếp' }, { value: 'CTV', label: 'Cộng tác viên' }]} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            {nguon === 'CTV' && (
              <Form.Item name="ctvId" label="CTV giới thiệu" rules={[{ required: true, message: 'Chọn CTV' }]}>
                <Select showSearch optionFilterProp="label"
                        options={ctv?.map((c) => ({ value: c.id, label: `${c.hoTen} – ${c.soDienThoai}` }))} />
              </Form.Item>
            )}
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="giamTru" label="Giảm trừ học phí (đ)">
              <SoTienInput min={0} step={50000} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            {!!giamTru && giamTru > 0 && (
              <Form.Item name="lyDoGiamTru" label="Lý do giảm trừ" rules={[{ required: true, message: 'Nhập lý do' }]}>
                <Input />
              </Form.Item>
            )}
          </Col>
        </Row>
        <Form.Item name="ghiChu" label="Ghi chú">
          <Input.TextArea rows={2} />
        </Form.Item>
      </Form>
    </Drawer>
  )
}
