/** `ai_status` hiển thị — màu/nhãn đúng bảng mục 5.2.1 của doc/11-ai-design.md. */
export type AiStatusBadgeKind = 'idle' | 'pending' | 'ready' | 'failed' | 'timeout'

const LABEL: Record<AiStatusBadgeKind, string> = {
  idle: 'Chưa xử lý',
  pending: 'Đang phân tích…',
  ready: 'Có gợi ý AI',
  failed: 'AI lỗi',
  timeout: 'AI không phản hồi',
}

export function StatusBadge({ kind }: { kind: AiStatusBadgeKind }) {
  return (
    <span className={`badge ${kind}`}>
      <span className="dot" />
      {LABEL[kind]}
    </span>
  )
}
