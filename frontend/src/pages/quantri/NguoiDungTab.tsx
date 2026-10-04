import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { EditOutlined, KeyOutlined, LockOutlined, PlusOutlined, UnlockOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import type { KetQuaCapMatKhau, NguoiDung, PageResponse, VaiTro } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { hienMatKhauTam } from '../../components/MatKhauTam'
import { NHAN_VAI_TRO, ngayGio } from '../../utils/format'

const MAU_VAI_TRO: Record<string, string> = { ADMIN: 'red', LE_TAN: 'blue', GIAO_VIEN: 'purple', HOC_VIEN: 'default' }

interface FormValues {
  tenDangNhap: string
  hoTen: string
  soDienThoai?: string
  email?: string
  vaiTro: VaiTro
}

/** FR-03: quan ly tai khoan. Tao moi chi cho ADMIN / LE_TAN; GV cap o tab Giao vien, HV cap o ho so. */
export default function NguoiDungTab() {
  const { message, modal } = App.useApp()
  const { user } = useAuth()
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const [vaiTro, setVaiTro] = useState<VaiTro>()
  const [page, setPage] = useState(0)
  const [sua, setSua] = useState<NguoiDung | 'moi' | null>(null)
  const [form] = Form.useForm<FormValues>()

  const { data, isFetching } = useQuery({
    queryKey: ['nguoi-dung', q, vaiTro, page],
    queryFn: async () =>
      (await api.get<PageResponse<NguoiDung>>('/nguoi-dung', { params: { q: q || undefined, vaiTro, page, size: 20 } })).data,
    placeholderData: (prev) => prev,
  })
  const lamMoi = () => qc.invalidateQueries({ queryKey: ['nguoi-dung'] })

  const luu = useMutation({
    mutationFn: async (v: FormValues) => {
      if (sua === 'moi') return (await api.post<KetQuaCapMatKhau>('/nguoi-dung', v)).data
      await api.put(`/nguoi-dung/${(sua as NguoiDung).id}`, v)
      return null
    },
    onSuccess: (kq) => {
      setSua(null)
      lamMoi()
      if (kq) hienMatKhauTam(modal, kq, 'Đã tạo tài khoản')
      else message.success('Đã lưu')
    },
    onError: (e) => message.error(loiApi(e)),
  })

  const doiTrangThai = (n: NguoiDung) => {
    modal.confirm({
      title: `${n.hoatDong ? 'Khóa' : 'Mở khóa'} tài khoản ${n.tenDangNhap}?`,
      content: n.hoatDong ? 'Người dùng sẽ bị đăng xuất khỏi mọi thiết bị ngay lập tức.' : undefined,
      okButtonProps: { danger: n.hoatDong },
      onOk: async () => {
        try {
          await api.patch(`/nguoi-dung/${n.id}/trang-thai`, { hoatDong: !n.hoatDong })
          message.success('Đã cập nhật')
          lamMoi()
        } catch (e) {
          message.error(loiApi(e))
        }
      },
    })
  }

  const datLaiMatKhau = (n: NguoiDung) => {
    modal.confirm({
      title: `Đặt lại mật khẩu cho ${n.tenDangNhap}?`,
      content: 'Hệ thống tạo mật khẩu tạm mới, đăng xuất người dùng khỏi mọi thiết bị và mở khóa tạm (nếu có).',
      onOk: async () => {
        try {
          const { data: kq } = await api.post<KetQuaCapMatKhau>(`/nguoi-dung/${n.id}/dat-lai-mat-khau`)
          lamMoi()
          hienMatKhauTam(modal, kq)
        } catch (e) {
          message.error(loiApi(e))
        }
      },
    })
  }

  const moSua = (n: NguoiDung | 'moi') => {
    form.resetFields()
    if (n !== 'moi') form.setFieldsValue({ tenDangNhap: n.tenDangNhap, hoTen: n.hoTen, soDienThoai: n.soDienThoai, email: n.email, vaiTro: n.vaiTro })
    else form.setFieldsValue({ vaiTro: 'LE_TAN' })
    setSua(n)
  }

  const laNhanVien = sua === 'moi' || (sua && (sua.vaiTro === 'ADMIN' || sua.vaiTro === 'LE_TAN'))

  return (
    <>
      <div className="toolbar">
        <Input.Search allowClear placeholder="Tìm tên, tên đăng nhập, SĐT" style={{ width: 260 }}
                      onSearch={(v) => { setQ(v.trim()); setPage(0) }} />
        <Select allowClear placeholder="Vai trò" style={{ width: 160 }} value={vaiTro}
                onChange={(v) => { setVaiTro(v); setPage(0) }}
                options={Object.entries(NHAN_VAI_TRO).map(([k, v]) => ({ value: k, label: v }))} />
        <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }} onClick={() => moSua('moi')}>
          Thêm nhân viên
        </Button>
      </div>
      <Table<NguoiDung>
        rowKey="id" size="middle" loading={isFetching} dataSource={data?.items} scroll={{ x: 800 }}
        pagination={{ current: page + 1, pageSize: 20, total: data?.total, showSizeChanger: false, onChange: (p) => setPage(p - 1) }}
        columns={[
          {
            title: 'Người dùng',
            render: (_, n) => (
              <div>
                <Typography.Text strong>{n.hoTen}</Typography.Text>
                <div style={{ fontSize: 12, color: '#888' }}>{n.tenDangNhap}{n.soDienThoai ? ` · ${n.soDienThoai}` : ''}</div>
              </div>
            ),
          },
          { title: 'Vai trò', dataIndex: 'vaiTro', render: (v: string) => <Tag color={MAU_VAI_TRO[v]}>{NHAN_VAI_TRO[v]}</Tag> },
          {
            title: 'Trạng thái',
            render: (_, n) => (
              <Space size={4} wrap>
                {n.hoatDong ? <Tag color="green">Hoạt động</Tag> : <Tag color="red">Đã khóa</Tag>}
                {n.dangKhoaTam && <Tag color="orange">Khóa tạm (sai MK)</Tag>}
                {n.phaiDoiMatKhau && <Tag>Mật khẩu tạm</Tag>}
              </Space>
            ),
          },
          { title: 'Đăng nhập gần nhất', dataIndex: 'lanDangNhapCuoi', render: (v) => ngayGio(v) || '—', className: 'hide-mobile' },
          {
            title: '', key: 'act', width: 130,
            render: (_, n) => (
              <Space>
                <Tooltip title="Sửa"><Button size="small" icon={<EditOutlined />} onClick={() => moSua(n)} /></Tooltip>
                <Tooltip title="Đặt lại mật khẩu"><Button size="small" icon={<KeyOutlined />} onClick={() => datLaiMatKhau(n)} /></Tooltip>
                {n.id !== user?.id && (
                  <Tooltip title={n.hoatDong ? 'Khóa' : 'Mở khóa'}>
                    <Button size="small" danger={n.hoatDong} icon={n.hoatDong ? <LockOutlined /> : <UnlockOutlined />}
                            onClick={() => doiTrangThai(n)} />
                  </Tooltip>
                )}
              </Space>
            ),
          },
        ]}
      />
      <Modal title={sua === 'moi' ? 'Thêm tài khoản nhân viên' : 'Sửa tài khoản'} open={!!sua} onCancel={() => setSua(null)}
             onOk={() => form.submit()} confirmLoading={luu.isPending} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => luu.mutate(v)}>
          <Form.Item name="tenDangNhap" label="Tên đăng nhập"
                     rules={[{ required: true, pattern: /^[A-Za-z0-9._-]{3,50}$/, message: '3–50 ký tự: chữ không dấu, số, . _ -' }]}>
            <Input disabled={sua !== 'moi'} />
          </Form.Item>
          <Form.Item name="hoTen" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
          <Form.Item name="soDienThoai" label="Số điện thoại" rules={[{ pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input maxLength={10} />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input /></Form.Item>
          <Form.Item name="vaiTro" label="Vai trò"
                     extra={laNhanVien ? undefined : 'Vai trò giáo viên / học viên không đổi ở đây.'}>
            <Select disabled={!laNhanVien}
                    options={laNhanVien
                      ? [{ value: 'LE_TAN', label: NHAN_VAI_TRO.LE_TAN }, { value: 'ADMIN', label: NHAN_VAI_TRO.ADMIN }]
                      : Object.entries(NHAN_VAI_TRO).map(([k, v]) => ({ value: k, label: v }))} />
          </Form.Item>
          {sua === 'moi' && (
            <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
              Hệ thống tạo mật khẩu tạm và hiện một lần sau khi lưu. Tài khoản giáo viên cấp ở tab Giáo viên;
              tài khoản học viên cấp trong hồ sơ học viên.
            </Typography.Paragraph>
          )}
        </Form>
      </Modal>
    </>
  )
}
