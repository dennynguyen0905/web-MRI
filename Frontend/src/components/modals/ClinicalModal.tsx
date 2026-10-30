import { type ReactNode, useEffect, useState } from 'react'
import { CLINICAL_SNAPSHOT } from '../../data/mock'
import { calcAge, dash, formatDateTime, formatDob, genderLabel } from '../../lib/format'
import type { WorklistRow } from '../../lib/types'
import { EmptyState, cx } from '../ui/controls'
import { FileText, Stethoscope, X } from '../ui/Icon'

const TABS = [
  'KẾT QUẢ TỔNG QUAN KHÁM',
  'KẾT QUẢ XÉT NGHIỆM',
  'KẾT QUẢ CHẨN ĐOÁN CHỨC NĂNG CHÍNH',
  'ĐƠN THUỐC/VẬT TƯ TRONG PHIẾU KHÁM',
  'DỮ LIỆU PHẪU THUẬT THỦ THUẬT',
] as const

/**
 * Ảnh 13–17 — modal toàn màn hình "Thông tin lâm sàng" (IN-11).
 * Dữ liệu đọc từ clinical_snapshot; tab 5 còn rỗng, chờ tích hợp HIS (scope 3.3).
 */
export function ClinicalModal({ open, onClose, row }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
}) {
  const [tab, setTab] = useState(0)

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open || !row) return null

  return (
    <div className="fixed inset-0 z-50 bg-[rgba(16,24,40,.45)] p-5" onMouseDown={onClose}>
      <div
        onMouseDown={e => e.stopPropagation()}
        className="mx-auto flex h-full max-w-[1180px] flex-col overflow-hidden rounded-lg bg-white shadow-[0_20px_60px_rgba(16,24,40,.3)]"
      >
        <div className="flex h-11 shrink-0 items-center gap-2 border-b border-line px-4">
          <Stethoscope size={14} className="text-brand-600" />
          <div className="flex-1 text-[13px] font-semibold text-ink">Thông tin lâm sàng</div>
          <button onClick={onClose} className="focus-ring rounded p-1 text-ink-4 transition-colors hover:bg-line-soft hover:text-ink">
            <X size={15} />
          </button>
        </div>

        <div className="flex min-h-0 flex-1">
          <nav className="w-[228px] shrink-0 overflow-y-auto border-r border-line p-2.5">
            {TABS.map((t, i) => (
              <button
                key={t}
                onClick={() => setTab(i)}
                className={cx(
                  'mb-1 block w-full rounded-md px-2.5 py-2 text-left text-[11px] font-semibold leading-snug transition-colors',
                  i === tab
                    ? 'border-l-[3px] border-brand-600 bg-brand-50 text-brand-700'
                    : 'text-ink-3 hover:bg-line-soft',
                )}
              >
                {t}
              </button>
            ))}
          </nav>

          <div className="min-h-0 flex-1 overflow-y-auto bg-app p-4">
            <PatientCard row={row} />
            {tab === 0 && <OverviewTab />}
            {tab === 1 && <LabsTab />}
            {tab === 2 && <FunctionalTab />}
            {tab === 3 && <DrugsTab />}
            {tab === 4 && (
              <Card title="DỮ LIỆU PHẪU THUẬT THỦ THUẬT">
                <EmptyState title="Chưa có dữ liệu phẫu thuật thủ thuật" icon={<FileText size={16} />} />
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/* ------------------------------------------------------------------ Khối chung */

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card mb-3 p-3.5">
      <h3 className="mb-2.5 text-[12px] font-bold tracking-wide text-[#1e3a6b]">{title}</h3>
      {children}
    </section>
  )
}

function PatientCard({ row }: { row: WorklistRow }) {
  const p = row.patient
  const items: [string, string][] = [
    ['Họ tên', p.fullName],
    ['Năm sinh', formatDob(p.dateOfBirth, p.dobPrecision)],
    ['Tuổi', calcAge(p.dateOfBirth, p.dobPrecision)],
    ['Giới tính', genderLabel(p.gender)],
    ['Mã bệnh nhân', p.patientCode],
    ['Mã khám sức khỏe', dash(p.healthCheckCode)],
    ['Chẩn đoán', row.order.clinicalDiagnosis],
    ['Bác sĩ', row.order.orderingDoctorName],
    ['Chỉ định', row.service.name],
    ['Thời gian chỉ định', formatDateTime(row.order.orderedAt)],
    ['Khoa', 'Khoa Cấp cứu C1.3'],
    ['Lịch sử vào viện', dash(row.order.visitCode)],
    ['KTV/BĐV/Thư ký', '—'],
  ]
  return (
    <Card title="THÔNG TIN BỆNH NHÂN">
      <div className="grid grid-cols-[auto_1fr_auto_1fr] gap-x-4 gap-y-1.5 text-[11.5px]">
        {items.map(([k, v]) => (
          <div key={k} className="col-span-2 flex gap-2">
            <span className="w-[112px] shrink-0 text-ink-4">{k}</span>
            <span className="flex-1 text-ink-2">{v}</span>
          </div>
        ))}
      </div>
    </Card>
  )
}

/* ------------------------------------------------------------------- Các tab */

function OverviewTab() {
  return (
    <Card title="KẾT QUẢ TỔNG QUAN KHÁM">
      <div className="space-y-1.5 text-[11.5px]">
        {Object.entries(CLINICAL_SNAPSHOT.overview).map(([k, v]) => (
          <div key={k} className="flex gap-3 leading-relaxed">
            <span className="w-[180px] shrink-0 text-ink-4">{k}</span>
            <span className="flex-1 text-ink-2">{v}</span>
          </div>
        ))}
      </div>
    </Card>
  )
}

function LabsTab() {
  const rows = CLINICAL_SNAPSHOT.labs
  return (
    <Card title="DANH SÁCH XÉT NGHIỆM">
      <div className="mb-2 text-[11.5px] text-ink-3">Số hồ sơ: 90000001</div>
      <DataTable
        headers={['STT', 'Số phiếu', 'Tên xét nghiệm', 'Trạng thái', 'Người tạo', 'Ngày tạo', 'Ngày thực hiện']}
        rows={rows.map(r => [String(r.stt), r.code, r.name, r.status, r.createdBy, r.createdAt, r.performedAt])}
      />
      <div className="mt-3 text-[11.5px] font-semibold text-[#1e3a6b]">KẾT QUẢ XÉT NGHIỆM: —</div>
    </Card>
  )
}

function FunctionalTab() {
  return (
    <Card title="KẾT QUẢ CHẨN ĐOÁN CHỨC NĂNG CHÍNH">
      <div className="space-y-3">
        {CLINICAL_SNAPSHOT.functional.map((f, i) => (
          <div key={f.code} className={cx('space-y-1 text-[11.5px]', i > 0 && 'border-t border-line-soft pt-3')}>
            {([
              ['Số phiếu', f.code],
              ['Ngày chỉ định', f.orderedAt],
              ['Ngày trả kết quả', f.resultAt],
              ['Mã nhóm dịch vụ', f.groupCode],
              ['Ghi chú', f.note],
              ['Tên dịch vụ', f.serviceName],
              ['Kết quả', f.result],
              ['Mã CĐHA', f.codhaCode],
            ] as [string, string][]).map(([k, v]) => (
              <div key={k} className="flex gap-3 leading-relaxed">
                <span className="w-[150px] shrink-0 text-ink-4">{k}</span>
                <span className="flex-1 text-ink-2">{v}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </Card>
  )
}

function DrugsTab() {
  const rows = CLINICAL_SNAPSHOT.drugs
  return (
    <Card title="DANH SÁCH ĐƠN THUỐC/VẬT TƯ TRONG PHIẾU KHÁM">
      <DataTable
        headers={['Số hồ sơ', 'Số phiếu', 'Số dòng', 'Mã thuốc/vật tư', 'Tên thuốc/vật tư', 'Thành phần chung', 'Đơn vị tính', 'Số lượng order']}
        rows={rows.map(r => [r.recordNo, r.formNo, String(r.seq), r.drugCode, r.drugName, r.ingredient, r.unit, String(r.qty)])}
        linkFirst
      />
    </Card>
  )
}

function DataTable({ headers, rows, linkFirst }: { headers: string[]; rows: string[][]; linkFirst?: boolean }) {
  return (
    <table className="w-full border-collapse overflow-hidden rounded-md text-[11.5px]">
      <thead>
        <tr className="bg-[#e7eefb] text-left text-[11px] font-semibold text-[#1e3a6b]">
          {headers.map(h => <th key={h} className="border-b border-brand-100 px-2.5 py-2">{h}</th>)}
        </tr>
      </thead>
      <tbody>
        {rows.map((r, i) => (
          <tr key={i} className="border-b border-line-soft last:border-0">
            {r.map((c, j) => (
              <td key={j} className={cx('px-2.5 py-2 align-top', linkFirst && j === 0 ? 'text-brand-600 underline' : 'text-ink-2')}>
                {c}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
