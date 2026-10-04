import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App, Button, Descriptions, Divider, Drawer, Form, Input, Modal, Progress, Radio, Select, Space, Table, Tag,
  Typography, Upload,
} from 'antd'
import { DollarOutlined, FilePdfOutlined, UploadOutlined } from '@ant-design/icons'
import { api, loiApi, taiTep } from '../../api/client'
import { hienMatKhauTam } from '../../components/MatKhauTam'
import type { CongNo, DangKy, HoaHong, KetQuaCapMatKhau, Khoa, PhieuThu, TienDo } from '../../api/types'
import { useCtv } from '../../api/hooks'
import { useAuth } from '../../auth/AuthContext'
import { NHAN_HINH_THUC, NHAN_NGUON, NHAN_TRANG_THAI_DK, NHAN_TRANG_THAI_HOA_HONG, NHAN_TRANG_THAI_KHOA, mucHoaHong, ngay, ngayGio, tien } from '../../utils/format'
import SoTienInput from '../../components/SoTienInput'

interface Props {
  dangKyId?: number
  onClose: () => void
  onChanged: () => void
}

export default function ChiTietDrawer({ dangKyId, onClose, onChanged }: Props) {
  const { message, modal } = App.useApp()
  const { coQuyen } = useAuth()
  const qc = useQueryClient()
  const [moThu, setMoThu] = useState(false)
  const [formThu] = Form.useForm()
  const [moChuyenKhoa, setMoChuyenKhoa] = useState(false)
  const [khoaMoiId, setKhoaMoiId] = useState<number>()
  const open = dangKyId != null

  const dk = useQuery({
    queryKey: ['dang-ky-ct', dangKyId],
    enabled: open,
    queryFn: async () => (await api.get<DangKy>(`/dang-ky/${dangKyId}`)).data,
  })
  const tienDo = useQuery({
    queryKey: ['tien-do', dangKyId],
    enabled: open,
    queryFn: async () => (await api.get<TienDo>(`/dang-ky/${dangKyId}/tien-do`)).data,
  })
  const phieu = useQuery({
    queryKey: ['phieu-thu', dangKyId],
    enabled: open,
    queryFn: async () => (await api.get<PhieuThu[]>('/phieu-thu', { params: { dangKyId } })).data,
  })
  const congNo = useQuery({
    queryKey: ['cong-no-dk', dangKyId],
    enabled: open,
    queryFn: async () => (await api.get<CongNo>(`/phieu-thu/cong-no/${dangKyId}`)).data,
  })
  const dsKhoa = useQuery({
    queryKey: ['khoa'],
    queryFn: async () => (await api.get<Khoa[]>('/khoa')).data,
  })


  const { data: dsCtv } = useCtv()
  const hoaHong = useQuery({
    queryKey: ['hoa-hong-dk', dangKyId],
    enabled: open,
    retry: false,
    queryFn: async () => {
      try {
        return (await api.get<HoaHong>(`/hoa-hong/dang-ky/${dangKyId}`)).data
      } catch {
        return null
      }
    },
  })

  /** Gan CTV (le tan khi chua co CTV; doi CTV da gan chi admin). Chon tu danh sach, khong go tay. */
  const ganCtv = (d: DangKy) => {
    let ctvId: number | undefined
    modal.confirm({
      title: d.ctvId ? 'Đổi CTV giới thiệu?' : 'Gắn CTV giới thiệu',
      content: (
        <Select style={{ width: '100%', marginTop: 8 }} placeholder="Chọn CTV đang hoạt động" showSearch optionFilterProp="label"
                onChange={(v: number) => (ctvId = v)}
                options={dsCtv?.filter((c) => c.id !== d.ctvId).map((c) => ({ value: c.id, label: `${c.hoTen} – ${c.soDienThoai}` }))} />
      ),
      onOk: async () => {
        if (!ctvId) { message.warning('Chọn CTV'); throw new Error('chua chon') }
        try {
          await api.patch(`/dang-ky/${d.id}/ctv`, { ctvId })
          message.success('Đã gắn CTV, hoa hồng được tính tự động')
          lamMoi()
          qc.invalidateQueries({ queryKey: ['hoa-hong-dk', dangKyId] })
        } catch (e) {
          message.error(loiApi(e))
          throw e
        }
      },
    })
  }

  const lamMoi = () => {
    qc.invalidateQueries({ queryKey: ['dang-ky-ct', dangKyId] })
    qc.invalidateQueries({ queryKey: ['phieu-thu', dangKyId] })
    qc.invalidateQueries({ queryKey: ['cong-no-dk', dangKyId] })
    qc.invalidateQueries({ queryKey: ['hoa-hong-dk', dangKyId] })
    onChanged()
  }

  const lapPhieu = useMutation({
    mutationFn: async (v: { soTien: number; hinhThuc: string; noiDung?: string }) =>
      (await api.post<{ phieu: PhieuThu }>('/phieu-thu', { dangKyId, ...v })).data,
    onSuccess: (res) => {
      message.success(`Đã lập phiếu ${res.phieu.soPhieu}`)
      setMoThu(false)
      formThu.resetFields()
      lamMoi()
      taiTep(`/phieu-thu/${res.phieu.id}/pdf`, undefined, true)
    },
    onError: (e) => message.error(loiApi(e)),
  })

  const hanhDong = async (fn: () => Promise<unknown>, thanhCong: string) => {
    try {
      await fn()
      message.success(thanhCong)
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  const nhapLyDo = (tieuDe: string, onOk: (lyDo: string) => Promise<void>) => {
    let lyDo = ''
    modal.confirm({
      title: tieuDe,
      content: <Input.TextArea rows={2} placeholder="Lý do (bắt buộc)" onChange={(e) => (lyDo = e.target.value)} />,
      okText: 'Xác nhận',
      cancelText: 'Đóng',
      onOk: async () => {
        if (!lyDo.trim()) {
          message.warning('Cần nhập lý do')
          throw new Error('thieu ly do')
        }
        await onOk(lyDo.trim())
      },
    })
  }

  const d = dk.data
  const td = tienDo.data
  const cn = congNo.data
  const phanTram = (a: number, b: number) => (b > 0 ? Math.min(100, Math.round((a / b) * 100)) : 0)

  return (
    <Drawer title={d ? `Hồ sơ ${d.maHoSo}` : 'Hồ sơ'} open={open} onClose={onClose} width={720} destroyOnClose>
      {d && (
        <>
          <Space wrap style={{ marginBottom: 16 }}>
            <Tag color={NHAN_TRANG_THAI_DK[d.trangThai]?.color}>{NHAN_TRANG_THAI_DK[d.trangThai]?.text}</Tag>
            {d.trangThai === 'CHO_DUYET' && (
              <Button type="primary" onClick={() => hanhDong(() => api.patch(`/dang-ky/${d.id}/duyet`), 'Đã duyệt hồ sơ')}>
                Duyệt hồ sơ
              </Button>
            )}
            {cn && cn.conNo > 0 && d.trangThai !== 'DA_HUY' && (
              <Button type="primary" icon={<DollarOutlined />} onClick={() => { formThu.setFieldsValue({ soTien: cn.conNo, hinhThuc: 'TIEN_MAT' }); setMoThu(true) }}>
                Lập phiếu thu
              </Button>
            )}
            {['CHO_DUYET', 'DA_TIEP_NHAN', 'CHUA_DAT'].includes(d.trangThai) && (
              <Button onClick={() => setMoChuyenKhoa(true)}>Chuyển khóa</Button>
            )}
            {['CHO_DUYET', 'DA_TIEP_NHAN', 'DANG_HOC', 'CHUA_DAT'].includes(d.trangThai) && (
              <Button danger onClick={() => nhapLyDo('Hủy hồ sơ đăng ký?', (lyDo) =>
                hanhDong(() => api.patch(`/dang-ky/${d.id}/huy`, { lyDo }), 'Đã hủy hồ sơ'))}>
                Hủy hồ sơ
              </Button>
            )}
          </Space>

          <Descriptions size="small" column={{ xs: 1, md: 2 }} bordered>
            <Descriptions.Item label="Họ tên">{d.hocVien.hoTen}</Descriptions.Item>
            <Descriptions.Item label="Mã học viên">{d.hocVien.maHocVien}</Descriptions.Item>
            <Descriptions.Item label="Ngày sinh">{ngay(d.hocVien.ngaySinh)}</Descriptions.Item>
            <Descriptions.Item label="CCCD">{d.hocVien.cccd}</Descriptions.Item>
            <Descriptions.Item label="Điện thoại">{d.hocVien.soDienThoai}</Descriptions.Item>
            <Descriptions.Item label="Tài khoản học viên">
              {d.hocVien.coTaiKhoan ? <>Tên đăng nhập: <b>{d.hocVien.cccd}</b></> : <Typography.Text type="secondary">Chưa cấp</Typography.Text>}
              {d.trangThai !== 'CHO_DUYET' && d.trangThai !== 'DA_HUY' && (
                <Button size="small" type="link" onClick={() => modal.confirm({
                  title: d.hocVien.coTaiKhoan ? 'Đặt lại mật khẩu cổng học viên?' : 'Cấp tài khoản cổng học viên?',
                  content: 'Hệ thống tạo mật khẩu tạm, chỉ hiện một lần. Học viên phải đổi mật khẩu ở lần đăng nhập đầu.',
                  onOk: async () => {
                    try {
                      const { data: kq } = await api.post<KetQuaCapMatKhau>(`/hoc-vien/${d.hocVien.id}/cap-tai-khoan`)
                      lamMoi()
                      hienMatKhauTam(modal, kq)
                    } catch (e) {
                      message.error(loiApi(e))
                    }
                  },
                })}>
                  {d.hocVien.coTaiKhoan ? 'Đặt lại mật khẩu' : 'Cấp tài khoản'}
                </Button>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="Địa chỉ">{d.hocVien.diaChi}</Descriptions.Item>
            <Descriptions.Item label="Khóa">{d.maKhoa} (hạng {d.hang})</Descriptions.Item>
            <Descriptions.Item label="Hình thức LT">{NHAN_HINH_THUC[d.hinhThucLyThuyet]}</Descriptions.Item>
            <Descriptions.Item label="Nguồn">
              {NHAN_NGUON[d.nguon]}{d.tenCtv ? <> – <b>{d.tenCtv}</b></> : ''}
              {d.trangThai !== 'DA_HUY' && (!d.ctvId || coQuyen('ADMIN')) && (
                <Button size="small" type="link" onClick={() => ganCtv(d)}>{d.ctvId ? 'Đổi CTV' : 'Gắn CTV'}</Button>
              )}
            </Descriptions.Item>
            {hoaHong.data && (
              <Descriptions.Item label="Hoa hồng CTV" span={2}>
                <b>{tien(hoaHong.data.soTien)}</b> ({mucHoaHong(hoaHong.data.kieu, hoaHong.data.giaTri)}){' '}
                <Tag color={NHAN_TRANG_THAI_HOA_HONG[hoaHong.data.trangThai].color}>{NHAN_TRANG_THAI_HOA_HONG[hoaHong.data.trangThai].text}</Tag>
                {hoaHong.data.ghiChu && <Typography.Text type="warning"> {hoaHong.data.ghiChu}</Typography.Text>}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="Ngày đăng ký">{ngayGio(d.ngayDangKy)}</Descriptions.Item>
            {d.soGiayXacNhan && (
              <Descriptions.Item label="Giấy xác nhận" span={2}>{d.soGiayXacNhan} – {ngay(d.ngayHoanThanh)}</Descriptions.Item>
            )}
          </Descriptions>

          <Divider orientation="left" plain>Ảnh hồ sơ</Divider>
          <Space wrap>
            {(['CHAN_DUNG', 'CCCD_TRUOC', 'CCCD_SAU'] as const).map((loai) => (
              <Upload key={loai} accept="image/jpeg,image/png" showUploadList={false}
                      customRequest={async ({ file, onSuccess, onError }) => {
                        const fd = new FormData()
                        fd.append('file', file as Blob)
                        try {
                          await api.post(`/hoc-vien/${d.hocVien.id}/anh`, fd, { params: { loai } })
                          message.success('Đã tải ảnh lên')
                          lamMoi()
                          onSuccess?.({})
                        } catch (e) {
                          message.error(loiApi(e))
                          onError?.(e as Error)
                        }
                      }}>
                <Button icon={<UploadOutlined />}>
                  {loai === 'CHAN_DUNG' ? 'Ảnh chân dung' : loai === 'CCCD_TRUOC' ? 'CCCD mặt trước' : 'CCCD mặt sau'}
                </Button>
              </Upload>
            ))}
            {d.hocVien.coAnhChanDung && (
              <Button type="link" onClick={() => taiTep(`/hoc-vien/${d.hocVien.id}/anh/CHAN_DUNG`, undefined, true)}>Xem ảnh</Button>
            )}
          </Space>

          <Divider orientation="left" plain>Tiến độ học</Divider>
          {td && (
            <div>
              <div>Thực hành: {td.gioThucHanhDaHoc}/{td.gioThucHanhQuyDinh} giờ</div>
              <Progress percent={phanTram(td.gioThucHanhDaHoc, td.gioThucHanhQuyDinh)} status={td.duThucHanh ? 'success' : 'active'} />
              {td.xetLyThuyet ? (
                <>
                  <div>Lý thuyết: {td.gioLyThuyetDaHoc}/{td.gioLyThuyetQuyDinh} giờ</div>
                  <Progress percent={phanTram(td.gioLyThuyetDaHoc, td.gioLyThuyetQuyDinh)} status={td.duLyThuyet ? 'success' : 'active'} />
                </>
              ) : (
                <Typography.Text type="secondary">Tự học lý thuyết – không tính giờ lý thuyết tại trung tâm.</Typography.Text>
              )}
            </div>
          )}

          <Divider orientation="left" plain>Học phí</Divider>
          {cn && (
            <Space size="large" wrap style={{ marginBottom: 12 }}>
              <span>Phải đóng: <b>{tien(cn.phaiDong)}</b></span>
              <span>Đã đóng: <b style={{ color: '#389e0d' }}>{tien(cn.daDong)}</b></span>
              <span>Còn nợ: <b style={{ color: cn.conNo > 0 ? '#cf1322' : undefined }}>{tien(cn.conNo)}</b></span>
            </Space>
          )}
          <Table<PhieuThu>
            rowKey="id" size="small" pagination={false} dataSource={phieu.data}
            columns={[
              { title: 'Số phiếu', dataIndex: 'soPhieu' },
              { title: 'Ngày', dataIndex: 'ngayThu', render: (v) => ngayGio(v) },
              { title: 'Số tiền', dataIndex: 'soTien', render: (v) => tien(v), align: 'right' },
              {
                title: 'Trạng thái', dataIndex: 'trangThai',
                render: (v, r) => (v === 'DA_HUY' ? <Tag color="red" title={r.lyDoHuy}>Đã hủy</Tag> : <Tag color="green">Hiệu lực</Tag>),
              },
              {
                title: '', key: 'act',
                render: (_, r) => (
                  <Space>
                    <Button size="small" icon={<FilePdfOutlined />} onClick={() => taiTep(`/phieu-thu/${r.id}/pdf`, undefined, true)} />
                    {coQuyen('ADMIN') && r.trangThai === 'HIEU_LUC' && (
                      <Button size="small" danger onClick={() => nhapLyDo(`Hủy phiếu ${r.soPhieu}?`, (lyDo) =>
                        hanhDong(() => api.patch(`/phieu-thu/${r.id}/huy`, { lyDo }), 'Đã hủy phiếu'))}>
                        Hủy
                      </Button>
                    )}
                  </Space>
                ),
              },
            ]}
          />
        </>
      )}

      <Modal title="Lập phiếu thu học phí" open={moThu} onCancel={() => setMoThu(false)} okText="Lưu & in phiếu"
             confirmLoading={lapPhieu.isPending} onOk={() => formThu.submit()} destroyOnClose>
        <Form form={formThu} layout="vertical" onFinish={(v) => lapPhieu.mutate(v)}>
          <Form.Item name="soTien" label={`Số tiền (còn nợ ${tien(cn?.conNo)})`}
                     rules={[{ required: true, message: 'Nhập số tiền' }]}>
            <SoTienInput min={1000} max={cn?.conNo} />
          </Form.Item>
          <Form.Item name="hinhThuc" label="Hình thức">
            <Radio.Group options={[{ value: 'TIEN_MAT', label: 'Tiền mặt' }, { value: 'CHUYEN_KHOAN', label: 'Chuyển khoản' }]} />
          </Form.Item>
          <Form.Item name="noiDung" label="Nội dung">
            <Input placeholder="Mặc định: Học phí khóa ..." />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Chuyển khóa" open={moChuyenKhoa} onCancel={() => setMoChuyenKhoa(false)} onOk={() => hanhDong(async () => {
        if (!khoaMoiId) throw new Error('Vui lòng chọn khóa mới')
        await api.patch(`/dang-ky/${d?.id}/chuyen-khoa`, { khoaMoiId })
        setMoChuyenKhoa(false)
      }, 'Chuyển khóa thành công')} destroyOnClose>
        <Select style={{ width: '100%' }} placeholder="Chọn khóa mới (cùng hạng)" value={khoaMoiId} onChange={setKhoaMoiId}
          options={dsKhoa.data?.filter(k => k.id !== d?.khoaId && (k.trangThai === 'DANG_TUYEN' || k.trangThai === 'DANG_DAO_TAO') && k.hangMa === d?.hang)
            .map(k => ({ value: k.id, label: `${k.maKhoa} (${NHAN_TRANG_THAI_KHOA[k.trangThai as keyof typeof NHAN_TRANG_THAI_KHOA]?.text})` }))} />
      </Modal>
    </Drawer>
  )
}
