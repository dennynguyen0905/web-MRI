import { formatAge, formatDateTime, formatDob, genderLabel } from '@/shared/lib/format'
import { STATUS_META } from '@/shared/lib/status'
import type { WorklistRow, WorklistTab } from '@/shared/lib/types'

function esc(value: string | number | null | undefined): string {
  const s = value == null || value === '' ? '—' : String(value)
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * Dựng trang HTML in danh sách bệnh nhân / ca chụp trên worklist (IN-25).
 * Cột Tuổi dùng `formatAge` (`~68` khi `dob_precision != DAY`, `—` khi chưa có ngày sinh, không ra `0`).
 */
export function buildWorklistPrintHtml(opts: {
  rows: WorklistRow[]
  tab: WorklistTab | 'ALL'
  printedBy: string
}): string {
  const { rows, tab, printedBy } = opts
  const stamp = new Date().toLocaleString('vi-VN')
  const tabLabel =
    tab === 'PENDING' ? 'Tab Chờ thực hiện'
      : tab === 'DONE' ? 'Tab Đã thực hiện'
        : 'Toàn bộ danh sách'

  const bodyRows = rows.length
    ? rows.map((r, i) => `
      <tr>
        <td style="text-align:center">${i + 1}</td>
        <td>${esc(r.patient.patientCode)}</td>
        <td><strong>${esc(r.patient.fullName)}</strong></td>
        <td>${esc(formatDob(r.patient.dateOfBirth, r.patient.dobPrecision))}</td>
        <td style="text-align:center">${esc(formatAge(r.patient))}</td>
        <td style="text-align:center">${esc(genderLabel(r.patient.gender))}</td>
        <td>${esc(r.order.orderCode)}</td>
        <td>${esc(r.service.name)}</td>
        <td>${esc(r.service.bodyPart)}</td>
        <td>${esc(r.order.orderingDoctorName)}</td>
        <td>${esc(r.reportSummary?.conclusion ?? r.order.clinicalDiagnosis)}</td>
        <td>${esc(STATUS_META[r.study.status]?.label ?? r.study.status)}</td>
        <td>${esc(formatDateTime(r.study.scheduledAt))}</td>
      </tr>
    `).join('')
    : `<tr><td colspan="13" style="text-align:center;color:#64748b;padding:14px">Không có ca chụp nào khớp bộ lọc hiện tại.</td></tr>`

  return `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<title>Danh sách bệnh nhân / ca chụp — ${esc(tabLabel)}</title>
<style>
  @page { size: A4 landscape; margin: 12mm; }
  * { box-sizing: border-box; }
  body { font-family: "Segoe UI", Arial, sans-serif; font-size: 11px; color: #0f172a; margin: 0; }
  .head { display: flex; justify-content: space-between; border-bottom: 2px solid #0284c7; padding-bottom: 6px; margin-bottom: 10px; }
  .brand { font-weight: 700; font-size: 13px; color: #0369a1; }
  .sub { font-size: 10px; color: #64748b; }
  h1 { font-size: 14px; text-align: center; text-transform: uppercase; margin: 6px 0 10px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { border: 1px solid #cbd5e1; padding: 4px 6px; text-align: left; vertical-align: top; }
  th { background: #f1f5f9; font-weight: 600; font-size: 10.5px; }
  .foot { margin-top: 12px; display: flex; justify-content: space-between; font-size: 10px; color: #64748b; }
</style>
</head>
<body>
  <div class="head">
    <div>
      <div class="brand">VR-RIS PACS — KHOA CHẨN ĐOÁN HÌNH ẢNH</div>
      <div class="sub">Hệ thống RIS tích hợp PACS (dữ liệu giả phục vụ đồ án — R-03)</div>
    </div>
    <div class="sub" style="text-align:right">
      Phạm vi: ${esc(tabLabel)}<br/>
      Tổng số dòng: ${rows.length}
    </div>
  </div>
  <h1>Danh sách bệnh nhân / ca chụp</h1>
  <table>
    <thead>
      <tr>
        <th style="width:32px;text-align:center">STT</th>
        <th>Mã BN</th>
        <th>Họ tên</th>
        <th>Ngày sinh</th>
        <th style="text-align:center">Tuổi</th>
        <th style="text-align:center">Giới</th>
        <th>Mã phiếu</th>
        <th>Chỉ định</th>
        <th>Bộ phận chụp</th>
        <th>Bác sĩ chỉ định</th>
        <th>Chẩn đoán / Kết luận</th>
        <th>Trạng thái</th>
        <th>TG lên lịch</th>
      </tr>
    </thead>
    <tbody>${bodyRows}</tbody>
  </table>
  <div class="foot">
    <span>Người in: ${esc(printedBy)} · In lúc ${esc(stamp)}</span>
    <span>Đã ghi nhận audit_log action EXPORT (IN-15)</span>
  </div>
</body>
</html>`
}
