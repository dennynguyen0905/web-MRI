import { api } from '@/shared/api/client'
import { toWorklistQuery, type WorklistFilters } from './worklistApi'

/**
 * `GET /studies/export` — tải danh sách ca chụp / bệnh nhân trên worklist dạng CSV (UTF-8 kèm BOM).
 * Backend ghi `audit_log` action `EXPORT` (`patient_id` có nếu 1 bệnh nhân, `NULL` nếu nhiều —
 * `doc/09-bao-mat-toan-ven-du-lieu.md` mục 2.2).
 */
export async function downloadStudiesCsv(
  filters: WorklistFilters,
  signal?: AbortSignal,
): Promise<{ blob: Blob; filename: string }> {
  const query = toWorklistQuery({
    ...filters,
    page: 1,
    pageSize: 5000,
  })
  return api.requestBlob({
    method: 'GET',
    path: '/studies/export',
    query: query as Record<string, never>,
    signal,
  })
}

/**
 * `GET /patients/export` — tải danh sách bệnh nhân dạng CSV (UTF-8 kèm BOM).
 */
export async function downloadPatientsCsv(
  params?: { q?: string; patientCode?: string; fullName?: string; sort?: string },
  signal?: AbortSignal,
): Promise<{ blob: Blob; filename: string }> {
  return api.requestBlob({
    method: 'GET',
    path: '/patients/export',
    query: {
      q: params?.q?.trim() || undefined,
      patient_code: params?.patientCode?.trim() || undefined,
      full_name: params?.fullName?.trim() || undefined,
      sort: params?.sort || undefined,
    },
    signal,
  })
}

/**
 * `POST /patients/export-events` — ghi nhận một lần xuất hoặc in danh sách từ trình duyệt (IN-25).
 */
export async function logListExportEvent(payload: {
  target: 'PATIENTS' | 'STUDIES'
  mode: 'EXPORT' | 'PRINT'
  rowCount: number
  patientId?: number | null
}): Promise<void> {
  await api.request<void>({
    method: 'POST',
    path: '/patients/export-events',
    body: {
      target: payload.target,
      mode: payload.mode,
      row_count: payload.rowCount,
      patient_id: payload.patientId ?? null,
    },
  })
}

