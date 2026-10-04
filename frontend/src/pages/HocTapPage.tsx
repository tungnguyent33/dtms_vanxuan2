import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { App, Button, Card, Col, List, Row, Table, Tag, Typography, Descriptions, Spin } from 'antd'
import { FilePdfOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { api, loiApi, taiTep } from '../api/client'
import type { BuoiHoc, DangKy, TienDo, CongNo, PhieuThu } from '../api/types'
import { gio, ngay, ngayGio, tien, NHAN_TRANG_THAI_DK } from '../utils/format'

function HoSoCard({ dk }: { dk: DangKy }) {
  const { message } = App.useApp()
  const phieu = useQuery({
    queryKey: ['phieu-thu', dk.id],
    queryFn: async () => (await api.get<PhieuThu[]>('/phieu-thu', { params: { dangKyId: dk.id } })).data,
  })
  const tienDo = useQuery({
    queryKey: ['tien-do', dk.id],
    queryFn: async () => (await api.get<TienDo>(`/dang-ky/${dk.id}/tien-do`)).data,
  })

  const congNo = useQuery({
    queryKey: ['cong-no', dk.id],
    queryFn: async () => (await api.get<CongNo>(`/phieu-thu/cong-no/${dk.id}`)).data,
  })

  return (
    <Card 
      title={`Hồ sơ: ${dk.maHoSo} - Hạng ${dk.hang}`} 
      extra={<Tag color={NHAN_TRANG_THAI_DK[dk.trangThai]?.color}>{NHAN_TRANG_THAI_DK[dk.trangThai]?.text}</Tag>}
      style={{ marginBottom: 16 }}
    >
      <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
        <Descriptions.Item label="Khóa học">{dk.maKhoa}</Descriptions.Item>
        <Descriptions.Item label="Lý thuyết">{dk.hinhThucLyThuyet === 'TU_HOC' ? 'Tự học' : 'Học tập trung'}</Descriptions.Item>
        <Descriptions.Item label="Học phí">{tien(dk.hocPhi - dk.giamTru)}</Descriptions.Item>
        <Descriptions.Item label="Đã đóng">{congNo.data ? tien(congNo.data.daDong) : <Spin size="small" />}</Descriptions.Item>
        <Descriptions.Item label="Còn nợ">{congNo.data ? tien(congNo.data.conNo) : <Spin size="small" />}</Descriptions.Item>
      </Descriptions>
      
      {tienDo.data && (
        <div style={{ marginTop: 16 }}>
          <Typography.Title level={5}>Tiến độ học tập</Typography.Title>
          <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
            <Descriptions.Item label="Giờ thực hành">{tienDo.data.gioThucHanhDaHoc} / {tienDo.data.gioThucHanhQuyDinh} giờ</Descriptions.Item>
            <Descriptions.Item label="Thực hành">{tienDo.data.duThucHanh ? <Tag color="green">Đủ giờ</Tag> : <Tag color="orange">Chưa đủ giờ</Tag>}</Descriptions.Item>
            {tienDo.data.xetLyThuyet ? (
              <>
                <Descriptions.Item label="Giờ lý thuyết">{tienDo.data.gioLyThuyetDaHoc} / {tienDo.data.gioLyThuyetQuyDinh} giờ</Descriptions.Item>
                <Descriptions.Item label="Lý thuyết">{tienDo.data.duLyThuyet ? <Tag color="green">Đủ giờ</Tag> : <Tag color="orange">Chưa đủ giờ</Tag>}</Descriptions.Item>
              </>
            ) : (
              <Descriptions.Item label="Lý thuyết" span={2}>Tự học – không tính giờ lý thuyết tại trung tâm</Descriptions.Item>
            )}
          </Descriptions>
        </div>
      )}

      {!!phieu.data?.length && (
        <div style={{ marginTop: 16 }}>
          <Typography.Title level={5}>Phiếu thu học phí</Typography.Title>
          <Table<PhieuThu>
            rowKey="id" size="small" pagination={false} dataSource={phieu.data} scroll={{ x: 420 }}
            columns={[
              { title: 'Số phiếu', dataIndex: 'soPhieu' },
              { title: 'Ngày', dataIndex: 'ngayThu', render: (v) => ngayGio(v) },
              { title: 'Số tiền', dataIndex: 'soTien', render: (v) => tien(v), align: 'right' },
              {
                title: '', key: 'act', width: 90,
                render: (_, r) => (r.trangThai === 'DA_HUY'
                  ? <Tag color="red">Đã hủy</Tag>
                  : <Button size="small" icon={<FilePdfOutlined />}
                            onClick={() => taiTep(`/phieu-thu/${r.id}/pdf`, undefined, true).catch((e) => message.error(loiApi(e)))}>PDF</Button>),
              },
            ]}
          />
        </div>
      )}
    </Card>
  )
}

export default function HocTapPage() {
  const homNay = dayjs().format('YYYY-MM-DD')
  const thangToi = dayjs().add(30, 'day').format('YYYY-MM-DD')

  const hoso = useQuery({
    queryKey: ['ho-so-cua-toi'],
    queryFn: async () => (await api.get<DangKy[]>('/dang-ky/cua-toi')).data,
  })

  const lich = useQuery({
    queryKey: ['lich-cua-toi', homNay, thangToi],
    queryFn: async () => (await api.get<BuoiHoc[]>('/buoi-hoc/cua-toi', { params: { tu: homNay, den: thangToi } })).data,
  })

  return (
    <>
      <h1 className="page-title">Quá trình học tập</h1>
      <Card size="small" style={{ marginBottom: 16 }}>
        Biết người muốn học lái xe? <Link to="/ctv-cua-toi">Giới thiệu bạn bè</Link> hoặc đăng ký làm cộng tác viên để nhận hoa hồng.
      </Card>
      <Row gutter={[24, 24]}>
        <Col xs={24} md={14}>
          <Typography.Title level={4}>Hồ sơ đăng ký</Typography.Title>
          {hoso.isLoading ? <Spin /> : hoso.data?.length ? (
            hoso.data.map(dk => <HoSoCard key={dk.id} dk={dk} />)
          ) : (
            <Typography.Text type="secondary">Bạn chưa có hồ sơ đăng ký nào.</Typography.Text>
          )}
        </Col>
        
        <Col xs={24} md={10}>
          <Card title="Lịch học sắp tới (30 ngày)" loading={lich.isFetching}>
            <List
              dataSource={lich.data || []}
              locale={{ emptyText: 'Chưa có lịch học sắp tới' }}
              renderItem={(b) => (
                <List.Item>
                  <List.Item.Meta
                    title={`${ngay(b.ngay)} | ${gio(b.gioBatDau)} - ${gio(b.gioKetThuc)}`}
                    description={
                      <>
                        Môn: <b>{b.loai === 'LY_THUYET' ? 'Lý thuyết' : 'Thực hành'}</b><br/>
                        Địa điểm: {b.diaDiem}{b.bienSoXe ? ` (Xe ${b.bienSoXe})` : ''}<br/>
                        Giáo viên: {b.giaoVien}
                      </>
                    }
                  />
                  {b.trangThai === 'DA_DAY' ? <Tag color="green">Đã học</Tag> : <Tag color="blue">Sắp tới</Tag>}
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}
