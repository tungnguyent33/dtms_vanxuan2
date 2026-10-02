import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, Col, DatePicker, Empty, Row, Segmented, Statistic } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/client'
import type { DongDoanhThu, TongQuan } from '../api/types'
import { tien } from '../utils/format'

const MAU: Record<string, string> = { A1: '#2e75b6', A: '#c55a11' }

export default function DashboardPage() {
  const [khoang, setKhoang] = useState<[Dayjs, Dayjs]>([dayjs().subtract(5, 'month').startOf('month'), dayjs().endOf('month')])
  const [nhom, setNhom] = useState<'THANG' | 'NGAY'>('THANG')

  const tq = useQuery({ queryKey: ['tong-quan'], queryFn: async () => (await api.get<TongQuan>('/bao-cao/tong-quan')).data })
  const dt = useQuery({
    queryKey: ['doanh-thu', khoang[0].format('YYYY-MM-DD'), khoang[1].format('YYYY-MM-DD'), nhom],
    queryFn: async () =>
      (await api.get<DongDoanhThu[]>('/bao-cao/doanh-thu', {
        params: { tu: khoang[0].format('YYYY-MM-DD'), den: khoang[1].format('YYYY-MM-DD'), nhom },
      })).data,
  })

  // Chuyen [{ky, hang, doanhThu}] thanh [{ky, A1: x, A: y}] de ve cot chong theo hang
  const hangs = Array.from(new Set((dt.data ?? []).map((r) => r.hang)))
  const theoKy = new Map<string, Record<string, number | string>>()
  for (const r of dt.data ?? []) {
    const row = theoKy.get(r.ky) ?? { ky: r.ky }
    row[r.hang] = Number(r.doanhThu)
    theoKy.set(r.ky, row)
  }
  const chart = Array.from(theoKy.values())
  const t = tq.data

  return (
    <>
      <h1 className="page-title">Tổng quan</h1>
      <Row gutter={[16, 16]}>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Đang học" value={t?.hocVienDangHoc} /></Card></Col>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Hồ sơ chờ duyệt" value={t?.hoSoChoDuyet} valueStyle={{ color: t?.hoSoChoDuyet ? '#d46b08' : undefined }} /></Card></Col>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Khóa đang tuyển" value={t?.khoaDangTuyen} /></Card></Col>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Hoàn thành trong năm" value={t?.hoanThanhTrongNam} /></Card></Col>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Doanh thu tháng này" value={t?.doanhThuThangNay} formatter={(v) => tien(Number(v))} /></Card></Col>
        <Col xs={12} md={8} xl={4}><Card loading={tq.isLoading}><Statistic title="Tổng công nợ" value={t?.tongCongNo} formatter={(v) => tien(Number(v))} valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>

      <Card title="Doanh thu theo hạng" style={{ marginTop: 16 }}
            extra={
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <Segmented value={nhom} onChange={(v) => setNhom(v as 'THANG' | 'NGAY')}
                           options={[{ value: 'THANG', label: 'Tháng' }, { value: 'NGAY', label: 'Ngày' }]} />
                <DatePicker.RangePicker value={khoang} format="DD/MM/YYYY" allowClear={false}
                                        onChange={(v) => v && v[0] && v[1] && setKhoang([v[0], v[1]])} />
              </div>
            }>
        {chart.length === 0 ? (
          <Empty description="Chưa có doanh thu trong khoảng này" />
        ) : (
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={chart}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="ky" />
              <YAxis tickFormatter={(v) => `${Math.round(Number(v) / 1_000_000)}tr`} />
              <Tooltip formatter={(v) => tien(Number(v))} />
              <Legend />
              {hangs.map((h) => (
                <Bar key={h} dataKey={h} name={`Hạng ${h}`} stackId="dt" fill={MAU[h] ?? '#888'} />
              ))}
            </BarChart>
          </ResponsiveContainer>
        )}
      </Card>
    </>
  )
}
