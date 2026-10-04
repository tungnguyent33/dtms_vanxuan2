import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Drawer, Form, Input, Select, Table, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import type { Ctv, TrangThaiCtv } from '../../api/types'
import { NHAN_HANG_CTV, NHAN_LOAI_CTV, NHAN_TRANG_THAI_CTV } from '../../utils/format'
import CtvForm from './CtvForm'
import { ctvBody, type CtvFormValues } from './ctvBody'
import CtvDrawer from './CtvDrawer'

/** Danh sach CTV. Le tan tao ho so (cho duyet); admin duyet trong ngan chi tiet. */
export default function CtvListTab() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [trangThai, setTrangThai] = useState<TrangThaiCtv>()
  const [q, setQ] = useState('')
  const [moTao, setMoTao] = useState(false)
  const [chon, setChon] = useState<number>()
  const [form] = Form.useForm<CtvFormValues>()

  const { data, isFetching } = useQuery({
    queryKey: ['ctv', 'tat-ca', trangThai, q],
    queryFn: async () => (await api.get<Ctv[]>('/ctv', { params: { tatCa: true, trangThai, q: q || undefined } })).data,
  })

  const tao = useMutation({
    mutationFn: async (v: CtvFormValues) => (await api.post<Ctv>('/ctv', ctvBody(v))).data,
    onSuccess: (c) => {
      message.success('Đã tạo hồ sơ CTV, chờ quản trị viên duyệt')
      setMoTao(false)
      form.resetFields()
      qc.invalidateQueries({ queryKey: ['ctv'] })
      setChon(c.id)
    },
    onError: (e) => message.error(loiApi(e)),
  })

  return (
    <>
      <div className="toolbar">
        <Input.Search allowClear placeholder="Tìm tên, SĐT, CCCD" style={{ width: 240 }} onSearch={(v) => setQ(v.trim())} />
        <Select allowClear placeholder="Trạng thái" style={{ width: 160 }} value={trangThai} onChange={setTrangThai}
                options={Object.entries(NHAN_TRANG_THAI_CTV).map(([k, v]) => ({ value: k, label: v.text }))} />
        <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }}
                onClick={() => { form.resetFields(); setMoTao(true) }}>Thêm CTV</Button>
      </div>
      <Table<Ctv>
        rowKey="id" size="middle" loading={isFetching} dataSource={data} scroll={{ x: 760 }}
        onRow={(c) => ({ onClick: () => setChon(c.id), style: { cursor: 'pointer' } })}
        columns={[
          {
            title: 'Cộng tác viên',
            render: (_, c) => (
              <div>
                <Typography.Text strong>{c.hoTen}</Typography.Text>
                <div style={{ fontSize: 12, color: '#888' }}>{c.soDienThoai} · {NHAN_LOAI_CTV[c.loai]}</div>
              </div>
            ),
          },
          { title: 'Hạng', dataIndex: 'hang', render: (v: string) => <Tag color={NHAN_HANG_CTV[v].color}>{NHAN_HANG_CTV[v].text}</Tag> },
          { title: 'Lead', dataIndex: 'soLead', align: 'right', className: 'hide-mobile' },
          { title: 'Học viên', dataIndex: 'soHocVien', align: 'right' },
          {
            title: 'Hồ sơ', className: 'hide-mobile',
            render: (_, c) => (
              <>
                {!c.cccd && <Tag color="red">Thiếu CCCD</Tag>}
                {!c.coCamKet && <Tag color="orange">Thiếu cam kết</Tag>}
                {c.cccd && c.coCamKet && <Tag color="green">Đủ</Tag>}
              </>
            ),
          },
          {
            title: 'Trạng thái', dataIndex: 'trangThai',
            render: (v: string) => <Tag color={NHAN_TRANG_THAI_CTV[v].color}>{NHAN_TRANG_THAI_CTV[v].text}</Tag>,
          },
        ]}
      />
      <Drawer title="Thêm cộng tác viên" open={moTao} onClose={() => setMoTao(false)} width={640} destroyOnClose
              extra={<Button type="primary" loading={tao.isPending} onClick={() => form.submit()}>Lưu (chờ duyệt)</Button>}>
        <Typography.Paragraph type="secondary">
          Hồ sơ mới ở trạng thái <b>Chờ duyệt</b>. Sau khi lưu, tải lên bản cam kết đã ký; quản trị viên (khác người nhập) sẽ duyệt và đặt hạng.
        </Typography.Paragraph>
        <CtvForm form={form} onFinish={(v) => tao.mutate(v)} />
      </Drawer>
      <CtvDrawer ctvId={chon} onClose={() => setChon(undefined)} />
    </>
  )
}
