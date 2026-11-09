import { type ReactNode, useEffect, useState } from 'react'
import { useAsync } from '@/shared/hooks/useAsync'
import { dash, formatAge, formatDateTime, formatDob, genderLabel } from '@/shared/lib/format'
import type { WorklistRow } from '@/shared/lib/types'
import { Button, EmptyState, cx } from '@/shared/ui/controls'
import { Eye, FileText, Stethoscope, X } from '@/shared/ui/Icon'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { can, deniedReason } from '@/shared/lib/auth'
import { openStudyViewer } from '@/features/studies/api/viewerApi'
import { fetchClinicalSnapshot } from '../api/clinicalApi'

/** 5 tab lâm sàng. Thứ tự và tên đúng bản thiết kế. */
export const CLINICAL_SECTIONS = [
  { key: 'overview', label: 'KẾT QUẢ TỔNG QUAN KHÁM' },
  { key: 'labs', label: 'KẾT QUẢ XÉT NGHIỆM' },
  { key: 'functional', label: 'KẾT QUẢ CHẨN ĐOÁN CHỨC NĂNG CHÍNH' },
  { key: 'drugs', label: 'ĐƠN THUỐC/VẬT TƯ TRONG PHIẾU KHÁM' },
  { key: 'surgery', label: 'DỮ LIỆU PHẪU THUẬT THỦ THUẬT' },
] as const

export type ClinicalSectionKey = (typeof CLINICAL_SECTIONS)[number]['key']

/**
 * Modal thông tin lâm sàng (IN-11, rút gọn theo `01-scope.md` mục 3.3):
 * tab 1 và 2 hiển thị đầy đủ từ `clinical_snapshot`; tab 3, 4, 5 để trống kèm ghi chú
 * "chờ tích hợp HIS" vì dữ liệu đó do HIS đẩy sang, RIS không tự sinh.
 */
