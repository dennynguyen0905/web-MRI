import type { ReportDraftFields } from '@/types/reportDraft'

const FIELD_LABELS: { key: keyof ReportDraftFields; label: string }[] = [
  { key: 'technique', label: 'Kỹ thuật' },
  { key: 'findings', label: 'Mô tả' },
  { key: 'diagnosis', label: 'Chẩn đoán' },
  { key: 'conclusion', label: 'Kết luận' },
  { key: 'recommendation', label: 'Khuyến nghị' },
]

/**
 * Xem trước 5 trường nháp cho báo cáo — doc/11-ai-design.md mục 5.2.5.b. KHÔNG phải ô nhập —
 * ô nhập là form báo cáo chính (`ReportModal`, không thuộc AI Panel). Nhãn trùng với
 * `ReportModal` thật trong RIS để không gây nhầm lẫn khi bác sĩ chuyển qua chuyển lại.
 */
export function ReportDraftPreview({ draft }: { draft: ReportDraftFields }) {
  return (
    <div>
      <div>
        <div className="body-text strong">Nội dung nháp cho báo cáo</div>
        <div className="meta-line" style={{ marginTop: 2 }}>Xem trước — không phải ô nhập</div>
      </div>
      <div className="report-box" style={{ marginTop: 10 }}>
        {FIELD_LABELS.map(f => (
          <div className="report-field" key={f.key}>
            <div className="k">{f.label}</div>
            <div className="v">{draft[f.key]}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
