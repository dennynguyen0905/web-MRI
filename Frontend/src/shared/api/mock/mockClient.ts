/**
 * Mock server chạy ngay trong trình duyệt. Cài đúng interface `HttpClient` nên khi backend
 * sẵn sàng chỉ cần đổi `VITE_API_MODE=live` là chạy thật, không sửa component hay hook.
 *
 * Đường dẫn, tham số và hình dạng phản hồi bám `doc/api/openapi.yaml`.
 */
import { ApiError, type HttpClient, type HttpRequest, type QueryValue } from '../http'
import type {
  ClinicalSnapshotDto, LoginResponseDto, PageDto, ReportDto, ReportWriteDto, StatusCountsDto,
  StatusHistoryEntryDto, StudyDto, TransitionRequestDto, TransitionResultDto, WorklistRowDto,
} from '../dto'
import type { ReportStatus, StudyStatus } from '@/shared/lib/types'
import {
  CLINICAL_SNAPSHOTS, DEMO_PASSWORD, DEPARTMENTS, DEVICES, IMAGING_ORDERS, PATIENTS, REPORTS,
  SERVICE_ITEMS, STATUS_HISTORY, STUDIES, USERS, userRef,
} from './seed'

/** Độ trễ giả để giao diện lộ ra trạng thái đang tải, giống gọi mạng thật. */
const LATENCY_MS = 120

const delay = () => new Promise<void>(resolve => setTimeout(resolve, LATENCY_MS))

/* --------------------------------------------------------------- Trạng thái */

// Bản sao có thể sửa: thao tác trong phiên (chuyển trạng thái, lưu nháp) ghi vào đây.
const studies = STUDIES.map(s => ({ ...s }))
const reports = REPORTS.map(r => ({ ...r }))
const history = STATUS_HISTORY.map(h => ({ ...h }))
let historySeq = history.length + 1
let reportSeq = reports.length + 1
let currentUserId: number | null = null

/* ------------------------------------------------------------------ Tiện ích */

function ageOf(dob: string | null, precision: string): { age: number | null; approximate: boolean } {
  if (!dob) return { age: null, approximate: precision !== 'DAY' }
  const d = new Date(dob)
  const now = new Date()
  let age = now.getFullYear() - d.getFullYear()
  if (precision === 'DAY' || precision === 'MONTH') {
    const before =
      now.getMonth() < d.getMonth() ||
      (precision === 'DAY' && now.getMonth() === d.getMonth() && now.getDate() < d.getDate())
    if (before) age -= 1
  }
  return { age: age < 0 ? null : age, approximate: precision !== 'DAY' }
}

function patientDto(id: number) {
  const p = PATIENTS.find(x => x.id === id)!
  const { age, approximate } = ageOf(p.date_of_birth ?? null, p.dob_precision)
  return { ...p, age, age_approximate: approximate }
}

function currentReportOf(studyId: number): ReportDto | undefined {
  return reports
    .filter(r => r.study_id === studyId)
    .sort((a, b) => b.version - a.version)[0]
}

function rowOf(study: StudyDto): WorklistRowDto {
  const order = IMAGING_ORDERS.find(o => o.id === study.order_id)!
  const report = currentReportOf(study.id)
  return {
    study,
    order,
    patient: patientDto(order.patient_id),
    service_item: SERVICE_ITEMS.find(s => s.id === study.service_item_id)!,
    device: study.device_id == null ? null : DEVICES.find(d => d.id === study.device_id)!,
    department: DEPARTMENTS.find(d => d.id === order.ordering_department_id) ?? null,
    report_summary: report
      ? { version: report.version, status: report.status, technique: report.technique, conclusion: report.conclusion }
      : null,
  }
}

/** Khoa cha kéo theo cả nhánh con (openapi: "gồm cả nhánh con"). */
function departmentBranch(ids: number[]): Set<number> {
  const out = new Set<number>(ids)
  let added = true
  while (added) {
    added = false
    for (const d of DEPARTMENTS) {
      if (d.parent_id != null && out.has(d.parent_id) && !out.has(d.id)) {
        out.add(d.id)
        added = true
      }
    }
  }
  return out
}

const asArray = (v: QueryValue): string[] =>
  v == null ? [] : Array.isArray(v) ? v.map(String) : [String(v)]

const asNumbers = (v: QueryValue): number[] => asArray(v).map(Number).filter(n => !Number.isNaN(n))

function timeFieldValue(study: StudyDto, order: { ordered_at: string }, field: string): string | null {
  if (field === 'ORDERED') return order.ordered_at
  if (field === 'COMPLETED') return study.completed_at ?? null
  return study.scheduled_at
}

