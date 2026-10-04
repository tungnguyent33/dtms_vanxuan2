import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, Form, Input, Skeleton, Space, Tooltip } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { api } from '../api/client'

interface CaptchaResponse {
  captchaId: string | null
  anh: string | null
  batBuoc: boolean
}

/**
 * O nhap ma xac nhan cho form cong khai. Dat ben trong <Form>: tu ghi captchaId + captcha vao gia tri form.
 * Ma chi dung mot lan -> doi `phienBan` (VD sau moi lan gui loi) de lay ma moi.
 */
export default function CaptchaField({ phienBan }: { phienBan: number }) {
  const form = Form.useFormInstance()
  const q = useQuery({
    queryKey: ['captcha', phienBan],
    queryFn: async () => (await api.get<CaptchaResponse>('/public/captcha')).data,
    gcTime: 0,
    staleTime: Infinity,
    refetchInterval: 240_000,   // ma het han sau 5 phut
    refetchOnWindowFocus: false,
  })

  useEffect(() => {
    if (q.data) form.setFieldsValue({ captchaId: q.data.captchaId ?? undefined, captcha: undefined })
  }, [q.data, form])

  if (q.data && !q.data.batBuoc) return null

  return (
    <>
      <Form.Item name="captchaId" hidden><Input /></Form.Item>
      <Form.Item label="Mã xác nhận" required style={{ marginBottom: 16 }}>
        <Space wrap>
          {q.data?.anh ? (
            <img src={q.data.anh} alt="Mã xác nhận" width={170} height={56}
                 style={{ borderRadius: 6, border: '1px solid #d9d9d9', display: 'block' }} />
          ) : <Skeleton.Image active style={{ width: 170, height: 56 }} />}
          <Tooltip title="Đổi mã khác">
            <Button icon={<ReloadOutlined />} onClick={() => q.refetch()} loading={q.isFetching} aria-label="Đổi mã khác" />
          </Tooltip>
          <Form.Item name="captcha" noStyle rules={[{ required: true, message: 'Nhập mã xác nhận' }]}>
            <Input placeholder="5 ký tự" maxLength={5} autoComplete="off" style={{ width: 120, textTransform: 'uppercase' }} />
          </Form.Item>
        </Space>
      </Form.Item>
    </>
  )
}
