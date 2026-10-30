/** Các kiểu dữ liệu bám sát ERD backend (diagrams/02-erd.mmd) để sau này thay mock bằng API là xong. */

export type StudyStatus =
  | 'CHO_THUC_HIEN'
  | 'CHUA_DOC'
  | 'DANG_DOC'
  | 'DOC_XONG'
  | 'DANG_DUYET'
  | 'DUYET_XONG'

export type Modality = 'CT' | 'MR' | 'CR' | 'DX' | 'US'
export type Gender = 'M' | 'F' | 'O' | 'U'
export type DobPrecision = 'DAY' | 'MONTH' | 'YEAR'
export type PatientClass = 'INPATIENT' | 'OUTPATIENT'
export type SyncStatus = 'NOT_SYNCED' | 'SYNCED' | 'RESULT_SENT' | 'FAILED'

export type RoleCode = 'ADMIN' | 'CLINICIAN' | 'TECHNICIAN' | 'RADIOLOGIST' | 'APPROVER'
export type ReportStatus = 'DRAFT' | 'FINAL' | 'APPROVED' | 'RETURNED'
export type ReportOrigin = 'MANUAL' | 'AI_DRAFT' | 'AI_DRAFT_EDITED'

export interface AppUser {
  id: number
  username: string
  fullName: string
  email: string
  departmentId: number | null
  roles: RoleCode[]
  isActive: boolean
}

/** Bảng REPORT trong ERD — 5 trường nội dung theo IN-10. */
export interface Report {
  id: number
  studyId: number
  version: number
  status: ReportStatus
  origin: ReportOrigin
  technique: string
  findings: string
  diagnosis: string
  conclusion: string
  recommendation: string
  createdByName: string
  createdAt: string
  approvedByName: string | null
  approvedAt: string | null
}

/** Bảng STUDY_STATUS_HISTORY trong ERD (IN-06). */
export interface StatusHistoryEntry {
  id: number
  studyId: number
  fromStatus: StudyStatus | null
  toStatus: StudyStatus
  changedByName: string
  changedAt: string
  reason: string | null
  note: string | null
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
  gender: Gender
  socialInsuranceNo: string | null
  phone: string
  address: string
  healthCheckCode: string | null
}

export interface ImagingOrder {
  id: number
  orderCode: string
  patientId: number
  visitCode: string | null
  source: 'MANUAL' | 'HIS'
  syncStatus: SyncStatus
  orderingDepartmentId: number
  orderingDoctorName: string
  patientClass: PatientClass
  room: string
  bed: string
  clinicalDiagnosis: string
  orderedAt: string
}

export interface Study {
  id: number
  accessionNumber: string
  orderId: number
  serviceItemId: number
  deviceId: number | null
  status: StudyStatus
  studyInstanceUid: string | null
  scheduledAt: string
  startedAt: string | null
  completedAt: string | null
  dueAt: string | null
  performedByName: string | null
  readingUserName: string | null
  approvingUserName: string | null
  technique: string | null
  conclusion: string | null
  bhytViolation: boolean
}

/** Một dòng worklist đã ghép sẵn (backend trả về dạng view này). */
export interface WorklistRow {
  study: Study
  order: ImagingOrder
  patient: Patient
  service: ServiceItem
  device: Device | null
  department: Department | null
}
