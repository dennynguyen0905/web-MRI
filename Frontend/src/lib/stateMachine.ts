import type { Permission } from './auth'
import type { StudyStatus } from './types'

/**
 * State machine 6 trạng thái (IN-06), dựng theo diagrams/05-state-machine.mmd.
 * Các nhánh có dấu (?) trong sơ đồ — bỏ dở, trả lại để sửa, mở lại — được coi là có thật
 * và đánh dấu `exceptional` để tách khỏi luồng xuôi.
 */
export interface Transition {
  from: StudyStatus
  to: StudyStatus
  label: string
  permission: Permission
  /** Nhánh ngoại lệ: cần nhập lý do, hiển thị kiểu cảnh báo. */
  exceptional?: boolean
}

export const TRANSITIONS: Transition[] = [
  { from: 'CHO_THUC_HIEN', to: 'CHUA_DOC', label: 'Xác nhận đã nhận đủ ảnh', permission: 'study.markReceived' },
  { from: 'CHUA_DOC', to: 'DANG_DOC', label: 'Mở ca để đọc', permission: 'report.write' },
  { from: 'DANG_DOC', to: 'DOC_XONG', label: 'Lưu báo cáo chính thức', permission: 'report.write' },
  { from: 'DANG_DOC', to: 'CHUA_DOC', label: 'Bỏ dở', permission: 'report.write', exceptional: true },
  { from: 'DOC_XONG', to: 'DANG_DUYET', label: 'Mở ca để duyệt', permission: 'report.approve' },
  { from: 'DANG_DUYET', to: 'DUYET_XONG', label: 'Duyệt', permission: 'report.approve' },
  { from: 'DANG_DUYET', to: 'DANG_DOC', label: 'Trả lại để sửa', permission: 'report.approve', exceptional: true },
  { from: 'DUYET_XONG', to: 'DANG_DOC', label: 'Mở lại ca', permission: 'study.reopen', exceptional: true },
]

export function transitionsFrom(status: StudyStatus): Transition[] {
  return TRANSITIONS.filter(t => t.from === status)
}

/** Ca đã chốt kết quả thì không cho sửa nội dung báo cáo nữa. */
export function canEditReport(status: StudyStatus): boolean {
  return status === 'DANG_DOC' || status === 'CHUA_DOC'
}
