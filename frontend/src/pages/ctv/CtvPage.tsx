import { useSearchParams } from 'react-router-dom'
import { Card, Tabs } from 'antd'
import { useAuth } from '../../auth/AuthContext'
import CtvListTab from './CtvListTab'
import LeadTab from './LeadTab'
import HoaHongTab from './HoaHongTab'
import ChinhSachTab from './ChinhSachTab'
import BaoCaoCtvTab from './BaoCaoCtvTab'

/** Quan ly CTV. Le tan: CTV + lead. Admin them: hoa hong & chi tra, chinh sach, bao cao. */
export default function CtvPage() {
  const { coQuyen } = useAuth()
  const laAdmin = coQuyen('ADMIN')
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'ctv'
  const items = [
    { key: 'ctv', label: 'Cộng tác viên', children: <CtvListTab /> },
    { key: 'lead', label: 'Lead', children: <LeadTab /> },
    ...(laAdmin ? [
      { key: 'hoa-hong', label: 'Hoa hồng & chi trả', children: <HoaHongTab /> },
      { key: 'chinh-sach', label: 'Chính sách hoa hồng', children: <ChinhSachTab /> },
      { key: 'bao-cao', label: 'Báo cáo CTV', children: <BaoCaoCtvTab /> },
    ] : []),
  ]
  return (
    <>
      <h1 className="page-title">Cộng tác viên</h1>
      <Card>
        <Tabs activeKey={items.some((i) => i.key === tab) ? tab : 'ctv'} destroyInactiveTabPane items={items}
              onChange={(k) => setParams({ tab: k }, { replace: true })} />
      </Card>
    </>
  )
}
