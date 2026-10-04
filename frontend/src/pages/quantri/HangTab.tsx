import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Col, Form, Input, InputNumber, Modal, Row, Switch, Table, Tag } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import { useHangGplx } from '../../api/hooks'
import type { HangGplx } from '../../api/types'
import SoTienInput from '../../components/SoTienInput'
import { tien } from '../../utils/format'

/** FR-02: thong so hang GPLX theo van ban phap luat - sua khi co thong tu moi, khong viet cung trong ma nguon. */
export default function HangTab() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const { data, isFetching } = useHangGplx()
  const [sua, setSua] = useState<HangGplx | null>(null)
  const [form] = Form.useForm()

  const luu = useMutation({
    mutationFn: async (v: Omit<HangGplx, 'ma' | 'ten'>) => api.put(`/hang-gplx/${sua!.ma}`, v),
    onSuccess: () => { message.success('Đã cập nhật'); setSua(null); qc.invalidateQueries({ queryKey: ['hang-gplx'] }) },
    onError: (e) => message.error(loiApi(e)),
  })

  return (
    <>
      <Table<HangGplx>
        rowKey="ma" size="middle" loading={isFetching} dataSource={data} pagination={false} scroll={{ x: 760 }}
        columns={[
          { title: 'Hạng', dataIndex: 'ma', width: 70, render: (v) => <b>{v}</b> },
          { title: 'Giờ LT', dataIndex: 'gioLyThuyet', align: 'right' },
          { title: 'Giờ TH', dataIndex: 'gioThucHanh', align: 'right' },
          { title: 'Tuổi tối thiểu', dataIndex: 'tuoiToiThieu', align: 'right' },
          { title: 'Số ngày khóa tối đa', dataIndex: 'soNgayKhoaToiDa', align: 'right' },
          { title: 'Học phí mặc định', dataIndex: 'hocPhiMacDinh', render: (v) => tien(v), align: 'right' },
          { title: 'Căn cứ', dataIndex: 'canCuPhapLy', className: 'hide-mobile' },
          { title: '', dataIndex: 'dangApDung', render: (v) => (v ? <Tag color="green">Áp dụng</Tag> : <Tag>Ngừng</Tag>) },
          {
            title: '', key: 'act', width: 60,
            render: (_, h) => <Button size="small" icon={<EditOutlined />} onClick={() => { form.setFieldsValue(h); setSua(h) }} />,
          },
        ]}
      />
      <Modal title={`Thông số hạng ${sua?.ma ?? ''}`} open={!!sua} onCancel={() => setSua(null)} onOk={() => form.submit()}
             confirmLoading={luu.isPending} destroyOnClose width={560}>
        <Alert type="info" showIcon style={{ marginBottom: 12 }}
               message="Chỉ sửa khi có văn bản pháp luật mới. Thay đổi áp dụng cho việc tính tiến độ và xét hoàn thành từ nay về sau; giá trị cũ được lưu trong nhật ký." />
        <Form form={form} layout="vertical" onFinish={(v) => luu.mutate(v)}>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="gioLyThuyet" label="Giờ lý thuyết" rules={[{ required: true }]}><InputNumber min={0} step={0.5} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={12}><Form.Item name="gioThucHanh" label="Giờ thực hành" rules={[{ required: true }]}><InputNumber min={0} step={0.5} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={12}><Form.Item name="tuoiToiThieu" label="Tuổi tối thiểu" rules={[{ required: true }]}><InputNumber min={16} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={12}><Form.Item name="soNgayKhoaToiDa" label="Số ngày khóa tối đa" rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={24}><Form.Item name="hocPhiMacDinh" label="Học phí mặc định" rules={[{ required: true }]}><SoTienInput min={0} /></Form.Item></Col>
            <Col span={24}><Form.Item name="canCuPhapLy" label="Căn cứ pháp lý"><Input /></Form.Item></Col>
            <Col span={24}><Form.Item name="dangApDung" label="Đang áp dụng" valuePropName="checked"><Switch /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </>
  )
}
