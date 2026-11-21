import { beforeEach, describe, expect, it } from 'vitest'
import { toReport, toStatusHistoryEntry, toStudy, toWorklistRow } from '@/shared/api/mappers'
import { ApiError } from '@/shared/api/http'
import { createMockClient, mockDbForTests, resetMockState } from '@/shared/api/mock/mockClient'
import { DEMO_PASSWORD } from '@/shared/api/mock/seed'
import type { TransitionResultDto } from '@/shared/api/dto'

describe('mappers: đồng bộ DTO Backend ↔ Frontend', () => {
  it('mappers tương thích cả DTO của mock (nested UserRef) và DTO của Backend (id/username phẳng)', () => {
    const historyEntry = toStatusHistoryEntry({
      id: 10,
      study_id: 5,
      from_status: 'UNREAD',
      to_status: 'READING',
      transition_code: 'T3',
      changed_by_user_id: 4,
      changed_by_username: 'bsdoc',
      changed_at: '2026-11-12T08:00:00',
      reason: null,
      note: null,
    })
    expect(historyEntry.changedBy.id).toBe(4)
    expect(historyEntry.changedBy.fullName).toContain('bsdoc')

    const report = toReport({
      id: 1,
      study_id: 5,
      version: 1,
      status: 'DRAFT',
      origin: 'MANUAL',
      technique: 'MRI sọ não',
      conclusion: 'Bình thường',
      created_by_user_id: 4,
      created_at: '2026-11-12T08:05:00',
      approved_by_user_id: null,
      approved_at: null,
    })
    expect(report.createdBy.id).toBe(4)
    expect(report.technique).toBe('MRI sọ não')

    const study = toStudy({
      id: 5,
      accession_number: '2026111200001',
      order_id: 2,
      service_item_id: 1,
      device_id: 1,
      status: 'READING',
      scheduled_at: '2026-11-12T07:30:00',
      reading_user_id: 4,
    })
    expect(study.readingUser?.id).toBe(4)

    // Tương thích với StudyDetailResponse phẳng từ GET /studies/{id} của Backend
    const row = toWorklistRow({
      id: 5,
      accession_number: '2026111200001',
      order_id: 2,
      service_item_id: 1,
      device_id: 1,
      status: 'READING',
      scheduled_at: '2026-11-12T07:30:00',
      order: {
        id: 2,
        order_code: 'PH20261112001',
        patient_id: 1,
        source: 'MANUAL',
        sync_status: 'NOT_SYNCED',
        ordering_department_id: 1,
        ordering_doctor_name: 'BS. Lâm Sàng',
        patient_class: 'OUTPATIENT',
        ordered_at: '2026-11-12T07:00:00',
      },
      patient: {
        id: 1,
        patient_code: 'TEST0001',
        full_name: 'Nguyễn Văn Giả',
        dob_precision: 'DAY',
        gender: 'M',
      },
      service_item: {
        id: 1,
        code: 'MR-SN',
        name: 'Chụp MRI sọ não',
        modality: 'MR',
        body_part: 'Sọ não',
      },
      current_report: {
        version: 1,
        status: 'DRAFT',
        technique: 'KT',
        conclusion: 'KL',
      },
    } as never)
    expect(row.study.accessionNumber).toBe('2026111200001')
    expect(row.reportSummary?.technique).toBe('KT')
  })
})

/* ------------------------------------------------------------------------------------------
 * Mock server chuyển trạng thái — phải khớp `StudyServiceImpl.transition` của backend (mã lỗi,
 * HTTP status, vai trò, người giữ ca). Mỗi test bắt đầu lại từ seed (`resetMockState`).
 *
 * Seed dùng ở đây (`shared/api/mock/seed.ts`):
 * - ca #2 READ (người đọc #4 bscdha, báo cáo FINAL)   - ca #3 APPROVING (bsduyet #5 đang duyệt)
 * - ca #4 READING (bscdha giữ, báo cáo DRAFT thiếu kết luận)
 * - ca #5 UNREAD (có UID, chưa có báo cáo)           - ca #6 UNREAD (có UID)
 * - ca #7 SCHEDULED (chưa có UID)
 * ---------------------------------------------------------------------------------------- */

type Client = ReturnType<typeof createMockClient>

