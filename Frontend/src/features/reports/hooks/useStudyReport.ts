import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '@/shared/api/http'
import { useAsync } from '@/shared/hooks/useAsync'
import type { Report, StatusHistoryEntry, StudyStatus } from '@/shared/lib/types'
import * as reportApi from '../api/reportApi'
import { ReportStepError, performSaveDraft, performTransition } from '../lib/reportSteps'

export interface ReportDraft {
  technique: string
  findings: string
  diagnosis: string
  conclusion: string
  recommendation: string
}

export const EMPTY_DRAFT: ReportDraft = {
  technique: '', findings: '', diagnosis: '', conclusion: '', recommendation: '',
}

export function draftFromReport(report: Report | null): ReportDraft {
  if (!report) return EMPTY_DRAFT
  return {
    technique: report.technique ?? '',
    findings: report.findings ?? '',
    diagnosis: report.diagnosis ?? '',
    conclusion: report.conclusion ?? '',
    recommendation: report.recommendation ?? '',
  }
}

/**
 * Báo cáo hiện hành + lịch sử trạng thái của một ca, kèm hai thao tác ghi.
 *
 * `onStatusChanged` được gọi khi một thao tác nhiều bước lỗi giữa chừng **sau khi** trạng thái ca
 * trên máy chủ đã đổi (ví dụ T3 thành công rồi lưu nháp lỗi) — màn hình cần tải lại để không còn
 * hiển thị trạng thái cũ. Trường hợp thành công thì giá trị trả về của thao tác đã mang trạng thái mới.
 */
export function useStudyReport(
  studyId: number | null,
  enabled: boolean,
  onStatusChanged?: (status: StudyStatus) => void,
) {
  const [saving, setSaving] = useState(false)
  const [actionError, setActionError] = useState<ApiError | null>(null)

  const report = useAsync<Report | null>(
    signal => reportApi.fetchCurrentReport(studyId!, signal),
    [studyId],
    { enabled: enabled && studyId != null },
  )

  const history = useAsync<StatusHistoryEntry[]>(
    signal => reportApi.fetchStatusHistory(studyId!, signal),
    [studyId],
    { enabled: enabled && studyId != null },
  )

  // `reload` của useAsync ổn định (useCallback với deps rỗng); object `report`/`history` thì tạo
  // mới mỗi lần render nên không đưa chúng vào deps của useCallback bên dưới.
  const reloadReport = report.reload
  const reloadHistory = history.reload

  // Giữ callback mới nhất mà không làm các thao tác bên dưới đổi danh tính mỗi lần render.
  const onStatusChangedRef = useRef(onStatusChanged)
  useEffect(() => {
    onStatusChangedRef.current = onStatusChanged
  })

  const run = useCallback(async (fn: () => Promise<StudyStatus | undefined>): Promise<StudyStatus | undefined> => {
    setSaving(true)
    setActionError(null)
    try {
      const next = await fn()
      reloadReport()
      reloadHistory()
      return next
    } catch (err) {
      const original = err instanceof ReportStepError ? err.original : err
      setActionError(original instanceof ApiError
        ? original
        : new ApiError(0, 'UNKNOWN', 'Thao tác không thành công.'))
      if (err instanceof ReportStepError) {
        // Trạng thái ca đã đổi trên máy chủ. Chỉ tải lại lịch sử — tải lại báo cáo sẽ làm form
        // ghi đè nội dung bác sĩ vừa gõ mà chưa lưu được.
        reloadHistory()
        onStatusChangedRef.current?.(err.reachedStatus)
      }
      throw original
    } finally {
      setSaving(false)
    }
  }, [reloadReport, reloadHistory])

  const saveDraft = useCallback(async (
    draft: ReportDraft,
    currentStatus?: StudyStatus,
  ): Promise<StudyStatus | undefined> => {
    if (studyId == null) return undefined
    return run(() => performSaveDraft(reportApi, studyId, draft, currentStatus))
  }, [studyId, run])

  const transition = useCallback(async (
    to: StudyStatus,
    draft: ReportDraft,
    reason: string | null,
    currentStatus?: StudyStatus,
  ): Promise<StudyStatus | undefined> => {
    if (studyId == null) return undefined
    return run(() => performTransition(reportApi, studyId, to, draft, reason, currentStatus))
  }, [studyId, run])

  const reload = useCallback(() => {
    reloadReport()
    reloadHistory()
  }, [reloadReport, reloadHistory])

  return {
    report: report.data ?? null,
    history: history.data ?? [],
    loading: report.loading || history.loading,
    saving,
    error: actionError ?? report.error,
    saveDraft,
    transition,
    reload,
  }
}
