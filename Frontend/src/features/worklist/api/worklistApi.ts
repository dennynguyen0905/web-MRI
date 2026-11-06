import { api } from '@/shared/api/client'
import type {
  DepartmentDto, DeviceDto, PageDto, ServiceItemDto, StatusCountsDto, WorklistQueryDto,
  WorklistRowDto,
} from '@/shared/api/dto'
import { toDepartment, toDevice, toPage, toServiceItem, toStatusCounts, toWorklistRow } from '@/shared/api/mappers'
import type {
  Department, Device, Modality, Page, PatientClass, ServiceItem, StatusCounts, StudyStatus,
  TimeField, WorklistRow, WorklistTab,
} from '@/shared/lib/types'

/** Bộ lọc worklist ở dạng kiểu miền; đổi sang query snake_case ngay dưới đây. */
export interface WorklistFilters {
  /** Bỏ trống nghĩa là lấy cả hai tab — dùng cho bảng "ca khác của cùng bệnh nhân". */
  tab: WorklistTab | ''
  timeField: TimeField
  dateFrom: string
  dateTo: string
  statuses: StudyStatus[]
  departmentIds: number[]
  deviceIds: number[]
  modality: Modality | ''
  patientClass: PatientClass | ''
  patientCode: string
  fullName: string
  visitCode: string
  orderCode: string
  page: number
  pageSize: number
  sort: string
}

export function toWorklistQuery(f: WorklistFilters): WorklistQueryDto {
  return {
    tab: f.tab || undefined,
    time_field: f.timeField,
    date_from: f.dateFrom || undefined,
    date_to: f.dateTo || undefined,
    status: f.statuses.length ? f.statuses : undefined,
    department_id: f.departmentIds.length ? f.departmentIds : undefined,
    device_id: f.deviceIds.length ? f.deviceIds : undefined,
    modality: f.modality || undefined,
    patient_class: f.patientClass || undefined,
    patient_code: f.patientCode.trim() || undefined,
    full_name: f.fullName.trim() || undefined,
    visit_code: f.visitCode.trim() || undefined,
    order_code: f.orderCode.trim() || undefined,
    page: f.page,
    page_size: f.pageSize,
    sort: f.sort || undefined,
  }
}

/** `GET /studies` — worklist (openapi `listStudies`). */
export async function fetchWorklist(filters: WorklistFilters, signal?: AbortSignal): Promise<Page<WorklistRow>> {
  const dto = await api.request<PageDto<WorklistRowDto>>({
    method: 'GET',
    path: '/studies',
    query: toWorklistQuery(filters) as Record<string, never>,
    signal,
  })
  return toPage(dto, toWorklistRow)
}

/** `GET /studies/status-counts` — số đếm theo nhóm trạng thái (IN-09). */
export async function fetchStatusCounts(signal?: AbortSignal): Promise<StatusCounts> {
  const dto = await api.request<StatusCountsDto>({ method: 'GET', path: '/studies/status-counts', signal })
  return toStatusCounts(dto)
}

/** `GET /studies/{id}` — một hàng worklist đã ghép sẵn. */
export async function fetchStudy(studyId: number, signal?: AbortSignal): Promise<WorklistRow> {
  const dto = await api.request<WorklistRowDto>({ method: 'GET', path: `/studies/${studyId}`, signal })
  return toWorklistRow(dto)
}

export async function fetchDepartments(signal?: AbortSignal): Promise<Department[]> {
  const dto = await api.request<DepartmentDto[]>({ method: 'GET', path: '/departments', signal })
  return dto.map(toDepartment)
}

export async function fetchDevices(signal?: AbortSignal): Promise<Device[]> {
  const dto = await api.request<DeviceDto[]>({ method: 'GET', path: '/devices', signal })
  return dto.map(toDevice)
}

export async function fetchServiceItems(signal?: AbortSignal): Promise<ServiceItem[]> {
  const dto = await api.request<ServiceItemDto[]>({ method: 'GET', path: '/service-items', signal })
  return dto.map(toServiceItem)
}