export function ClinicalModal({ open, onClose, row }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
}) {
  const [tab, setTab] = useState(0)
  const { user } = useAuth()
  const canViewImage = can(user, 'study.viewImage')

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const orderId = row?.order.id ?? null
  const snapshot = useAsync(
    signal => fetchClinicalSnapshot(orderId!, signal),
    [orderId],
    { enabled: open && orderId != null },
  )

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
            {CLINICAL_SECTIONS.map((s, i) => (
              <button
                key={s.key}
                onClick={() => setTab(i)}
                className={cx(
                  'mb-1 block w-full rounded-md px-2.5 py-2 text-left text-[11px] font-semibold leading-snug transition-colors',
                  i === tab
                    ? 'border-l-[3px] border-brand-600 bg-brand-50 text-brand-700'
                    : 'text-ink-3 hover:bg-line-soft',
                )}
              >
                {s.label}
              </button>
            ))}
          </nav>

          <div className="min-h-0 flex-1 overflow-y-auto bg-app p-4">
            <PatientCard row={row} />

            {snapshot.loading && (
              <div className="card p-6 text-center text-[12px] text-ink-4">Đang tải dữ liệu lâm sàng…</div>
            )}

            {snapshot.error && (
              <Card title={CLINICAL_SECTIONS[tab].label}>
                <EmptyState
                  title={snapshot.error.status === 404 ? 'Chỉ định này chưa có dữ liệu lâm sàng' : 'Không tải được dữ liệu lâm sàng'}
                  hint={snapshot.error.message}
                  icon={<FileText size={16} />}
                />
              </Card>
            )}

            {snapshot.data && (
              <>
                {tab === 0 && (
                  <Card title="KẾT QUẢ TỔNG QUAN KHÁM">
                    {Object.keys(snapshot.data.overview).length === 0
                      ? <EmptyState title="Chưa có dữ liệu tổng quan khám" icon={<FileText size={16} />} />
                      : (
                        <div className="space-y-1.5 text-[11.5px]">
                          {Object.entries(snapshot.data.overview).map(([k, v]) => (
                            <div key={k} className="flex gap-3 leading-relaxed">
                              <span className="w-[180px] shrink-0 text-ink-4">{k}</span>
                              <span className="flex-1 text-ink-2">{v}</span>
                            </div>
                          ))}
                        </div>
                      )}
                  </Card>
                )}

                {tab === 1 && (
                  <Card title="DANH SÁCH XÉT NGHIỆM">
                    {snapshot.data.labs.length === 0
                      ? <EmptyState title="Chưa có kết quả xét nghiệm" icon={<FileText size={16} />} />
                      : (
                        <DataTable
                          headers={['STT', 'Tên xét nghiệm', 'Trạng thái']}
                          rows={snapshot.data.labs.map(l => [String(l.stt), l.ten, l.trang_thai])}
                        />
                      )}
                  </Card>
                )}

                {tab === 2 && (
                  <Card title="KẾT QUẢ CHẨN ĐOÁN CHỨC NĂNG CHÍNH">
                    <div className="overflow-hidden rounded-md border border-line-soft">
                      <table className="w-full border-collapse text-[11.5px]">
                        <thead>
                          <tr className="bg-[#e7eefb] text-left text-[11px] font-semibold text-[#1e3a6b]">
                            <th className="border-b border-brand-100 px-3 py-2">Tên dịch vụ</th>
                            <th className="border-b border-brand-100 px-3 py-2">Bộ phận</th>
                            <th className="border-b border-brand-100 px-3 py-2">Kết luận</th>
                            <th className="border-b border-brand-100 px-3 py-2 text-right">Xem ảnh</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr className="border-b border-line-soft last:border-0 hover:bg-[#f8fafd]">
                            <td className="px-3 py-2.5 font-medium text-ink">
                              {row.service.name}
                              <div className="text-[10.5px] text-ink-4">{row.service.modality} · {row.service.code}</div>
                            </td>
                            <td className="px-3 py-2.5 text-ink-3">{row.service.bodyPart}</td>
                            <td className="px-3 py-2.5 text-ink-2">
                              {row.reportSummary?.conclusion || <span className="italic text-ink-4">Chưa có kết luận</span>}
                            </td>
                            <td className="px-3 py-2.5 text-right">
                              {(() => {
                                const hasUid = Boolean(row.study.studyInstanceUid)
                                if (!hasUid) {
                                  return (
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      disabled
                                      icon={<Eye size={12} />}
                                      title="Ca chưa có ảnh trong PACS"
                                    >
                                      Xem ảnh
                                    </Button>
                                  )
                                }
                                if (!canViewImage) {
                                  return (
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      disabled
                                      icon={<Eye size={12} />}
                                      title={deniedReason('study.viewImage')}
                                    >
                                      Xem ảnh
                                    </Button>
                                  )
                                }
                                return (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    icon={<Eye size={12} />}
                                    title="Xem ảnh trong OHIF"
                                    onClick={() => openStudyViewer(row.study.id)}
                                  >
                                    Xem ảnh
                                  </Button>
                                )
                              })()}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </Card>
                )}

                {tab === 3 && (
                  <Card title="DANH SÁCH ĐƠN THUỐC/VẬT TƯ TRONG PHIẾU KHÁM">
                    {snapshot.data.drugs.length === 0
                      ? <PendingHis />
                      : (
                        <DataTable
                          headers={['Tên thuốc/vật tư', 'Số lượng', 'Đơn vị']}
                          rows={snapshot.data.drugs.map(d => [d.ten, String(d.so_luong), d.don_vi])}
                        />
                      )}
                  </Card>
                )}

                {tab === 4 && (
                  <Card title="DỮ LIỆU PHẪU THUẬT THỦ THUẬT">
                    <PendingHis />
                  </Card>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/* ---------------------------------------------------------------- Khối dùng chung */

function PendingHis() {
  return (
    <EmptyState
      title="Chờ tích hợp HIS"
      hint="Dữ liệu tab này do HIS đẩy sang, RIS không tự sinh (01-scope.md mục 3.3)."
      icon={<FileText size={16} />}
    />
  )
}

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
    ['Tuổi', formatAge(p)],
    ['Giới tính', genderLabel(p.gender)],
    ['Mã bệnh nhân', p.patientCode],
    ['Mã BHXH', dash(p.socialInsuranceNo)],
    ['Chẩn đoán', dash(row.order.clinicalDiagnosis)],
    ['Bác sĩ chỉ định', row.order.orderingDoctorName],
    ['Chỉ định', row.service.name],
    ['Thời gian chỉ định', formatDateTime(row.order.orderedAt)],
    ['Khoa chỉ định', row.department?.name ?? '—'],
    ['Mã LSVV', dash(row.order.visitCode)],
  ]
  return (
    <Card title="THÔNG TIN BỆNH NHÂN">
      <div className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-[11.5px]">
        {items.map(([k, v]) => (
          <div key={k} className="flex gap-2">
            <span className="w-[112px] shrink-0 text-ink-4">{k}</span>
            <span className="flex-1 text-ink-2">{v}</span>
          </div>
        ))}
      </div>
    </Card>
  )
}

function DataTable({ headers, rows }: { headers: string[]; rows: string[][] }) {
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
            {r.map((c, j) => <td key={j} className="px-2.5 py-2 align-top text-ink-2">{c}</td>)}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
