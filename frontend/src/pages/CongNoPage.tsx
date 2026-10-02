import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Select, Switch, Table, Typography } from 'antd'
import { FileExcelOutlined } from '@ant-design/icons'
import { api, loiApi, taiTep } from '../api/client'
import { useKhoa } from '../api/hooks'
import type { DongCongNo } from '../api/types'
import { tien } from '../utils/format'

export default function CongNoPage() {
  const { message } = App.useApp()
  const { data: khoa } = useKhoa()
  const [khoaId, setKhoaId] = useState<number>()
  const [chiConNo, setChiConNo] = useState(true)

  const { data, isFetching } = useQuery({
    queryKey: ['cong-no', khoaId, chiConNo],
    queryFn: async () => (await api.get<DongCongNo[]>('/cong-no', { params: { khoaId, chiConNo } })).data,
  })
  const tong = (data ?? []).reduce((s, r) => s + Number(r.conNo), 0)

  return (
    <>
      <h1 className="page-title">Công nợ học phí</h1>
      <div className="toolbar">
        <Select allowClear placeholder="Tất cả khóa" style={{ width: 240 }} value={khoaId} onChange={setKhoaId}
                options={khoa?.map((k) => ({ value: k.id, label: `${k.maKhoa} – hạng ${k.hangMa}` }))} />
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Switch checked={chiConNo} onChange={setChiConNo} /> Chỉ học viên còn nợ
        </span>
        <Button icon={<FileExcelOutlined />} style={{ marginLeft: 'auto' }}
                onClick={() => taiTep(`/cong-no/excel?chiConNo=${chiConNo}${khoaId ? `&khoaId=${khoaId}` : ''}`, 'cong-no.xlsx')
                  .catch((e) => message.error(loiApi(e)))}>
          Xuất Excel
        </Button>
      </div>
      <Typography.Paragraph>
        Tổng còn nợ: <b style={{ color: '#cf1322' }}>{tien(tong)}</b> ({data?.length ?? 0} hồ sơ)
      </Typography.Paragraph>
      <Table<DongCongNo>
        rowKey="dangKyId" size="middle" loading={isFetching} dataSource={data} scroll={{ x: 700 }}
        columns={[
          { title: 'Mã hồ sơ', dataIndex: 'maHoSo' },
          { title: 'Họ tên', dataIndex: 'hoTen' },
          { title: 'SĐT', dataIndex: 'soDienThoai' },
          { title: 'Khóa', dataIndex: 'maKhoa' },
          { title: 'Phải đóng', dataIndex: 'phaiDong', render: (v) => tien(v), align: 'right' },
          { title: 'Đã đóng', dataIndex: 'daDong', render: (v) => tien(v), align: 'right' },
          {
            title: 'Còn nợ', dataIndex: 'conNo', align: 'right',
            render: (v) => <b style={{ color: v > 0 ? '#cf1322' : undefined }}>{tien(v)}</b>,
            sorter: (a, b) => a.conNo - b.conNo,
          },
        ]}
      />
    </>
  )
}
