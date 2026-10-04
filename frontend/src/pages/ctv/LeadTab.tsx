import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Dropdown, Form, Input, Modal, Radio, Select, Space, Table, Tag, Typography } from 'antd'
import { DownOutlined, PlusOutlined, SolutionOutlined, SwapOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import { useCtv, useHangGplx } from '../../api/hooks'
import type { Lead, PageResponse, TrangThaiLead } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { NHAN_NGUON_LEAD, NHAN_TRANG_THAI_LEAD, ngayGio } from '../../utils/format'
import TiepNhanDrawer, { type MacDinhTiepNhan } from '../hocvien/TiepNhanDrawer'

interface LeadForm {
  hoTen: string
  soDienThoai: string
  diaChi?: string
  hangMuonHoc?: string
  ghiChu?: string
  nguon: 'CTV' | 'VAN_PHONG'
  ctvId?: number
}

/** Lead: le tan nhap thay CTV (Zalo / form giay) hoac khach tu den; chot thanh ho so bang "Tao ho so". */
export default function LeadTab() {
  const { message, modal } = App.useApp()
  const { coQuyen } = useAuth()
  const qc = useQueryClient()
  const { data: ctv } = useCtv()
  const { data: hang } = useHangGplx()
  const [trangThai, setTrangThai] = useState<TrangThaiLead>()
  const [ctvLoc, setCtvLoc] = useState<number>()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [moTao, setMoTao] = useState(false)
  const [trung, setTrung] = useState<string>()
  const [tiepNhan, setTiepNhan] = useState<MacDinhTiepNhan>()
  const [form] = Form.useForm<LeadForm>()
  const nguon = Form.useWatch('nguon', form)

  const { data, isFetching } = useQuery({
    queryKey: ['lead', trangThai, ctvLoc, q, page],
    queryFn: async () => (await api.get<PageResponse<Lead>>('/lead', {
      params: { trangThai, ctvId: ctvLoc, q: q || undefined, page, size: 20 },
    })).data,
    placeholderData: (prev) => prev,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['lead'] })

  const tao = useMutation({
    mutationFn: async (v: LeadForm) => api.post('/lead', v),
    onSuccess: () => { message.success('Đã ghi nhận lead'); setMoTao(false); lamMoi() },
    onError: (e) => message.error(loiApi(e)),
  })

  // Chong trung ngay khi nhap SDT
  const kiemTraTrung = async (sdt: string) => {
    setTrung(undefined)
    if (!/^0[0-9]{9}$/.test(sdt)) return
    const { data: kq } = await api.get<{ trung: boolean; moTa?: string }>('/lead/kiem-tra-trung', { params: { sdt } })
    if (kq.trung) setTrung(kq.moTa)
  }

  const doiTrangThai = async (l: Lead, tt: string) => {
    try {
      await api.patch(`/lead/${l.id}/trang-thai`, { trangThai: tt })
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  const giaoLai = (l: Lead) => {
    let ctvId: number | undefined
    modal.confirm({
      title: `Giao lead ${l.hoTen} cho CTV khác?`,
      content: (
        <Select style={{ width: '100%', marginTop: 8 }} placeholder="Chọn CTV" showSearch optionFilterProp="label"
                onChange={(v: number) => (ctvId = v)}
                options={ctv?.filter((c) => c.id !== l.ctvId).map((c) => ({ value: c.id, label: `${c.hoTen} – ${c.soDienThoai}` }))} />
      ),
      onOk: async () => {
        if (!ctvId) { message.warning('Chọn CTV'); throw new Error('chua chon') }
        try {
          await api.patch(`/lead/${l.id}/ctv`, { ctvId })
          message.success('Đã giao lại lead')
          lamMoi()
        } catch (e) {
          message.error(loiApi(e))
          throw e
        }
      },
    })
  }

  return (
    <>
      <div className="toolbar">
        <Input.Search allowClear placeholder="Tìm tên, SĐT" style={{ width: 200 }} onSearch={(v) => { setQ(v.trim()); setPage(0) }} />
        <Select allowClear placeholder="Trạng thái" style={{ width: 150 }} value={trangThai} onChange={(v) => { setTrangThai(v); setPage(0) }}
                options={Object.entries(NHAN_TRANG_THAI_LEAD).map(([k, v]) => ({ value: k, label: v.text }))} />
        <Select allowClear placeholder="CTV" style={{ width: 200 }} value={ctvLoc} onChange={(v) => { setCtvLoc(v); setPage(0) }}
                showSearch optionFilterProp="label" options={ctv?.map((c) => ({ value: c.id, label: c.hoTen }))} />
        <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }}
                onClick={() => { form.resetFields(); setTrung(undefined); setMoTao(true) }}>Thêm lead</Button>
      </div>
      <Table<Lead>
        rowKey="id" size="middle" loading={isFetching} dataSource={data?.items} scroll={{ x: 860 }}
        pagination={{ current: page + 1, pageSize: 20, total: data?.total, showSizeChanger: false, onChange: (p) => setPage(p - 1) }}
        columns={[
          {
            title: 'Khách',
            render: (_, l) => (
              <div>
                <Typography.Text strong>{l.hoTen}</Typography.Text>
                <div style={{ fontSize: 12, color: '#888' }}>{l.soDienThoai}{l.hangMuonHoc ? ` · hạng ${l.hangMuonHoc}` : ''}</div>
              </div>
            ),
          },
          { title: 'Nguồn', render: (_, l) => (l.nguon === 'CTV' ? <>CTV <b>{l.tenCtv}</b></> : NHAN_NGUON_LEAD[l.nguon]) },
          { title: 'Ngày', dataIndex: 'createdAt', render: (v) => ngayGio(v), className: 'hide-mobile' },
          { title: 'Người nhập', dataIndex: 'nguoiNhap', className: 'hide-mobile' },
          {
            title: 'Trạng thái',
            render: (_, l) => (
              <>
                <Tag color={NHAN_TRANG_THAI_LEAD[l.trangThai].color}>{NHAN_TRANG_THAI_LEAD[l.trangThai].text}</Tag>
                {l.maHoSo && <div style={{ fontSize: 12 }}>{l.maHoSo}</div>}
              </>
            ),
          },
          {
            title: '', key: 'act', width: 230,
            render: (_, l) => l.trangThai !== 'DA_CHOT' && (
              <Space>
                <Button size="small" type="primary" ghost icon={<SolutionOutlined />}
                        onClick={() => setTiepNhan({ leadId: l.id, hoTen: l.hoTen, soDienThoai: l.soDienThoai, diaChi: l.diaChi,
                          nguon: l.nguon, ctvId: l.ctvId, tenCtv: l.tenCtv })}>Tạo hồ sơ</Button>
                <Dropdown menu={{
                  items: (['MOI', 'DA_LIEN_HE', 'KHONG_THANH'] as const).filter((t) => t !== l.trangThai)
                    .map((t) => ({ key: t, label: NHAN_TRANG_THAI_LEAD[t].text })),
                  onClick: (e) => doiTrangThai(l, e.key),
                }}>
                  <Button size="small">Trạng thái <DownOutlined /></Button>
                </Dropdown>
                {coQuyen('ADMIN') && <Button size="small" icon={<SwapOutlined />} title="Giao CTV khác" onClick={() => giaoLai(l)} />}
              </Space>
            ),
          },
        ]}
      />

      <Modal title="Thêm lead" open={moTao} onCancel={() => setMoTao(false)} onOk={() => form.submit()}
             confirmLoading={tao.isPending} okButtonProps={{ disabled: !!trung }} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => tao.mutate(v)} initialValues={{ nguon: 'CTV' }}>
          <Form.Item name="nguon" label="Nguồn">
            <Radio.Group options={[{ value: 'CTV', label: 'CTV giới thiệu' }, { value: 'VAN_PHONG', label: 'Khách tự đến / gọi điện' }]} />
          </Form.Item>
          {nguon === 'CTV' && (
            <Form.Item name="ctvId" label="CTV" rules={[{ required: true, message: 'Chọn CTV từ danh sách' }]}
                       extra="Chỉ chọn từ danh sách CTV đang hoạt động, không gõ tay tên CTV">
              <Select showSearch optionFilterProp="label" placeholder="Chọn CTV"
                      options={ctv?.map((c) => ({ value: c.id, label: `${c.hoTen} – ${c.soDienThoai}` }))} />
            </Form.Item>
          )}
          <Form.Item name="soDienThoai" label="SĐT khách" rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input maxLength={10} onBlur={(e) => kiemTraTrung(e.target.value)} />
          </Form.Item>
          {trung && <Alert type="warning" showIcon style={{ marginBottom: 12 }} message="Trùng lead" description={trung} />}
          <Form.Item name="hoTen" label="Họ tên khách" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
          <Form.Item name="hangMuonHoc" label="Hạng muốn học">
            <Select allowClear options={hang?.map((h) => ({ value: h.ma, label: `Hạng ${h.ma}` }))} />
          </Form.Item>
          <Form.Item name="diaChi" label="Địa chỉ"><Input /></Form.Item>
          <Form.Item name="ghiChu" label="Ghi chú"><Input /></Form.Item>
        </Form>
      </Modal>

      <TiepNhanDrawer open={!!tiepNhan} macDinh={tiepNhan} onClose={() => setTiepNhan(undefined)}
                      onDone={() => { setTiepNhan(undefined); lamMoi(); message.success('Lead đã chốt thành hồ sơ') }} />
    </>
  )
}
