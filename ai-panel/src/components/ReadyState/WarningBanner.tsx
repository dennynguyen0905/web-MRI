/**
 * Dòng cảnh báo cố định, KHÔNG thể ẩn — doc/11-ai-design.md mục 5.2.5.c. Nguyên văn câu chữ lấy
 * đúng từ tài liệu/mockup, không diễn đạt lại.
 */
export function WarningBanner() {
  return (
    <div className="warn-box">
      <span className="mark">!</span>
      <p>
        Đây là gợi ý do AI sinh ra, không phải chẩn đoán chính thức. AI chỉ đánh dấu một số vùng
        nghi ngờ, không thay thế việc đọc toàn bộ ảnh. Bác sĩ chịu trách nhiệm xác nhận.
      </p>
    </div>
  )
}
