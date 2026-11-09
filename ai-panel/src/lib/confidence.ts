export type ConfidenceLevel = 'low' | 'medium' | 'high'

/**
 * Ngưỡng quy đổi confidence [0,1] → mức hiển thị — ĐẶT Ở MỘT CHỖ DUY NHẤT (theo yêu cầu nhiệm
 * vụ), đổi tại đây là áp dụng toàn bộ AI Panel. Đây là ngưỡng TẠM, CHƯA được hiệu chỉnh/đánh giá
 * trên dữ liệu thật — doc/11-ai-design.md mục 1.2 ("❓ ngưỡng cụ thể chưa đo được vì chưa chọn
 * model"), mục 7 (rủi ro "điểm tin cậy gây cảm giác chính xác giả"), mục 12 câu 5 (❓ chưa chốt
 * có hiện số thập phân hay chỉ hiện mức). Không hiển thị số thập phân thô ở UI theo đúng mục 7.
 */
const LOW_MAX = 0.5
const MEDIUM_MAX = 0.8

export function confidenceLevel(confidence: number): ConfidenceLevel {
  if (confidence < LOW_MAX) return 'low'
  if (confidence <= MEDIUM_MAX) return 'medium'
  return 'high'
}

export const CONFIDENCE_LABEL: Record<ConfidenceLevel, string> = {
  low: 'Thấp',
  medium: 'Trung bình',
  high: 'Cao',
}

export const CONFIDENCE_DOTS: Record<ConfidenceLevel, string> = {
  low: '●',
  medium: '●●',
  high: '●●●',
}
