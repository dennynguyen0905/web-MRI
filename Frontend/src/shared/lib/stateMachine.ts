import type { Permission } from './auth'
import type { StudyStatus } from './types'

/**
 * State machine ca chụp (IN-06), mã trạng thái theo ERD.
 * Nguồn: `doc/03-state-machine.md` mục 2.2 và 2.5, đối chiếu `doc/06-rbac-matrix.md` mục 4 (T1–T10).
 * Backend luôn kiểm lại — Frontend chỉ ẩn/khóa nút (`doc/06` mục 1).
 *
 * T10 (`SCHEDULED → CANCELLED`) chưa có ở đây vì `CANCELLED` còn ❓, chưa nằm trong
 * `enu.StudyStatus` của backend.
 */
export interface Transition {
  code: string
  from: StudyStatus
  to: StudyStatus
  label: string
  permission: Permission
  /** Nhánh ngoại lệ: bắt buộc nhập lý do, hiển thị kiểu cảnh báo. */
  exceptional?: boolean
}

export const TRANSITIONS: Transition[] = [
  { code: 'T2', from: 'SCHEDULED', to: 'UNREAD', label: 'Xác nhận đã nhận đủ ảnh', permission: 'study.markReceived' },
  { code: 'T3', from: 'UNREAD', to: 'READING', label: 'Mở ca để đọc', permission: 'report.write' },
  { code: 'T5', from: 'READING', to: 'READ', label: 'Lưu báo cáo chính thức', permission: 'report.write' },
  { code: 'T4', from: 'READING', to: 'UNREAD', label: 'Bỏ dở', permission: 'report.write', exceptional: true },
  { code: 'T6', from: 'READ', to: 'APPROVING', label: 'Mở ca để duyệt', permission: 'report.approve' },
  { code: 'T7', from: 'APPROVING', to: 'APPROVED', label: 'Duyệt', permission: 'report.approve' },
  { code: 'T8', from: 'APPROVING', to: 'READING', label: 'Trả lại để sửa', permission: 'report.approve', exceptional: true },
  { code: 'T9', from: 'APPROVED', to: 'READING', label: 'Mở lại ca', permission: 'study.reopen', exceptional: true },
]

export function transitionsFrom(status: StudyStatus): Transition[] {
  return TRANSITIONS.filter(t => t.from === status)
}

/** Nội dung báo cáo chỉ sửa được khi ca đang đọc (`PUT …/reports/current` yêu cầu `READING`). */
export function canEditReport(status: StudyStatus): boolean {
  return status === 'READING' || status === 'UNREAD'
}
