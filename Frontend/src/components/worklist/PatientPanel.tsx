import { DEPARTMENTS } from '../../data/mock'
import { type Permission, can, deniedReason } from '../../lib/auth'
import { calcAge, dash, formatDateTime, formatDob, genderLabel } from '../../lib/format'
import type { AppUser, Report, WorklistRow } from '../../lib/types'
import { Button, EmptyState } from '../ui/controls'
import { Edit, FileText, Printer, Stethoscope, Sync, User } from '../ui/Icon'

interface Props {
  row: WorklistRow | null
  user: AppUser
  report: Report | null
  onUpdate: () => void
  onUpdateFromHis: () => void
  onAddOrder: () => void
  onClinical: () => void
  onReport: () => void
}

export function PatientPanel({ row, user, report, onUpdate, onUpdateFromHis, onAddOrder, onClinical, onReport }: Props) {
  return (
    <aside className="flex w-[268px] shrink-0 flex-col overflow-hidden border-l border-line bg-white">
      <div className="flex h-[30px] shrink-0 items-center gap-1.5 border-b border-line bg-[#e7eefb] px-3 text-[11.5px] font-semibold text-[#1e3a6b]">
        <User size={12} />
        Thông tin bệnh nhân
      </div>

      {!row ? (
        <EmptyState title="Chọn một ca chụp để xem chi tiết" icon={<User size={16} />} />
      ) : (
        <>
          <div className="flex shrink-0 flex-wrap gap-1 border-b border-line p-2">
            <Gated user={user} need="patient.update" icon={<FileText size={12} />} onClick={onUpdate}>Cập nhật</Gated>
            <Gated user={user} need="order.sync" icon={<Sync size={12} />} onClick={onUpdateFromHis}>Cập nhật từ HIS</Gated>
            <Gated user={user} need="patient.create" icon={<FileText size={12} />} onClick={onAddOrder}>Thêm chỉ định</Gated>
            <Gated user={user} need="clinical.view" icon={<Stethoscope size={12} />} onClick={onClinical}>Lâm sàng</Gated>
            <Button variant="primary" icon={<Edit size={12} />} onClick={onReport}>Báo cáo</Button>
            <Button variant="outline" icon={<Printer size={12} />} disabled={!can(user, 'list.print')}>In</Button>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2.5">
            <Group>
              <Field label="Họ tên" value={row.patient.fullName} strong />
              <Field label="Năm sinh" value={formatDob(row.patient.dateOfBirth, row.patient.dobPrecision)} />
              <Field label="Tuổi" value={calcAge(row.patient.dateOfBirth, row.patient.dobPrecision)} />
              <Field label="Giới tính" value={genderLabel(row.patient.gender)} />
              <Field label="Mã bệnh nhân" value={row.patient.patientCode} />
              <Field label="Mã khám sức khỏe" value={dash(row.patient.healthCheckCode)} />
              <Field label="Địa chỉ" value={dash(row.patient.address)} />
            </Group>

            <Group>
              <Field label="Thời gian chỉ định" value={formatDateTime(row.order.orderedAt)} />
              <Field label="Bác sĩ chỉ định" value={row.order.orderingDoctorName} />
              <Field label="Khoa chỉ định" value={deptName(row.order.orderingDepartmentId)} />
              <Field label="Chỉ định" value={row.service.name} multiline />
            </Group>

            <Group>
              <Field label="Kỹ thuật" value={dash(report?.technique ?? row.study.technique)} multiline />
              <Field label="Mô tả" value={dash(report?.findings)} multiline />
              <Field label="Chẩn đoán" value={dash(report?.diagnosis) === '—' ? row.order.clinicalDiagnosis : report!.diagnosis} multiline />
              <Field label="Kết luận" value={dash(report?.conclusion ?? row.study.conclusion)} multiline />
              <Field label="Khuyến nghị" value={dash(report?.recommendation)} multiline />
            </Group>

            <div className="mt-4 text-right text-[11px] italic text-ink-4">
              {dash(row.study.performedByName) === '—' ? 'Người thực hiện' : row.study.performedByName}
            </div>
          </div>
        </>
      )}
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

function deptName(id: number) {
  return DEPARTMENTS.find(d => d.id === id)?.name ?? '—'
}

function Group({ children }: { children: React.ReactNode }) {
  return <div className="mb-3 space-y-[5px] border-b border-line-soft pb-3 last:border-0">{children}</div>
}

function Field({ label, value, strong, multiline }: { label: string; value: string; strong?: boolean; multiline?: boolean }) {
  return (
    <div className="flex gap-2 text-[11.5px] leading-snug">
      <span className="w-[86px] shrink-0 text-ink-4">{label}</span>
      <span className={multiline ? 'flex-1 break-words' : 'flex-1 truncate'} title={value}>
        <span className={strong ? 'font-semibold text-ink' : 'text-ink-2'}>{value}</span>
      </span>
    </div>
  )
}
