import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card, Col, DatePicker, Row, Statistic, Table, Tag } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { api } from '../../api/client'
import type { BaoCaoCtv } from '../../api/types'
import { NHAN_HANG_CTV, NHAN_TRANG_THAI_CTV, tien } from '../../utils/format'

/** So lead, ty le chot, doanh thu va hoa hong theo tung CTV; cong no hoa hong chua tra. */
export default function BaoCaoCtvTab() {
  const [khoang, setKhoang] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()])
  const { data, isFetching } = useQuery({
    queryKey: ['bao-cao-ctv', khoang[0].format('YYYY-MM-DD'), khoang[1].format('YYYY-MM-DD')],
    queryFn: async () => (await api.get<BaoCaoCtv[]>('/hoa-hong/bao-cao', {
      params: { tu: khoang[0].format('YYYY-MM-DD'), den: khoang[1].format('YYYY-MM-DD') },
    })).data,
  })
  const tong = (k: keyof BaoCaoCtv) => (data ?? []).reduce((s, r) => s + Number(r[k]), 0)

  return (
    <>
      <div className="toolbar">
        <DatePicker.RangePicker value={khoang} format="DD/MM/YYYY" allowClear={false}
                                onChange={(v) => v && v[0] && v[1] && setKhoang([v[0], v[1]])} />
      </div>
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Lead" value={tong('soLead')} /></Card></Col>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Chốt thành học viên" value={tong('soChot')} /></Card></Col>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Doanh thu từ CTV" value={tong('doanhThu')} formatter={(v) => tien(Number(v))} /></Card></Col>
        <Col xs={12} md={6}>
          <Card size="small"><Statistic title="Hoa hồng còn phải trả" value={tong('conPhaiTra')} formatter={(v) => tien(Number(v))} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
      </Row>
      <Table<BaoCaoCtv>
        rowKey="ctvId" size="middle" loading={isFetching} dataSource={data} pagination={false} scroll={{ x: 1000 }}
        columns={[
          {
            title: 'CTV', render: (_, r) => (
              <>
                <b>{r.tenCtv}</b>{' '}
                <Tag color={NHAN_HANG_CTV[r.hang].color}>{NHAN_HANG_CTV[r.hang].text}</Tag>
                {r.trangThai !== 'HOAT_DONG' && <Tag color={NHAN_TRANG_THAI_CTV[r.trangThai].color}>{NHAN_TRANG_THAI_CTV[r.trangThai].text}</Tag>}
              </>
            ),
          },
          { title: 'Lead', dataIndex: 'soLead', align: 'right', sorter: (a, b) => a.soLead - b.soLead },
          { title: 'Chốt', dataIndex: 'soChot', align: 'right' },
          { title: 'Tỷ lệ chốt', dataIndex: 'tyLeChot', render: (v) => `${v}%`, align: 'right', sorter: (a, b) => a.tyLeChot - b.tyLeChot },
          { title: 'Học viên', dataIndex: 'soHocVien', align: 'right' },
          { title: 'Doanh thu', dataIndex: 'doanhThu', render: (v) => tien(v), align: 'right', sorter: (a, b) => a.doanhThu - b.doanhThu },
          { title: 'Hoa hồng phát sinh', dataIndex: 'hoaHongPhatSinh', render: (v) => tien(v), align: 'right' },
          { title: 'Đã chi', dataIndex: 'daChi', render: (v) => tien(v), align: 'right' },
          { title: 'Còn phải trả', dataIndex: 'conPhaiTra', render: (v) => <b style={{ color: v > 0 ? '#cf1322' : undefined }}>{tien(v)}</b>, align: 'right' },
        ]}
      />
    </>
  )
}
