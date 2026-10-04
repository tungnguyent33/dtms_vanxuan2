import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Empty, Row, Skeleton, Steps, Tag, Typography } from 'antd'
import {
  CalendarOutlined, CarOutlined, FileSearchOutlined, FormOutlined, IdcardOutlined, ReadOutlined,
  SafetyCertificateOutlined, TeamOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { api } from '../../api/client'
import type { HangCongKhai, KhoaCongKhai } from '../../api/types'
import { ngay, tien } from '../../utils/format'

const MAU_HANG: Record<string, string> = { A1: 'blue', A: 'volcano' }

/** Trang chu cua tac nhan Khach: khoa dang tuyen, thong tin hang, quy trinh hoc (FR-07). */
export default function TrangChuKhachPage() {
  const navigate = useNavigate()
  const khoa = useQuery({
    queryKey: ['khoa-cong-khai'],
    queryFn: async () => (await api.get<KhoaCongKhai[]>('/public/khoa-dang-tuyen')).data,
  })
  const hang = useQuery({
    queryKey: ['hang-cong-khai'],
    queryFn: async () => (await api.get<HangCongKhai[]>('/public/hang-gplx')).data,
    staleTime: 600_000,
  })

  return (
    <>
      <section className="pub-hero">
        <div className="pub-container">
          <Typography.Title level={1} className="pub-hero-title">Học lái xe mô tô hạng A1, A</Typography.Title>
          <Typography.Paragraph className="pub-hero-sub">
            Khóa học ngắn ngày, lịch học linh hoạt, được chọn tự học hoặc học tập trung phần lý thuyết.
            Đăng ký trực tuyến, trung tâm gọi lại xác nhận.
          </Typography.Paragraph>
          <div className="pub-hero-actions">
            <Button size="large" type="primary" icon={<FormOutlined />} onClick={() => navigate('/dang-ky')}>
              Đăng ký ngay
            </Button>
            <Button size="large" ghost icon={<FileSearchOutlined />} onClick={() => navigate('/tra-cuu')}>
              Tra cứu hồ sơ
            </Button>
          </div>
        </div>
      </section>

      <section className="pub-section pub-container" id="khoa-hoc">
        <Typography.Title level={3}>Khóa đang tuyển sinh</Typography.Title>
        {khoa.isLoading ? (
          <Skeleton active />
        ) : !khoa.data?.length ? (
          <Card><Empty description="Hiện chưa có khóa mở tuyển. Bạn vẫn có thể để lại đăng ký khi có khóa mới." /></Card>
        ) : (
          <Row gutter={[16, 16]}>
            {khoa.data.map((k) => <KhoaCard key={k.id} k={k} onDangKy={() => navigate(`/dang-ky?khoaId=${k.id}`)} />)}
          </Row>
        )}
      </section>

      <section className="pub-section pub-container">
        <Typography.Title level={3}>Hạng đào tạo</Typography.Title>
        {hang.isLoading ? <Skeleton active /> : (
          <Row gutter={[16, 16]}>
            {hang.data?.map((h) => (
              <Col xs={24} md={12} key={h.ma}>
                <Card title={<><Tag color={MAU_HANG[h.ma] ?? 'default'}>Hạng {h.ma}</Tag>{h.ten}</>} style={{ height: '100%' }}>
                  <Row gutter={[8, 12]}>
                    <Col span={12}><Thong icon={<ReadOutlined />} nhan="Lý thuyết" giaTri={`${h.gioLyThuyet} giờ`} /></Col>
                    <Col span={12}><Thong icon={<CarOutlined />} nhan="Thực hành" giaTri={`${h.gioThucHanh} giờ`} /></Col>
                    <Col span={12}><Thong icon={<IdcardOutlined />} nhan="Tuổi tối thiểu" giaTri={`${h.tuoiToiThieu} tuổi`} /></Col>
                    <Col span={12}><Thong icon={<CalendarOutlined />} nhan="Thời gian khóa" giaTri={`≤ ${h.soNgayKhoaToiDa} ngày`} /></Col>
                  </Row>
                </Card>
              </Col>
            ))}
          </Row>
        )}
        <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
          Lý thuyết: được chọn tự học hoặc học tập trung tại trung tâm. Thực hành: bắt buộc học tập trung tại sân tập.
        </Typography.Paragraph>
      </section>

      <section className="pub-section pub-container">
        <Typography.Title level={3}>Quy trình học</Typography.Title>
        <Card>
          <Steps
            direction="vertical"
            responsive
            current={-1}
            items={[
              { icon: <FormOutlined />, title: 'Đăng ký trực tuyến', description: 'Chọn khóa, điền thông tin cá nhân và nhận mã hồ sơ.' },
              { icon: <TeamOutlined />, title: 'Xác nhận và nộp hồ sơ', description: 'Trung tâm gọi điện xác nhận. Bạn mang CCCD đến nộp hồ sơ và học phí.' },
              { icon: <ReadOutlined />, title: 'Học lý thuyết và thực hành', description: 'Theo lịch của khóa. Tiến độ giờ học được ghi nhận sau mỗi buổi.' },
              { icon: <SafetyCertificateOutlined />, title: 'Hoàn thành khóa và sát hạch', description: 'Học đủ giờ quy định thì được cấp giấy xác nhận hoàn thành và dự sát hạch.' },
            ]}
          />
        </Card>
      </section>

      <section className="pub-section pub-container">
        <Card className="pub-cta">
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} md={16}>
              <Typography.Title level={4} style={{ margin: 0 }}>Đã đăng ký? Kiểm tra tình trạng hồ sơ</Typography.Title>
              <Typography.Text type="secondary">Nhập mã hồ sơ và số CCCD để biết hồ sơ đang ở bước nào.</Typography.Text>
            </Col>
            <Col xs={24} md={8} style={{ textAlign: 'right' }}>
              <Button size="large" icon={<FileSearchOutlined />} onClick={() => navigate('/tra-cuu')}>Tra cứu hồ sơ</Button>
            </Col>
          </Row>
        </Card>
      </section>
    </>
  )
}

