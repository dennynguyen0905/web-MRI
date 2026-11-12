import { useMemo, useState } from 'react'
import { dash, formatDob, genderLabel } from '@/shared/lib/format'
import type { AppUser, WorklistRow } from '@/shared/lib/types'
import { Button, Checkbox, Input, Modal } from '@/shared/ui/controls'
import { Download, Warning } from '@/shared/ui/Icon'

interface CompareField {
  label: string
  current: string
  next: string
  /** Trường lệch so với bản hiện tại — tô vàng như ảnh 11. */
  changed?: boolean
  allowAppend?: boolean
  /** Trường định danh bệnh nhân — `CLINICIAN` chỉ xem, không sửa (`doc/06-rbac-matrix.md` mục 5, D24). */
  identityField?: boolean
}

const IDENTITY_LOCK_TOOLTIP = 'Chỉ KTV hoặc Admin được sửa trường này'

/** Ảnh 11 — "Cập nhật ca chụp": so sánh thông tin hiện tại ↔ thông tin cập nhật (IN-04). */
export function UpdateStudyModal({ open, onClose, row, user }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
  user: AppUser
}) {
  // CLINICIAN không có ADMIN/TECHNICIAN hợp lệ: khóa trường định danh, chỉ xem (D24, ❓ chưa chốt
  // về mã lỗi backend — Frontend ở đây chọn "disable, không ẩn" theo yêu cầu đã giao).
  const lockIdentityFields = !user.roles.some(r => r === 'ADMIN' || r === 'TECHNICIAN')

  const fields = useMemo<CompareField[]>(() => {
    if (!row) return []
    const p = row.patient
    return [
      { label: 'Mã', current: p.patientCode, next: p.patientCode, allowAppend: true, identityField: true },
      { label: 'Họ tên', current: p.fullName, next: p.fullName, identityField: true },
      // Lệch dữ liệu điển hình: HIS gửi năm sinh khác RIS (quy tắc đối chiếu IN-05, RC-02).
      { label: 'Ngày sinh', current: formatDob(p.dateOfBirth, p.dobPrecision), next: '1952', changed: true, identityField: true },
      { label: 'Giới tính', current: genderLabel(p.gender), next: genderLabel(p.gender), identityField: true },
      { label: 'Địa chỉ', current: dash(p.address), next: '' },
      { label: 'BHXH', current: dash(p.socialInsuranceNo), next: dash(p.socialInsuranceNo), identityField: true },
      { label: 'Chẩn đoán ban đầu', current: dash(row.order.clinicalDiagnosis), next: dash(row.order.clinicalDiagnosis) },
      { label: 'StudyInstanceUID', current: dash(row.study.studyInstanceUid), next: row.study.studyInstanceUid ?? '' },
    ]
  }, [row])

  const [appendMode, setAppendMode] = useState(false)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Cập nhật ca chụp"
      width={660}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Hủy</Button>
          <Button icon={<Download size={12} />} onClick={onClose}>Lưu</Button>
        </>
      }
    >
      <div className="mb-3 flex items-start gap-1.5 rounded-md border border-[#f6e0b5] bg-[#fdf9ef] px-2.5 py-2 text-[11.5px] text-[#92400e]">
        <Warning size={13} className="mt-px shrink-0" />
        Trường có thay đổi so với thông tin hiện tại
      </div>

      <table className="w-full border-collapse overflow-hidden rounded-md border border-line text-[12px]">
        <thead>
          <tr className="bg-[#e7eefb] text-left text-[11.5px] font-semibold text-[#1e3a6b]">
            <th className="w-[140px] border-b border-brand-100 px-2.5 py-2">Trường thông tin</th>
            <th className="w-[200px] border-b border-brand-100 px-2.5 py-2">Thông tin hiện tại</th>
            <th className="border-b border-brand-100 px-2.5 py-2">Thông tin cập nhật</th>
          </tr>
        </thead>
        <tbody>
          {fields.map(f => (
            <tr key={f.label} className={f.changed ? 'bg-[#fdf9ef]' : 'bg-white'}>
              <td className="border-b border-line-soft px-2.5 py-2 text-ink-3">{f.label}</td>
              <td className="border-b border-line-soft px-2.5 py-2 text-ink-2">{f.current}</td>
              <td className="border-b border-line-soft px-2.5 py-1.5">
                <div className="flex items-center gap-2">
                  <Input
                    defaultValue={f.next}
                    placeholder={f.next === '' ? `Nhập ${f.label.toLowerCase()}` : undefined}
                    disabled={f.identityField && lockIdentityFields}
                    title={f.identityField && lockIdentityFields ? IDENTITY_LOCK_TOOLTIP : undefined}
                  />
                  {f.allowAppend && (
                    <Checkbox
                      checked={appendMode}
                      onChange={setAppendMode}
                      label={<span className="whitespace-nowrap text-ink-3">Thêm mới</span>}
                    />
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {row && (
        <div className="mt-3 text-[11px] text-ink-4">
          Mã phiếu {row.order.orderCode} · Mã hồ sơ {row.study.accessionNumber}
        </div>
      )}
    </Modal>
  )
}
