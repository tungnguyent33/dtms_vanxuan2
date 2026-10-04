import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Card, List, Segmented, Space, Typography } from 'antd'
import { NotificationOutlined, SyncOutlined } from '@ant-design/icons'
import { api, loiApi } from '../api/client'
import type { PageResponse, ThongBao } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { BIEU_TUONG_THONG_BAO, thoiGianTuongDoi } from '../utils/thongBao'
import { ngayGio } from '../utils/format'

/** Toan bo thong bao cua nguoi dang dang nhap (FR-15, FR-16). */
export default function ThongBaoPage() {
  const { message } = App.useApp()
  const { coQuyen } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [chuaDoc, setChuaDoc] = useState(false)
  const [page, setPage] = useState(0)
  const [dangChay, setDangChay] = useState(false)

  const { data, isFetching } = useQuery({
    queryKey: ['thong-bao', 'trang', chuaDoc, page],
    queryFn: async () => (await api.get<PageResponse<ThongBao>>('/thong-bao', { params: { chuaDoc, page, size: 20 } })).data,
    placeholderData: (prev) => prev,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['thong-bao'] })

  const mo = async (t: ThongBao) => {
    if (!t.daDoc) {
      await api.patch(`/thong-bao/${t.id}/da-doc`).catch(() => undefined)
      lamMoi()
    }
    if (t.duongDan) navigate(t.duongDan)
  }

  const chayNhacViec = async () => {
    setDangChay(true)
    try {
      const { data: kq } = await api.post<{ nhacLich: number; nhacNo: number; baoCaoSo: number; xeBaoDuong: number; hoSoChoDuyet: number }>('/thong-bao/chay-nhac-viec')
      const tong = kq.nhacLich + kq.nhacNo + kq.baoCaoSo + kq.xeBaoDuong + kq.hoSoChoDuyet
      message.success(tong ? `Đã tạo ${tong} thông báo (lịch ${kq.nhacLich}, nợ ${kq.nhacNo}, báo cáo Sở ${kq.baoCaoSo}, xe ${kq.xeBaoDuong}, hồ sơ chờ ${kq.hoSoChoDuyet})`
        : 'Không có việc mới cần nhắc (các nhắc việc hôm nay đã gửi trước đó)')
      lamMoi()
    } catch (e) {
      message.error(loiApi(e))
    } finally {
      setDangChay(false)
    }
  }

  return (
    <>
      <h1 className="page-title">Thông báo</h1>
      <div className="toolbar">
        <Segmented value={chuaDoc ? 'chua' : 'tat'} onChange={(v) => { setChuaDoc(v === 'chua'); setPage(0) }}
                   options={[{ value: 'tat', label: 'Tất cả' }, { value: 'chua', label: 'Chưa đọc' }]} />
        <Space style={{ marginLeft: 'auto' }} wrap>
          {coQuyen('ADMIN') && (
            <Button icon={<SyncOutlined />} loading={dangChay} onClick={chayNhacViec}>Chạy nhắc việc ngay</Button>
          )}
          <Button onClick={async () => { await api.patch('/thong-bao/da-doc-tat-ca'); lamMoi() }}>Đánh dấu đã đọc tất cả</Button>
        </Space>
      </div>
      <Card>
        <List
          loading={isFetching}
          dataSource={data?.items}
          locale={{ emptyText: chuaDoc ? 'Không có thông báo chưa đọc' : 'Chưa có thông báo' }}
          pagination={data && data.total > 20 ? { current: page + 1, pageSize: 20, total: data.total, onChange: (p) => setPage(p - 1), showSizeChanger: false } : false}
          renderItem={(t) => (
            <List.Item onClick={() => mo(t)} style={{ cursor: t.duongDan || !t.daDoc ? 'pointer' : undefined, background: t.daDoc ? undefined : '#f0f7ff', paddingInline: 12 }}
                       extra={<Typography.Text type="secondary" style={{ fontSize: 12 }} title={ngayGio(t.thoiGian)}>{thoiGianTuongDoi(t.thoiGian)}</Typography.Text>}>
              <List.Item.Meta
                avatar={<span style={{ fontSize: 20 }}>{BIEU_TUONG_THONG_BAO[t.loai] ?? <NotificationOutlined />}</span>}
                title={<span style={{ fontWeight: t.daDoc ? 400 : 600 }}>{t.tieuDe}</span>}
                description={t.noiDung}
              />
            </List.Item>
          )}
        />
      </Card>
    </>
  )
}
