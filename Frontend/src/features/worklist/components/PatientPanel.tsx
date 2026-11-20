import { type Permission, can, deniedReason } from '@/shared/lib/auth'
import { dash, formatAge, formatDateTime, formatDob, genderLabel } from '@/shared/lib/format'
import type { AppUser, Report, WorklistRow } from '@/shared/lib/types'
import { Button } from '@/shared/ui/controls'
import {
  Check, ChevronRight, Edit, Eye, FileText, Lock, Printer, Refresh, Stethoscope, Sync, Trash, User, Warning,
} from '@/shared/ui/Icon'

interface Props {
  row: WorklistRow
  user: AppUser
  report: Report | null
  onClose: () => void
  onUpdate: () => void
  onUpdateFromHis: () => void
  onAddOrder: () => void
  onClinical: () => void
  onViewImage: () => void
  onReport: () => void
  onPrint: () => void
  /** T2 — xác nhận PACS đã nhận đủ ảnh. */
  onMarkReceived: () => void
  /** T10 — hủy ca chưa có ảnh. */
  onCancelStudy: () => void
  /** T3 — mở ca để đọc (UNREAD -> READING). */
  onStartReading: () => void
  /** T4 — bỏ dở hoặc giải phóng khóa đọc (READING -> UNREAD). */
  onAbandonReading: () => void
  /** T5 — chốt đọc / lưu báo cáo chính thức (READING -> READ). */
  onFinishReading: () => void
  /** T6 — mở ca để duyệt (READ -> APPROVING). */
  onStartApproving: () => void
  /** T7 — duyệt kết quả (APPROVING -> APPROVED). */
  onApproveStudy: () => void
  /** T8 — trả lại để sửa (APPROVING -> READING). */
  onReturnForRevision: () => void
  /** T9 — mở lại ca đã duyệt (APPROVED -> READING). */
  onReopenStudy: () => void
}

/**
 * Panel chi tiết bên phải (IN-08, story US-BSCDHA-05).
 * Hiển thị đầy đủ các nút luồng đọc (T3/T4/T5), duyệt (T6/T7/T8/T9), nhận ảnh (T2) và hủy ca (T10)
 * theo trạng thái hiện hành của ca chụp và phân quyền RBAC.
 */
