import type { ClinicalSnapshot } from '@/features/clinical/api/clinicalApi'
import { dash, formatAge, formatDateTime, formatDob, genderLabel } from '@/shared/lib/format'
import type { Report, WorklistRow } from '@/shared/lib/types'
import { PRINT_SECTION_LABEL, type PrintSection } from '../api/printApi'

/**
 * Dựng HTML phiếu in rồi đưa vào một iframe ẩn để gọi `print()`.
 * Cách này cho người dùng chọn máy in **hoặc** "Save as PDF" ngay trong hộp thoại của trình duyệt,
 * và hiển thị đúng tiếng Việt vì dùng font của hệ thống.
 *
 * Mỗi phần được chọn là **một tờ riêng** (`page-break-after`), đúng yêu cầu của task.
 */

const esc = (s: string) =>
  s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]!))

function rowsTable(headers: string[], rows: string[][]): string {
  if (rows.length === 0) return '<p class="empty">Chưa có dữ liệu — chờ tích hợp HIS.</p>'
  return `<table>
    <thead><tr>${headers.map(h => `<th>${esc(h)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</tbody>
  </table>`
}

function patientBlock(row: WorklistRow): string {
  const p = row.patient
  const items: [string, string][] = [
    ['Họ tên', p.fullName],
    ['Mã bệnh nhân', p.patientCode],
    ['Ngày sinh', formatDob(p.dateOfBirth, p.dobPrecision)],
    ['Tuổi', formatAge(p)],
    ['Giới tính', genderLabel(p.gender)],
    ['Mã BHXH', dash(p.socialInsuranceNo)],
    ['Mã phiếu', row.order.orderCode],
    ['Mã LSVV', dash(row.order.visitCode)],
    ['Khoa chỉ định', row.department?.name ?? '—'],
    ['Bác sĩ chỉ định', row.order.orderingDoctorName],
    ['Chỉ định', row.service.name],
    ['Thời gian chỉ định', formatDateTime(row.order.orderedAt)],
  ]
  return `<div class="pt-grid">${items
    .map(([k, v]) => `<div class="pt-row"><span class="pt-key">${esc(k)}</span><span>${esc(v)}</span></div>`)
    .join('')}</div>`
}

function sectionBody(
  section: PrintSection,
  row: WorklistRow,
  snapshot: ClinicalSnapshot | null,
  report: Report | null,
): string {
  switch (section) {
    case 'OVERVIEW': {
      const entries = Object.entries(snapshot?.overview ?? {})
      if (entries.length === 0) return '<p class="empty">Chỉ định này chưa có dữ liệu tổng quan khám.</p>'
      return `<div class="pt-grid">${entries
        .map(([k, v]) => `<div class="pt-row"><span class="pt-key">${esc(k)}</span><span>${esc(String(v))}</span></div>`)
        .join('')}</div>`
    }
    case 'LABS':
      return rowsTable(
        ['STT', 'Tên xét nghiệm', 'Trạng thái'],
        (snapshot?.labs ?? []).map(l => [String(l.stt), l.ten, l.trang_thai]),
      )
    case 'FUNCTIONAL': {
      // Phần duy nhất là dữ liệu thật của RIS: tên dịch vụ + nội dung báo cáo hiện hành.
      const fields: [string, string | null][] = [
        ['Tên dịch vụ', row.service.name],
        ['Máy chụp', row.device?.name ?? null],
        ['Kỹ thuật', report?.technique ?? row.reportSummary?.technique ?? null],
        ['Mô tả', report?.findings ?? null],
        ['Chẩn đoán', report?.diagnosis ?? row.order.clinicalDiagnosis ?? null],
        ['Kết luận', report?.conclusion ?? row.reportSummary?.conclusion ?? null],
        ['Khuyến nghị', report?.recommendation ?? null],
      ]
      const body = `<div class="pt-grid">${fields
        .map(([k, v]) => `<div class="pt-row"><span class="pt-key">${esc(k)}</span><span>${esc(v ?? '—')}</span></div>`)
        .join('')}</div>`
      const signer = report?.approvedBy?.fullName ?? report?.createdBy.fullName ?? ''
      return `${body}<div class="sign"><div>Bác sĩ đọc kết quả</div><div class="sign-name">${esc(signer)}</div></div>`
    }
    case 'DRUGS':
      return rowsTable(
        ['Tên thuốc/vật tư', 'Số lượng', 'Đơn vị'],
        (snapshot?.drugs ?? []).map(d => [d.ten, String(d.so_luong), d.don_vi]),
      )
    case 'SURGERY':
      return rowsTable(['Nội dung'], [])
  }
}

const STYLES = `
  * { box-sizing: border-box; }
  body { font-family: "Segoe UI", Arial, sans-serif; font-size: 12px; color: #111928; margin: 0; }
  .sheet { padding: 18mm 15mm; page-break-after: always; }
  .sheet:last-child { page-break-after: auto; }
  h1 { font-size: 15px; text-align: center; margin: 0 0 2px; text-transform: uppercase; }
  .sub { text-align: center; color: #6b7280; font-size: 11px; margin-bottom: 14px; }
  h2 { font-size: 12px; margin: 14px 0 6px; color: #1e3a6b; text-transform: uppercase; }
  .pt-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 3px 16px; }
  .pt-row { display: flex; gap: 8px; line-height: 1.5; }
  .pt-key { width: 110px; flex: none; color: #6b7280; }
  table { width: 100%; border-collapse: collapse; margin-top: 4px; }
  th, td { border: 1px solid #d5dbe4; padding: 5px 7px; text-align: left; vertical-align: top; }
  th { background: #eef3fb; font-size: 11px; }
  .empty { color: #6b7280; font-style: italic; }
  .sign { margin-top: 28px; text-align: right; }
  .sign-name { margin-top: 42px; font-weight: 600; }
  .foot { margin-top: 18px; border-top: 1px solid #d5dbe4; padding-top: 6px; color: #9aa4b2; font-size: 10px; }
  @page { size: A4; margin: 0; }
`

export interface PrintSheetInput {
  row: WorklistRow
  snapshot: ClinicalSnapshot | null
  report: Report | null
  sections: PrintSection[]
  printedBy: string
}

export function buildPrintHtml({ row, snapshot, report, sections, printedBy }: PrintSheetInput): string {
  const sheets = sections.map(section => `
    <div class="sheet">
      <h1>Phiếu kết quả — ${esc(PRINT_SECTION_LABEL[section])}</h1>
      <div class="sub">VR-RIS PACS · Dữ liệu giả phục vụ đồ án, không dùng cho mục đích lâm sàng</div>
      <h2>Thông tin bệnh nhân</h2>
      ${patientBlock(row)}
      <h2>${esc(PRINT_SECTION_LABEL[section])}</h2>
      ${sectionBody(section, row, snapshot, report)}
      <div class="foot">Người in: ${esc(printedBy)} · In lúc ${esc(formatDateTime(new Date().toISOString()))} · Mã hồ sơ ${esc(row.study.accessionNumber)}</div>
    </div>
  `).join('')

  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Phiếu kết quả</title><style>${STYLES}</style></head><body>${sheets}</body></html>`
}

/** Mở hộp thoại in của trình duyệt với nội dung đã dựng (in giấy hoặc lưu PDF). */
export function printHtml(html: string): void {
  const frame = document.createElement('iframe')
  frame.setAttribute('aria-hidden', 'true')
  frame.style.position = 'fixed'
  frame.style.right = '0'
  frame.style.bottom = '0'
  frame.style.width = '0'
  frame.style.height = '0'
  frame.style.border = '0'
  document.body.appendChild(frame)

  const doc = frame.contentDocument
  if (!doc) {
    frame.remove()
    return
  }
  doc.open()
  doc.write(html)
  doc.close()

  const run = () => {
    frame.contentWindow?.focus()
    frame.contentWindow?.print()
    // Giữ iframe đủ lâu cho hộp thoại in đọc xong nội dung rồi mới gỡ.
    setTimeout(() => frame.remove(), 1000)
  }

  if (doc.readyState === 'complete') run()
  else frame.onload = run
}