async function loginAs(client: Client, username: string) {
  await client.request({ method: 'POST', path: '/auth/login', body: { username, password: DEMO_PASSWORD } })
}

function transitionReq(client: Client, studyId: number, toStatus: string, reason: string | null = null) {
  return client.request<TransitionResultDto>({
    method: 'POST',
    path: `/studies/${studyId}/transitions`,
    body: { to_status: toStatus, reason },
  })
}

async function expectApiError(promise: Promise<unknown>, status: number, code: string) {
  const err = await promise.then(() => null, (e: unknown) => e)
  expect(err).toBeInstanceOf(ApiError)
  expect((err as ApiError).status).toBe(status)
  expect((err as ApiError).code).toBe(code)
}

const FULL_REPORT = {
  technique: 'CT 64 dãy không tiêm',
  findings: 'Nhu mô bình thường',
  diagnosis: 'Bình thường',
  conclusion: 'Chưa thấy bất thường',
  recommendation: null,
}

describe('mock: chuyển trạng thái khớp backend', () => {
  let client: Client

  beforeEach(() => {
    resetMockState()
    client = createMockClient()
  })

  it('T10: KTV hủy ca chưa có ảnh, có lý do', async () => {
    await loginAs(client, 'ktv')
    const res = await transitionReq(client, 7, 'CANCELLED', 'Bệnh nhân đổi lịch khám')
    expect(res.study.status).toBe('CANCELLED')
    expect(res.history_entry.transition_code).toBe('T10')
    expect(res.history_entry.reason).toBe('Bệnh nhân đổi lịch khám')
  })

  it('T10: ca đã có ảnh trả 422 CANNOT_CANCEL_STUDY_WITH_IMAGE', async () => {
    await loginAs(client, 'ktv')
    // Ca #6 đang UNREAD nên dựng một ca SCHEDULED có UID trước: KTV điền UID cho ca #8.
    await client.request({ method: 'PATCH', path: '/studies/8', body: { study_instance_uid: '1.2.3.999' } })
    await expectApiError(transitionReq(client, 8, 'CANCELLED', 'Thử hủy'), 422, 'CANNOT_CANCEL_STUDY_WITH_IMAGE')
  })

  it('nhánh ngoại lệ thiếu lý do (hoặc chỉ khoảng trắng) trả 422 TRANSITION_REASON_REQUIRED', async () => {
    await loginAs(client, 'ktv')
    await expectApiError(transitionReq(client, 7, 'CANCELLED', '   '), 422, 'TRANSITION_REASON_REQUIRED')
  })

  it('chuyển không có trong state machine trả 409 INVALID_TRANSITION', async () => {
    await loginAs(client, 'bscdha')
    await expectApiError(transitionReq(client, 5, 'APPROVED'), 409, 'INVALID_TRANSITION')
  })

  it('sai vai trò trả 403 FORBIDDEN (CLINICIAN mở đọc, BS CĐHA làm T2)', async () => {
    await loginAs(client, 'bslamsang')
    await expectApiError(transitionReq(client, 5, 'READING'), 403, 'FORBIDDEN')

    // Vai trò được kiểm trước điều kiện UID, giống backend.
    await loginAs(client, 'bscdha')
    await expectApiError(transitionReq(client, 7, 'UNREAD'), 403, 'FORBIDDEN')
  })

  it('T2 chưa có UID trả 422 STUDY_NO_DICOM_IMAGE', async () => {
    await loginAs(client, 'ktv')
    await expectApiError(transitionReq(client, 7, 'UNREAD'), 422, 'STUDY_NO_DICOM_IMAGE')
  })

  it('T2 đặt completed_at (kết thúc chụp, openapi Study.completed_at)', async () => {
    await loginAs(client, 'ktv')
    await client.request({ method: 'PATCH', path: '/studies/8', body: { study_instance_uid: '1.2.3.997' } })
    const res = await transitionReq(client, 8, 'UNREAD')
    expect(res.study.status).toBe('UNREAD')
    expect(res.study.completed_at).not.toBeNull()
  })

  it('T3 không đặt started_at (mở đọc không phải bắt đầu chụp)', async () => {
    const study = mockDbForTests.studies.find(s => s.id === 5)!
    study.started_at = null
    await loginAs(client, 'bscdha')
    const t3 = await transitionReq(client, 5, 'READING')
    expect(t3.study.started_at).toBeNull()
  })

  it('T3 tạo DRAFT v1 và giữ khóa; T4 bỏ dở bắt buộc lý do và nhả khóa', async () => {
    await loginAs(client, 'bscdha')
    const t3 = await transitionReq(client, 5, 'READING')
    expect(t3.study.status).toBe('READING')
    expect(t3.study.reading_user?.id).toBe(4)
    expect(t3.report?.status).toBe('DRAFT')
    expect(t3.report?.version).toBe(1)

    await expectApiError(transitionReq(client, 5, 'UNREAD'), 422, 'TRANSITION_REASON_REQUIRED')
    const t4 = await transitionReq(client, 5, 'UNREAD', 'Bàn giao ca trực')
    expect(t4.study.status).toBe('UNREAD')
    expect(t4.study.reading_user).toBeNull()
  })

  it('T3 khi ca đang bị BS khác giữ trả 409 STUDY_LOCKED_BY_ANOTHER', async () => {
    await loginAs(client, 'bscdha')
    await transitionReq(client, 5, 'READING')
    // Dữ liệu lệch: ca về UNREAD nhưng vẫn còn reading_user của BS khác — T3 phải từ chối.
    const study = mockDbForTests.studies.find(s => s.id === 5)!
    study.status = 'UNREAD'
    await loginAs(client, 'bscdha2')
    await expectApiError(transitionReq(client, 5, 'READING'), 409, 'STUDY_LOCKED_BY_ANOTHER')
  })

  it('T4 / T5 / lưu nháp bởi BS không giữ ca trả 403 REPORT_NOT_HOLDER', async () => {
    await loginAs(client, 'bscdha2')
    await expectApiError(transitionReq(client, 4, 'UNREAD', 'Thử bỏ dở'), 403, 'REPORT_NOT_HOLDER')
    await expectApiError(transitionReq(client, 4, 'READ'), 403, 'REPORT_NOT_HOLDER')
    await expectApiError(
      client.request({ method: 'PUT', path: '/studies/4/reports/current', body: FULL_REPORT }),
      403, 'REPORT_NOT_HOLDER')
  })

  it('T5 thiếu kết luận trả 422 REPORT_INCOMPLETE; lưu đủ rồi chốt thì báo cáo thành FINAL', async () => {
    await loginAs(client, 'bscdha')
    await expectApiError(transitionReq(client, 4, 'READ'), 422, 'REPORT_INCOMPLETE')

    await client.request({ method: 'PUT', path: '/studies/4/reports/current', body: FULL_REPORT })
    const t5 = await transitionReq(client, 4, 'READ')
    expect(t5.study.status).toBe('READ')
    expect(t5.report?.status).toBe('FINAL')
  })

  it('lưu nháp khi ca không ở READING trả 409 STUDY_NOT_READING', async () => {
    await loginAs(client, 'bscdha')
    await client.request({ method: 'PUT', path: '/studies/4/reports/current', body: FULL_REPORT })
    await transitionReq(client, 4, 'READ')
    // Ca #4 giờ READ — vẫn là người giữ ca nhưng không còn sửa được.
    await expectApiError(
      client.request({ method: 'PUT', path: '/studies/4/reports/current', body: FULL_REPORT }),
      409, 'STUDY_NOT_READING')
  })

  it('T6 → T7: duyệt giữ nguyên completed_at và chuyển báo cáo sang APPROVED', async () => {
    await loginAs(client, 'bsduyet')
    const t6 = await transitionReq(client, 2, 'APPROVING')
    expect(t6.study.approving_user?.id).toBe(5)
    const completedBefore = t6.study.completed_at

    const t7 = await transitionReq(client, 2, 'APPROVED')
    expect(t7.study.status).toBe('APPROVED')
    expect(t7.study.completed_at).toBe(completedBefore)
    expect(t7.report?.status).toBe('APPROVED')
    expect(t7.report?.approved_by?.id).toBe(5)
  })

  it('T7 / T8 bởi BS duyệt khác người đang mở duyệt trả 403 NOT_APPROVING_HOLDER', async () => {
    await loginAs(client, 'bsduyet2')
    await expectApiError(transitionReq(client, 3, 'APPROVED'), 403, 'NOT_APPROVING_HOLDER')
    await expectApiError(transitionReq(client, 3, 'READING', 'Trả lại hộ'), 403, 'NOT_APPROVING_HOLDER')
  })

  it('T7 bởi ADMIN trả 403 ADMIN_CANNOT_APPROVE', async () => {
    await loginAs(client, 'admin')
    await expectApiError(transitionReq(client, 3, 'APPROVED'), 403, 'ADMIN_CANNOT_APPROVE')
  })

  it('T7 khi ca không có báo cáo (dữ liệu lệch) trả 422 REPORT_INCOMPLETE', async () => {
    const idx = mockDbForTests.reports.findIndex(r => r.study_id === 3)
    mockDbForTests.reports.splice(idx, 1)
    await loginAs(client, 'bsduyet')
    await expectApiError(transitionReq(client, 3, 'APPROVED'), 422, 'REPORT_INCOMPLETE')
  })

  it('T8 bởi người đang duyệt: báo cáo RETURNED; T4 rồi T3 lại vẫn giữ RETURNED', async () => {
    await loginAs(client, 'bsduyet')
    const t8 = await transitionReq(client, 3, 'READING', 'Cần bổ sung mô tả xoang')
    expect(t8.study.status).toBe('READING')
    expect(t8.study.approving_user).toBeNull()
    expect(t8.study.reading_user?.id).toBe(4)
    expect(t8.report?.status).toBe('RETURNED')

    await loginAs(client, 'bscdha')
    await transitionReq(client, 3, 'UNREAD', 'Tạm dừng')
    const t3 = await transitionReq(client, 3, 'READING')
    expect(t3.report?.status).toBe('RETURNED')
    expect(t3.report?.version).toBe(t8.report?.version)
  })

  it('T9: mở lại ca đã duyệt giữ completed_at và tạo DRAFT version + 1 chép nội dung', async () => {
    await loginAs(client, 'bsduyet')
    await transitionReq(client, 2, 'APPROVING')
    const t7 = await transitionReq(client, 2, 'APPROVED')

    await expectApiError(transitionReq(client, 2, 'READING'), 422, 'TRANSITION_REASON_REQUIRED')
    const t9 = await transitionReq(client, 2, 'READING', 'Cần bổ sung hội chẩn')
    expect(t9.study.status).toBe('READING')
    expect(t9.study.completed_at).toBe(t7.study.completed_at)
    expect(t9.report?.status).toBe('DRAFT')
    expect(t9.report?.version).toBe(t7.report!.version + 1)
    expect(t9.report?.conclusion).toBe(t7.report?.conclusion)
    // Tác giả bản mới là người đọc cũ, không phải người mở lại.
    expect(t9.report?.created_by?.id).toBe(4)
  })
})