function requireAuth() {
  if (currentUserId == null) {
    throw new ApiError(401, 'UNAUTHENTICATED', 'Chưa đăng nhập hoặc phiên đã hết hạn.')
  }
}

/* --------------------------------------------------------------- Định tuyến */

function matchPath(pattern: string, path: string): Record<string, string> | null {
  const p = pattern.split('/')
  const a = path.split('/')
  if (p.length !== a.length) return null
  const params: Record<string, string> = {}
  for (let i = 0; i < p.length; i++) {
    if (p[i].startsWith(':')) params[p[i].slice(1)] = decodeURIComponent(a[i])
    else if (p[i] !== a[i]) return null
  }
  return params
}

function listStudies(query: Record<string, QueryValue>): PageDto<WorklistRowDto> {
  // Không truyền `tab` nghĩa là lấy cả hai tab (dùng cho bảng "ca khác của cùng bệnh nhân").
  const tab = query.tab as string | undefined
  const timeField = (query.time_field as string) ?? 'SCHEDULED'
  const statuses = asArray(query.status) as StudyStatus[]
  const deptIds = asNumbers(query.department_id)
  const deviceIds = asNumbers(query.device_id)
  const branch = deptIds.length ? departmentBranch(deptIds) : null

  const dateFrom = query.date_from ? new Date(`${query.date_from}T00:00:00`) : null
  const dateTo = query.date_to ? new Date(`${query.date_to}T23:59:59`) : null

  const text = (v: QueryValue) => (v == null ? '' : String(v).trim().toLowerCase())
  const patientCode = text(query.patient_code)
  const fullName = text(query.full_name)
  const visitCode = text(query.visit_code)
  const orderCode = text(query.order_code)

  let rows = studies.map(rowOf)

  rows = rows.filter(r => {
    const inPending = r.study.status === 'SCHEDULED'
    if (tab === 'PENDING' && !inPending) return false
    if (tab === 'DONE' && inPending) return false

    if (statuses.length && !statuses.includes(r.study.status)) return false
    if (branch && (r.device == null || !branch.has(r.device.department_id)) && !branch.has(r.order.ordering_department_id)) return false
    if (deviceIds.length && (r.study.device_id == null || !deviceIds.includes(r.study.device_id))) return false
    if (query.modality && r.service_item.modality !== query.modality) return false
    if (query.patient_class && r.order.patient_class !== query.patient_class) return false
    if (patientCode && !r.patient.patient_code.toLowerCase().includes(patientCode)) return false
    if (fullName && !r.patient.full_name.toLowerCase().includes(fullName)) return false
    if (visitCode && !(r.order.visit_code ?? '').toLowerCase().includes(visitCode)) return false
    if (orderCode && !r.order.order_code.toLowerCase().includes(orderCode)) return false

    if (dateFrom || dateTo) {
      const raw = timeFieldValue(r.study, r.order, timeField)
      if (!raw) return false
      const at = new Date(raw)
      if (dateFrom && at < dateFrom) return false
      if (dateTo && at > dateTo) return false
    }
    return true
  })

  const sort = (query.sort as string) ?? ''
  if (sort) {
    const [field, dir] = sort.split(',')
    const sign = dir === 'desc' ? -1 : 1
    const pick = (r: WorklistRowDto): string => {
      switch (field) {
        case 'patient_code': return r.patient.patient_code
        case 'full_name': return r.patient.full_name
        case 'status': return r.study.status
        case 'order_code': return r.order.order_code
        case 'ordered_at': return r.order.ordered_at
        case 'completed_at': return r.study.completed_at ?? ''
        default: return r.study.scheduled_at
      }
    }
    rows = [...rows].sort((a, b) => pick(a).localeCompare(pick(b), 'vi', { numeric: true }) * sign)
  }

  const page = Number(query.page ?? 1)
  const pageSize = Number(query.page_size ?? 20)
  return {
    page,
    page_size: pageSize,
    total: rows.length,
    items: rows.slice((page - 1) * pageSize, page * pageSize),
  }
}

