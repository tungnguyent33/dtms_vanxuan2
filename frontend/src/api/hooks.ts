import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { Ctv, GiaoVien, HangGplx, Khoa, TrangThaiKhoa, Xe } from './types'

/** Cac hook doc danh muc dung chung (TanStack Query tu cache, khong goi lai API lien tuc). */

export const useKhoa = (trangThai?: TrangThaiKhoa) =>
  useQuery({
    queryKey: ['khoa', trangThai ?? 'all'],
    queryFn: async () => (await api.get<Khoa[]>('/khoa', { params: { trangThai } })).data,
  })

export const useHangGplx = () =>
  useQuery({ queryKey: ['hang-gplx'], queryFn: async () => (await api.get<HangGplx[]>('/hang-gplx')).data, staleTime: 600_000 })

export const useGiaoVien = () =>
  useQuery({ queryKey: ['giao-vien'], queryFn: async () => (await api.get<GiaoVien[]>('/giao-vien')).data, staleTime: 300_000 })

export const useXe = () =>
  useQuery({ queryKey: ['xe'], queryFn: async () => (await api.get<Xe[]>('/xe-tap-lai')).data, staleTime: 300_000 })

export const useCtv = () =>
  useQuery({ queryKey: ['ctv'], queryFn: async () => (await api.get<Ctv[]>('/ctv')).data, staleTime: 300_000 })
