import type { ReactNode } from 'react'
import {
  CalendarOutlined, CarOutlined, DollarOutlined, FileTextOutlined, SolutionOutlined, TrophyOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

/** Bieu tuong theo loai thong bao (khop hang so trong ThongBaoService.java). */
export const BIEU_TUONG_THONG_BAO: Record<string, ReactNode> = {
  HO_SO: <SolutionOutlined style={{ color: '#1677ff' }} />,
  HOC_PHI: <DollarOutlined style={{ color: '#389e0d' }} />,
  NHAC_NO: <DollarOutlined style={{ color: '#cf1322' }} />,
  LICH_HOC: <CalendarOutlined style={{ color: '#722ed1' }} />,
  BAO_CAO_SO: <FileTextOutlined style={{ color: '#d46b08' }} />,
  KET_QUA: <TrophyOutlined style={{ color: '#13c2c2' }} />,
  XE: <CarOutlined style={{ color: '#8c8c8c' }} />,
}

/** "3 phút trước", "2 ngày trước" (dayjs locale vi da nap o main.tsx). */
export const thoiGianTuongDoi = (v: string) => dayjs(v).fromNow()
