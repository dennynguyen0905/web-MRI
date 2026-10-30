import { useEffect, useState } from 'react'
import { can, deniedReason } from '../../lib/auth'
import { calcAge, formatDateTime, formatDob, genderLabel } from '../../lib/format'
import { STATUS_META } from '../../lib/status'
import { canEditReport, transitionsFrom } from '../../lib/stateMachine'
import type { AppUser, Report, ReportStatus, StatusHistoryEntry, StudyStatus, WorklistRow } from '../../lib/types'
import { StatusChip } from '../ui/StatusChip'
import { Button, Modal, Tag, Textarea, cx } from '../ui/controls'
import { Check, Clock, FileText, Warning } from '../ui/Icon'

export type ReportDraft = Pick<Report, 'technique' | 'findings' | 'diagnosis' | 'conclusion' | 'recommendation'>

const EMPTY_DRAFT: ReportDraft = { technique: '', findings: '', diagnosis: '', conclusion: '', recommendation: '' }

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

interface Props {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
  user: AppUser | null
  report: Report | null
  history: StatusHistoryEntry[]
  onSaveDraft: (draft: ReportDraft) => void
  onTransition: (to: StudyStatus, draft: ReportDraft, reason: string | null) => void
}

/**
 * Báo cáo kết quả (IN-10): 5 trường nội dung, vòng đời nháp → chính thức → duyệt,
 * nút chuyển trạng thái lấy thẳng từ state machine và lọc theo vai trò.
 */
export function ReportModal({ open, onClose, row, user, report, history, onSaveDraft, onTransition }: Props) {
  const [draft, setDraft] = useState<ReportDraft>(EMPTY_DRAFT)
  const [reason, setReason] = useState('')
  const [dirty, setDirty] = useState(false)

  const studyId = row?.study.id ?? null

  // Nạp lại nội dung mỗi khi mở modal cho một ca khác.
  useEffect(() => {
    if (!open || !row) return
    setDraft(report
      ? { technique: report.technique, findings: report.findings, diagnosis: report.diagnosis, conclusion: report.conclusion, recommendation: report.recommendation }
      : { ...EMPTY_DRAFT, technique: row.study.technique ?? '', conclusion: row.study.conclusion ?? '' })
    setReason('')
    setDirty(false)
  }, [open, studyId, report, row])

  if (!row) return null

  const status = row.study.status
  const editable = canEditReport(status) && can(user, 'report.write')
  const transitions = transitionsFrom(status).filter(t => can(user, t.permission))
  const needsReason = transitions.some(t => t.exceptional)

  const set = (key: keyof ReportDraft, value: string) => {
    setDraft(d => ({ ...d, [key]: value }))
    setDirty(true)
  }

  const run = (to: StudyStatus, exceptional: boolean) => {
    onTransition(to, draft, exceptional ? reason.trim() || null : null)
    setDirty(false)
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
            disabled={!editable}
            title={editable ? undefined : deniedReason('report.write')}
            onClick={() => { onSaveDraft(draft); setDirty(false) }}
          >
            Lưu nháp
          </Button>
          {transitions.map(t => (
            <Button
              key={t.to + t.label}
              variant={t.exceptional ? 'danger' : 'primary'}
              icon={t.exceptional ? <Warning size={12} /> : <Check size={12} />}
              onClick={() => run(t.to, !!t.exceptional)}
            >
              {t.label}
            </Button>
          ))}
        </>
      }
    >
      <div className="flex min-h-0">
        {/* Nội dung báo cáo */}
        <div className="min-w-0 flex-1 border-r border-line p-4">
          <div className="mb-3 flex flex-wrap items-center gap-2 rounded-md bg-line-soft px-3 py-2">
            <span className="text-[13px] font-semibold text-ink">{row.patient.fullName}</span>
            <span className="text-[11.5px] text-ink-3">
              {row.patient.patientCode} · {genderLabel(row.patient.gender)} ·{' '}
              {formatDob(row.patient.dateOfBirth, row.patient.dobPrecision)} ·{' '}
              {calcAge(row.patient.dateOfBirth, row.patient.dobPrecision)} tuổi
            </span>
            <span className="ml-auto flex items-center gap-2">
              {report && <Tag tone={REPORT_STATUS_META[report.status].tone}>{REPORT_STATUS_META[report.status].label} · v{report.version}</Tag>}
              <StatusChip status={status} />
            </span>
          </div>

          <div className="mb-3 space-y-1 text-[11.5px]">
            <Line label="Chỉ định" value={row.service.name} />
            <Line label="Bác sĩ chỉ định" value={row.order.orderingDoctorName} />
            <Line label="Chẩn đoán ban đầu" value={row.order.clinicalDiagnosis} />
            <Line label="Máy chụp" value={row.device?.name ?? '—'} />
          </div>

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
                  rows={f.rows}
                  disabled={!editable}
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

        {/* Lịch sử trạng thái */}
        <div className="w-[228px] shrink-0 overflow-y-auto p-4">
          <div className="mb-2 section-label">Lịch sử trạng thái</div>
          {history.length === 0 ? (
            <p className="text-[11.5px] leading-relaxed text-ink-4">Chưa có chuyển trạng thái nào được ghi trong phiên này.</p>
          ) : (
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
                  <div className="text-[10.5px] text-ink-4">{h.changedByName}</div>
                  {h.reason && <div className="mt-0.5 text-[10.5px] italic text-[#b45309]">{h.reason}</div>}
                </li>
              ))}
            </ol>
          )}

          {report && (
            <div className="mt-4 border-t border-line pt-3 text-[10.5px] leading-relaxed text-ink-4">
              <div>Người viết: {report.createdByName}</div>
              <div>Lúc: {formatDateTime(report.createdAt)}</div>
              {report.approvedByName && (
                <>
                  <div className="mt-1">Người duyệt: {report.approvedByName}</div>
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
    <div className={cx('flex gap-2 leading-snug')}>
      <span className="w-[112px] shrink-0 text-ink-4">{label}</span>
      <span className="flex-1 text-ink-2">{value}</span>
    </div>
  )
}