function applyTransition(studyId: number, body: TransitionRequestDto): TransitionResultDto {
  const study = studies.find(s => s.id === studyId)
  if (!study) throw new ApiError(404, 'STUDY_NOT_FOUND', 'Không tìm thấy ca chụp.')

  const from = study.status
  const to = body.to_status
  const actor = userRef(currentUserId!)
  const now = new Date().toISOString()

  study.status = to
  if (to === 'READING') {
    study.started_at ??= now
    study.reading_user = actor
  }
  if (to === 'UNREAD' && from === 'READING') study.reading_user = null
  if (to === 'UNREAD' && from === 'SCHEDULED') study.completed_at ??= now
  if (to === 'APPROVING') study.approving_user = actor
  if (to === 'APPROVED') study.approving_user = actor

  const entry: StatusHistoryEntryDto = {
    id: historySeq++,
    study_id: studyId,
    from_status: from,
    to_status: to,
    changed_by: actor,
    changed_at: now,
    reason: body.reason ?? null,
    note: body.note ?? null,
  }
  history.push(entry)

  // Trạng thái báo cáo đi kèm bước chuyển: chỉ nhánh "trả lại" (APPROVING → READING) là RETURNED.
  const nextReportStatus: ReportStatus | null =
    to === 'READ' ? 'FINAL'
      : to === 'APPROVED' ? 'APPROVED'
        : to === 'READING' ? (from === 'APPROVING' ? 'RETURNED' : 'DRAFT')
          : null

  let report = currentReportOf(studyId)
  if (nextReportStatus) {
    if (!report) {
      report = {
        id: reportSeq++, study_id: studyId, version: 1, status: nextReportStatus, origin: 'MANUAL',
        technique: null, findings: null, diagnosis: null, conclusion: null, recommendation: null,
        created_by: actor, created_at: now, approved_by: null, approved_at: null,
      }
      reports.push(report)
    } else if (report.status === 'RETURNED' && nextReportStatus === 'FINAL') {
      // Chốt lại sau khi bị trả lại là một version mới (`UK(study_id, version)`).
      report = { ...report, id: reportSeq++, version: report.version + 1, status: 'FINAL', created_by: actor, created_at: now }
      reports.push(report)
    } else {
      report.status = nextReportStatus
    }
    if (to === 'APPROVED') {
      report.approved_by = actor
      report.approved_at = now
    }
  }

  return { study, history_entry: entry, report: report ?? null }
}

/* ------------------------------------------------------------------- Client */

