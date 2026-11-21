import type { StudyStatus } from '@/shared/lib/types'

/**
 * Chuỗi gọi API của form báo cáo, tách khỏi hook để test được bằng vitest thuần (không cần
 * render React).
 *
 * Các thao tác ở đây gồm nhiều request nối tiếp (ví dụ T3 rồi lưu nháp) và **không nguyên tử**:
 * bước đầu có thể đã đổi trạng thái ca trên máy chủ trước khi bước sau lỗi. Khi đó hàm ném
 * `ReportStepError` mang theo `reachedStatus` — trạng thái ca **thực tế** trên máy chủ — để hook
 * báo cho màn hình, tránh UI vẫn tưởng ca còn ở trạng thái cũ rồi gửi lại bước đã làm (409).
 */

export interface ReportDraftLike {
  technique: string
  findings: string
  diagnosis: string
  conclusion: string
  recommendation: string
}

export interface ReportStepsApi {
  transitionStudy(studyId: number, toStatus: StudyStatus, reason: string | null): Promise<unknown>
  saveReportDraft(studyId: number, draft: ReportDraftLike): Promise<unknown>
}

/** Một bước giữa chừng lỗi sau khi bước trước đã đổi trạng thái ca trên máy chủ. */
export class ReportStepError extends Error {
  readonly reachedStatus: StudyStatus
  readonly original: unknown

  constructor(cause: unknown, reachedStatus: StudyStatus) {
    super(cause instanceof Error ? cause.message : 'Thao tác không thành công.')
    this.name = 'ReportStepError'
    this.original = cause
    this.reachedStatus = reachedStatus
  }
}

export const hasAnyDraftContent = (d: ReportDraftLike): boolean =>
  Object.values(d).some(v => v.trim() !== '')

/**
 * Chạy lần lượt các bước; bước nào lỗi sau khi trạng thái đã đổi thì bọc lỗi bằng
 * `ReportStepError`. Lỗi ngay ở bước đầu (trạng thái chưa đổi) ném nguyên lỗi gốc.
 */
async function runSteps(
  initialStatus: StudyStatus | undefined,
  steps: { run: () => Promise<unknown>; statusAfter?: StudyStatus }[],
): Promise<StudyStatus | undefined> {
  let reached = initialStatus
  for (const step of steps) {
    try {
      await step.run()
    } catch (err) {
      if (reached !== initialStatus && reached !== undefined) throw new ReportStepError(err, reached)
      throw err
    }
    if (step.statusAfter) reached = step.statusAfter
  }
  return reached
}

/** "Lưu nháp": ca còn `UNREAD` thì tự mở ca (T3) trước, vì `PUT …/reports/current` yêu cầu `READING`. */
export function performSaveDraft(
  api: ReportStepsApi,
  studyId: number,
  draft: ReportDraftLike,
  currentStatus: StudyStatus | undefined,
): Promise<StudyStatus | undefined> {
  const steps: { run: () => Promise<unknown>; statusAfter?: StudyStatus }[] = []
  if (currentStatus === 'UNREAD') {
    steps.push({ run: () => api.transitionStudy(studyId, 'READING', null), statusAfter: 'READING' })
  }
  steps.push({ run: () => api.saveReportDraft(studyId, draft) })
  return runSteps(currentStatus, steps)
}

/**
 * Chuyển trạng thái từ form báo cáo.
 * - T3 (`UNREAD → READING`): mở ca, có nội dung đã gõ thì lưu luôn.
 * - T5 (`→ READ`): lưu nội dung trước rồi mới chốt; **lưu lỗi thì không chốt**, để bản chính
 *   thức không mang nội dung cũ khác với những gì bác sĩ vừa gõ.
 */
export function performTransition(
  api: ReportStepsApi,
  studyId: number,
  to: StudyStatus,
  draft: ReportDraftLike,
  reason: string | null,
  currentStatus: StudyStatus | undefined,
): Promise<StudyStatus | undefined> {
  const steps: { run: () => Promise<unknown>; statusAfter?: StudyStatus }[] = []

  if (to === 'READING' && currentStatus === 'UNREAD') {
    steps.push({ run: () => api.transitionStudy(studyId, 'READING', reason), statusAfter: 'READING' })
    if (hasAnyDraftContent(draft)) steps.push({ run: () => api.saveReportDraft(studyId, draft) })
    return runSteps(currentStatus, steps)
  }

  if (to === 'READ') {
    if (currentStatus === 'UNREAD') {
      steps.push({ run: () => api.transitionStudy(studyId, 'READING', null), statusAfter: 'READING' })
    }
    steps.push({ run: () => api.saveReportDraft(studyId, draft) })
  }
  steps.push({ run: () => api.transitionStudy(studyId, to, reason), statusAfter: to })
  return runSteps(currentStatus, steps)
}
