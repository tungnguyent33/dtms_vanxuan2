import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Table, Tag, Tooltip, Typography } from 'antd'
import { EditOutlined, KeyOutlined, PlusOutlined, StopOutlined, UndoOutlined, UserAddOutlined } from '@ant-design/icons'
import { api, loiApi } from '../../api/client'
import type { GiaoVien, KetQuaCapMatKhau } from '../../api/types'
import { hienMatKhauTam } from '../../components/MatKhauTam'
import { NHAN_LOAI_GIANG_DAY } from '../../utils/format'

type FormValues = Pick<GiaoVien, 'hoTen' | 'soDienThoai' | 'soGiayChungNhanGv' | 'loaiGiangDay'>

/** FR-03: danh muc giao vien + cap tai khoan dang nhap cho giao vien. */
export default function GiaoVienTab() {
  const { message, modal } = App.useApp()
  const qc = useQueryClient()
  const [sua, setSua] = useState<GiaoVien | 'moi' | null>(null)
  const [capTk, setCapTk] = useState<GiaoVien | null>(null)
  const [form] = Form.useForm<FormValues>()
  const [formTk] = Form.useForm<{ tenDangNhap: string }>()

  const { data, isFetching } = useQuery({
    queryKey: ['giao-vien', 'tat-ca'],
    queryFn: async () => (await api.get<GiaoVien[]>('/giao-vien', { params: { tatCa: true } })).data,
  })
  const lamMoi = () => { qc.invalidateQueries({ queryKey: ['giao-vien'] }); qc.invalidateQueries({ queryKey: ['nguoi-dung'] }) }

  const luu = useMutation({
    mutationFn: async (v: FormValues) =>
      sua === 'moi' ? api.post('/giao-vien', v) : api.put(`/giao-vien/${(sua as GiaoVien).id}`, v),
    onSuccess: () => { message.success('Đã lưu'); setSua(null); lamMoi() },
    onError: (e) => message.error(loiApi(e)),
  })

  const capTaiKhoan = async (g: GiaoVien, tenDangNhap?: string) => {
    try {
      const { data: kq } = await api.post<KetQuaCapMatKhau>(`/giao-vien/${g.id}/tai-khoan`, { tenDangNhap })
      setCapTk(null)
      lamMoi()
      hienMatKhauTam(modal, kq, g.tenDangNhap ? 'Đã đặt lại mật khẩu' : 'Đã cấp tài khoản giáo viên')
    } catch (e) {
      message.error(loiApi(e))
    }
  }

  const doiTrangThai = (g: GiaoVien) => {
    modal.confirm({
      title: `${g.hoatDong ? 'Ngừng' : 'Kích hoạt lại'} giáo viên ${g.hoTen}?`,
      content: g.hoatDong ? 'Tài khoản đăng nhập của giáo viên (nếu có) cũng bị khóa.' : undefined,
      okButtonProps: { danger: g.hoatDong },
      onOk: async () => {
        try {
          await api.patch(`/giao-vien/${g.id}/trang-thai`, { hoatDong: !g.hoatDong })
          message.success('Đã cập nhật')
          lamMoi()
        } catch (e) {
          message.error(loiApi(e))
        }
      },
    })
  }

  const moSua = (g: GiaoVien | 'moi') => {
    form.resetFields()
    if (g !== 'moi') form.setFieldsValue(g)
    else form.setFieldsValue({ loaiGiangDay: 'CA_HAI' })
    setSua(g)
  }

  return (
    <>
      <div className="toolbar">
        <Button type="primary" icon={<PlusOutlined />} style={{ marginLeft: 'auto' }} onClick={() => moSua('moi')}>Thêm giáo viên</Button>
      </div>
      <Table<GiaoVien>
        rowKey="id" size="middle" loading={isFetching} dataSource={data} pagination={false} scroll={{ x: 760 }}
        columns={[
          {
            title: 'Giáo viên',
            render: (_, g) => (
              <div style={{ opacity: g.hoatDong ? 1 : 0.55 }}>
                <Typography.Text strong>{g.hoTen}</Typography.Text>
                <div style={{ fontSize: 12, color: '#888' }}>{g.soDienThoai}{g.soGiayChungNhanGv ? ` · GCN ${g.soGiayChungNhanGv}` : ''}</div>
              </div>
            ),
          },
          { title: 'Giảng dạy', dataIndex: 'loaiGiangDay', render: (v) => NHAN_LOAI_GIANG_DAY[v] },
          { title: 'Tài khoản', dataIndex: 'tenDangNhap', render: (v) => (v ? <Tag color="blue">{v}</Tag> : <Tag>Chưa cấp</Tag>) },
          { title: 'Trạng thái', dataIndex: 'hoatDong', render: (v) => (v ? <Tag color="green">Đang dạy</Tag> : <Tag>Ngừng</Tag>) },
          {
            title: '', key: 'act', width: 140,
            render: (_, g) => (
              <Space>
                <Tooltip title="Sửa"><Button size="small" icon={<EditOutlined />} onClick={() => moSua(g)} /></Tooltip>
                {g.hoatDong && (g.tenDangNhap ? (
                  <Tooltip title="Đặt lại mật khẩu">
                    <Button size="small" icon={<KeyOutlined />}
                            onClick={() => modal.confirm({ title: `Đặt lại mật khẩu cho ${g.tenDangNhap}?`, onOk: () => capTaiKhoan(g) })} />
                  </Tooltip>
                ) : (
                  <Tooltip title="Cấp tài khoản">
                    <Button size="small" icon={<UserAddOutlined />} onClick={() => { formTk.resetFields(); setCapTk(g) }} />
                  </Tooltip>
                ))}
                <Tooltip title={g.hoatDong ? 'Ngừng' : 'Kích hoạt lại'}>
                  <Button size="small" danger={g.hoatDong} icon={g.hoatDong ? <StopOutlined /> : <UndoOutlined />} onClick={() => doiTrangThai(g)} />
                </Tooltip>
              </Space>
            ),
          },
        ]}
      />
      <Modal title={sua === 'moi' ? 'Thêm giáo viên' : 'Sửa giáo viên'} open={!!sua} onCancel={() => setSua(null)}
             onOk={() => form.submit()} confirmLoading={luu.isPending} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={(v) => luu.mutate(v)}>
          <Form.Item name="hoTen" label="Họ tên" rules={[{ required: true, message: 'Nhập họ tên' }]}><Input /></Form.Item>
          <Form.Item name="soDienThoai" label="Số điện thoại" rules={[{ required: true, pattern: /^0[0-9]{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
            <Input maxLength={10} />
          </Form.Item>
          <Form.Item name="soGiayChungNhanGv" label="Số giấy chứng nhận giáo viên"><Input /></Form.Item>
          <Form.Item name="loaiGiangDay" label="Giảng dạy">
            <Select options={Object.entries(NHAN_LOAI_GIANG_DAY).map(([k, v]) => ({ value: k, label: v }))} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal title={`Cấp tài khoản cho ${capTk?.hoTen ?? ''}`} open={!!capTk} onCancel={() => setCapTk(null)}
             onOk={() => formTk.submit()} okText="Cấp tài khoản" destroyOnClose>
        <Form form={formTk} layout="vertical" onFinish={(v) => capTk && capTaiKhoan(capTk, v.tenDangNhap)}>
          <Form.Item name="tenDangNhap" label="Tên đăng nhập"
                     rules={[{ required: true, pattern: /^[A-Za-z0-9._-]{3,50}$/, message: '3–50 ký tự: chữ không dấu, số, . _ -' }]}>
            <Input placeholder="gv02" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