export function PatientPanel({
  row, user, report, onClose, onUpdate, onUpdateFromHis, onAddOrder, onClinical, onViewImage, onReport,
  onPrint, onMarkReceived, onCancelStudy, onStartReading, onAbandonReading, onFinishReading,
  onStartApproving, onApproveStudy, onReturnForRevision, onReopenStudy,
}: Props) {
  // Kỹ thuật và kết luận lấy từ báo cáo hiện hành — nguồn duy nhất (`doc/02-erd.md` mục 10).
  const technique = report?.technique ?? row.reportSummary?.technique ?? null
  const conclusion = report?.conclusion ?? row.reportSummary?.conclusion ?? null

  const hasImage = Boolean(row.study.studyInstanceUid)
  const canViewImage = can(user, 'study.viewImage')
  const viewImageDenied = !hasImage ? 'Chưa có ảnh chụp' : !canViewImage ? deniedReason('study.viewImage') : undefined

  const status = row.study.status
  const isScheduled = status === 'SCHEDULED'

  // T2 chỉ hợp lệ khi PACS đã có ảnh (SM-12).
  const markReceivedDenied = !can(user, 'study.markReceived')
    ? deniedReason('study.markReceived')
    : !hasImage
      ? 'Ca chưa có StudyInstanceUID — chưa có ảnh trong PACS để đánh dấu'
      : undefined

  // T10 ngược lại: chỉ hủy được ca CHƯA có ảnh (RBAC-12).
  const cancelDenied = !can(user, 'study.cancel')
    ? deniedReason('study.cancel')
    : hasImage
      ? 'Ca đã có ảnh trong PACS nên không hủy được'
      : undefined

  const reportReadyForT5 = Boolean(technique?.trim() && conclusion?.trim())

  return (
    <aside className="flex w-[260px] shrink-0 flex-col overflow-hidden border-l border-line bg-white desktop:w-[300px]">
      <div className="flex h-[30px] shrink-0 items-center gap-1.5 border-b border-line bg-[#e7eefb] px-2 text-[11.5px] font-semibold text-[#1e3a6b]">
        <User size={12} />
        <span className="flex-1">Thông tin bệnh nhân</span>
        <button
          onClick={onClose}
          title="Đóng panel, trả lại chiều ngang cho danh sách"
          className="focus-ring rounded p-0.5 text-[#1e3a6b] transition-colors hover:bg-brand-100"
        >
          <ChevronRight size={13} />
        </button>
      </div>

      <div className="flex shrink-0 flex-wrap gap-1 border-b border-line p-2">
        <Gated user={user} need="patient.update" icon={<FileText size={12} />} onClick={onUpdate}>Cập nhật</Gated>
        <Gated user={user} need="order.sync" icon={<Sync size={12} />} onClick={onUpdateFromHis}>Cập nhật từ HIS</Gated>
        <Gated user={user} need="patient.create" icon={<FileText size={12} />} onClick={onAddOrder}>Thêm chỉ định</Gated>
        <Gated user={user} need="clinical.view" icon={<Stethoscope size={12} />} onClick={onClinical}>Lâm sàng</Gated>

        {isScheduled ? (
          <>
            {can(user, 'study.markReceived') && (
              <Button
                variant="primary"
                icon={<Check size={12} />}
                disabled={Boolean(markReceivedDenied)}
                title={markReceivedDenied}
                onClick={onMarkReceived}
              >
                Đánh dấu đã có ảnh
              </Button>
            )}
            {can(user, 'study.cancel') && (
              <Button
                variant="danger"
                icon={<Trash size={12} />}
                disabled={Boolean(cancelDenied)}
                title={cancelDenied}
                onClick={onCancelStudy}
              >
                Hủy ca
              </Button>
            )}
          </>
        ) : (
          <>
            <Button
              variant="primary"
              icon={<Eye size={12} />}
              disabled={Boolean(viewImageDenied)}
              title={viewImageDenied}
              onClick={onViewImage}
            >
              Xem ảnh chụp
            </Button>
            <Button variant="outline" icon={<Edit size={12} />} onClick={onReport}>Báo cáo</Button>

            {/* Luồng đọc: UNREAD -> READING (T3) */}
            {status === 'UNREAD' && can(user, 'report.write') && (
              <Button
                variant="primary"
                icon={<Edit size={12} />}
                onClick={onStartReading}
              >
                Mở ca để đọc
              </Button>
            )}

            {/* Luồng đọc: READING -> READ (T5) & READING -> UNREAD (T4 bỏ dở / giải phóng khóa) */}
            {status === 'READING' && can(user, 'report.write') && (
              <>
                <Button
                  variant="primary"
                  icon={<Check size={12} />}
                  disabled={!reportReadyForT5}
                  title={reportReadyForT5 ? undefined : 'Cần nhập Kỹ thuật và Kết luận trong mục Báo cáo trước khi chốt đọc'}
                  onClick={onFinishReading}
                >
                  Chốt đọc
                </Button>
                <Button
                  variant="danger"
                  icon={<Warning size={12} />}
                  onClick={onAbandonReading}
                >
                  Bỏ dở
                </Button>
              </>
            )}

            {status === 'READING' && !can(user, 'report.write') && can(user, 'study.release') && (
              <Button
                variant="danger"
                icon={<Lock size={12} />}
                onClick={onAbandonReading}
              >
                Giải phóng khóa
              </Button>
            )}

            {/* Luồng duyệt: READ -> APPROVING (T6) */}
            {status === 'READ' && can(user, 'report.approve') && (
              <Button
                variant="primary"
                icon={<Check size={12} />}
                onClick={onStartApproving}
              >
                Mở ca để duyệt
              </Button>
            )}

            {/* Luồng duyệt: APPROVING -> APPROVED (T7) & APPROVING -> READING (T8) */}
            {status === 'APPROVING' && can(user, 'report.approve') && (
              <>
                <Button
                  variant="primary"
                  icon={<Check size={12} />}
                  onClick={onApproveStudy}
                >
                  Duyệt
                </Button>
                <Button
                  variant="danger"
                  icon={<Warning size={12} />}
                  onClick={onReturnForRevision}
                >
                  Trả lại để sửa
                </Button>
              </>
            )}

            {/* Mở lại ca đã duyệt: APPROVED -> READING (T9) */}
            {status === 'APPROVED' && can(user, 'study.reopen') && (
              <Button
                variant="danger"
                icon={<Refresh size={12} />}
                onClick={onReopenStudy}
              >
                Mở lại ca
              </Button>
            )}
          </>
        )}

        <Gated user={user} need="report.print" icon={<Printer size={12} />} onClick={onPrint}>In</Gated>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2.5 desktop:px-4 desktop:py-3">
        <Group>
          <Field label="Họ tên" value={row.patient.fullName} strong />
          <Field label="Năm sinh" value={formatDob(row.patient.dateOfBirth, row.patient.dobPrecision)} />
          <Field
            label="Tuổi"
            value={formatAge(row.patient)}
            hint={row.patient.ageApproximate ? 'Xấp xỉ vì ngày sinh chỉ có năm hoặc tháng' : undefined}
          />
          <Field label="Giới tính" value={genderLabel(row.patient.gender)} />
          <Field label="Mã bệnh nhân" value={row.patient.patientCode} />
          <Field label="Mã BHXH" value={dash(row.patient.socialInsuranceNo)} />
          <Field label="Địa chỉ" value={dash(row.patient.address)} multiline />
        </Group>

        <Group>
          <Field label="Mã phiếu" value={row.order.orderCode} />
          <Field label="Thời gian chỉ định" value={formatDateTime(row.order.orderedAt)} />
          <Field label="Bác sĩ chỉ định" value={row.order.orderingDoctorName} />
          <Field label="Khoa chỉ định" value={row.department?.name ?? '—'} />
          <Field label="Chỉ định" value={row.service.name} multiline />
          <Field label="Máy chụp" value={row.device?.name ?? '—'} />
        </Group>

        <Group>
          <Field label="Kỹ thuật" value={dash(technique)} multiline />
          <Field label="Mô tả" value={dash(report?.findings)} multiline />
          <Field label="Chẩn đoán" value={dash(report?.diagnosis ?? row.order.clinicalDiagnosis)} multiline />
          <Field label="Kết luận" value={dash(conclusion)} multiline />
          <Field label="Khuyến nghị" value={dash(report?.recommendation)} multiline />
        </Group>

        <div className="mt-4 text-right text-[11px] italic text-ink-4">
          {row.study.readingUser?.fullName ?? 'Người thực hiện'}
        </div>
      </div>
    </aside>
  )
}

