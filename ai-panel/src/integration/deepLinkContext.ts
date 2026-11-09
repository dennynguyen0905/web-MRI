/**
 * GIẢ ĐỊNH: trong một extension OHIF thật, `StudyInstanceUID` của ca đang mở lấy từ context nội
 * bộ của OHIF (`ServicesManager` / `useViewportGrid`, theo đúng yêu cầu mục B — "AI Panel lấy
 * StudyInstanceUID từ context study đang mở trong OHIF"), KHÔNG phải tự đọc URL. Vì gói này là
 * component độc lập (không vendor OHIF, xem README.md), nơi khả dĩ nhất để lấy cùng giá trị khi
 * chạy thật là query string `StudyInstanceUIDs` mà chính RIS đã ghép vào deep-link
 * (`doc/16-fe-viewer-entry-points.md` mục 0: "<OHIF_BASE_URL>/viewer?StudyInstanceUIDs=<uid>"),
 * OHIF cũng đọc chính tham số này để mở ca — nên tái dùng cùng nguồn cho chạy thử ngoài OHIF.
 *
 * `aiPreviouslyReviewed` — cờ cho hành vi T9 (mục 5.3: ca mở lại không tự hiện lại gợi ý cũ) —
 * CHƯA có trong `GET /studies/{id}/viewer-link` thật (`doc/16` mục 3.1 chưa định nghĩa tham số
 * này). Đề xuất ở đây: RIS thêm `&aiPreviouslyReviewed=1` vào deep-link khi ca đang ở lượt đọc
 * thứ hai trở lên (sau khi đã từng lưu báo cáo) — ❓ CHƯA CHỐT, cần người phụ trách IN-23/
 * `viewer-link` xác nhận. Thiếu tham số này, AI Panel coi mọi lần mở là lần đầu (an toàn hơn:
 * hiện đầy đủ READY panel thay vì ẩn — không vi phạm nguyên tắc "không tự bung gợi ý" vì khối
 * gợi ý vẫn phải bấm mới mở, chỉ là không có thêm banner T9 riêng).
 */
export interface RisDeepLinkContext {
  studyInstanceUid: string | null
  aiPreviouslyReviewed: boolean
}

export function readRisDeepLinkContext(location: Location = window.location): RisDeepLinkContext {
  const params = new URLSearchParams(location.search)
  return {
    studyInstanceUid: params.get('StudyInstanceUIDs'),
    aiPreviouslyReviewed: params.get('aiPreviouslyReviewed') === '1',
  }
}
