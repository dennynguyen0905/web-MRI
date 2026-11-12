import { StatusBadge } from './StatusBadge'

/**
 * `ai_status = PENDING` — doc/11-ai-design.md mục 5.2.3. Chỉ có nút "Làm mới" gọi lại trạng
 * thái một lần, KHÔNG tự poll liên tục (tránh phức tạp — đúng yêu cầu của tài liệu).
 */
export function PendingState({ onRefresh, refreshing }: { onRefresh: () => void; refreshing: boolean }) {
  return (
    <>
      <StatusBadge kind="pending" />
      <div className="spin-row">
        <div className="spinner" />
        <div className="body-text">Đang phân tích ảnh (ước tính còn vài phút)</div>
      </div>
      <button type="button" className="btn btn-ghost" style={{ width: 'fit-content', padding: '0 14px' }} onClick={onRefresh} disabled={refreshing}>
        {refreshing ? 'Đang làm mới…' : 'Làm mới'}
      </button>
    </>
  )
}
