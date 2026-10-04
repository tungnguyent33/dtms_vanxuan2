import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App, Button, Col, DatePicker, Form, Input, InputNumber, Modal, Row, Select, Table, Tag, Tooltip } from 'antd'
import { EditOutlined, PlusOutlined, WarningOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../../api/client'
import { useHangGplx, useXe } from '../../api/hooks'
import type { Xe } from '../../api/types'
import { NHAN_TRANG_THAI_XE, ngay } from '../../utils/format'

interface FormValues {
  bienSo: string
  hangMa: string
  nhanHieu?: string
  namSanXuat?: number
  trangThai: Xe['trangThai']
  ngayBaoDuongTiep?: Dayjs
}

/** FR-03: danh muc xe tap lai. Khong xoa: chuyen trang thai "Ngung dung". */
export default function XeTab() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const { data, isFetching } = useXe()
  const { data: hang } = useHangGplx()
  const [sua, setSua] = useState<Xe | 'moi' | null>(null)
  const [form] = Form.useForm<FormValues>()

  const luu = useMutation({
    mutationFn: async (v: FormValues) => {
      const body = { ...v, ngayBaoDuongTiep: v.ngayBaoDuongTiep?.format('YYYY-MM-DD') }
      return sua === 'moi' ? api.post('/xe-tap-lai', body) : api.put(`/xe-tap-lai/${(sua as Xe).id}`, body)
    },
    onSuccess: () => { message.success('Đã lưu'); setSua(null); qc.invalidateQueries({ queryKey: ['xe'] }) },
    onError: (e) => message.error(loiApi(e)),
  })

  const moSua = (x: Xe | 'moi') => {
    form.resetFields()
    if (x !== 'moi') {
      form.setFieldsValue({
        bienSo: x.bienSo, hangMa: x.hang, nhanHieu: x.nhanHieu, namSanXuat: x.namSanXuat, trangThai: x.trangThai,
        ngayBaoDuongTiep: x.ngayBaoDuongTiep ? dayjs(x.ngayBaoDuongTiep) : undefined,
      })
    } else {
      form.setFieldsValue({ trangThai: 'SAN_SANG' })
    }
    setSua(x)
  }

  return (
    <>
      <div className="toolbar">
        <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }} onClick={() => moSua('moi')}>Thêm xe</Button>
      </div>
      <Table<Xe>
        rowKey="id" size="middle" loading={isFetching} dataSource={data} pagination={false} scroll={{ x: 700 }}
        columns={[
          { title: 'Biển số', dataIndex: 'bienSo', render: (v) => <b>{v}</b> },
          { title: 'Hạng', dataIndex: 'hang', width: 70 },
          { title: 'Nhãn hiệu', render: (_, x) => [x.nhanHieu, x.namSanXuat].filter(Boolean).join(' · ') },
          {
            title: 'Bảo dưỡng tiếp', dataIndex: 'ngayBaoDuongTiep',
            render: (v?: string) => {
              if (!v) return '—'
              const sap = dayjs(v).diff(dayjs(), 'day') <= 7
              return sap ? <Tooltip title="Sắp đến hạn hoặc quá hạn bảo dưỡng"><span style={{ color: '#d46b08' }}><WarningOutlined /> {ngay(v)}</span></Tooltip> : ngay(v)
            },
          },
          { title: 'Trạng thái', dataIndex: 'trangThai', render: (v: string) => <Tag color={NHAN_TRANG_THAI_XE[v]?.color}>{NHAN_TRANG_THAI_XE[v]?.text}</Tag> },
          { title: '', key: 'act', width: 60, render: (_, x) => <Button size="small" icon={<EditOutlined />} onClick={() => moSua(x)} /> },
        ]}
      />
      <Modal title={sua === 'moi' ? 'Thêm xe tập lái' : 'Sửa xe tập lái'} open={!!sua} onCancel={() => setSua(null)}
             onOk={() => form.submit()} confirmLoading={luu.isPending} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => luu.mutate(v)}>
          <Row gutter={12}>
            <Col span={14}>
              <Form.Item name="bienSo" label="Biển số" rules={[{ required: true, message: 'Nhập biển số' }]}>
                <Input placeholder="19-TL 000.03" style={{ textTransform: 'uppercase' }} />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="hangMa" label="Hạng" rules={[{ required: true, message: 'Chọn hạng' }]}>
                <Select options={hang?.map((h) => ({ value: h.ma, label: `Hạng ${h.ma}` }))} />
              </Form.Item>
            </Col>
            <Col span={14}><Form.Item name="nhanHieu" label="Nhãn hiệu"><Input placeholder="Honda Wave" /></Form.Item></Col>
            <Col span={10}>
              <Form.Item name="namSanXuat" label="Năm sản xuất">
                <InputNumber min={1990} max={dayjs().year() + 1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="trangThai" label="Trạng thái">
                <Select options={Object.entries(NHAN_TRANG_THAI_XE).map(([k, v]) => ({ value: k, label: v.text }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="ngayBaoDuongTiep" label="Ngày bảo dưỡng tiếp">
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  )
}
