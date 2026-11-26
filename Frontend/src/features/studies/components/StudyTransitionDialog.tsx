import { useEffect, useState } from 'react'
import { ApiError } from '@/shared/api/http'
import { formatDateTime } from '@/shared/lib/format'
import { STATUS_META } from '@/shared/lib/status'
import type { Transition } from '@/shared/lib/stateMachine'
import type { WorklistRow } from '@/shared/lib/types'
import { StatusChip } from '@/shared/ui/StatusChip'
import { Button, Modal, Textarea } from '@/shared/ui/controls'
import { Check, Warning } from '@/shared/ui/Icon'
import { type TransitionResult, transitionStudy } from '../api/studiesApi'

const REASON_HINT_ID = 'transition-reason-hint'
const REASON_ERROR_ID = 'transition-reason-error'

const REASON_PLACEHOLDERS: Record<string, string> = {
  T4: 'Ví dụ: ảnh nhiễu, thiếu chuỗi xung hoặc bàn giao ca trực cho bác sĩ khác…',
  T8: 'Ví dụ: cần bổ sung mô tả nhu mô não và đối chiếu lâm sàng…',
  T9: 'Ví dụ: phát hiện thêm tổn thương sau hội chẩn, cần hiệu chỉnh kết luận…',
  T10: 'Ví dụ: bệnh nhân đã xuất viện hoặc đổi chỉ định, không còn nhu cầu chụp…',
}

/**
 * Hộp thoại xác nhận bước chuyển trạng thái gọi từ `PatientPanel` (T2…T10: nhận ảnh,
 * mở đọc, bỏ dở/giải phóng khóa, chốt đọc, mở duyệt, duyệt, trả lại, mở lại ca, hủy ca).
 *
 * Nhánh ngoại lệ (`exceptional`) bắt buộc nhập lý do — Frontend chặn trước cho đỡ một vòng gọi,
 * nhưng backend vẫn kiểm lại và có thể trả 422 (`doc/03-state-machine.md` mục 2.4).
 */
