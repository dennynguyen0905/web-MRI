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

  const run = useCallback(async (fn: () => Promise<void>) => {
    setSaving(true)
    setActionError(null)
    try {
      await fn()
    } catch (err) {
      setActionError(err instanceof ApiError ? err : new ApiError(0, 'UNKNOWN', 'Thao tác không thành công.'))
      throw err
    } finally {
      setSaving(false)
    }
  }, [])

  const saveDraft = useCallback(async (draft: ReportDraft) => {
    if (studyId == null) return
    await run(async () => {
      await reportApi.saveReportDraft(studyId, draft)
      report.reload()
    })
  }, [studyId, run, report])

  const transition = useCallback(async (to: StudyStatus, draft: ReportDraft, reason: string | null) => {
    if (studyId == null) return
    await run(async () => {
      // Lưu nội dung trước khi chốt, để bản chính thức mang đúng những gì bác sĩ vừa gõ.
      if (to === 'READ') {
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
