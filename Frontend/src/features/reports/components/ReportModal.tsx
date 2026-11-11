import { useEffect, useRef, useState } from 'react'
import { can, deniedReason } from '@/shared/lib/auth'
import { formatAge, formatDateTime, formatDob, genderLabel } from '@/shared/lib/format'
import { STATUS_META } from '@/shared/lib/status'
import { canEditReport, transitionsFrom } from '@/shared/lib/stateMachine'
import type { AppUser, ReportStatus, WorklistRow } from '@/shared/lib/types'
import { StatusChip } from '@/shared/ui/StatusChip'
import { Button, Modal, Tag, Textarea } from '@/shared/ui/controls'
import { Check, Clock, FileText, Warning } from '@/shared/ui/Icon'
import { type ReportDraft, draftFromReport, useStudyReport } from '../hooks/useStudyReport'
import { type AiDraftFieldKey, attachAiDraftBridge } from '../integration/aiDraftBridge'

const FIELDS: { key: keyof ReportDraft; label: string; rows: number; placeholder: string }[] = [
  { key: 'technique', label: 'Kỹ thuật', rows: 2, placeholder: 'Tư thế chụp, thông số máy, thuốc cản quang…' },
  { key: 'findings', label: 'Mô tả', rows: 5, placeholder: 'Mô tả hình ảnh quan sát được trên phim' },
  { key: 'diagnosis', label: 'Chẩn đoán', rows: 2, placeholder: 'Chẩn đoán hình ảnh' },
  { key: 'conclusion', label: 'Kết luận', rows: 3, placeholder: 'Kết luận cuối cùng của bác sĩ đọc phim' },
  { key: 'recommendation', label: 'Khuyến nghị', rows: 2, placeholder: 'Đề nghị chụp thêm, theo dõi, hội chẩn…' },
]

const REPORT_STATUS_META: Record<ReportStatus, { label: string; tone: 'gray' | 'blue' | 'green' | 'amber' }> = {
  DRAFT: { label: 'Bản nháp', tone: 'gray' },
  FINAL: { label: 'Chính thức', tone: 'blue' },
  APPROVED: { label: 'Đã duyệt', tone: 'green' },
  RETURNED: { label: 'Bị trả lại', tone: 'amber' },
}

/**
 * Báo cáo kết quả (IN-10): 5 trường nội dung, vòng đời nháp → chính thức → duyệt.
 * Nút chuyển trạng thái sinh từ state machine rồi lọc theo vai trò; backend vẫn kiểm lại
 * và có thể trả 403/409 — lỗi đó hiện ngay trong modal.
 */
