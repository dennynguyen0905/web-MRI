/**
 * Danh sách cửa sổ OHIF do chính RIS mở bằng `window.open` (xem `../api/viewerApi.ts`). Dùng để
 * kiểm `event.source` khi nhận postMessage AI_DRAFT_APPLY/AI_DRAFT_DISMISS từ OHIF
 * (`doc/11-ai-design.md` mục 4, 5.2.5.d) — đúng origin chưa đủ, phải đúng cả cửa sổ đã mở.
 *
 * Giới hạn đã biết: registry sống trong bộ nhớ của tab RIS, mất khi tab RIS tải lại — cửa sổ OHIF
 * đã mở trước đó (nếu vẫn còn mở) sẽ không còn được RIS nhận diện cho tới khi bác sĩ bấm "Xem ảnh"
 * lại. Nêu trong báo cáo cuối của task nối kênh (rủi ro còn lại), không tự xử lý thêm ở đây.
 */
const openedWindows = new Set<Window>()

export function registerOhifWindow(win: Window | null): void {
  if (win) openedWindows.add(win)
}

export function isKnownOhifWindow(source: MessageEventSource | null): boolean {
  if (!source) return false
  for (const win of openedWindows) {
    if (win.closed) {
      openedWindows.delete(win)
      continue
    }
    if (win === source) return true
  }
  return false
}
