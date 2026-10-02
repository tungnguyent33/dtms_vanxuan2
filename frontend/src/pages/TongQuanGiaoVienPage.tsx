import { useQuery } from '@tanstack/react-query'
import { Card, Col, List, Row, Table, Tag } from 'antd'
import dayjs from 'dayjs'
import { api } from '../api/client'
import type { BuoiHoc, DangKy } from '../api/types'
import { gio, ngay, NHAN_TRANG_THAI_DK } from '../utils/format'

export default function TongQuanGiaoVienPage() {
  const homNay = dayjs().format('YYYY-MM-DD')
  const tuanToi = dayjs().add(7, 'day').format('YYYY-MM-DD')

  const lich = useQuery({
    queryKey: ['lich-gv', homNay, tuanToi],
    queryFn: async () => (await api.get<BuoiHoc[]>('/buoi-hoc/cua-toi', { params: { tu: homNay, den: tuanToi } })).data,
  })

  const hocVien = useQuery({
    queryKey: ['hoc-vien-gv'],
    queryFn: async () => (await api.get<DangKy[]>('/dang-ky/cua-toi')).data,
  })

  return (
    <>
      <h1 className="page-title">Tổng quan giáo viên</h1>
      <Row gutter={[24, 24]}>
        <Col xs={24} md={12}>
          <Card title="Lịch dạy 7 ngày tới" loading={lich.isFetching}>
            <List
              dataSource={lich.data || []}
              locale={{ emptyText: 'Chưa có lịch dạy trong 7 ngày tới' }}
              renderItem={(b) => (
                <List.Item>
                  <List.Item.Meta
                    title={`${ngay(b.ngay)} | ${gio(b.gioBatDau)} - ${gio(b.gioKetThuc)}`}
                    description={
                      <>
                        Khóa: <b>{b.maKhoa}</b><br/>
                        Loại: {b.loai === 'LY_THUYET' ? 'Lý thuyết' : 'Thực hành'}<br/>
                        Địa điểm: {b.diaDiem}{b.bienSoXe ? ` (Xe ${b.bienSoXe})` : ''}
                      </>
                    }
                  />
                  {b.trangThai === 'DA_DAY' ? <Tag color="green">Đã dạy</Tag> : <Tag color="blue">Chưa dạy</Tag>}
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title="Danh sách học viên phụ trách" loading={hocVien.isFetching}>
            <Table
              size="small"
              dataSource={hocVien.data || []}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              locale={{ emptyText: 'Chưa quản lý học viên nào' }}
              columns={[
                { title: 'Học viên', dataIndex: ['hocVien', 'hoTen'], render: (v) => <b>{v}</b> },
                { title: 'Khóa', dataIndex: 'maKhoa' },
                { title: 'Trạng thái', dataIndex: 'trangThai', render: (v) => <Tag color={NHAN_TRANG_THAI_DK[v]?.color}>{NHAN_TRANG_THAI_DK[v]?.text}</Tag> },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}