export function ReportModal({ open, onClose, row, user, onChanged }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
  user: AppUser
  onChanged: () => void
}) {
  const studyId = row?.study.id ?? null
  const { report, history, loading, saving, error, saveDraft, transition } = useStudyReport(studyId, open)

  const [draft, setDraft] = useState<ReportDraft>(() => draftFromReport(null))
  const [reason, setReason] = useState('')
  const [dirty, setDirty] = useState(false)

  useEffect(() => {
    if (!open) return
    setDraft(draftFromReport(report))
    setReason('')
    setDirty(false)
  }, [open, studyId, report])

  /* -------------------------------------- Kênh AI Panel (OHIF) → form báo cáo (postMessage) */
  // Ref giữ state mới nhất để listener (gắn 1 lần ở mount) không bị "đóng kín" giá trị cũ —
  // component này luôn được WorklistPage mount (chỉ ẩn render khi `!open`), nên effect gắn
  // listener chạy đúng 1 lần là đủ, không phụ thuộc open/row/draft (doc/11-ai-design.md mục 4,
  // 5.2.5.d — chi tiết thiết kế kênh ở báo cáo cuối của task nối kênh này).
  const aiBridgeStateRef = useRef({ open, studyInstanceUid: row?.study.studyInstanceUid ?? null, draft })
  useEffect(() => {
    aiBridgeStateRef.current = { open, studyInstanceUid: row?.study.studyInstanceUid ?? null, draft }
  }, [open, row, draft])

  const fieldRefs = useRef<Partial<Record<keyof ReportDraft, HTMLTextAreaElement>>>({})

  useEffect(() => {
    return attachAiDraftBridge({
      getState: () => aiBridgeStateRef.current,
      applyFields: (fields, focusFirst) => {
        setDraft(d => ({ ...d, ...fields }))
        setDirty(true)
        if (focusFirst) {
          const firstKey = FIELDS.find(f => fields[f.key] !== undefined)?.key
          // Chờ 1 tick cho React render lại draft mới rồi mới focus.
          requestAnimationFrame(() => fieldRefs.current[firstKey as keyof ReportDraft]?.focus())
        }
      },
      confirmOverwrite: (keys: AiDraftFieldKey[]) => {
        const labels = keys.map(k => FIELDS.find(f => f.key === k)?.label ?? k).join(', ')
        // Quyết định an toàn thêm ngoài tài liệu gốc (chưa có nút xác nhận ghi đè trong thiết kế
        // Figma) — dùng confirm() có sẵn của trình duyệt thay vì tự vẽ modal mới, cần nhóm duyệt
        // lại UX (xem báo cáo cuối của task nối kênh).
        return window.confirm(
          `Các trường sau đã có nội dung bác sĩ nhập: ${labels}. Ghi đè bằng gợi ý AI?`,
        )
      },
    })
  }, [])

  if (!open || !row) return null

  const status = row.study.status
  const editable = canEditReport(status) && can(user, 'report.write')
  const transitions = transitionsFrom(status).filter(t => can(user, t.permission))
  const needsReason = transitions.some(t => t.exceptional)

  const set = (key: keyof ReportDraft, value: string) => {
    setDraft(d => ({ ...d, [key]: value }))
    setDirty(true)
  }

  const run = async (fn: () => Promise<void>) => {
    try {
      await fn()
      setDirty(false)
      onChanged()
    } catch {
      // Thông điệp lỗi đã nằm trong `error` của hook, không cần xử lý thêm ở đây.
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Báo cáo kết quả"
      icon={<FileText size={14} className="text-brand-600" />}
      width={820}
      bodyClass="p-0"
      footer={
        <>
          {dirty && <span className="mr-auto text-[11px] text-[#b45309]">Có thay đổi chưa lưu</span>}
          <Button variant="ghost" onClick={onClose}>Đóng</Button>
          <Button
            variant="outline"
            disabled={!editable || saving}
            title={editable ? undefined : deniedReason('report.write')}
            onClick={() => run(() => saveDraft(draft))}
          >
            Lưu nháp
          </Button>
          {transitions.map(t => (
            <Button
              key={t.code}
              variant={t.exceptional ? 'danger' : 'primary'}
              icon={t.exceptional ? <Warning size={12} /> : <Check size={12} />}
              disabled={saving || (t.exceptional && reason.trim() === '')}
              title={t.exceptional && reason.trim() === '' ? 'Thao tác ngoại lệ bắt buộc nhập lý do' : undefined}
              onClick={() => run(() => transition(t.to, draft, t.exceptional ? reason.trim() : null))}
            >
              {t.label}
            </Button>
          ))}
        </>
      }
    >
      <div className="flex min-h-0">
        <div className="min-w-0 flex-1 border-r border-line p-4">
          <div className="mb-3 flex flex-wrap items-center gap-2 rounded-md bg-line-soft px-3 py-2">
            <span className="text-[13px] font-semibold text-ink">{row.patient.fullName}</span>
            <span className="text-[11.5px] text-ink-3">
              {row.patient.patientCode} · {genderLabel(row.patient.gender)} ·{' '}
              {formatDob(row.patient.dateOfBirth, row.patient.dobPrecision)} · {formatAge(row.patient)} tuổi
            </span>
            <span className="ml-auto flex items-center gap-2">
              {report && (
                <Tag tone={REPORT_STATUS_META[report.status].tone}>
                  {REPORT_STATUS_META[report.status].label} · v{report.version}
                </Tag>
              )}
              <StatusChip status={status} />
            </span>
          </div>

          <div className="mb-3 space-y-1 text-[11.5px]">
            <Line label="Chỉ định" value={row.service.name} />
            <Line label="Bác sĩ chỉ định" value={row.order.orderingDoctorName} />
            <Line label="Chẩn đoán ban đầu" value={row.order.clinicalDiagnosis ?? '—'} />
            <Line label="Máy chụp" value={row.device?.name ?? '—'} />
          </div>

          {error && (
            <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f7cfcf] bg-[#fdf2f2] px-2.5 py-2 text-[11.5px] text-[#c81e1e]">
              <Warning size={13} className="mt-px shrink-0" />
              <span>
                {error.isForbidden ? 'Bạn không đủ quyền cho thao tác này. ' : ''}
                {error.message}
              </span>
            </div>
          )}

          {!editable && (
            <div className="mb-3 flex items-start gap-1.5 rounded-md border border-line bg-line-soft px-2.5 py-2 text-[11.5px] text-ink-3">
              <Warning size={13} className="mt-px shrink-0" />
              {can(user, 'report.write')
                ? `Ca đang ở trạng thái "${STATUS_META[status].label}" nên nội dung báo cáo chỉ xem, không sửa được.`
                : `Bạn không có quyền nhập báo cáo. ${deniedReason('report.write')}.`}
            </div>
          )}

          <div className="space-y-2.5">
            {FIELDS.map(f => (
              <label key={f.key} className="block">
                <div className="mb-1 text-[11.5px] font-medium text-ink-3">{f.label}</div>
                <Textarea
                  ref={(el: HTMLTextAreaElement | null) => { fieldRefs.current[f.key] = el ?? undefined }}
                  rows={f.rows}
                  disabled={!editable || saving}
                  placeholder={f.placeholder}
                  value={draft[f.key]}
                  onChange={e => set(f.key, e.target.value)}
                />
              </label>
            ))}

            {needsReason && (
              <label className="block">
                <div className="mb-1 text-[11.5px] font-medium text-ink-3">Lý do (bắt buộc với thao tác ngoại lệ)</div>
                <Textarea
                  rows={2}
                  placeholder="Ví dụ: ảnh thiếu lát cắt, cần chụp lại"
                  value={reason}
                  onChange={e => setReason(e.target.value)}
                />
              </label>
            )}
          </div>
        </div>

        <div className="w-[228px] shrink-0 overflow-y-auto p-4">
          <div className="mb-2 section-label">Lịch sử trạng thái</div>
          {loading && <p className="text-[11.5px] text-ink-4">Đang tải…</p>}
          {!loading && history.length === 0 && (
            <p className="text-[11.5px] leading-relaxed text-ink-4">Chưa có chuyển trạng thái nào được ghi.</p>
          )}
          <ol className="space-y-2.5">
            {history.map(h => (
              <li key={h.id} className="relative pl-4">
                <span
                  className="absolute left-0 top-1 h-2 w-2 rounded-full"
                  style={{ background: STATUS_META[h.toStatus].color }}
                />
                <div className="text-[11.5px] font-medium text-ink-2">
                  {h.fromStatus ? `${STATUS_META[h.fromStatus].label} → ` : ''}
                  {STATUS_META[h.toStatus].label}
                </div>
                <div className="mt-0.5 flex items-center gap-1 text-[10.5px] text-ink-4">
                  <Clock size={10} />
                  {formatDateTime(h.changedAt)}
                </div>
                <div className="text-[10.5px] text-ink-4">{h.changedBy.fullName}</div>
                {h.reason && <div className="mt-0.5 text-[10.5px] italic text-[#b45309]">{h.reason}</div>}
              </li>
            ))}
          </ol>

          {report && (
            <div className="mt-4 border-t border-line pt-3 text-[10.5px] leading-relaxed text-ink-4">
              <div>Người viết: {report.createdBy.fullName}</div>
              <div>Lúc: {formatDateTime(report.createdAt)}</div>
              {report.approvedBy && (
                <>
                  <div className="mt-1">Người duyệt: {report.approvedBy.fullName}</div>
                  <div>Lúc: {formatDateTime(report.approvedAt)}</div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </Modal>
  )
}

function Line({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-2 leading-snug">
      <span className="w-[112px] shrink-0 text-ink-4">{label}</span>
      <span className="flex-1 text-ink-2">{value}</span>
    </div>
  )
}
