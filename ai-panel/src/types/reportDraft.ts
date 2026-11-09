import type { AiFinding } from './aiAnalysis'

/**
 * 5 trường nội dung báo cáo — khớp tên trường `draft_*` của `ai_finding` (doc/11-ai-design.md
 * mục 3.2) và khớp nhãn hiển thị của `ReportModal` thật trong RIS
 * (`Frontend/src/features/reports/components/ReportModal.tsx`: Kỹ thuật/Mô tả/Chẩn đoán/
 * Kết luận/Khuyến nghị) để nội dung nháp dán thẳng vào đúng 5 ô, không cần ánh xạ lại tên trường.
 */
export interface ReportDraftFields {
  technique: string
  findings: string
  diagnosis: string
  conclusion: string
  recommendation: string
}

/**
 * GIẢ ĐỊNH (chưa chốt, xem README.md "Điểm giả định"): schema `ai_finding` gắn 5 trường nháp
 * vào TỪNG finding (mỗi finding có draft_* riêng), nhưng AI Panel chỉ chép MỘT bộ nháp vào
 * form báo cáo chính (form có đúng 5 ô, không phải một ô cho mỗi finding). Ở đây chọn finding
 * có `confidence` cao nhất làm nguồn nội dung nháp cho ba nút hành động — vì đó là gợi ý AI tin
 * tưởng nhất. Đổi được nếu nhóm AI Backend thiết kế lại cách gộp nháp từ nhiều finding.
 */
export function primaryDraftOf(findings: AiFinding[]): { draft: ReportDraftFields; source: AiFinding } | null {
  if (findings.length === 0) return null
  const source = [...findings].sort((a, b) => b.confidence - a.confidence)[0]
  return {
    source,
    draft: {
      technique: source.draft_technique,
      findings: source.draft_findings,
      diagnosis: source.draft_diagnosis,
      conclusion: source.draft_conclusion,
      recommendation: source.draft_recommendation,
    },
  }
}
