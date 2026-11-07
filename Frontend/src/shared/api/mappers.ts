/** Đổi DTO snake_case của API sang kiểu miền camelCase. Một chiều: API → app. */
import type {
  AppUser, Department, Device, ImagingOrder, Page, Patient, Report, ReportSummary, ServiceItem,
  StatusCounts, StatusHistoryEntry, Study, StudyStatus, UserRef, WorklistRow,
} from '../lib/types'
import { STATUS_ORDER } from '../lib/status'
import type {
  CurrentUserDto, DepartmentDto, DeviceDto, ImagingOrderDto, PageDto, PatientDto, ReportDto,
  ReportSummaryDto, ServiceItemDto, StatusCountsDto, StatusHistoryEntryDto, StudyDto, UserRefDto,
  WorklistRowDto,
} from './dto'

const nz = <T>(v: T | undefined | null): T | null => (v === undefined ? null : v)

export const toUserRef = (d: UserRefDto): UserRef => ({
  id: d.id,
  username: d.username,
  fullName: d.full_name,
})

const toUserRefOrNull = (d: UserRefDto | null | undefined): UserRef | null =>
  d == null ? null : toUserRef(d)

export const toAppUser = (d: CurrentUserDto): AppUser => ({
  id: d.id,
  username: d.username,
  fullName: d.full_name,
  email: d.email ?? '',
  departmentId: nz(d.department_id),
  roles: d.roles,
})

export const toDepartment = (d: DepartmentDto): Department => ({
  id: d.id,
  code: d.code,
  name: d.name,
  parentId: nz(d.parent_id),
  type: d.type,
  sortOrder: d.sort_order,
})

export const toDevice = (d: DeviceDto): Device => ({
  id: d.id,
  code: d.code,
  name: d.name,
  modality: d.modality,
  departmentId: d.department_id,
  aeTitle: nz(d.ae_title),
})

export const toServiceItem = (d: ServiceItemDto): ServiceItem => ({
  id: d.id,
  code: d.code,
  name: d.name,
  modality: d.modality,
  bodyPart: d.body_part,
})

export const toPatient = (d: PatientDto): Patient => ({
  id: d.id,
  patientCode: d.patient_code,
  hisPatientId: nz(d.his_patient_id),
  fullName: d.full_name,
  dateOfBirth: nz(d.date_of_birth),
  dobPrecision: d.dob_precision,
  age: nz(d.age),
  // Backend suy ra từ dob_precision; nếu thiếu thì tự suy để không hiển thị tuổi chính xác giả.
  ageApproximate: d.age_approximate ?? d.dob_precision !== 'DAY',
  gender: d.gender,
  socialInsuranceNo: nz(d.social_insurance_no),
  phone: nz(d.phone),
  address: nz(d.address),
  healthCheckCode: nz(d.health_check_code),
})

export const toImagingOrder = (d: ImagingOrderDto): ImagingOrder => ({
  id: d.id,
  orderCode: d.order_code,
  patientId: d.patient_id,
  visitCode: nz(d.visit_code),
  source: d.source,
  hisOrderId: nz(d.his_order_id),
  syncStatus: d.sync_status,
  orderingDepartmentId: d.ordering_department_id,
  orderingDoctorName: d.ordering_doctor_name,
  patientClass: d.patient_class,
  room: nz(d.room),
  bed: nz(d.bed),
  clinicalDiagnosis: nz(d.clinical_diagnosis),
  orderedAt: d.ordered_at,
})

export const toStudy = (d: StudyDto): Study => ({
  id: d.id,
  accessionNumber: d.accession_number,
  orderId: d.order_id,
  serviceItemId: d.service_item_id,
  deviceId: nz(d.device_id),
  status: d.status,
  studyInstanceUid: nz(d.study_instance_uid),
  scheduledAt: d.scheduled_at,
  startedAt: nz(d.started_at),
  completedAt: nz(d.completed_at),
  dueAt: nz(d.due_at),
  performedBy: toUserRefOrNull(d.performed_by),
  readingUser: toUserRefOrNull(d.reading_user),
  approvingUser: toUserRefOrNull(d.approving_user),
})

export const toReportSummary = (d: ReportSummaryDto | null | undefined): ReportSummary | null =>
  d == null ? null : {
    version: d.version,
    status: d.status,
    technique: nz(d.technique),
    conclusion: nz(d.conclusion),
  }

export const toReport = (d: ReportDto): Report => ({
  id: d.id,
  studyId: d.study_id,
  version: d.version,
  status: d.status,
  origin: d.origin,
  technique: nz(d.technique),
  findings: nz(d.findings),
  diagnosis: nz(d.diagnosis),
  conclusion: nz(d.conclusion),
  recommendation: nz(d.recommendation),
  createdBy: toUserRef(d.created_by),
  createdAt: d.created_at,
  approvedBy: toUserRefOrNull(d.approved_by),
  approvedAt: nz(d.approved_at),
})

export const toStatusHistoryEntry = (d: StatusHistoryEntryDto): StatusHistoryEntry => ({
  id: d.id,
  studyId: d.study_id,
  fromStatus: nz(d.from_status),
  toStatus: d.to_status,
  changedBy: toUserRef(d.changed_by),
  changedAt: d.changed_at,
  reason: nz(d.reason),
  note: nz(d.note),
})

export const toWorklistRow = (d: WorklistRowDto): WorklistRow => ({
  study: toStudy(d.study),
  order: toImagingOrder(d.order),
  patient: toPatient(d.patient),
  service: toServiceItem(d.service_item),
  device: d.device == null ? null : toDevice(d.device),
  department: d.department == null ? null : toDepartment(d.department),
  reportSummary: toReportSummary(d.report_summary),
})

export const toPage = <D, T>(d: PageDto<D>, map: (item: D) => T): Page<T> => ({
  page: d.page,
  pageSize: d.page_size,
  total: d.total,
  items: d.items.map(map),
})

export const toStatusCounts = (d: StatusCountsDto): StatusCounts => {
  const byStatus = Object.fromEntries(
    STATUS_ORDER.map(s => [s, d.by_status[s] ?? 0]),
  ) as Record<StudyStatus, number>
  return { total: d.total, byStatus }
}
