/**
 * Banner T9 — doc/11-ai-design.md mục 5.3. Khi mở lại ca đã từng đọc, AI Panel KHÔNG tự hiện
 * lại gợi ý cũ — chỉ hiện dòng này + nút bấm chủ động để xem lại (chống "kém tập trung khi đọc
 * lại", mục 7). Xem integration/deepLinkContext.ts cho giả định cách panel biết đây là lượt đọc
 * thứ hai (cờ `aiPreviouslyReviewed` trên deep-link, ❓ chưa chốt phía RIS).
 */
export function PriorReadingNotice({ onReveal }: { onReveal: () => void }) {
  return (
    <div className="prior-notice">
      <p>Ca này đã có gợi ý AI từ lượt đọc trước.</p>
      <button type="button" className="retry-btn" onClick={onReveal}>
        Xem lại gợi ý AI
      </button>
    </div>
  )
}
