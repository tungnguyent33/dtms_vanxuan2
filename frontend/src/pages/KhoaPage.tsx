import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Alert, App, Button, Col, DatePicker, Dropdown, Form, Input, InputNumber, Modal, Row, Select, Space, Table, Tag,
} from 'antd'
import { DownOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../api/client'
import { useHangGplx, useKhoa } from '../api/hooks'
import type { KetQuaXet, Khoa, TrangThaiKhoa } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { NHAN_HINH_THUC, NHAN_TRANG_THAI_KHOA, ngay, tien } from '../utils/format'
import SoTienInput from '../components/SoTienInput'

const CHUYEN: Record<TrangThaiKhoa, TrangThaiKhoa[]> = {
  DU_KIEN: ['DANG_TUYEN', 'HUY'],
  DANG_TUYEN: ['DANG_DAO_TAO', 'HUY'],
  DANG_DAO_TAO: ['DA_KET_THUC'],
  DA_KET_THUC: [],
  HUY: [],
}

export default function KhoaPage() {
  const { message, modal } = App.useApp()
  const { coQuyen } = useAuth()
  const laAdmin = coQuyen('ADMIN')
  const qc = useQueryClient()
  const [loc, setLoc] = useState<TrangThaiKhoa>()
  const { data, isFetching } = useKhoa(loc)
  const { data: hang } = useHangGplx()
  const [moTao, setMoTao] = useState(false)
  const [form] = Form.useForm()
  const [xet, setXet] = useState<{ khoa: Khoa; ketQua: KetQuaXet[] } | null>(null)
  const [dangXacNhan, setDangXacNhan] = useState(false)
  const [khoaSua, setKhoaSua] = useState<Khoa | null>(null)
  const hangMaChon = Form.useWatch('hangMa', form)

  const lamMoi = () => qc.invalidateQueries({ queryKey: ['khoa'] })

  const tao = useMutation({
    mutationFn: async (v: { maKhoa: string; hangMa: string; thoiGian: [Dayjs, Dayjs]; siSoToiDa: number; hocPhi?: number; ghiChu?: string }) =>
      api.post('/khoa', {
        maKhoa: v.maKhoa, hangMa: v.hangMa,
        ngayKhaiGiang: v.thoiGian[0].format('YYYY-MM-DD'), ngayBeGiang: v.thoiGian[1].format('YYYY-MM-DD'),
        siSoToiDa: v.siSoToiDa, hocPhi: v.hocPhi, ghiChu: v.ghiChu,
      }),
    onSuccess: () => { message.success('Đã tạo khóa'); setMoTao(false); form.resetFields(); lamMoi() },
    onError: (e) => message.error(loiApi(e)),
  })

  const sua = useMutation({
    mutationFn: async (v: { maKhoa: string; hangMa: string; thoiGian: [Dayjs, Dayjs]; siSoToiDa: number; hocPhi?: number; ghiChu?: string }) =>
      api.put(`/khoa/${khoaSua!.id}`, {
        maKhoa: v.maKhoa, hangMa: v.hangMa,
        ngayKhaiGiang: v.thoiGian[0].format('YYYY-MM-DD'), ngayBeGiang: v.thoiGian[1].format('YYYY-MM-DD'),
        siSoToiDa: v.siSoToiDa, hocPhi: v.hocPhi, ghiChu: v.ghiChu,
      }),
    onSuccess: () => { message.success('Đã sửa khóa'); setKhoaSua(null); lamMoi() },
    onError: (e) => message.error(loiApi(e)),
  })

  const doiTrangThai = (k: Khoa, moi: TrangThaiKhoa) => {
    modal.confirm({
      title: `Chuyển khóa ${k.maKhoa} sang "${NHAN_TRANG_THAI_KHOA[moi].text}"?`,
      content: moi === 'DANG_DAO_TAO' ? 'Học viên "Đã tiếp nhận" sẽ chuyển sang "Đang học". Nhớ gửi báo cáo đăng ký khóa về Sở Xây dựng.' : undefined,
      onOk: async () => {
        try {
          await api.patch(`/khoa/${k.id}/trang-thai`, { trangThai: moi })
          message.success('Đã cập nhật')
          lamMoi()
        } catch (e) {
          message.error(loiApi(e))
        }
      },
    })
  }

  const moXet = async (k: Khoa) => {
    try {
      const { data: kq } = await api.get<KetQuaXet[]>(`/khoa/${k.id}/xet-hoan-thanh`)
      setXet({ khoa: k, ketQua: kq })
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  const xacNhanXet = async () => {
    if (!xet) return
    setDangXacNhan(true)
    try {
      const { data: kq } = await api.post<KetQuaXet[]>(`/khoa/${xet.khoa.id}/xet-hoan-thanh`)
      message.success(`Đã xét: ${kq.filter((r) => r.dat).length}/${kq.length} học viên hoàn thành`)
      setXet(null)
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    } finally {
      setDangXacNhan(false)
    }
  }

  const columns: ColumnsType<Khoa> = [
    { title: 'Mã khóa', dataIndex: 'maKhoa' },
    { title: 'Hạng', dataIndex: 'hangMa', width: 70 },
    { title: 'Thời gian', render: (_, k) => `${ngay(k.ngayKhaiGiang)} – ${ngay(k.ngayBeGiang)} (${k.soNgay} ngày)` },
    { title: 'Sĩ số', render: (_, k) => `${k.soDangKy}/${k.siSoToiDa}`, width: 80 },
    { title: 'Học phí', dataIndex: 'hocPhi', render: (v) => tien(v), align: 'right', className: 'hide-mobile' },
    {
      title: 'Trạng thái', dataIndex: 'trangThai',
      render: (v: TrangThaiKhoa) => <Tag color={NHAN_TRANG_THAI_KHOA[v].color}>{NHAN_TRANG_THAI_KHOA[v].text}</Tag>,
    },
    {
      title: '', key: 'act', width: 220,
      render: (_, k) => laAdmin && (
        <Space>
          {CHUYEN[k.trangThai].length > 0 && (
            <Dropdown menu={{
              items: CHUYEN[k.trangThai].map((t) => ({ key: t, label: NHAN_TRANG_THAI_KHOA[t].text })),
              onClick: (e) => doiTrangThai(k, e.key as TrangThaiKhoa),
            }}>
              <Button size="small">Chuyển <DownOutlined /></Button>
            </Dropdown>
          )}
          <Button size="small" icon={<EditOutlined />} onClick={() => {
            form.setFieldsValue({
              maKhoa: k.maKhoa, hangMa: k.hangMa, siSoToiDa: k.siSoToiDa, hocPhi: k.hocPhi, ghiChu: k.ghiChu,
              thoiGian: [dayjs(k.ngayKhaiGiang), dayjs(k.ngayBeGiang)]
            })
            setKhoaSua(k)
          }}>Sửa</Button>
          {(k.trangThai === 'DANG_DAO_TAO' || k.trangThai === 'DA_KET_THUC') && (
            <Button size="small" type="primary" ghost onClick={() => moXet(k)}>Xét hoàn thành</Button>
          )}
        </Space>
      ),
    },
  ]

  const hangChon = hang?.find((h) => h.ma === hangMaChon)

  return (
    <>
      <h1 className="page-title">Khóa đào tạo</h1>
      <div className="toolbar">
        <Select allowClear placeholder="Lọc trạng thái" style={{ width: 180 }} value={loc} onChange={setLoc}
                options={Object.entries(NHAN_TRANG_THAI_KHOA).map(([k, v]) => ({ value: k, label: v.text }))} />
        {laAdmin && (
          <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }} onClick={() => setMoTao(true)}>
            Tạo khóa
          </Button>
        )}
      </div>
      <Table rowKey="id" size="middle" loading={isFetching} columns={columns} dataSource={data} scroll={{ x: 800 }} />

      <Modal title={khoaSua ? 'Sửa khóa đào tạo' : 'Tạo khóa đào tạo'} open={moTao || !!khoaSua} onCancel={() => { setMoTao(false); setKhoaSua(null) }} onOk={() => form.submit()}
             confirmLoading={tao.isPending || sua.isPending} okText={khoaSua ? 'Lưu' : 'Tạo'} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => khoaSua ? sua.mutate(v) : tao.mutate(v)} initialValues={{ siSoToiDa: 60 }}>
          <Row gutter={12}>
            <Col span={14}>
              <Form.Item name="maKhoa" label="Mã khóa" rules={[{ required: true, message: 'Nhập mã khóa' }]}>
                <Input placeholder={`A1-${dayjs().format('YYYY-MM')}-01`} />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="hangMa" label="Hạng" rules={[{ required: true, message: 'Chọn hạng' }]}>
                <Select options={hang?.map((h) => ({ value: h.ma, label: `Hạng ${h.ma}` }))} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="thoiGian" label="Khai giảng – bế giảng" rules={[{ required: true, message: 'Chọn thời gian' }]}
                     extra={hangChon && `Hạng ${hangChon.ma}: tối đa ${hangChon.soNgayKhoaToiDa} ngày; ${hangChon.gioLyThuyet} giờ LT + ${hangChon.gioThucHanh} giờ TH`}>
            <DatePicker.RangePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={10}>
              <Form.Item name="siSoToiDa" label="Sĩ số tối đa" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={14}>
              <Form.Item name="hocPhi" label="Học phí (để trống = mặc định của hạng)">
                <SoTienInput min={0} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="ghiChu" label="Ghi chú"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title={xet ? `Xét hoàn thành khóa ${xet.khoa.maKhoa}` : ''} open={!!xet} width={900}
             onCancel={() => setXet(null)} okText="Xác nhận kết quả" onOk={xacNhanXet} confirmLoading={dangXacNhan}>
        <Alert type="info" showIcon style={{ marginBottom: 12 }}
               message="Đây là bản xem trước. Bấm “Xác nhận” để ghi kết quả và cấp số giấy xác nhận cho học viên đạt." />
        <Table<KetQuaXet>
          rowKey="dangKyId" size="small" pagination={false} dataSource={xet?.ketQua} scroll={{ x: 700 }}
          columns={[
            { title: 'Hồ sơ', dataIndex: 'maHoSo' },
            { title: 'Học viên', dataIndex: 'hoTen' },
            { title: 'Hình thức', dataIndex: 'hinhThucLyThuyet', render: (v) => NHAN_HINH_THUC[v] },
            { title: 'Giờ TH', render: (_, r) => `${r.gioThucHanh}/${r.gioThucHanhQuyDinh}` },
            { title: 'Giờ LT', render: (_, r) => (r.hinhThucLyThuyet === 'TAP_TRUNG' ? `${r.gioLyThuyet}/${r.gioLyThuyetQuyDinh}` : '—') },
            { title: 'Còn nợ', dataIndex: 'conNo', render: (v) => tien(v), align: 'right' },
            {
              title: 'Kết quả',
              render: (_, r) => (r.dat ? <Tag color="green">Đạt</Tag> : <Tag color="red" title={r.lyDo.join('\n')}>Chưa đạt</Tag>),
            },
            { title: 'Lý do', render: (_, r) => r.lyDo.join('; ') },
          ]}
        />
      </Modal>
    </>
  )
}
