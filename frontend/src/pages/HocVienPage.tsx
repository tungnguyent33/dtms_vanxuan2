import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, Input, Select, Space, Table, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { api } from '../api/client'
import { useKhoa } from '../api/hooks'
import type { DangKy, PageResponse, TrangThaiDangKy } from '../api/types'
import { NHAN_HINH_THUC, NHAN_TRANG_THAI_DK, ngay, tien } from '../utils/format'
import TiepNhanDrawer from './hocvien/TiepNhanDrawer'
import ChiTietDrawer from './hocvien/ChiTietDrawer'

export default function HocVienPage() {
  const [q, setQ] = useState('')
  const [khoaId, setKhoaId] = useState<number>()
  const [trangThai, setTrangThai] = useState<TrangThaiDangKy>()
  const [page, setPage] = useState(0)
  const [moTiepNhan, setMoTiepNhan] = useState(false)
  const [chonId, setChonId] = useState<number>()
  const { data: khoa } = useKhoa()

  const { data, isFetching, refetch } = useQuery({
    queryKey: ['dang-ky', q, khoaId, trangThai, page],
    queryFn: async () =>
      (await api.get<PageResponse<DangKy>>('/dang-ky', { params: { q: q || undefined, khoaId, trangThai, page, size: 20 } })).data,
    placeholderData: (prev) => prev,
  })

  const columns: ColumnsType<DangKy> = [
    { title: 'Mã hồ sơ', dataIndex: 'maHoSo', width: 120 },
    {
      title: 'Học viên',
      render: (_, r) => (
        <div>
          <Typography.Text strong>{r.hocVien.hoTen}</Typography.Text>
          <div style={{ fontSize: 12, color: '#888' }}>{r.hocVien.cccd} · {r.hocVien.soDienThoai}</div>
        </div>
      ),
    },
    { title: 'Khóa', dataIndex: 'maKhoa', className: 'hide-mobile' },
    { title: 'Hình thức', dataIndex: 'hinhThucLyThuyet', render: (v) => NHAN_HINH_THUC[v], className: 'hide-mobile' },
    { title: 'Học phí', render: (_, r) => tien(r.hocPhi - r.giamTru), align: 'right', className: 'hide-mobile' },
    { title: 'Ngày ĐK', dataIndex: 'ngayDangKy', render: (v) => ngay(v), className: 'hide-mobile' },
    {
      title: 'Trạng thái',
      dataIndex: 'trangThai',
      render: (v: string) => <Tag color={NHAN_TRANG_THAI_DK[v]?.color}>{NHAN_TRANG_THAI_DK[v]?.text ?? v}</Tag>,
    },
  ]

  return (
    <>
      <h1 className="page-title">Học viên & hồ sơ đăng ký</h1>
      <div className="toolbar">
        <Input.Search
          allowClear
          placeholder="Tìm tên, CCCD, SĐT, mã hồ sơ"
          style={{ width: 280 }}
          onSearch={(v) => { setQ(v.trim()); setPage(0) }}
        />
        <Select
          allowClear
          placeholder="Lọc theo khóa"
          style={{ width: 200 }}
          value={khoaId}
          onChange={(v) => { setKhoaId(v); setPage(0) }}
          options={khoa?.map((k) => ({ value: k.id, label: `${k.maKhoa} (${k.hangMa})` }))}
        />
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 170 }}
          value={trangThai}
          onChange={(v) => { setTrangThai(v); setPage(0) }}
          options={Object.entries(NHAN_TRANG_THAI_DK).map(([k, v]) => ({ value: k, label: v.text }))}
        />
        <Space style={{ marginLeft: 'auto' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setMoTiepNhan(true)}>
            Tiếp nhận hồ sơ
          </Button>
        </Space>
      </div>
      <Table
        rowKey="id"
        size="middle"
        loading={isFetching}
        columns={columns}
        dataSource={data?.items}
        onRow={(r) => ({ onClick: () => setChonId(r.id), style: { cursor: 'pointer' } })}
        pagination={{
          current: page + 1,
          pageSize: 20,
          total: data?.total,
          showSizeChanger: false,
          onChange: (p) => setPage(p - 1),
          showTotal: (t) => `${t} hồ sơ`,
        }}
      />
      <TiepNhanDrawer
        open={moTiepNhan}
        onClose={() => setMoTiepNhan(false)}
        onDone={(id) => { setMoTiepNhan(false); refetch(); setChonId(id) }}
      />
      <ChiTietDrawer dangKyId={chonId} onClose={() => setChonId(undefined)} onChanged={() => refetch()} />
    </>
  )
}
