import { describe, expect, it } from 'vitest'
import { toReport, toStatusHistoryEntry, toStudy, toWorklistRow } from '@/shared/api/mappers'
import { createMockClient } from '@/shared/api/mock/mockClient'
import { DEMO_PASSWORD } from '@/shared/api/mock/seed'
import type { TransitionResultDto } from '@/shared/api/dto'

describe('luồng đọc, duyệt, hủy ca và đồng bộ DTO Backend ↔ Frontend', () => {
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

  it('luồng đầy đủ T3 -> T4 -> T3 -> T5 -> T6 -> T8 -> T5 -> T6 -> T7 -> T9 và T10 hủy ca', async () => {
    const client = createMockClient()
    await client.request({
      method: 'POST',
      path: '/auth/login',
      body: { username: 'bscdha', password: DEMO_PASSWORD },
    })

    // Ca #7 (SCHEDULED, chưa có UID) -> Hủy ca (T10)
    const cancelRes = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/7/transitions',
      body: { to_status: 'CANCELLED', reason: 'Bệnh nhân đổi lịch khám' },
    })
    expect(cancelRes.study.status).toBe('CANCELLED')
    expect(cancelRes.history_entry.transition_code).toBe('T10')

    // Ca #5 (UNREAD) -> Mở đọc (T3) -> Bỏ dở (T4) -> Mở đọc lại (T3)
    const t3First = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READING', reason: null },
    })
    expect(t3First.study.status).toBe('READING')
    expect(t3First.report?.status).toBe('DRAFT')

    const t4Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'UNREAD', reason: 'Bàn giao ca trực' },
    })
    expect(t4Res.study.status).toBe('UNREAD')
    expect(t4Res.study.reading_user).toBeNull()

    await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READING', reason: null },
    })

    // Lưu nháp báo cáo và chốt đọc (T5)
    await client.request({
      method: 'PUT',
      path: '/studies/5/reports/current',
      body: {
        technique: 'CT 64 dãy không tiêm',
        findings: 'Nhu mô bình thường',
        diagnosis: 'Bình thường',
        conclusion: 'Chưa thấy bất thường',
        recommendation: null,
      },
    })

    const t5Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READ', reason: null },
    })
    expect(t5Res.study.status).toBe('READ')
    expect(t5Res.report?.status).toBe('FINAL')

    // Mở duyệt (T6) -> Trả lại để sửa (T8) -> giữ nguyên reading_user và đổi report sang RETURNED
    const t6Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'APPROVING', reason: null },
    })
    expect(t6Res.study.status).toBe('APPROVING')

    const t8Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READING', reason: 'Cần bổ sung mô tả xoang' },
    })
    expect(t8Res.study.status).toBe('READING')
    expect(t8Res.study.approving_user).toBeNull()
    expect(t8Res.report?.status).toBe('RETURNED')

    // Chốt đọc lại (T5) -> Mở duyệt (T6) -> Duyệt (T7) -> Mở lại ca (T9 tạo version + 1)
    await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READ', reason: null },
    })
    await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'APPROVING', reason: null },
    })
    const t7Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'APPROVED', reason: null },
    })
    expect(t7Res.study.status).toBe('APPROVED')
    expect(t7Res.report?.status).toBe('APPROVED')
    const approvedVersion = t7Res.report!.version

    const t9Res = await client.request<TransitionResultDto>({
      method: 'POST',
      path: '/studies/5/transitions',
      body: { to_status: 'READING', reason: 'Cần bổ sung hội chẩn' },
    })
    expect(t9Res.study.status).toBe('READING')
    expect(t9Res.report?.status).toBe('DRAFT')
    expect(t9Res.report?.version).toBe(approvedVersion + 1)
  })
})
