import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, DatePicker, Form, Input, Modal, Radio, Segmented, Select, Space, Table, Tag, Typography } from 'antd'
import { CheckOutlined, DollarOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../../api/client'
import type { HoaHong, PhieuChi, TongHopKy, TrangThaiHoaHong } from '../../api/types'
import { NHAN_TRANG_THAI_HOA_HONG, mucHoaHong, ngay, tien } from '../../utils/format'

/**
 * Admin: chot ky thang. Chua du dieu kien -> Du dieu kien (tu dong khi hoc vien dong du) -> Da duyet -> Da chi.
 * Moi lan chi ghi ngay, so tien, nguoi chi (tu dong lay nguoi dang nhap).
 */
export default function HoaHongTab() {
  const { message, modal } = App.useApp()
  const qc = useQueryClient()
  const [ky, setKy] = useState<Dayjs>(dayjs())
  const [xem, setXem] = useState<'tong-hop' | 'chi-tiet' | 'phieu-chi'>('tong-hop')
  const [trangThai, setTrangThai] = useState<TrangThaiHoaHong>()
  const [chon, setChon] = useState<number[]>([])
  const [chi, setChi] = useState<TongHopKy | null>(null)
  const [formChi] = Form.useForm<{ ngayChi: Dayjs; hinhThuc: 'TIEN_MAT' | 'CHUYEN_KHOAN'; ghiChu?: string }>()
  const kyStr = ky.format('YYYY-MM')

  const tongHop = useQuery({
    queryKey: ['hoa-hong', 'tong-hop', kyStr],
    enabled: xem === 'tong-hop',
    queryFn: async () => (await api.get<TongHopKy[]>('/hoa-hong/tong-hop', { params: { ky: kyStr } })).data,
  })
  const chiTiet = useQuery({
    queryKey: ['hoa-hong', 'ds', trangThai],
    enabled: xem === 'chi-tiet',
    queryFn: async () => (await api.get<HoaHong[]>('/hoa-hong', { params: { trangThai } })).data,
  })
  const phieuChi = useQuery({
    queryKey: ['hoa-hong', 'phieu-chi'],
    enabled: xem === 'phieu-chi',
    queryFn: async () => (await api.get<PhieuChi[]>('/hoa-hong/phieu-chi')).data,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['hoa-hong'] })

  const duyetIds = async (ids: number[]) => {
    try {
      const { data } = await api.post<{ soDaDuyet: number }>('/hoa-hong/duyet', { ids })
      message.success(`Đã duyệt ${data.soDaDuyet} khoản`)
      setChon([])
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  /** Duyet moi khoan du dieu kien cua mot CTV tu ky dang chon tro ve truoc. */
  const duyetCtv = (r: TongHopKy) => modal.confirm({
    title: `Duyệt hoa hồng của ${r.tenCtv}?`,
    content: `${tien(r.duDieuKien)} đủ điều kiện đến kỳ ${kyStr}.`,
    onOk: async () => {
      const { data } = await api.get<HoaHong[]>('/hoa-hong', { params: { ctvId: r.ctvId, trangThai: 'DU_DIEU_KIEN' } })
      const ids = data.filter((h) => h.ky && h.ky <= kyStr).map((h) => h.id)
      if (ids.length) await duyetIds(ids)
    },
  })

  return (
    <>
      <div className="toolbar">
        <Segmented value={xem} onChange={(v) => setXem(v as typeof xem)}
                   options={[{ value: 'tong-hop', label: 'Chốt kỳ & chi' }, { value: 'chi-tiet', label: 'Chi tiết hoa hồng' }, { value: 'phieu-chi', label: 'Phiếu chi' }]} />
        {xem === 'tong-hop' && (
          <DatePicker picker="month" value={ky} onChange={(v) => v && setKy(v)} format="MM/YYYY" allowClear={false} />
        )}
        {xem === 'chi-tiet' && (
          <Select allowClear placeholder="Trạng thái" style={{ width: 180 }} value={trangThai} onChange={(v) => { setTrangThai(v); setChon([]) }}
                  options={Object.entries(NHAN_TRANG_THAI_HOA_HONG).map(([k, v]) => ({ value: k, label: v.text }))} />
        )}
        {xem === 'chi-tiet' && chon.length > 0 && (
          <Button type="primary" icon={<CheckOutlined />} style={{ marginLeft: 'auto' }} onClick={() => duyetIds(chon)}>
            Duyệt {chon.length} khoản
          </Button>
        )}
      </div>

      {xem === 'tong-hop' && (
        <>
          <Alert type="info" showIcon style={{ marginBottom: 12 }}
                 message={`Kỳ ${ky.format('MM/YYYY')}: các khoản đủ điều kiện đến hết tháng này, chi vào ngày chi hằng tháng của tháng sau. Khoản kỳ trước chưa chi vẫn được gộp vào.`} />
          <Table<TongHopKy>
            rowKey="ctvId" size="middle" loading={tongHop.isFetching} dataSource={tongHop.data} pagination={false} scroll={{ x: 900 }}
            locale={{ emptyText: 'Không có hoa hồng cần xử lý trong kỳ' }}
            columns={[
              { title: 'CTV', render: (_, r) => <><b>{r.tenCtv}</b><div style={{ fontSize: 12, color: '#888' }}>{r.soDienThoai}</div></> },
              { title: 'Nhận tiền', render: (_, r) => (r.soTaiKhoan ? `${r.nganHang ?? ''} · ${r.soTaiKhoan}` : <Tag>Tiền mặt</Tag>), className: 'hide-mobile' },
              { title: 'Số đơn', dataIndex: 'soDon', align: 'right' },
              { title: 'Đủ điều kiện', dataIndex: 'duDieuKien', render: (v) => tien(v), align: 'right' },
              { title: 'Đã duyệt', dataIndex: 'daDuyet', render: (v) => <b>{tien(v)}</b>, align: 'right' },
              { title: 'Đã chi kỳ này', dataIndex: 'daChi', render: (v) => tien(v), align: 'right' },
              {
                title: '', key: 'act', width: 190,
                render: (_, r) => (
                  <Space>
                    {r.duDieuKien > 0 && <Button size="small" icon={<CheckOutlined />} onClick={() => duyetCtv(r)}>Duyệt</Button>}
                    {r.daDuyet > 0 && (
                      <Button size="small" type="primary" icon={<DollarOutlined />}
                              onClick={() => { formChi.setFieldsValue({ ngayChi: dayjs(), hinhThuc: r.soTaiKhoan ? 'CHUYEN_KHOAN' : 'TIEN_MAT' }); setChi(r) }}>
                        Ghi nhận chi
                      </Button>
                    )}
                  </Space>
                ),
              },
            ]}
          />
        </>
      )}

      {xem === 'chi-tiet' && (
        <Table<HoaHong>
          rowKey="id" size="small" loading={chiTiet.isFetching} dataSource={chiTiet.data} scroll={{ x: 1000 }}
          rowSelection={{
            selectedRowKeys: chon,
            onChange: (k) => setChon(k as number[]),
            getCheckboxProps: (h) => ({ disabled: h.trangThai !== 'DU_DIEU_KIEN' }),
          }}
          columns={[
            { title: 'Hồ sơ', render: (_, h) => <><b>{h.maHoSo}</b><div style={{ fontSize: 12 }}>{h.hocVien}</div></> },
            { title: 'CTV', dataIndex: 'tenCtv' },
            { title: 'Khóa', render: (_, h) => `${h.maKhoa} (${h.hang})`, className: 'hide-mobile' },
            { title: 'Mức', render: (_, h) => mucHoaHong(h.kieu, h.giaTri) },
            { title: 'Hoa hồng', dataIndex: 'soTien', render: (v) => <b>{tien(v)}</b>, align: 'right' },
            { title: 'Kỳ', dataIndex: 'ky' },
            {
              title: 'Trạng thái',
              render: (_, h) => (
                <>
                  <Tag color={NHAN_TRANG_THAI_HOA_HONG[h.trangThai].color}>{NHAN_TRANG_THAI_HOA_HONG[h.trangThai].text}</Tag>
                  {h.soPhieuChi && <div style={{ fontSize: 12 }}>{h.soPhieuChi}</div>}
                  {h.ghiChu && <Typography.Text type="warning" style={{ fontSize: 12 }}>{h.ghiChu}</Typography.Text>}
                </>
              ),
            },
          ]}
        />
      )}

      {xem === 'phieu-chi' && (
        <Table<PhieuChi>
          rowKey="id" size="middle" loading={phieuChi.isFetching} dataSource={phieuChi.data} scroll={{ x: 800 }}
          columns={[
            { title: 'Số phiếu', dataIndex: 'soPhieu' },
            { title: 'CTV', dataIndex: 'tenCtv' },
            { title: 'Kỳ', dataIndex: 'ky' },
            { title: 'Số tiền', dataIndex: 'soTien', render: (v) => <b>{tien(v)}</b>, align: 'right' },
            { title: 'Hình thức', dataIndex: 'hinhThuc', render: (v) => (v === 'TIEN_MAT' ? 'Tiền mặt' : 'Chuyển khoản') },
            { title: 'Ngày chi', dataIndex: 'ngayChi', render: (v) => ngay(v) },
            { title: 'Người chi', dataIndex: 'nguoiChi' },
            { title: 'Số đơn', dataIndex: 'soDon', align: 'right' },
          ]}
        />
      )}

      <Modal title={`Ghi nhận chi hoa hồng – ${chi?.tenCtv ?? ''}`} open={!!chi} onCancel={() => setChi(null)}
             onOk={() => formChi.submit()} okText="Xác nhận đã chi" destroyOnClose>
        {chi && (
          <Typography.Paragraph>
            Số tiền: <b style={{ fontSize: 18 }}>{tien(chi.daDuyet)}</b> (các khoản đã duyệt đến kỳ {kyStr})
            {chi.soTaiKhoan && <><br />Chuyển vào: {chi.nganHang} · {chi.soTaiKhoan} · {chi.chuTaiKhoan}</>}
          </Typography.Paragraph>
        )}
        <Form form={formChi} layout="vertical" onFinish={async (v) => {
          try {
            await api.post('/hoa-hong/chi', { ctvId: chi!.ctvId, ky: kyStr, ngayChi: v.ngayChi.format('YYYY-MM-DD'), hinhThuc: v.hinhThuc, ghiChu: v.ghiChu })
            message.success('Đã ghi nhận chi')
            setChi(null)
            lamMoi()
          } catch (e) {
            message.error(loiApi(e))
          }
        }}>
          <Form.Item name="ngayChi" label="Ngày chi" rules={[{ required: true }]}>
            <DatePicker format="DD/MM/YYYY" disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
          </Form.Item>
          <Form.Item name="hinhThuc" label="Hình thức">
            <Radio.Group options={[{ value: 'CHUYEN_KHOAN', label: 'Chuyển khoản' }, { value: 'TIEN_MAT', label: 'Tiền mặt' }]} />
          </Form.Item>
          <Form.Item name="ghiChu" label="Ghi chú (mã giao dịch, người nhận…)"><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
