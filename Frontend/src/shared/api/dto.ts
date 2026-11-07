/**
 * DTO đúng như `doc/api/openapi.yaml` trả về: JSON snake_case.
 * Chỉ dùng trong `shared/api` và `features/<ten>/api`; phần còn lại của app dùng kiểu miền
 * ở `shared/lib/types.ts`. Đổi tên trường ở `mappers.ts`.
 */
import type {
  DobPrecision, Gender, Modality, OrderSource, PatientClass, ReportOrigin, ReportStatus,
  RoleCode, StudyStatus, SyncStatus,
} from '../lib/types'

export interface UserRefDto {
  id: number
  username?: string
  full_name: string
}

export interface CurrentUserDto {
  id: number
  username: string
  full_name: string
  email?: string
  department_id?: number | null
  roles: RoleCode[]
}

export interface LoginResponseDto {
  access_token: string
  token_type: string
  expires_in: number
  user: CurrentUserDto
}

export interface DepartmentDto {
  id: number
  code: string
  name: string
  parent_id: number | null
  type: 'KHOA' | 'NHOM'
  sort_order: number
}

export interface DeviceDto {
  id: number
  code: string
  name: string
  modality: Modality
  department_id: number
  ae_title?: string | null
}

export interface ServiceItemDto {
  id: number
  code: string
  name: string
  modality: Modality
  body_part: string
}

export interface PatientDto {
  id: number
  patient_code: string
  his_patient_id?: string | null
  full_name: string
  date_of_birth?: string | null
  dob_precision: DobPrecision
  age?: number | null
  age_approximate?: boolean
  gender: Gender
  social_insurance_no?: string | null
  phone?: string | null
  address?: string | null
  health_check_code?: string | null
}

export interface ImagingOrderDto {
  id: number
  order_code: string
  patient_id: number
  visit_code?: string | null
  source: OrderSource
  his_order_id?: string | null
  sync_status: SyncStatus
  ordering_department_id: number
  ordering_doctor_name: string
  patient_class: PatientClass
  room?: string | null
  bed?: string | null
  clinical_diagnosis?: string | null
  ordered_at: string
}

export interface StudyDto {
  id: number
  accession_number: string
  order_id: number
  service_item_id: number
  device_id?: number | null
  status: StudyStatus
  study_instance_uid?: string | null
  scheduled_at: string
  started_at?: string | null
  completed_at?: string | null
  due_at?: string | null
  performed_by?: UserRefDto | null
  reading_user?: UserRefDto | null
  approving_user?: UserRefDto | null
}

export interface ReportSummaryDto {
  version: number
  status: ReportStatus
  technique?: string | null
  conclusion?: string | null
}

export interface ReportDto {
  id: number
  study_id: number
  version: number
  status: ReportStatus
  origin: ReportOrigin
  technique?: string | null
  findings?: string | null
  diagnosis?: string | null
  conclusion?: string | null
  recommendation?: string | null
  created_by: UserRefDto
  created_at: string
  approved_by?: UserRefDto | null
  approved_at?: string | null
}

export interface ReportWriteDto {
  technique?: string | null
  findings?: string | null
  diagnosis?: string | null
  conclusion?: string | null
  recommendation?: string | null
}

export interface StatusHistoryEntryDto {
  id: number
  study_id: number
  from_status?: StudyStatus | null
  to_status: StudyStatus
  changed_by: UserRefDto
  changed_at: string
  reason?: string | null
  note?: string | null
}

export interface TransitionRequestDto {
  to_status: StudyStatus
  reason?: string | null
  note?: string | null
}

export interface TransitionResultDto {
  study: StudyDto
  history_entry: StatusHistoryEntryDto
  report?: ReportDto | null
}

export interface WorklistRowDto {
  study: StudyDto
  order: ImagingOrderDto
  patient: PatientDto
  service_item: ServiceItemDto
  device?: DeviceDto | null
  department?: DepartmentDto | null
  report_summary?: ReportSummaryDto | null
}

export interface PageDto<T> {
  page: number
  page_size: number
  total: number
  items: T[]
}

export interface StatusCountsDto {
  total: number
  by_status: Record<string, number>
}

export interface ClinicalSnapshotDto {
  order_id: number
  source: 'SEED' | 'HIS'
  fetched_at: string
  payload: Record<string, unknown>
}

/** Tham số của `GET /studies` (openapi: operationId `listStudies`). */
export interface WorklistQueryDto {
  tab?: 'PENDING' | 'DONE'
  time_field?: string
  date_from?: string
  date_to?: string
  status?: StudyStatus[]
  department_id?: number[]
  device_id?: number[]
  modality?: Modality
  patient_class?: PatientClass
  patient_code?: string
  full_name?: string
  visit_code?: string
  order_code?: string
  page?: number
  page_size?: number
  sort?: string
}
