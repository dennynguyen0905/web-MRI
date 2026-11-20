import { useCallback, useState } from 'react'
import { ApiError } from '@/shared/api/http'
import { useAsync } from '@/shared/hooks/useAsync'
import type { Report, StatusHistoryEntry, StudyStatus } from '@/shared/lib/types'
import * as reportApi from '../api/reportApi'

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

const hasAnyDraftContent = (d: ReportDraft): boolean =>
  Object.values(d).some(v => v.trim() !== '')

/** Báo cáo hiện hành + lịch sử trạng thái của một ca, kèm hai thao tác ghi. */
export function useStudyReport(studyId: number | null, enabled: boolean) {
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

  const run = useCallback(async <T>(fn: () => Promise<T>): Promise<T> => {
    setSaving(true)
    setActionError(null)
    try {
      return await fn()
    } catch (err) {
      setActionError(err instanceof ApiError ? err : new ApiError(0, 'UNKNOWN', 'Thao tác không thành công.'))
      throw err
    } finally {
      setSaving(false)
    }
  }, [])

  const saveDraft = useCallback(async (
    draft: ReportDraft,
    currentStatus?: StudyStatus,
  ): Promise<StudyStatus | undefined> => {
    if (studyId == null) return undefined
    return run(async () => {
      let nextStatus = currentStatus
      // Nếu bác sĩ nhập nháp ngay khi ca còn ở UNREAD thì tự động mở ca (T3: UNREAD -> READING) trước khi lưu.
      if (currentStatus === 'UNREAD') {
        await reportApi.transitionStudy(studyId, 'READING', null)
        nextStatus = 'READING'
      }
      await reportApi.saveReportDraft(studyId, draft)
      report.reload()
      history.reload()
      return nextStatus
    })
  }, [studyId, run, report, history])

  const transition = useCallback(async (
    to: StudyStatus,
    draft: ReportDraft,
    reason: string | null,
    currentStatus?: StudyStatus,
  ): Promise<StudyStatus | undefined> => {
    if (studyId == null) return undefined
    return run(async () => {
      // Nếu mở ca để đọc (T3: UNREAD -> READING) mà bác sĩ đã gõ sẵn nội dung trên form thì lưu luôn sau khi mở.
      if (to === 'READING' && currentStatus === 'UNREAD') {
        await reportApi.transitionStudy(studyId, 'READING', reason)
        if (hasAnyDraftContent(draft)) {
          await reportApi.saveReportDraft(studyId, draft)
        }
        report.reload()
        history.reload()
        return 'READING'
      }

      // Lưu nội dung trước khi chốt (T5: -> READ), để bản chính thức mang đúng những gì bác sĩ vừa gõ.
      if (to === 'READ') {
        if (currentStatus === 'UNREAD') {
          await reportApi.transitionStudy(studyId, 'READING', null)
        }
        try {
          await reportApi.saveReportDraft(studyId, draft)
        } catch (err) {
          // Ca không còn ở READING thì bước chuyển bên dưới sẽ báo lỗi rõ hơn.
          if (!(err instanceof ApiError) || err.status !== 409) throw err
        }
      }

      await reportApi.transitionStudy(studyId, to, reason)
      report.reload()
      history.reload()
      return to
    })
  }, [studyId, run, report, history])

  return {
    report: report.data ?? null,
    history: history.data ?? [],
    loading: report.loading || history.loading,
    saving,
    error: actionError ?? report.error,
    saveDraft,
    transition,
    reload: () => {
      report.reload()
      history.reload()
    },
  }
}
