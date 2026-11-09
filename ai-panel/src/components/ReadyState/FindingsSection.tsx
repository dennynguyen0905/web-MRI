import type { AiFinding } from '@/types/aiAnalysis'
import { CONFIDENCE_DOTS, CONFIDENCE_LABEL, confidenceLevel } from '@/lib/confidence'

/**
 * Khối gợi ý chẩn đoán (phần danh sách nhãn) — doc/11-ai-design.md mục 5.2.5.b, mục 7. `open`
 * do `ReadyState` sở hữu và dùng CHUNG cho cả danh sách nhãn này và `ReportDraftPreview` (cùng
 * một khối "b", cùng một thao tác mở — KHÔNG tự bung khi mở ca, bác sĩ phải bấm mở). Dùng từ
 * "nhãn AI đề xuất" / "gợi ý" — KHÔNG dùng "chẩn đoán AI" hay "kết quả AI" (mục 7).
 */
export function FindingsSection({
  findings,
  open,
  onToggle,
}: {
  findings: AiFinding[]
  open: boolean
  onToggle: () => void
}) {
  return (
    <>
      <button type="button" className="findings-toggle" onClick={onToggle} aria-expanded={open}>
        <span className="body-text strong">Nhãn AI đề xuất ({findings.length})</span>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ transform: open ? 'rotate(180deg)' : undefined }}>
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>
      {open && (
        <div>
          {findings.map(f => {
            const level = confidenceLevel(f.confidence)
            return (
              <div className="finding-row" key={f.label}>
                <span className="label">{f.label}</span>
                <span className={`conf-badge ${level}`}>
                  {CONFIDENCE_DOTS[level]} {CONFIDENCE_LABEL[level]}
                </span>
              </div>
            )
          })}
        </div>
      )}
    </>
  )
}
