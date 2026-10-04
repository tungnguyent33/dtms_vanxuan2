import { useSearchParams } from 'react-router-dom'
import { Card, Tabs } from 'antd'
import NguoiDungTab from './NguoiDungTab'
import GiaoVienTab from './GiaoVienTab'
import XeTab from './XeTab'
import HangTab from './HangTab'

/** FR-02, FR-03: danh muc va nguoi dung (chi quan tri vien). Tab dang mo luu tren URL de F5 khong mat. */
export default function QuanTriPage() {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'nguoi-dung'
  return (
    <>
      <h1 className="page-title">Danh mục & người dùng</h1>
      <Card>
        <Tabs
          activeKey={tab}
          onChange={(k) => setParams({ tab: k }, { replace: true })}
          destroyInactiveTabPane
          items={[
            { key: 'nguoi-dung', label: 'Người dùng', children: <NguoiDungTab /> },
            { key: 'giao-vien', label: 'Giáo viên', children: <GiaoVienTab /> },
            { key: 'xe', label: 'Xe tập lái', children: <XeTab /> },
            { key: 'hang', label: 'Hạng GPLX', children: <HangTab /> },
          ]}
        />
      </Card>
    </>
  )
}
