import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Descriptions, Drawer, Dropdown, Form, Input, Modal, Select, Space, Tag, Upload } from 'antd'
import { CheckOutlined, CloseOutlined, DownOutlined, EditOutlined, FileProtectOutlined, KeyOutlined, UploadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { api, loiApi, taiTep } from '../../api/client'
import type { Ctv, HangCtv, KetQuaCapMatKhau, TrangThaiCtv } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { hienMatKhauTam } from '../../components/MatKhauTam'
import { NHAN_HANG_CTV, NHAN_LOAI_CTV, NHAN_TRANG_THAI_CTV, ngay, ngayGio } from '../../utils/format'
import CtvForm from './CtvForm'
import { ctvBody, type CtvFormValues } from './ctvBody'

const HANG_OPTIONS = Object.entries(NHAN_HANG_CTV).map(([k, v]) => ({ value: k, label: v.text }))

/** Chi tiet CTV. Le tan: sua / tai cam ket khi con cho duyet. Admin: duyet, khoa, doi hang, cap tai khoan. */
export default function CtvDrawer({ ctvId, onClose }: { ctvId?: number; onClose: () => void }) {
  const { message, modal } = App.useApp()
  const { coQuyen, user } = useAuth()
  const laAdmin = coQuyen('ADMIN')
  const qc = useQueryClient()
  const [dangSua, setDangSua] = useState(false)
  const [duyet, setDuyet] = useState(false)
  const [form] = Form.useForm<CtvFormValues>()
  const [formDuyet] = Form.useForm<{ hang: HangCtv }>()

  const { data: c } = useQuery({
    queryKey: ['ctv-ct', ctvId],
    enabled: ctvId != null,
    queryFn: async () => (await api.get<Ctv>(`/ctv/${ctvId}`)).data,
  })
  const lamMoi = () => { qc.invalidateQueries({ queryKey: ['ctv'] }); qc.invalidateQueries({ queryKey: ['ctv-ct', ctvId] }) }

  const goi = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      await fn()
      message.success(ok)
      lamMoi()
      return true
    } catch (e) {
      message.error(loiApi(e))
      return false
    }
  }

  const nhapLyDo = (tieuDe: string, xuLy: (lyDo: string) => Promise<unknown>) => {
    let lyDo = ''
    modal.confirm({
      title: tieuDe,
      content: <Input.TextArea rows={2} placeholder="Lý do (bắt buộc)" onChange={(e) => (lyDo = e.target.value)} />,
      onOk: async () => {
        if (!lyDo.trim()) { message.warning('Cần nhập lý do'); throw new Error('thieu ly do') }
        await xuLy(lyDo.trim())
      },
    })
  }

  const choDuyet = c?.trangThai === 'CHO_DUYET'
  const duocSua = !!c && (laAdmin || choDuyet)
  const tuDuyet = c?.nguoiTaoId != null && c.nguoiTaoId === user?.id

  return (
    <Drawer title={c ? `CTV ${c.hoTen}` : 'Cộng tác viên'} open={ctvId != null} onClose={() => { setDangSua(false); onClose() }}
            width={720} destroyOnClose
            extra={dangSua && <Button type="primary" onClick={() => form.submit()}>Lưu</Button>}>
      {c && !dangSua && (
        <>
          <Space wrap style={{ marginBottom: 16 }}>
            <Tag color={NHAN_TRANG_THAI_CTV[c.trangThai].color}>{NHAN_TRANG_THAI_CTV[c.trangThai].text}</Tag>
            <Tag color={NHAN_HANG_CTV[c.hang].color}>Hạng {NHAN_HANG_CTV[c.hang].text}</Tag>
            {duocSua && (
              <Button icon={<EditOutlined />} onClick={() => {
                form.setFieldsValue({ ...c, ngayBatDau: c.ngayBatDau ? dayjs(c.ngayBatDau) : undefined } as CtvFormValues)
                setDangSua(true)
              }}>Sửa hồ sơ</Button>
            )}
            {laAdmin && choDuyet && (
              <>
                <Button type="primary" icon={<CheckOutlined />} disabled={tuDuyet}
                        title={tuDuyet ? 'Người nhập hồ sơ không được tự duyệt' : undefined}
                        onClick={() => { formDuyet.setFieldsValue({ hang: 'THUONG' }); setDuyet(true) }}>Duyệt</Button>
                <Button danger icon={<CloseOutlined />}
                        onClick={() => nhapLyDo('Từ chối hồ sơ CTV?', (lyDo) => goi(() => api.post(`/ctv/${c.id}/tu-choi`, { lyDo }), 'Đã từ chối'))}>
                  Từ chối
                </Button>
              </>
            )}
            {laAdmin && !choDuyet && (
              <>
                <Dropdown menu={{
                  items: (['HOAT_DONG', 'TAM_KHOA', 'NGUNG'] as TrangThaiCtv[]).filter((t) => t !== c.trangThai)
                    .map((t) => ({ key: t, label: NHAN_TRANG_THAI_CTV[t].text })),
                  onClick: (e) => nhapLyDo(`Chuyển CTV sang "${NHAN_TRANG_THAI_CTV[e.key].text}"?`,
                    (lyDo) => goi(() => api.patch(`/ctv/${c.id}/trang-thai`, { trangThai: e.key, lyDo }), 'Đã cập nhật')),
                }}>
                  <Button>Trạng thái <DownOutlined /></Button>
                </Dropdown>
                <Dropdown menu={{
                  items: HANG_OPTIONS.filter((h) => h.value !== c.hang).map((h) => ({ key: h.value, label: `Hạng ${h.label}` })),
                  onClick: (e) => modal.confirm({
                    title: `Đổi hạng CTV sang ${NHAN_HANG_CTV[e.key].text}?`,
                    content: 'Áp dụng cho học viên giới thiệu từ nay; hoa hồng đã tính giữ nguyên.',
                    onOk: () => goi(() => api.patch(`/ctv/${c.id}/hang`, { hang: e.key }), 'Đã đổi hạng'),
                  }),
                }}>
                  <Button>Đổi hạng <DownOutlined /></Button>
                </Dropdown>
                {c.trangThai === 'HOAT_DONG' && !c.hocVienId && (
                  <Button icon={<KeyOutlined />} onClick={() => modal.confirm({
                    title: c.tenDangNhap ? 'Đặt lại mật khẩu tài khoản CTV?' : 'Cấp tài khoản cho CTV?',
                    content: `Tên đăng nhập là số điện thoại ${c.soDienThoai}. Mật khẩu tạm chỉ hiện một lần.`,
                    onOk: async () => {
                      try {
                        const { data } = await api.post<KetQuaCapMatKhau>(`/ctv/${c.id}/tai-khoan`)
                        lamMoi()
                        hienMatKhauTam(modal, data)
                      } catch (e) { message.error(loiApi(e)) }
                    },
                  })}>{c.tenDangNhap ? 'Đặt lại mật khẩu' : 'Cấp tài khoản'}</Button>
                )}
              </>
            )}
          </Space>

          {choDuyet && (
            <Alert type="info" showIcon style={{ marginBottom: 12 }}
                   message={laAdmin
                     ? (tuDuyet ? 'Bạn là người nhập hồ sơ này nên không được tự duyệt (người nhập ≠ người duyệt).'
                       : 'Kiểm tra CCCD, cam kết đã ký và tài khoản nhận tiền trước khi duyệt.')
                     : 'Hồ sơ chờ quản trị viên duyệt. Hãy tải lên bản cam kết đã ký.'} />
          )}

          <Descriptions size="small" column={{ xs: 1, md: 2 }} bordered>
            <Descriptions.Item label="Họ tên">{c.hoTen}</Descriptions.Item>
            <Descriptions.Item label="Loại">{NHAN_LOAI_CTV[c.loai]}</Descriptions.Item>
            <Descriptions.Item label="Điện thoại">{c.soDienThoai}{c.zalo ? ` · Zalo ${c.zalo}` : ''}</Descriptions.Item>
            <Descriptions.Item label="CCCD">{c.cccd ?? <Tag color="red">Thiếu</Tag>}</Descriptions.Item>
            <Descriptions.Item label="Địa chỉ" span={2}>{[c.diaChi, c.diaBan].filter(Boolean).join(' · ') || '—'}</Descriptions.Item>
            <Descriptions.Item label="Nhận hoa hồng" span={2}>
              {c.soTaiKhoan ? `${c.nganHang ?? ''} · ${c.soTaiKhoan} · ${c.chuTaiKhoan ?? ''}` : 'Chưa có tài khoản (chi tiền mặt)'}
            </Descriptions.Item>
            <Descriptions.Item label="Cam kết">
              <Space wrap>
                {c.coCamKet
                  ? <Button size="small" icon={<FileProtectOutlined />} onClick={() => taiTep(`/ctv/${c.id}/cam-ket`, undefined, true)}>Xem</Button>
                  : <Tag color="red">Chưa có</Tag>}
                {(laAdmin || choDuyet) && (
                  <Upload accept="image/jpeg,image/png,application/pdf" showUploadList={false}
                          customRequest={async ({ file, onSuccess, onError }) => {
                            const fd = new FormData()
                            fd.append('file', file as Blob)
                            const ok = await goi(() => api.post(`/ctv/${c.id}/cam-ket`, fd), 'Đã tải cam kết')
                            if (ok) onSuccess?.({})
                            else onError?.(new Error('upload'))
                          }}>
                    <Button size="small" icon={<UploadOutlined />}>{c.coCamKet ? 'Thay' : 'Tải lên'}</Button>
                  </Upload>
                )}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày bắt đầu">{ngay(c.ngayBatDau) || '—'}</Descriptions.Item>
            <Descriptions.Item label="Tài khoản">{c.hocVienId ? 'Dùng tài khoản học viên' : c.tenDangNhap ?? 'Chưa cấp'}</Descriptions.Item>
            <Descriptions.Item label="Kết quả">{c.soLead} lead · {c.soHocVien} học viên</Descriptions.Item>
            <Descriptions.Item label="Người tạo">{c.nguoiTao ?? '—'} · {ngayGio(c.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="Người duyệt">{c.nguoiDuyet ? `${c.nguoiDuyet} · ${ngayGio(c.ngayDuyet)}` : '—'}</Descriptions.Item>
            {c.ghiChu && <Descriptions.Item label="Ghi chú" span={2}>{c.ghiChu}</Descriptions.Item>}
          </Descriptions>
        </>
      )}

      {c && dangSua && (
        <CtvForm form={form} onFinish={async (v) => {
          if (await goi(() => api.put(`/ctv/${c.id}`, ctvBody(v)), 'Đã lưu')) setDangSua(false)
        }} />
      )}

      <Modal title={`Duyệt CTV ${c?.hoTen ?? ''}`} open={duyet} onCancel={() => setDuyet(false)} onOk={() => formDuyet.submit()}
             okText="Duyệt" destroyOnClose>
        <Form form={formDuyet} layout="vertical" onFinish={async (v) => {
          if (c && await goi(() => api.post(`/ctv/${c.id}/duyet`, v), 'Đã duyệt CTV')) setDuyet(false)
        }}>
          <Form.Item name="hang" label="Hạng CTV" extra="Hạng quyết định mức hoa hồng theo bảng chính sách">
            <Select options={HANG_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </Drawer>
  )
}
