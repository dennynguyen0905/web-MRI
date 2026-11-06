/**
 * Mô hình miền của Frontend. Tên trường viết camelCase cho hợp JS, còn **giá trị** của các
 * enum bám đúng mã trong DB/API (`doc/02-erd.md` mục 4.5, `doc/api/openapi.yaml`).
 * Ánh xạ snake_case ↔ camelCase nằm ở `shared/api/mappers.ts`, không rải trong component.
 */

/** 6 mã trạng thái ca chụp theo ERD. `CANCELLED` còn ❓ nên chưa đưa vào (khớp `enu.StudyStatus` của backend). */
export type StudyStatus = 'SCHEDULED' | 'UNREAD' | 'READING' | 'READ' | 'APPROVING' | 'APPROVED'

export type Modality = 'CT' | 'MR' | 'CR' | 'DX' | 'US'
export type Gender = 'M' | 'F' | 'O' | 'U'
export type DobPrecision = 'DAY' | 'MONTH' | 'YEAR'
export type PatientClass = 'INPATIENT' | 'OUTPATIENT'
export type SyncStatus = 'NOT_SYNCED' | 'SYNCED' | 'RESULT_SENT' | 'FAILED'
export type OrderSource = 'MANUAL' | 'HIS'
export type RoleCode = 'ADMIN' | 'CLINICIAN' | 'TECHNICIAN' | 'RADIOLOGIST' | 'APPROVER'
export type ReportStatus = 'DRAFT' | 'FINAL' | 'APPROVED' | 'RETURNED'
export type ReportOrigin = 'MANUAL' | 'AI_DRAFT' | 'AI_DRAFT_EDITED'

/** Loại mốc thời gian dùng để lọc worklist (`time_field` của `GET /studies`). */
export type TimeField = 'SCHEDULED' | 'ORDERED' | 'COMPLETED'

export type WorklistTab = 'PENDING' | 'DONE'

export interface UserRef {
  id: number
  username?: string
  fullName: string
}

export interface AppUser {
  id: number
  username: string
  fullName: string
  email: string
  departmentId: number | null
  roles: RoleCode[]
}

export interface Department {
  id: number
  code: string
  name: string
  parentId: number | null
  type: 'KHOA' | 'NHOM'
  sortOrder: number
}

export interface Device {
  id: number
  code: string
  name: string
  modality: Modality
  departmentId: number
  aeTitle: string | null
}

export interface ServiceItem {
  id: number
  code: string
  name: string
  modality: Modality
  bodyPart: string
}

export interface Patient {
  id: number
  patientCode: string
  hisPatientId: string | null
  fullName: string
  dateOfBirth: string | null
  dobPrecision: DobPrecision
  /** Backend tính sẵn; `null` khi chưa có ngày sinh. */
  age: number | null
  /** `true` khi `dobPrecision` là YEAR hoặc MONTH → hiển thị dạng xấp xỉ "~68". */
  ageApproximate: boolean
  gender: Gender
  socialInsuranceNo: string | null
  phone: string | null
  address: string | null
  healthCheckCode: string | null
}

export interface ImagingOrder {
  id: number
  orderCode: string
  patientId: number
  visitCode: string | null
  source: OrderSource
  hisOrderId: string | null
  syncStatus: SyncStatus
  orderingDepartmentId: number
  orderingDoctorName: string
  patientClass: PatientClass
  room: string | null
  bed: string | null
  clinicalDiagnosis: string | null
  orderedAt: string
}

export interface Study {
  id: number
  accessionNumber: string
  orderId: number
  serviceItemId: number
  deviceId: number | null
  status: StudyStatus
  /** Khóa để mở OHIF bằng deep-link. API không bao giờ trả ảnh (R-02). */
  studyInstanceUid: string | null
  scheduledAt: string
  startedAt: string | null
  completedAt: string | null
  dueAt: string | null
  performedBy: UserRef | null
  readingUser: UserRef | null
  approvingUser: UserRef | null
}

/** Tóm tắt báo cáo hiện hành, nguồn duy nhất của kỹ thuật và kết luận (`02-erd.md` mục 10). */
export interface ReportSummary {
  version: number
  status: ReportStatus
  technique: string | null
  conclusion: string | null
}

export interface Report {
  id: number
  studyId: number
  version: number
  status: ReportStatus
  origin: ReportOrigin
  technique: string | null
  findings: string | null
  diagnosis: string | null
  conclusion: string | null
  recommendation: string | null
  createdBy: UserRef
  createdAt: string
  approvedBy: UserRef | null
  approvedAt: string | null
}

export interface StatusHistoryEntry {
  id: number
  studyId: number
  fromStatus: StudyStatus | null
  toStatus: StudyStatus
  changedBy: UserRef
  changedAt: string
  reason: string | null
  note: string | null
}

/** Một hàng worklist, backend ghép sẵn các bảng liên quan (`WorklistRow` trong openapi). */
export interface WorklistRow {
  study: Study
  order: ImagingOrder
  patient: Patient
  service: ServiceItem
  device: Device | null
  department: Department | null
  reportSummary: ReportSummary | null
}

export interface PageMeta {
  page: number
  pageSize: number
  total: number
}

export interface Page<T> extends PageMeta {
  items: T[]
}

export interface StatusCounts {
  total: number
  byStatus: Record<StudyStatus, number>
}