/** Nút bị khóa khi vai trò không có quyền, tooltip nói rõ cần vai trò nào (IN-14). */
function Gated({ user, need, icon, onClick, children }: {
  user: AppUser
  need: Permission
  icon: React.ReactNode
  onClick: () => void
  children: React.ReactNode
}) {
  const allowed = can(user, need)
  return (
    <Button
      variant="outline"
      icon={icon}
      disabled={!allowed}
      title={allowed ? undefined : deniedReason(need)}
      onClick={onClick}
    >
      {children}
    </Button>
  )
}

function Group({ children }: { children: React.ReactNode }) {
  return <div className="mb-3 space-y-[5px] border-b border-line-soft pb-3 last:border-0">{children}</div>
}

function Field({ label, value, strong, multiline, hint }: {
  label: string
  value: string
  strong?: boolean
  multiline?: boolean
  hint?: string
}) {
  return (
    <div className="flex gap-2 text-[11.5px] leading-snug desktop:text-[12px]">
      <span className="w-[92px] shrink-0 text-ink-4 desktop:w-[104px]">{label}</span>
      <span className={multiline ? 'flex-1 break-words' : 'flex-1 truncate'} title={hint ?? value}>
        <span className={strong ? 'font-semibold text-ink' : 'text-ink-2'}>{value}</span>
      </span>
    </div>
  )
}
