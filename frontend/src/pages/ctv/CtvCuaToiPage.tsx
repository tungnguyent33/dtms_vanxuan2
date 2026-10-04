import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Col, Form, Input, Result, Row, Select, Statistic, Table, Tag, Typography } from 'antd'
import { SendOutlined, UserAddOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import type { CtvCuaToi, HoaHong, Lead } from '../../api/types'
import { NHAN_HANG_CTV, NHAN_TRANG_THAI_CTV, NHAN_TRANG_THAI_HOA_HONG, NHAN_TRANG_THAI_LEAD, ngay, ngayGio, tien } from '../../utils/format'

/**
 * Cong CTV - chi du lieu cua chinh minh.
 * - Tai khoan CTV: gui lead, xem lead va hoa hong cua minh.
 * - Hoc vien: gui SDT nguoi quen; xin lam CTV (tao yeu cau cho duyet); neu da la CTV hoat dong thi nhu tren.
 */
export default function CtvCuaToiPage() {
  const { message } = App.useApp()
  const { coQuyen } = useAuth()
  const laHocVien = coQuyen('HOC_VIEN')
  const qc = useQueryClient()
  const [formLead] = Form.useForm()
  const [formXin] = Form.useForm()
  const [dangGui, setDangGui] = useState(false)

  const toi = useQuery({ queryKey: ['ctv-cua-toi'], queryFn: async () => (await api.get<CtvCuaToi | ''>('/ctv-cua-toi')).data || null })
  const lead = useQuery({ queryKey: ['ctv-cua-toi', 'lead'], queryFn: async () => (await api.get<Lead[]>('/ctv-cua-toi/lead')).data })
  const laCtv = toi.data?.trangThai === 'HOAT_DONG'
  const hoaHong = useQuery({
    queryKey: ['ctv-cua-toi', 'hoa-hong'],
    enabled: !!toi.data,
    queryFn: async () => (await api.get<HoaHong[]>('/ctv-cua-toi/hoa-hong')).data,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['ctv-cua-toi'] })

  const guiLead = async (v: Record<string, string>) => {
    setDangGui(true)
    try {
      await api.post('/ctv-cua-toi/lead', v)
      message.success('Đã gửi. Trung tâm sẽ liên hệ tư vấn.')
      formLead.resetFields()
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    } finally {
      setDangGui(false)
    }
  }

  const xinLamCtv = async (v: Record<string, string>) => {
    try {
      await api.post('/ctv-cua-toi/xin-lam-ctv', v)
      message.success('Đã gửi yêu cầu. Trung tâm sẽ liên hệ để ký cam kết.')
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  if (!laHocVien && toi.isSuccess && !toi.data) {
    return <Result status="warning" title="Tài khoản chưa gắn hồ sơ cộng tác viên" subTitle="Liên hệ trung tâm để được hỗ trợ." />
  }

  return (
    <>
      <h1 className="page-title">{laHocVien && !laCtv ? 'Giới thiệu bạn bè' : 'Cộng tác viên'}</h1>

      {toi.data && (
        <Card style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 12 }}>
            <Typography.Text strong style={{ fontSize: 16 }}>{toi.data.hoTen}</Typography.Text>{' '}
            <Tag color={NHAN_TRANG_THAI_CTV[toi.data.trangThai].color}>{NHAN_TRANG_THAI_CTV[toi.data.trangThai].text}</Tag>
            {laCtv && <Tag color={NHAN_HANG_CTV[toi.data.hang].color}>Hạng {NHAN_HANG_CTV[toi.data.hang].text}</Tag>}
          </div>
          {toi.data.trangThai === 'CHO_DUYET' && (
            <Alert type="info" showIcon message="Yêu cầu làm CTV đang chờ trung tâm duyệt. Lễ tân sẽ liên hệ để bạn ký bản cam kết." />
          )}
          {laCtv && (
            <Row gutter={[12, 12]}>
              <Col xs={12} md={6}><Statistic title="Đã giới thiệu" value={toi.data.soLead} suffix={`/ ${toi.data.soChot} chốt`} /></Col>
              <Col xs={12} md={6}><Statistic title="Chờ trung tâm duyệt" value={toi.data.choDuyet} formatter={(v) => tien(Number(v))} /></Col>
              <Col xs={12} md={6}><Statistic title="Đã duyệt, chờ chi" value={toi.data.choChi} formatter={(v) => tien(Number(v))} /></Col>
              <Col xs={12} md={6}><Statistic title="Đã nhận" value={toi.data.daNhan} formatter={(v) => tien(Number(v))} valueStyle={{ color: '#389e0d' }} /></Col>
            </Row>
          )}
        </Card>
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          {(laCtv || laHocVien) && (
            <Card title={laCtv ? 'Gửi thông tin người muốn học' : 'Giới thiệu người quen học lái xe'} style={{ marginBottom: 16 }}>
              <Form form={formLead} layout="vertical" onFinish={guiLead}>
                <Form.Item name="hoTen" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
                <Form.Item name="soDienThoai" label="Số điện thoại" rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
                  <Input maxLength={10} />
                </Form.Item>
                <Form.Item name="hangMuonHoc" label="Hạng muốn học">
                  <Select allowClear options={[{ value: 'A1', label: 'Hạng A1' }, { value: 'A', label: 'Hạng A' }]} />
                </Form.Item>
                <Form.Item name="diaChi" label="Địa chỉ"><Input /></Form.Item>
                <Form.Item name="ghiChu" label="Ghi chú"><Input /></Form.Item>
                <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={dangGui} block>Gửi</Button>
              </Form>
              {!laCtv && (
                <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0, fontSize: 12 }}>
                  Muốn nhận hoa hồng khi giới thiệu? Đăng ký làm cộng tác viên bên dưới.
                </Typography.Paragraph>
              )}
            </Card>
          )}

          {laHocVien && toi.isSuccess && !toi.data && (
            <Card title={<><UserAddOutlined /> Đăng ký làm cộng tác viên</>}>
              <Typography.Paragraph type="secondary">
                Thông tin cá nhân lấy từ hồ sơ học viên. Nhập tài khoản nhận hoa hồng (có thể bổ sung sau).
                Trung tâm duyệt và ký cam kết trước khi bạn nhận hoa hồng.
              </Typography.Paragraph>
              <Form form={formXin} layout="vertical" onFinish={xinLamCtv}>
                <Form.Item name="zalo" label="Zalo (nếu khác SĐT)" rules={[{ pattern: /^0[0-9]{9}$/, message: '10 số' }]}><Input maxLength={10} /></Form.Item>
                <Form.Item name="nganHang" label="Ngân hàng"><Input /></Form.Item>
                <Form.Item name="soTaiKhoan" label="Số tài khoản" rules={[{ pattern: /^[0-9]{0,30}$/, message: 'Chỉ gồm chữ số' }]}><Input /></Form.Item>
                <Form.Item name="chuTaiKhoan" label="Chủ tài khoản"><Input style={{ textTransform: 'uppercase' }} /></Form.Item>
                <Button htmlType="submit" block>Gửi yêu cầu</Button>
              </Form>
            </Card>
          )}
        </Col>

        <Col xs={24} lg={14}>
          <Card title="Người tôi đã giới thiệu" style={{ marginBottom: 16 }}>
            <Table<Lead>
              rowKey="id" size="small" loading={lead.isFetching} dataSource={lead.data} pagination={{ pageSize: 8 }} scroll={{ x: 420 }}
              locale={{ emptyText: 'Chưa giới thiệu ai' }}
              columns={[
                { title: 'Họ tên', render: (_, l) => <><b>{l.hoTen}</b><div style={{ fontSize: 12 }}>{l.soDienThoai}</div></> },
                { title: 'Ngày gửi', dataIndex: 'createdAt', render: (v) => ngayGio(v) },
                { title: 'Trạng thái', dataIndex: 'trangThai', render: (v: string) => <Tag color={NHAN_TRANG_THAI_LEAD[v].color}>{NHAN_TRANG_THAI_LEAD[v].text}</Tag> },
              ]}
            />
          </Card>
          {toi.data && (
            <Card title="Hoa hồng của tôi">
              <Table<HoaHong>
                rowKey="id" size="small" loading={hoaHong.isFetching} dataSource={hoaHong.data} pagination={{ pageSize: 8 }} scroll={{ x: 480 }}
                locale={{ emptyText: 'Chưa có hoa hồng' }}
                columns={[
                  { title: 'Học viên', render: (_, h) => <><b>{h.hocVien}</b><div style={{ fontSize: 12 }}>Khóa {h.maKhoa}</div></> },
                  { title: 'Hoa hồng', dataIndex: 'soTien', render: (v) => <b>{tien(v)}</b>, align: 'right' },
                  { title: 'Kỳ', dataIndex: 'ky', render: (v, h) => v ?? (h.ngayDuDieuKien ? ngay(h.ngayDuDieuKien) : '—') },
                  {
                    title: 'Trạng thái', dataIndex: 'trangThai',
                    render: (v: string) => <Tag color={NHAN_TRANG_THAI_HOA_HONG[v].color}>{NHAN_TRANG_THAI_HOA_HONG[v].text}</Tag>,
                  },
                ]}
              />
              <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
                Hoa hồng đủ điều kiện khi học viên đóng học phí theo quy định của trung tâm; chi theo kỳ tháng.
              </Typography.Paragraph>
            </Card>
          )}
        </Col>
      </Row>
    </>
  )
}
