import { useCallback, useRef, useState } from 'react'
import type { ReportDraft } from '../components/modals/ReportModal'
import { WORKLIST } from '../data/mock'
import type { AppUser, Report, ReportStatus, StatusHistoryEntry, StudyStatus, WorklistRow } from './types'

/**
 * Trạng thái ca chụp và báo cáo được giữ trong bộ nhớ phiên làm việc.
 * Khi backend sẵn sàng, mỗi hàm dưới đây đổi thành một lời gọi API tương ứng
 * (PATCH /studies/{id}/status, PUT /studies/{id}/report) và state chỉ còn là cache.
 */
export function useWorklistStore(user: AppUser | null) {
  const [rows, setRows] = useState<WorklistRow[]>(() => WORKLIST)
  const [reports, setReports] = useState<Record<number, Report>>({})
  const [history, setHistory] = useState<Record<number, StatusHistoryEntry[]>>({})
  const seqRef = useRef(1)

  const actorName = user?.fullName ?? 'Hệ thống'

  const saveDraft = useCallback((studyId: number, draft: ReportDraft) => {
    setReports(prev => ({ ...prev, [studyId]: upsert(prev[studyId], studyId, draft, 'DRAFT', actorName) }))
  }, [actorName])

  const transition = useCallback(
    (studyId: number, to: StudyStatus, draft: ReportDraft, reason: string | null) => {
      const now = new Date().toISOString()
      const from = rows.find(r => r.study.id === studyId)?.study.status ?? null
      if (from === to) return

      setRows(prev =>
        prev.map(r => (r.study.id === studyId ? { ...r, study: applyStatus(r.study, to, actorName, now) } : r)),
      )

      setReports(prev => {
        const next = reportStatusFor(from, to)
        if (!next && !prev[studyId]) return prev
        const base = upsert(prev[studyId], studyId, draft, next ?? prev[studyId]?.status ?? 'DRAFT', actorName)
        return {
          ...prev,
          [studyId]: to === 'DUYET_XONG'
            ? { ...base, approvedByName: actorName, approvedAt: now }
            : base,
        }
      })

      const entry: StatusHistoryEntry = {
        id: seqRef.current++, studyId, fromStatus: from, toStatus: to,
        changedByName: actorName, changedAt: now, reason, note: null,
      }
      setHistory(prev => ({ ...prev, [studyId]: [entry, ...(prev[studyId] ?? [])] }))
    },
    [actorName, rows],
  )

  return { rows, reports, history, saveDraft, transition }
}

/* ----------------------------------------------------------------- Chi tiết */

function applyStatus(study: WorklistRow['study'], to: StudyStatus, actor: string, now: string) {
  return {
    ...study,
    status: to,
    startedAt: to === 'DANG_DOC' && !study.startedAt ? now : study.startedAt,
    completedAt: to === 'CHUA_DOC' && !study.completedAt ? now : study.completedAt,
    readingUserName: to === 'DOC_XONG' ? actor : study.readingUserName,
    approvingUserName: to === 'DUYET_XONG' ? actor : study.approvingUserName,
  }
}

/**
 * Trạng thái báo cáo đi kèm mỗi bước của state machine.
 * Chỉ nhánh "trả lại để sửa" (Đang duyệt → Đang đọc) mới là RETURNED;
 * mở ca từ Chưa đọc hay mở lại ca đã duyệt đều quay về bản nháp.
 */
function reportStatusFor(from: StudyStatus | null, to: StudyStatus): ReportStatus | null {
  if (to === 'DOC_XONG') return 'FINAL'
  if (to === 'DUYET_XONG') return 'APPROVED'
  if (to === 'DANG_DOC') return from === 'DANG_DUYET' ? 'RETURNED' : 'DRAFT'
  return null
}

function upsert(
  existing: Report | undefined,
  studyId: number,
  draft: ReportDraft,
  status: ReportStatus,
  actor: string,
): Report {
  if (existing) {
    // Version chỉ tăng khi báo cáo bị trả lại rồi được chốt chính thức lần nữa;
    // nháp → chính thức và chính thức → duyệt vẫn là cùng một bản.
    const bump = existing.status === 'RETURNED' && status === 'FINAL'
    return { ...existing, ...draft, status, version: bump ? existing.version + 1 : existing.version }
  }
  return {
    id: studyId,
    studyId,
    version: 1,
    status,
    origin: 'MANUAL',
    ...draft,
    createdByName: actor,
    createdAt: new Date().toISOString(),
    approvedByName: null,
    approvedAt: null,
  }
}
