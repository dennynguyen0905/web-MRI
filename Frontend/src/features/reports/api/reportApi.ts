import { api } from '@/shared/api/client'
import type {
  ReportDto, ReportWriteDto, StatusHistoryEntryDto, TransitionRequestDto, TransitionResultDto,
} from '@/shared/api/dto'
import { toReport, toStatusHistoryEntry, toStudy } from '@/shared/api/mappers'
import type { Report, StatusHistoryEntry, Study, StudyStatus } from '@/shared/lib/types'

/** `GET /studies/{id}/reports/current` — bản có `version` lớn nhất. */
export async function fetchCurrentReport(studyId: number, signal?: AbortSignal): Promise<Report | null> {
  try {
    const dto = await api.request<ReportDto>({
      method: 'GET',
      path: `/studies/${studyId}/reports/current`,
      signal,
    })
    return toReport(dto)
  } catch (err) {
    // 404 nghĩa là ca chưa có báo cáo — không phải lỗi, chỉ là chưa ai viết.
    if (typeof err === 'object' && err !== null && (err as { status?: number }).status === 404) return null
    throw err
  }
}

/** `PUT /studies/{id}/reports/current` — lưu nháp, chỉ khi ca đang `READING`. */
export async function saveReportDraft(studyId: number, body: ReportWriteDto): Promise<Report> {
  const dto = await api.request<ReportDto>({
    method: 'PUT',
    path: `/studies/${studyId}/reports/current`,
    body,
  })
  return toReport(dto)
}

export interface TransitionResult {
  study: Study
  historyEntry: StatusHistoryEntry
  report: Report | null
}

/** `POST /studies/{id}/transitions` — chuyển trạng thái, backend kiểm vai trò và điều kiện. */
export async function transitionStudy(
  studyId: number,
  toStatus: StudyStatus,
  reason: string | null,
): Promise<TransitionResult> {
  const body: TransitionRequestDto = { to_status: toStatus, reason }
  const dto = await api.request<TransitionResultDto>({
    method: 'POST',
    path: `/studies/${studyId}/transitions`,
    body,
  })
  return {
    study: toStudy(dto.study),
    historyEntry: toStatusHistoryEntry(dto.history_entry),
    report: dto.report ? toReport(dto.report) : null,
  }
}

/** `GET /studies/{id}/status-history` (IN-06). */
export async function fetchStatusHistory(studyId: number, signal?: AbortSignal): Promise<StatusHistoryEntry[]> {
  const dto = await api.request<StatusHistoryEntryDto[]>({
    method: 'GET',
    path: `/studies/${studyId}/status-history`,
    signal,
  })
  return dto.map(toStatusHistoryEntry)
}
