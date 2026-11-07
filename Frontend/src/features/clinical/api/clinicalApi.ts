import { api } from '@/shared/api/client'
import type { ClinicalSnapshotDto } from '@/shared/api/dto'

/** 5 tab lâm sàng trong `clinical_snapshot.payload` (`doc/01-scope.md` mục 3.3). */
export interface ClinicalSnapshot {
  orderId: number
  source: 'SEED' | 'HIS'
  fetchedAt: string
  overview: Record<string, string>
  labs: { stt: number; ten: string; trang_thai: string }[]
  functional: { ten_dich_vu: string; ket_qua: string }[]
  drugs: { ten: string; so_luong: number; don_vi: string }[]
  surgery: unknown[]
}

const arr = <T>(v: unknown): T[] => (Array.isArray(v) ? (v as T[]) : [])

/** `GET /imaging-orders/{id}/clinical-snapshot`. */
export async function fetchClinicalSnapshot(orderId: number, signal?: AbortSignal): Promise<ClinicalSnapshot> {
  const dto = await api.request<ClinicalSnapshotDto>({
    method: 'GET',
    path: `/imaging-orders/${orderId}/clinical-snapshot`,
    signal,
  })
  const p = dto.payload ?? {}
  return {
    orderId: dto.order_id,
    source: dto.source,
    fetchedAt: dto.fetched_at,
    overview: (p.overview as Record<string, string>) ?? {},
    labs: arr(p.labs),
    functional: arr(p.functional),
    drugs: arr(p.drugs),
    surgery: arr(p.surgery),
  }
}
