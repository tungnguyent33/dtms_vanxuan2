import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Col, DatePicker, Empty, Form, Input, Modal, Radio, Row, Select, Table, Tag, TimePicker } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { api, loiApi } from '../api/client'
import { useGiaoVien, useKhoa, useXe } from '../api/hooks'
import type { BuoiHoc } from '../api/types'
import { gio, ngay } from '../utils/format'

export default function LichHocPage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const { data: khoa } = useKhoa()
  const { data: gv } = useGiaoVien()
  const { data: xe } = useXe()
  const [khoaId, setKhoaId] = useState<number>()
  const [moTao, setMoTao] = useState(false)
  const [form] = Form.useForm()
  const loai = Form.useWatch('loai', form)
  const khoaChon = khoa?.find((k) => k.id === khoaId)

  const { data, isFetching } = useQuery({
    queryKey: ['buoi-hoc', khoaId],
    enabled: !!khoaId,
    queryFn: async () => (await api.get<BuoiHoc[]>('/buoi-hoc', { params: { khoaId } })).data,
  })

  const tao = useMutation({
    mutationFn: async (v: { loai: string; ngay: Dayjs; gio: [Dayjs, Dayjs]; diaDiem: string; giaoVienId: number; xeId?: number }) =>
      api.post('/buoi-hoc', {
        khoaId, loai: v.loai, ngay: v.ngay.format('YYYY-MM-DD'),
        gioBatDau: v.gio[0].format('HH:mm:ss'), gioKetThuc: v.gio[1].format('HH:mm:ss'),
        diaDiem: v.diaDiem, giaoVienId: v.giaoVienId, xeId: v.loai === 'THUC_HANH' ? v.xeId : undefined,
      }),
    onSuccess: () => {
      message.success('Đã thêm buổi học')
      setMoTao(false)
      form.resetFields()
      qc.invalidateQueries({ queryKey: ['buoi-hoc', khoaId] })
    },
    onError: (e) => message.error(loiApi(e)),
  })

  return (
    <>
      <h1 className="page-title">Lịch học</h1>
      <div className="toolbar">
        <Select placeholder="Chọn khóa" style={{ width: 260 }} value={khoaId} onChange={setKhoaId}
                options={khoa?.filter((k) => k.trangThai !== 'HUY').map((k) => ({ value: k.id, label: `${k.maKhoa} – hạng ${k.hangMa}` }))} />
        <Button type="primary" icon={<PlusOutlined />} disabled={!khoaId} style={{ marginLeft: 'auto' }} onClick={() => setMoTao(true)}>
          Thêm buổi học
        </Button>
      </div>
      {!khoaId ? (
        <Empty description="Chọn một khóa để xem lịch" />
      ) : (
        <Table<BuoiHoc>
          rowKey="id" size="middle" loading={isFetching} dataSource={data} pagination={false} scroll={{ x: 700 }}
          columns={[
            { title: 'Ngày', dataIndex: 'ngay', render: (v) => ngay(v) },
            { title: 'Giờ', render: (_, b) => `${gio(b.gioBatDau)} – ${gio(b.gioKetThuc)} (${b.thoiLuongGio}h)` },
            { title: 'Loại', dataIndex: 'loai', render: (v) => (v === 'LY_THUYET' ? <Tag color="blue">Lý thuyết</Tag> : <Tag color="purple">Thực hành</Tag>) },
            { title: 'Giáo viên', dataIndex: 'giaoVien' },
            { title: 'Xe', dataIndex: 'bienSoXe' },
            { title: 'Địa điểm', dataIndex: 'diaDiem' },
            {
              title: 'Trạng thái', dataIndex: 'trangThai',
              render: (v) => (v === 'DA_DAY' ? <Tag color="green">Đã dạy</Tag> : v === 'HUY' ? <Tag>Hủy</Tag> : <Tag color="orange">Kế hoạch</Tag>),
            },
          ]}
        />
      )}

      <Modal title="Thêm buổi học" open={moTao} onCancel={() => setMoTao(false)} onOk={() => form.submit()}
             confirmLoading={tao.isPending} okText="Thêm" destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => tao.mutate(v)}
              initialValues={{ loai: 'THUC_HANH', diaDiem: 'Sân tập Vạn Xuân' }}>
          <Form.Item name="loai" label="Loại buổi">
            <Radio.Group options={[{ value: 'THUC_HANH', label: 'Thực hành' }, { value: 'LY_THUYET', label: 'Lý thuyết' }]} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="ngay" label="Ngày" rules={[{ required: true, message: 'Chọn ngày' }]}>
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }}
                            disabledDate={(d) => !!khoaChon && (d.isBefore(dayjs(khoaChon.ngayKhaiGiang), 'day') || d.isAfter(dayjs(khoaChon.ngayBeGiang), 'day'))} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="gio" label="Giờ học" rules={[{ required: true, message: 'Chọn giờ' }]}>
                <TimePicker.RangePicker format="HH:mm" minuteStep={15} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="giaoVienId" label="Giáo viên" rules={[{ required: true, message: 'Chọn giáo viên' }]}>
            <Select options={gv?.map((g) => ({ value: g.id, label: g.hoTen }))} />
          </Form.Item>
          {loai === 'THUC_HANH' && (
            <Form.Item name="xeId" label="Xe tập lái">
              <Select allowClear
                      options={xe?.filter((x) => x.trangThai === 'SAN_SANG' && (!khoaChon || x.hang === khoaChon.hangMa))
                        .map((x) => ({ value: x.id, label: `${x.bienSo} – ${x.nhanHieu ?? ''}` }))} />
            </Form.Item>
          )}
          <Form.Item name="diaDiem" label="Địa điểm" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
