import { Col, DatePicker, Form, Input, Row, Select } from 'antd'
import type { FormInstance } from 'antd'
import type { CtvFormValues } from './ctvBody'
import { NHAN_LOAI_CTV } from '../../utils/format'

/** Ho so CTV - le tan nhap mot lan. CCCD bat buoc (doi soat khi chi tien). */
export default function CtvForm({ form, onFinish }: { form: FormInstance<CtvFormValues>; onFinish: (v: CtvFormValues) => void }) {
  return (
    <Form form={form} layout="vertical" onFinish={onFinish} initialValues={{ loai: 'DOI_TAC' }}>
      <Row gutter={12}>
        <Col xs={24} md={14}>
          <Form.Item name="hoTen" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
        </Col>
        <Col xs={24} md={10}>
          <Form.Item name="loai" label="Loại CTV">
            <Select options={Object.entries(NHAN_LOAI_CTV).map(([k, v]) => ({ value: k, label: v }))} />
          </Form.Item>
        </Col>
        <Col xs={12} md={8}>
          <Form.Item name="soDienThoai" label="Số điện thoại" rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input maxLength={10} />
          </Form.Item>
        </Col>
        <Col xs={12} md={8}>
          <Form.Item name="zalo" label="Zalo (nếu khác SĐT)" rules={[{ pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input maxLength={10} />
          </Form.Item>
        </Col>
        <Col xs={24} md={8}>
          <Form.Item name="cccd" label="Số CCCD" rules={[{ required: true, pattern: /^[0-9]{12}$/, message: 'CCCD gồm 12 chữ số' }]}
                     tooltip="Bắt buộc để đối soát khi chi trả hoa hồng">
            <Input maxLength={12} />
          </Form.Item>
        </Col>
        <Col xs={24} md={16}><Form.Item name="diaChi" label="Địa chỉ"><Input /></Form.Item></Col>
        <Col xs={24} md={8}><Form.Item name="diaBan" label="Địa bàn hoạt động"><Input placeholder="Tam Nông" /></Form.Item></Col>
        <Col xs={24} md={8}><Form.Item name="nganHang" label="Ngân hàng"><Input placeholder="Vietcombank" /></Form.Item></Col>
        <Col xs={12} md={8}>
          <Form.Item name="soTaiKhoan" label="Số tài khoản" rules={[{ pattern: /^[0-9]{0,30}$/, message: 'Chỉ gồm chữ số' }]}>
            <Input />
          </Form.Item>
        </Col>
        <Col xs={12} md={8}><Form.Item name="chuTaiKhoan" label="Chủ tài khoản"><Input style={{ textTransform: 'uppercase' }} /></Form.Item></Col>
        <Col xs={24} md={8}>
          <Form.Item name="ngayBatDau" label="Ngày bắt đầu"><DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} /></Form.Item>
        </Col>
        <Col xs={24} md={16}><Form.Item name="ghiChu" label="Ghi chú"><Input /></Form.Item></Col>
      </Row>
    </Form>
  )
}
