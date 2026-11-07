import { useEffect, useState } from 'react'
import { fetchClinicalSnapshot } from '@/features/clinical/api/clinicalApi'
import { fetchCurrentReport } from '@/features/reports/api/reportApi'
import { isMockMode } from '@/shared/api/client'
import { ApiError } from '@/shared/api/http'
import { useAsync } from '@/shared/hooks/useAsync'
import type { AppUser, WorklistRow } from '@/shared/lib/types'
import { Button, Checkbox, Modal, cx } from '@/shared/ui/controls'
import { Download, Printer, Warning } from '@/shared/ui/Icon'
import { PRINT_SECTIONS, type PrintSection, downloadPrintDocument, logPrintEvent, saveBlob } from '../api/printApi'
import { buildPrintHtml, printHtml } from '../lib/printSheet'

/**
 * In phiếu kết quả của một bệnh nhân (Task 5).
 *
 * Phạm vi đã thu hẹp có chủ ý theo cảnh báo trong mô tả task: trong 8 tuần chỉ
 * **Kết quả chẩn đoán chức năng chính** là dữ liệu thật của RIS; tổng quan khám và xét nghiệm
 * là dữ liệu giả trong `clinical_snapshot`; đơn thuốc và phẫu thuật thủ thuật còn rỗng
 * (`01-scope.md` mục 3.3). Mỗi phần được chọn in ra **một tờ/một file riêng**.
 */
export function PrintModal({ open, onClose, row, user }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
  user: AppUser
}) {
  const [selected, setSelected] = useState<Set<PrintSection>>(new Set(['FUNCTIONAL']))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const orderId = row?.order.id ?? null
  const studyId = row?.study.id ?? null

  const snapshot = useAsync(
    signal => fetchClinicalSnapshot(orderId!, signal).catch(() => null),
    [orderId],
    { enabled: open && orderId != null },
  )
  const report = useAsync(
    signal => fetchCurrentReport(studyId!, signal),
    [studyId],
    { enabled: open && studyId != null },
  )

  useEffect(() => {
    if (open) setError(null)
  }, [open, studyId])

  if (!open || !row) return null

  const sections = PRINT_SECTIONS.filter(s => selected.has(s.key)).map(s => s.key)
  const loading = snapshot.loading || report.loading

  const toggle = (key: PrintSection) =>
    setSelected(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })

  const handleBrowserPrint = async () => {
    setBusy(true)
    setError(null)
    try {
      const html = buildPrintHtml({
        row,
        snapshot: snapshot.data ?? null,
        report: report.data ?? null,
        sections,
        printedBy: user.fullName,
      })
      // Ghi audit PRINT trước khi mở hộp thoại: in là mang dữ liệu bệnh nhân ra khỏi hệ thống.
      await logPrintEvent(row.patient.id, row.study.id, sections)
      printHtml(html)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không in được, thử lại.')
    } finally {
      setBusy(false)
    }
  }

  const handleServerPdf = async () => {
    setBusy(true)
    setError(null)
    try {
      for (const section of sections) {
        const { blob, filename } = await downloadPrintDocument(row.patient.id, row.study.id, section)
        saveBlob(blob, filename)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được phiếu PDF từ máy chủ.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="In phiếu kết quả"
      icon={<Printer size={14} className="text-brand-600" />}
      width={520}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Đóng</Button>
          <Button
            variant="outline"
            icon={<Download size={12} />}
            disabled={busy || loading || sections.length === 0 || isMockMode}
            title={isMockMode ? 'Cần backend thật (VITE_API_MODE=live) để dựng PDF phía máy chủ' : undefined}
            onClick={handleServerPdf}
          >
            Tải PDF từ máy chủ
          </Button>
          <Button
            icon={<Printer size={12} />}
            disabled={busy || loading || sections.length === 0}
            onClick={handleBrowserPrint}
          >
            In / Lưu PDF
          </Button>
        </>
      }
    >
      <div className="mb-3 rounded-md bg-line-soft px-3 py-2 text-[11.5px]">
        <span className="font-semibold text-ink">{row.patient.fullName}</span>
        <span className="text-ink-3"> · {row.patient.patientCode} · Mã phiếu {row.order.orderCode}</span>
      </div>

      <div className="mb-1.5 section-label">Chọn nội dung cần in — mỗi mục là một tờ riêng</div>

      <div className="space-y-1">
        {PRINT_SECTIONS.map(s => (
          <label
            key={s.key}
            className={cx(
              'flex cursor-pointer items-start gap-2.5 rounded-md border px-2.5 py-2 transition-colors',
              selected.has(s.key) ? 'border-brand-200 bg-brand-50' : 'border-line bg-white hover:bg-line-soft',
            )}
          >
            <span className="pt-0.5">
              <Checkbox checked={selected.has(s.key)} onChange={() => toggle(s.key)} />
            </span>
            <span className="min-w-0 flex-1 leading-tight">
              <span className="block text-[12px] font-medium text-ink">{s.label}</span>
              {s.note && (
                <span className={cx('mt-0.5 block text-[10.5px]', s.hasRealData ? 'text-ink-4' : 'text-[#b45309]')}>
                  {s.note}
                </span>
              )}
            </span>
          </label>
        ))}
      </div>

      {error && (
        <div className="mt-3 flex items-start gap-1.5 rounded-md border border-[#f7cfcf] bg-[#fdf2f2] px-2.5 py-2 text-[11.5px] text-[#c81e1e]">
          <Warning size={13} className="mt-px shrink-0" />
          {error}
        </div>
      )}

      <p className="mt-3 text-[11px] leading-relaxed text-ink-4">
        Mỗi lần in đều ghi <code className="rounded bg-line-soft px-1 font-mono text-[10.5px]">audit_log</code> action
        {' '}<code className="rounded bg-line-soft px-1 font-mono text-[10.5px]">PRINT</code> kèm mã bệnh nhân (IN-15).
      </p>
    </Modal>
  )
}
