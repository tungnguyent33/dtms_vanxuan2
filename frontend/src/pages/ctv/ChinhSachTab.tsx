import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Col, DatePicker, Form, InputNumber, Modal, Radio, Row, Select, Table, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../../api/client'
import { useHangGplx } from '../../api/hooks'
import type { ChinhSach, HangCtv } from '../../api/types'
import { NHAN_HANG_CTV, mucHoaHong, ngay } from '../../utils/format'

interface ChinhSachForm {
  hangCtv: HangCtv
  hangGplx: string
  kieu: 'PHAN_TRAM' | 'CO_DINH'
  giaTri: number
  hieuLucTu: Dayjs
}

/** Bang chinh sach hoa hong theo hang CTV x hang bang, co ngay hieu luc; dieu kien phat sinh va ngay chi. */
export default function ChinhSachTab() {
  const { message, modal } = App.useApp()
  const qc = useQueryClient()
  const { data: hang } = useHangGplx()
  const [moThem, setMoThem] = useState(false)
  const [form] = Form.useForm<ChinhSachForm>()
  const [formCh] = Form.useForm<{ phanTramDongToiThieu: number; ngayChi: number }>()
  const kieu = Form.useWatch('kieu', form)

  const cs = useQuery({ queryKey: ['chinh-sach'], queryFn: async () => (await api.get<ChinhSach[]>('/hoa-hong/chinh-sach')).data })
  useQuery({
    queryKey: ['hoa-hong-cau-hinh'],
    queryFn: async () => {
      const d = (await api.get<{ phanTramDongToiThieu: number; ngayChi: number }>('/hoa-hong/cau-hinh')).data
      formCh.setFieldsValue(d)
      return d
    },
  })

  const them = async (v: ChinhSachForm) => {
    try {
      await api.post('/hoa-hong/chinh-sach', { ...v, hieuLucTu: v.hieuLucTu.format('YYYY-MM-DD') })
      message.success('Đã thêm mức hoa hồng')
      setMoThem(false)
      qc.invalidateQueries({ queryKey: ['chinh-sach'] })
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  return (
    <>
      <Card size="small" title="Điều kiện phát sinh & ngày chi" style={{ marginBottom: 16 }}>
        <Form form={formCh} layout="inline" onFinish={async (v) => {
          try {
            await api.put('/hoa-hong/cau-hinh', v)
            message.success('Đã lưu cấu hình')
          } catch (e) {
            message.error(loiApi(e))
          }
        }}>
          <Form.Item name="phanTramDongToiThieu" label="Phát sinh khi học viên đã đóng" rules={[{ required: true }]}>
            <InputNumber min={1} max={100} addonAfter="% học phí" style={{ width: 170 }} />
          </Form.Item>
          <Form.Item name="ngayChi" label="Chi vào ngày" rules={[{ required: true }]}>
            <InputNumber min={1} max={28} addonAfter="hằng tháng" style={{ width: 170 }} />
          </Form.Item>
          <Button htmlType="submit" icon={<SaveOutlined />}>Lưu</Button>
        </Form>
      </Card>

      <div className="toolbar">
        <Alert type="info" showIcon style={{ flex: 1 }}
               message="Đổi chính sách = thêm mức mới có ngày hiệu lực. Mỗi hồ sơ lưu mức áp dụng lúc đăng ký, nên mức mới không làm sai hoa hồng cũ." />
        <Button type="primary" icon={<PlusOutlined />}
                onClick={() => { form.setFieldsValue({ hangCtv: 'THUONG', kieu: 'CO_DINH', hieuLucTu: dayjs() }); setMoThem(true) }}>
          Thêm mức
        </Button>
      </div>
      <Table<ChinhSach>
        rowKey="id" size="middle" loading={cs.isFetching} dataSource={cs.data} pagination={false} scroll={{ x: 640 }}
        columns={[
          { title: 'Hạng bằng', dataIndex: 'hangGplx', render: (v) => <b>{v}</b> },
          { title: 'Hạng CTV', dataIndex: 'hangCtv', render: (v: string) => <Tag color={NHAN_HANG_CTV[v].color}>{NHAN_HANG_CTV[v].text}</Tag> },
          { title: 'Mức', render: (_, c) => <b>{mucHoaHong(c.kieu, c.giaTri)}</b> },
          { title: 'Hiệu lực từ', dataIndex: 'hieuLucTu', render: (v) => ngay(v) },
          {
            title: '', render: (_, c) => (c.dangApDung ? <Tag color="green">Đang áp dụng</Tag>
              : dayjs(c.hieuLucTu).isAfter(dayjs(), 'day') ? <Tag color="blue">Sắp áp dụng</Tag> : <Tag>Hết hiệu lực</Tag>),
          },
          { title: 'Đơn đã dùng', dataIndex: 'soDonDaDung', align: 'right' },
          {
            title: '', key: 'xoa', width: 50,
            render: (_, c) => dayjs(c.hieuLucTu).isAfter(dayjs(), 'day') && c.soDonDaDung === 0 && (
              <Button size="small" danger icon={<DeleteOutlined />} onClick={() => modal.confirm({
                title: 'Xóa mức hoa hồng chưa hiệu lực?',
                onOk: async () => {
                  try {
                    await api.delete(`/hoa-hong/chinh-sach/${c.id}`)
                    qc.invalidateQueries({ queryKey: ['chinh-sach'] })
                  } catch (e) {
                    message.error(loiApi(e))
                  }
                },
              })} />
            ),
          },
        ]}
      />
      <Modal title="Thêm mức hoa hồng" open={moThem} onCancel={() => setMoThem(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={them}>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="hangGplx" label="Hạng bằng" rules={[{ required: true, message: 'Chọn hạng' }]}>
                <Select options={hang?.map((h) => ({ value: h.ma, label: `Hạng ${h.ma}` }))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="hangCtv" label="Hạng CTV">
                <Select options={Object.entries(NHAN_HANG_CTV).map(([k, v]) => ({ value: k, label: v.text }))} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="kieu" label="Kiểu">
                <Radio.Group options={[{ value: 'CO_DINH', label: 'Số tiền cố định' }, { value: 'PHAN_TRAM', label: '% học phí phải đóng' }]} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="giaTri" label={kieu === 'PHAN_TRAM' ? 'Tỷ lệ (%)' : 'Số tiền (đ)'} rules={[{ required: true }]}>
                <InputNumber<number> min={0} max={kieu === 'PHAN_TRAM' ? 100 : undefined} style={{ width: '100%' }}
                             step={kieu === 'PHAN_TRAM' ? 0.5 : 10000}
                             formatter={(v) => (kieu === 'PHAN_TRAM' ? `${v ?? ''}` : `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.'))}
                             parser={(v) => Number((v ?? '').replace(/\./g, ''))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="hieuLucTu" label="Hiệu lực từ" rules={[{ required: true }]}>
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} disabledDate={(d) => d.isBefore(dayjs(), 'day')} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  )
}
