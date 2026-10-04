import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Badge, Button, Empty, List, Popover, Spin, Typography } from 'antd'
import { BellOutlined, NotificationOutlined } from '@ant-design/icons'
import { api } from '../api/client'
import type { PageResponse, ThongBao } from '../api/types'
import { BIEU_TUONG_THONG_BAO, thoiGianTuongDoi } from '../utils/thongBao'

/** Chuong thong bao tren thanh tieu de (FR-16): dem chua doc 60 giay mot lan, bam de xem 8 thong bao moi nhat. */
export default function ThongBaoChuong() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [mo, setMo] = useState(false)

  const dem = useQuery({
    queryKey: ['thong-bao', 'so-chua-doc'],
    queryFn: async () => (await api.get<{ soChuaDoc: number }>('/thong-bao/so-chua-doc')).data.soChuaDoc,
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  })
  const ds = useQuery({
    queryKey: ['thong-bao', 'moi-nhat'],
    enabled: mo,
    queryFn: async () => (await api.get<PageResponse<ThongBao>>('/thong-bao', { params: { size: 8 } })).data.items,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['thong-bao'] })

  const moThongBao = async (t: ThongBao) => {
    setMo(false)
    if (!t.daDoc) {
      await api.patch(`/thong-bao/${t.id}/da-doc`).catch(() => undefined)
      lamMoi()
    }
    if (t.duongDan) navigate(t.duongDan)
  }

  const docTatCa = async () => {
    await api.patch('/thong-bao/da-doc-tat-ca').catch(() => undefined)
    lamMoi()
  }

  const noiDung = (
    <div style={{ width: 360, maxWidth: 'calc(100vw - 32px)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Typography.Text strong>Thông báo</Typography.Text>
        {!!dem.data && <Button type="link" size="small" onClick={docTatCa}>Đánh dấu đã đọc tất cả</Button>}
      </div>
      {ds.isLoading ? <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div> : !ds.data?.length ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có thông báo" />
      ) : (
        <List
          size="small"
          dataSource={ds.data}
          style={{ maxHeight: 420, overflowY: 'auto' }}
          renderItem={(t) => (
            <List.Item onClick={() => moThongBao(t)}
                       style={{ cursor: 'pointer', background: t.daDoc ? undefined : '#f0f7ff', padding: '8px 10px', borderRadius: 6 }}>
              <List.Item.Meta
                avatar={BIEU_TUONG_THONG_BAO[t.loai] ?? <NotificationOutlined />}
                title={<span style={{ fontWeight: t.daDoc ? 400 : 600 }}>{t.tieuDe}</span>}
                description={
                  <>
                    <div style={{ fontSize: 12 }}>{t.noiDung}</div>
                    <div style={{ fontSize: 11, color: '#999', marginTop: 2 }}>{thoiGianTuongDoi(t.thoiGian)}</div>
                  </>
                }
              />
            </List.Item>
          )}
        />
      )}
      <div style={{ textAlign: 'center', borderTop: '1px solid #f0f0f0', paddingTop: 8, marginTop: 4 }}>
        <Button type="link" onClick={() => { setMo(false); navigate('/thong-bao') }}>Xem tất cả</Button>
      </div>
    </div>
  )

  return (
    <Popover content={noiDung} trigger="click" placement="bottomRight" open={mo} onOpenChange={setMo} arrow={false}>
      <Badge count={dem.data ?? 0} size="small" offset={[-4, 4]}>
        <Button type="text" shape="circle" icon={<BellOutlined style={{ fontSize: 18 }} />} aria-label="Thông báo" />
      </Badge>
    </Popover>
  )
}
