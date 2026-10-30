import type { StudyStatus } from './types'

export interface StatusMeta {
  key: StudyStatus
  label: string
  color: string
  bg: string
  /** Hình dáng chấm trạng thái trong danh sách bộ lọc. */
  shape: 'clock' | 'dot' | 'ring' | 'check' | 'square'
}

export const STATUS_META: Record<StudyStatus, StatusMeta> = {
  CHO_THUC_HIEN: { key: 'CHO_THUC_HIEN', label: 'Chờ thực hiện', color: 'var(--color-st-cho)', bg: '#f3f4f6', shape: 'clock' },
  CHUA_DOC: { key: 'CHUA_DOC', label: 'Chưa đọc', color: 'var(--color-st-chuadoc)', bg: '#fdecec', shape: 'square' },
  DANG_DOC: { key: 'DANG_DOC', label: 'Đang đọc', color: 'var(--color-st-dangdoc)', bg: '#e8f0fe', shape: 'dot' },
  DOC_XONG: { key: 'DOC_XONG', label: 'Đọc xong', color: 'var(--color-st-docxong)', bg: '#e3f5f9', shape: 'dot' },
  DANG_DUYET: { key: 'DANG_DUYET', label: 'Đang duyệt', color: 'var(--color-st-dangduyet)', bg: '#fdf4e3', shape: 'ring' },
  DUYET_XONG: { key: 'DUYET_XONG', label: 'Duyệt xong', color: 'var(--color-st-duyetxong)', bg: '#e6f5ef', shape: 'check' },
}

export const STATUS_ORDER: StudyStatus[] = [
  'CHO_THUC_HIEN', 'CHUA_DOC', 'DANG_DOC', 'DOC_XONG', 'DANG_DUYET', 'DUYET_XONG',
]

/** Tab "Chờ" chỉ gồm ca chưa chụp xong; còn lại nằm ở tab "Đã thực hiện". */
export const PENDING_STATUSES: StudyStatus[] = ['CHO_THUC_HIEN']