export function createMockClient(): HttpClient {
  async function handle(req: HttpRequest): Promise<unknown> {
    await delay()
    const { method, path } = req
    const query = req.query ?? {}
    let m: Record<string, string> | null

    if (method === 'POST' && path === '/auth/login') {
      const { username, password } = req.body as { username: string; password: string }
      const user = USERS.find(u => u.username === username.trim().toLowerCase())
      if (!user || password !== DEMO_PASSWORD) {
        throw new ApiError(401, 'BAD_CREDENTIALS', 'Tên đăng nhập hoặc mật khẩu không đúng.')
      }
      currentUserId = user.id
      const res: LoginResponseDto = {
        access_token: `mock.${user.username}.${Date.now()}`,
        token_type: 'Bearer',
        expires_in: 3600,
        user,
      }
      return res
    }

    if (method === 'POST' && path === '/auth/logout') {
      currentUserId = null
      return undefined
    }

    if (method === 'GET' && path === '/auth/me') {
      requireAuth()
      return USERS.find(u => u.id === currentUserId)!
    }

    requireAuth()

    if (method === 'GET' && path === '/departments') return DEPARTMENTS
    if (method === 'GET' && path === '/devices') return DEVICES
    if (method === 'GET' && path === '/service-items') return SERVICE_ITEMS

    if (method === 'GET' && path === '/studies') return listStudies(query)

    if (method === 'GET' && path === '/studies/status-counts') {
      const by: Record<string, number> = {}
      for (const s of studies) by[s.status] = (by[s.status] ?? 0) + 1
      const res: StatusCountsDto = { total: studies.length, by_status: by }
      return res
    }

    if ((m = matchPath('/studies/:id/status-history', path)) && method === 'GET') {
      const id = Number(m.id)
      return history.filter(h => h.study_id === id).sort((a, b) => b.id - a.id)
    }

    if ((m = matchPath('/studies/:id/reports/current', path))) {
      const id = Number(m.id)
      if (method === 'GET') {
        const r = currentReportOf(id)
        if (!r) throw new ApiError(404, 'REPORT_NOT_FOUND', 'Ca chụp chưa có báo cáo.')
        return r
      }
      if (method === 'PUT') {
        const study = studies.find(s => s.id === id)
        if (!study) throw new ApiError(404, 'STUDY_NOT_FOUND', 'Không tìm thấy ca chụp.')
        if (study.status !== 'READING') {
          throw new ApiError(409, 'STUDY_NOT_READING', 'Chỉ sửa được báo cáo khi ca đang ở trạng thái Đang đọc.')
        }
        const body = req.body as ReportWriteDto
        let r = currentReportOf(id)
        if (!r) {
          r = {
            id: reportSeq++, study_id: id, version: 1, status: 'DRAFT', origin: 'MANUAL',
            technique: null, findings: null, diagnosis: null, conclusion: null, recommendation: null,
            created_by: userRef(currentUserId!), created_at: new Date().toISOString(),
            approved_by: null, approved_at: null,
          }
          reports.push(r)
        }
        Object.assign(r, body)
        return r
      }
    }

    if ((m = matchPath('/studies/:id/transitions', path)) && method === 'POST') {
      return applyTransition(Number(m.id), req.body as TransitionRequestDto)
    }

    if ((m = matchPath('/studies/:id/viewer-link', path)) && method === 'GET') {
      const id = Number(m.id)
      const s = studies.find(x => x.id === id)
      if (!s) throw new ApiError(404, 'STUDY_NOT_FOUND', 'Không tìm thấy ca chụp.')
      if (!s.study_instance_uid) {
        throw new ApiError(422, 'STUDY_NO_DICOM_IMAGE', 'Ca chụp chưa có ảnh trong PACS (study_instance_uid is null).')
      }
      const base = import.meta.env.VITE_OHIF_BASE_URL || 'http://localhost:8043'
      // UID phai nam trong PATH ("/viewer/<uid>"), KHONG phai query string ("?StudyInstanceUIDs=").
      // Ban OHIF dang chay (ohif/viewer:v4.12.51.21579) dung route cu "ConnectedStandaloneRouting"
      // (schema "servers.dicomWeb", xem deploy/docker/ohif-app-config.js) - route nay chi khop path
      // "/viewer/:studyInstanceUids"; dang "?StudyInstanceUIDs=" roi ve route khac (doc file don le
      // qua "?url="), gay loi "No URL was specified" (phat hien bang tay 2026-11-09, xem
      // doc/07-pacs-dicom.md).
      const viewerUrl = `${base.replace(/\/$/, '')}/viewer/${s.study_instance_uid}`
      const order = IMAGING_ORDERS.find(o => o.id === s.order_id)
      console.info('[mock] audit_log VIEW_IMAGE', {
        study_id: s.id,
        patient_id: order?.patient_id ?? 1,
        study_instance_uid: s.study_instance_uid,
        viewer_url: viewerUrl,
      })
      return {
        study_id: s.id,
        patient_id: order?.patient_id ?? 1,
        study_instance_uid: s.study_instance_uid,
        viewer_url: viewerUrl,
      }
    }

    if ((m = matchPath('/studies/:id', path)) && method === 'GET') {
      const s = studies.find(x => x.id === Number(m!.id))
      if (!s) throw new ApiError(404, 'STUDY_NOT_FOUND', 'Không tìm thấy ca chụp.')
      return rowOf(s)
    }

    if ((m = matchPath('/patients/:id/print-events', path)) && method === 'POST') {
      // Backend ghi audit_log action PRINT kem patient_id; mock chi ghi ra console de kiem chung.
      const body = req.body as { study_id: number; sections: string[] }
      const patient = PATIENTS.find(p => p.id === Number(m!.id))
      if (!patient) throw new ApiError(404, 'PATIENT_NOT_FOUND', 'Không tìm thấy bệnh nhân.')
      console.info('[mock] audit_log PRINT', {
        patient_id: patient.id,
        patient_code: patient.patient_code,
        study_id: body.study_id,
        sections: body.sections,
      })
      return undefined
    }

    if ((m = matchPath('/imaging-orders/:id/clinical-snapshot', path)) && method === 'GET') {
      const id = Number(m.id)
      const payload = CLINICAL_SNAPSHOTS[id]
      if (!payload) throw new ApiError(404, 'SNAPSHOT_NOT_FOUND', 'Chỉ định này chưa có snapshot lâm sàng.')
      const res: ClinicalSnapshotDto = {
        order_id: id,
        source: 'SEED',
        fetched_at: '2026-10-31T03:00:00Z',
        payload: payload as unknown as Record<string, unknown>,
      }
      return res
    }

    throw new ApiError(404, 'NOT_FOUND', `Mock server chưa cài endpoint ${method} ${path}.`)
  }

  return {
    request: <T,>(req: HttpRequest) => handle(req) as Promise<T>,
    requestBlob: async () => {
      throw new ApiError(
        501,
        'MOCK_NO_PDF',
        'Mock server không dựng được PDF. Chuyển VITE_API_MODE=live để tải phiếu từ backend, hoặc dùng "In / Lưu PDF" của trình duyệt.',
      )
    },
  }
}
