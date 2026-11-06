import { api } from '@/shared/api/client'

/** Năm phần in được, khớp 5 tab lâm sàng + báo cáo kết quả của RIS. */
export type PrintSection = 'OVERVIEW' | 'LABS' | 'FUNCTIONAL' | 'DRUGS' | 'SURGERY'

export const PRINT_SECTIONS: { key: PrintSection; label: string; hasRealData: boolean; note?: string }[] = [
  { key: 'OVERVIEW', label: 'Kết quả tổng quan khám', hasRealData: true, note: 'Dữ liệu giả từ clinical_snapshot' },
  { key: 'LABS', label: 'Kết quả xét nghiệm', hasRealData: true, note: 'Dữ liệu giả từ clinical_snapshot' },
  { key: 'FUNCTIONAL', label: 'Kết quả chẩn đoán chức năng chính', hasRealData: true, note: 'Dữ liệu thật của RIS' },
  { key: 'DRUGS', label: 'Đơn thuốc/vật tư trong phiếu khám', hasRealData: false, note: 'Chờ tích hợp HIS' },
  { key: 'SURGERY', label: 'Dữ liệu phẫu thuật thủ thuật', hasRealData: false, note: 'Chờ tích hợp HIS' },
]

export const PRINT_SECTION_LABEL: Record<PrintSection, string> =
  Object.fromEntries(PRINT_SECTIONS.map(s => [s.key, s.label])) as Record<PrintSection, string>

/**
 * `GET /patients/{id}/print-documents` — mỗi lần gọi trả **một** PDF cho **một** phần,
 * đúng yêu cầu "mỗi thông tin là 1 tờ/file riêng". Backend ghi `audit_log` action `PRINT`
 * kèm `patient_id` cho từng lần gọi (`doc/09` mục 2.2).
 */
export async function downloadPrintDocument(
  patientId: number,
  studyId: number,
  section: PrintSection,
): Promise<{ blob: Blob; filename: string }> {
  return api.requestBlob({
    method: 'GET',
    path: `/patients/${patientId}/print-documents`,
    query: { study_id: studyId, section },
  })
}

/**
 * Khi người dùng in bằng hộp thoại của trình duyệt (không tải PDF từ máy chủ), dữ liệu vẫn
 * rời khỏi hệ thống nên vẫn phải ghi `audit_log`. Endpoint này chỉ ghi log, không trả file.
 */
export async function logPrintEvent(
  patientId: number,
  studyId: number,
  sections: PrintSection[],
): Promise<void> {
  await api.request<void>({
    method: 'POST',
    path: `/patients/${patientId}/print-events`,
    body: { study_id: studyId, sections },
  })
}

export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
