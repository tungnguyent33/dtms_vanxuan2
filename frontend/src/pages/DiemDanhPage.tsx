import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { App, Button, Card, Checkbox, DatePicker, Empty, InputNumber, List, Segmented, Space, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { api, loiApi } from '../api/client'
import type { BuoiHoc, DanhSachBuoi, DongDiemDanh } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { gio } from '../utils/format'

/** UC09 - Giao vien diem danh tai san tap (giao dien uu tien dien thoai). */
export default function DiemDanhPage() {
  const { message } = App.useApp()
  const { user } = useAuth()
  const [ngayChon, setNgayChon] = useState(dayjs())
  const [buoiId, setBuoiId] = useState<number>()
  const [dong, setDong] = useState<DongDiemDanh[]>([])

  // Giao vien: lich cua minh; Admin: tat ca buoi trong ngay
  const lich = useQuery({
    queryKey: ['lich-ngay', user?.vaiTro, ngayChon.format('YYYY-MM-DD')],
    queryFn: async () => {
      const d = ngayChon.format('YYYY-MM-DD')
      return user?.vaiTro === 'GIAO_VIEN'
        ? (await api.get<BuoiHoc[]>('/buoi-hoc/cua-toi', { params: { ngay: d } })).data
        : (await api.get<BuoiHoc[]>('/buoi-hoc', { params: { tu: d, den: d } })).data
    },
  })

  // Tu chon buoi dau tien trong ngay (Segmented luon to sang mot muc, neu de trong thi bam vao muc do khong co tac dung)
  useEffect(() => {
    if (lich.data && lich.data.length > 0 && !lich.data.some((b) => b.id === buoiId)) setBuoiId(lich.data[0].id)
  }, [lich.data, buoiId])

  const ds = useQuery({
    queryKey: ['danh-sach-buoi', buoiId],
    enabled: !!buoiId,
    queryFn: async () => (await api.get<DanhSachBuoi>(`/buoi-hoc/${buoiId}/danh-sach`)).data,
  })

  useEffect(() => {
    if (ds.data) {
      const tl = ds.data.buoi.thoiLuongGio
      // Mac dinh: co mat du gio (giao vien chi can bo tick nguoi vang)
      setDong(ds.data.hocVien.map((h) => ({ ...h, coMat: h.coMat ?? true, soGio: h.soGio ?? tl })))
    }
  }, [ds.data])

  const luu = useMutation({
    mutationFn: async () =>
      (await api.put<DanhSachBuoi>(`/buoi-hoc/${buoiId}/diem-danh`, {
        danhSach: dong.map((d) => ({ dangKyId: d.dangKyId, coMat: !!d.coMat, soGio: d.coMat ? d.soGio ?? 0 : 0, ghiChu: d.ghiChu })),
      })).data,
    onSuccess: () => {
      message.success('Đã lưu điểm danh')
      ds.refetch()
      lich.refetch()
    },
    onError: (e) => message.error(loiApi(e)),
  })

  const capNhat = (id: number, patch: Partial<DongDiemDanh>) =>
    setDong((cu) => cu.map((d) => (d.dangKyId === id ? { ...d, ...patch } : d)))

  const buoi = ds.data?.buoi
  const soCoMat = dong.filter((d) => d.coMat).length

  return (
    <>
      <h1 className="page-title">Điểm danh</h1>
      <div className="toolbar">
        <DatePicker value={ngayChon} format="DD/MM/YYYY" allowClear={false}
                    onChange={(d) => { setNgayChon(d); setBuoiId(undefined) }} />
      </div>

      {lich.data && lich.data.length === 0 && <Empty description={`Không có buổi học ngày ${ngayChon.format('DD/MM/YYYY')}`} />}
      {lich.data && lich.data.length > 0 && (
        <Segmented block style={{ marginBottom: 16 }} value={buoiId}
                   onChange={(v) => setBuoiId(v as number)}
                   options={lich.data.map((b) => ({
                     value: b.id,
                     label: `${gio(b.gioBatDau)} ${b.loai === 'LY_THUYET' ? 'LT' : 'TH'} · ${b.maKhoa}${b.trangThai === 'DA_DAY' ? ' ✓' : ''}`,
                   }))} />
      )}

      {buoi && (
        <Card
          title={<>{buoi.loai === 'LY_THUYET' ? 'Lý thuyết' : 'Thực hành'} · {gio(buoi.gioBatDau)}–{gio(buoi.gioKetThuc)}</>}
          extra={<Tag>{soCoMat}/{dong.length} có mặt</Tag>}
          loading={ds.isFetching}
        >
          <Typography.Paragraph type="secondary">
            {buoi.diaDiem}{buoi.bienSoXe ? ` · xe ${buoi.bienSoXe}` : ''} · thời lượng {buoi.thoiLuongGio} giờ.
            {buoi.loai === 'LY_THUYET' && ' Danh sách chỉ gồm học viên học lý thuyết tập trung.'}
          </Typography.Paragraph>
          <List
            dataSource={dong}
            locale={{ emptyText: 'Không có học viên' }}
            renderItem={(d) => (
              <List.Item
                actions={[
                  <InputNumber key="gio" size="small" min={0} max={buoi.thoiLuongGio} step={0.5} disabled={!d.coMat}
                               value={d.coMat ? d.soGio ?? 0 : 0} style={{ width: 80 }} addonAfter="h"
                               onChange={(v) => capNhat(d.dangKyId, { soGio: v ?? 0 })} />,
                ]}
              >
                <Checkbox checked={!!d.coMat} onChange={(e) => capNhat(d.dangKyId, { coMat: e.target.checked })}>
                  <div>
                    <b>{d.hoTen}</b>
                    <div style={{ fontSize: 12, color: '#888' }}>{d.maHoSo} · {d.soDienThoai}</div>
                  </div>
                </Checkbox>
              </List.Item>
            )}
          />
          <Space style={{ marginTop: 12 }}>
            <Button type="primary" size="large" loading={luu.isPending} disabled={dong.length === 0} onClick={() => luu.mutate()}>
              Lưu điểm danh
            </Button>
          </Space>
        </Card>
      )}
    </>
  )
}