describe('mock: PATCH khớp luật định danh của backend', () => {
  let client: Client

  beforeEach(() => {
    resetMockState()
    client = createMockClient()
  })

  it('CLINICIAN gửi trường định danh bị 403, chỉ gửi SĐT thì được', async () => {
    await loginAs(client, 'bslamsang')
    await expectApiError(
      client.request({ method: 'PATCH', path: '/patients/1', body: { full_name: 'Tên mới', phone: '0900000000' } }),
      403, 'FORBIDDEN')
    const ok = await client.request<{ phone: string }>({
      method: 'PATCH', path: '/patients/1', body: { phone: '0900000000' },
    })
    expect(ok.phone).toBe('0900000000')
  })

  it('CLINICIAN đặt study_instance_uid bị 403, KTV thì được', async () => {
    await loginAs(client, 'bslamsang')
    await expectApiError(
      client.request({ method: 'PATCH', path: '/studies/8', body: { study_instance_uid: '1.2.3.4' } }),
      403, 'FORBIDDEN')
    await loginAs(client, 'ktv')
    const ok = await client.request<{ study_instance_uid: string }>({
      method: 'PATCH', path: '/studies/8', body: { study_instance_uid: '1.2.3.4' },
    })
    expect(ok.study_instance_uid).toBe('1.2.3.4')
  })

  it('chưa đăng nhập trả 401 UNAUTHORIZED như backend', async () => {
    await expectApiError(client.request({ method: 'GET', path: '/studies' }), 401, 'UNAUTHORIZED')
  })
})