export function StudyTransitionDialog({ open, onClose, row, transition, onDone }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
  transition: Transition | null
  onDone: (result: TransitionResult) => void
}) {
  const [reason, setReason] = useState('')
  const [reasonTouched, setReasonTouched] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    setReason('')
    setError(null)
    setBusy(false)
    setReasonTouched(false)
  }, [open, row?.study.id, transition?.code])

  if (!open || !row || !transition) return null

  const needsReason = Boolean(transition.exceptional)
  const reasonMissing = needsReason && reason.trim() === ''
  // Chỉ báo lỗi sau khi người dùng đã chạm vào ô rồi để trống, hoặc đã bấm nút xác nhận —
  // không bật đỏ ngay lúc hộp thoại vừa mở (lỗi UX-01).
  const showReasonError = reasonMissing && reasonTouched

  const confirm = async () => {
    setBusy(true)
    setError(null)
    try {
      const result = await transitionStudy(row.study.id, transition.to, needsReason ? reason.trim() : null)
      onDone(result)
      onClose()
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.isForbidden
          ? `Bạn không đủ quyền thực hiện thao tác này. ${err.message}`
          : err.message)
      } else {
        setError('Không gọi được máy chủ, thử lại.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={transition.label}
      icon={transition.exceptional
        ? <Warning size={14} className="text-[#b45309]" />
        : <Check size={14} className="text-brand-600" />}
      width={520}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>Hủy bỏ</Button>
          <Button
            variant={transition.exceptional ? 'danger' : 'primary'}
            icon={transition.exceptional ? <Warning size={12} /> : <Check size={12} />}
            disabled={busy || reasonMissing}
            title={reasonMissing ? 'Thao tác này bắt buộc nhập lý do' : undefined}
            onClick={() => void confirm()}
          >
            {busy ? 'Đang gửi…' : transition.label}
          </Button>
        </>
      }
    >
      <div className="mb-3 rounded-md bg-line-soft px-3 py-2 text-[11.5px]">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-[13px] font-semibold text-ink">{row.patient.fullName}</span>
          <span className="text-ink-3">
            {row.patient.patientCode} · Mã phiếu {row.order.orderCode} · Mã hồ sơ {row.study.accessionNumber}
          </span>
        </div>
        <div className="mt-1.5 flex items-center gap-2 text-ink-3">
          <StatusChip status={transition.from} />
          <span>→</span>
          <StatusChip status={transition.to} />
        </div>
      </div>

      <div className="mb-3 space-y-1 text-[11.5px]">
        <Line label="Chỉ định" value={row.service.name} />
        <Line label="Máy chụp" value={row.device?.name ?? '—'} />
        <Line label="Thời gian chụp dự kiến" value={formatDateTime(row.study.scheduledAt)} />
        <Line label="StudyInstanceUID" value={row.study.studyInstanceUid ?? '— (chưa có ảnh trong PACS)'} />
      </div>

      {transition.code === 'T2' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-brand-100 bg-brand-50 px-2.5 py-2 text-[11.5px] text-brand-700">
          <Check size={13} className="mt-px shrink-0" />
          <span>
            Dùng khi máy chụp đã gửi đủ ảnh vào PACS. Ca sẽ chuyển sang
            {' '}<strong>{STATUS_META.UNREAD.label}</strong> để bác sĩ CĐHA đọc.
          </span>
        </div>
      )}

      {transition.code === 'T3' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-brand-100 bg-brand-50 px-2.5 py-2 text-[11.5px] text-brand-700">
          <Check size={13} className="mt-px shrink-0" />
          <span>
            Nhận khóa đọc ca chụp này và chuyển sang <strong>{STATUS_META.READING.label}</strong>.
            Cửa sổ nhập báo cáo sẽ tự động mở ngay sau khi xác nhận.
          </span>
        </div>
      )}

      {transition.code === 'T4' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f6e0b5] bg-[#fdf9ef] px-2.5 py-2 text-[11.5px] text-[#92400e]">
          <Warning size={13} className="mt-px shrink-0" />
          <span>
            Giải phóng khóa đọc và trả ca về <strong>{STATUS_META.UNREAD.label}</strong> để bác sĩ CĐHA
            khác có thể nhận đọc. Nội dung nháp đã lưu (nếu có) vẫn được giữ lại.
          </span>
        </div>
      )}

      {transition.code === 'T5' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-brand-100 bg-brand-50 px-2.5 py-2 text-[11.5px] text-brand-700">
          <Check size={13} className="mt-px shrink-0" />
          <span>
            Chốt báo cáo chính thức và chuyển ca sang <strong>{STATUS_META.READ.label}</strong> để chờ bác sĩ duyệt.
          </span>
        </div>
      )}

      {transition.code === 'T6' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-brand-100 bg-brand-50 px-2.5 py-2 text-[11.5px] text-brand-700">
          <Check size={13} className="mt-px shrink-0" />
          <span>
            Nhận khóa duyệt và chuyển ca sang <strong>{STATUS_META.APPROVING.label}</strong>.
            Cửa sổ báo cáo sẽ mở để bạn kiểm tra và duyệt hoặc trả lại.
          </span>
        </div>
      )}

      {transition.code === 'T7' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-brand-100 bg-brand-50 px-2.5 py-2 text-[11.5px] text-brand-700">
          <Check size={13} className="mt-px shrink-0" />
          <span>
            Phê duyệt chính thức kết quả chẩn đoán hình ảnh. Ca sẽ chuyển sang
            {' '}<strong>{STATUS_META.APPROVED.label}</strong> và sẵn sàng in trả kết quả.
          </span>
        </div>
      )}

      {transition.code === 'T8' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f6e0b5] bg-[#fdf9ef] px-2.5 py-2 text-[11.5px] text-[#92400e]">
          <Warning size={13} className="mt-px shrink-0" />
          <span>
            Trả ca về trạng thái <strong>{STATUS_META.READING.label}</strong> cho bác sĩ đọc ban đầu
            để chỉnh sửa báo cáo theo yêu cầu của người duyệt (SM-03).
          </span>
        </div>
      )}

      {transition.code === 'T9' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f6e0b5] bg-[#fdf9ef] px-2.5 py-2 text-[11.5px] text-[#92400e]">
          <Warning size={13} className="mt-px shrink-0" />
          <span>
            Mở lại ca đã duyệt sang <strong>{STATUS_META.READING.label}</strong>. Bản báo cáo đã duyệt
            được giữ nguyên và hệ thống tạo bản nháp mới (tăng version) để chỉnh sửa (SM-04).
          </span>
        </div>
      )}

      {transition.code === 'T10' && (
        <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f6e0b5] bg-[#fdf9ef] px-2.5 py-2 text-[11.5px] text-[#92400e]">
          <Warning size={13} className="mt-px shrink-0" />
          <span>
            Ca bị hủy sẽ chuyển sang trạng thái <strong>{STATUS_META.CANCELLED.label}</strong> và rời khỏi
            tab &quot;Chờ&quot;. Chỉ hủy được ca <strong>chưa có ảnh</strong> trong PACS.
          </span>
        </div>
      )}

      {needsReason && (
        <label className="block">
          <div className="mb-1 text-[11.5px] font-medium text-ink-3">
            Lý do <span className="text-[#c81e1e]">*</span>
          </div>
          <Textarea
            rows={3}
            autoFocus
            disabled={busy}
            aria-invalid={showReasonError || undefined}
            aria-describedby={showReasonError ? REASON_ERROR_ID : REASON_HINT_ID}
            placeholder={REASON_PLACEHOLDERS[transition.code] ?? 'Nhập lý do chuyển trạng thái…'}
            value={reason}
            onChange={e => setReason(e.target.value)}
            onBlur={() => setReasonTouched(true)}
          />
          {showReasonError ? (
            <div id={REASON_ERROR_ID} role="alert" className="mt-1 text-[10.5px] font-medium text-[#c81e1e]">
              Thao tác này bắt buộc nhập lý do.
            </div>
          ) : (
            <div id={REASON_HINT_ID} className="mt-1 text-[10.5px] text-ink-4">
              Lý do được ghi vào lịch sử trạng thái của ca chụp (IN-06).
            </div>
          )}
        </label>
      )}

      {error && (
        <div className="mt-3 flex items-start gap-1.5 rounded-md border border-[#f7cfcf] bg-[#fdf2f2] px-2.5 py-2 text-[11.5px] text-[#c81e1e]">
          <Warning size={13} className="mt-px shrink-0" />
          {error}
        </div>
      )}
    </Modal>
  )
}

function Line({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-2 leading-snug">
      <span className="w-[150px] shrink-0 text-ink-4">{label}</span>
      <span className="flex-1 break-words text-ink-2">{value}</span>
    </div>
  )
}