function KhoaCard({ k, onDangKy }: { k: KhoaCongKhai; onDangKy: () => void }) {
  const soNgay = dayjs(k.ngayBeGiang).diff(dayjs(k.ngayKhaiGiang), 'day') + 1
  const hetCho = k.conCho === 0
  return (
    <Col xs={24} sm={12} lg={8}>
      <Card style={{ height: '100%' }}
            title={<><Tag color={MAU_HANG[k.hang] ?? 'default'}>Hạng {k.hang}</Tag>{k.maKhoa}</>}
            actions={[
              <Button key="dk" type="primary" disabled={hetCho} onClick={onDangKy} style={{ width: 'calc(100% - 32px)' }}>
                {hetCho ? 'Đã đủ chỗ' : 'Đăng ký khóa này'}
              </Button>,
            ]}>
        <div className="pub-khoa-gia">{tien(k.hocPhi)}</div>
        <div><CalendarOutlined /> Khai giảng <b>{ngay(k.ngayKhaiGiang)}</b></div>
        <div style={{ color: '#888' }}>Bế giảng {ngay(k.ngayBeGiang)} · {soNgay} ngày</div>
        <div style={{ marginTop: 12 }}>
          {hetCho ? <Tag color="red">Hết chỗ</Tag>
            : k.conCho <= 5 ? <Tag color="orange">Chỉ còn {k.conCho} chỗ</Tag>
            : <Tag color="green">Còn {k.conCho} chỗ</Tag>}
        </div>
      </Card>
    </Col>
  )
}

function Thong({ icon, nhan, giaTri }: { icon: ReactNode; nhan: string; giaTri: string }) {
  return (
    <div>
      <div style={{ color: '#888', fontSize: 12 }}>{icon} {nhan}</div>
      <div style={{ fontWeight: 600 }}>{giaTri}</div>
    </div>
  )
}
