import { StatusBadge } from './StatusBadge'

/** `ai_status = NOT_TRIGGERED` — doc/11-ai-design.md mục 5.2.2. */
export function NotTriggeredState({ reason }: { reason?: string }) {
  return (
    <div className="empty-wrap">
      <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 8v4M12 16h.01" />
      </svg>
      <StatusBadge kind="idle" />
      <div className="t1">Chưa có kết quả AI cho ca này.</div>
      <div className="t2">{reason ?? 'Loại ảnh này chưa được AI hỗ trợ cho ca này.'}</div>
    </div>
  )
}
