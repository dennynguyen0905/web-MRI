import { ChevronLeft, ChevronRight } from '../ui/Icon'
import { Select, cx } from '../ui/controls'

interface Props {
  page: number
  pageSize: number
  total: number
  onPage: (p: number) => void
  onPageSize: (n: number) => void
}

/** Dải số trang rút gọn: 1 2 3 … n */
function pageItems(current: number, last: number): (number | '…')[] {
  if (last <= 7) return Array.from({ length: last }, (_, i) => i + 1)
  const out: (number | '…')[] = []
  const add = (v: number | '…') => out.push(v)
  add(1)
  const from = Math.max(2, current - 1)
  const to = Math.min(last - 1, current + 1)
  if (from > 2) add('…')
  for (let i = from; i <= to; i++) add(i)
  if (to < last - 1) add('…')
  add(last)
  return out
}

export function Pagination({ page, pageSize, total, onPage, onPageSize }: Props) {
  const last = Math.max(1, Math.ceil(total / pageSize))
  const start = total === 0 ? 0 : (page - 1) * pageSize + 1
  const end = Math.min(total, page * pageSize)

  return (
    <div className="flex h-10 shrink-0 items-center gap-3 border-t border-line bg-white px-3">
      <span className="text-[11.5px] text-ink-3">
        Hiển thị {start}–{end} trên {total} ca chụp
      </span>

      <div className="flex-1" />

      <div className="flex items-center gap-0.5">
        <PageBtn disabled={page === 1} onClick={() => onPage(page - 1)}>
          <ChevronLeft size={13} />
        </PageBtn>
        {pageItems(page, last).map((p, i) =>
          p === '…' ? (
            <span key={`gap-${i}`} className="px-1 text-[11.5px] text-ink-4">…</span>
          ) : (
            <button
              key={p}
              onClick={() => onPage(p)}
              className={cx(
                'focus-ring h-6 min-w-6 rounded px-1.5 text-[11.5px] font-medium tabular-nums transition-colors',
                p === page ? 'bg-brand-600 text-white' : 'text-ink-2 hover:bg-line-soft',
              )}
            >
              {p}
            </button>
          ),
        )}
        <PageBtn disabled={page === last} onClick={() => onPage(page + 1)}>
          <ChevronRight size={13} />
        </PageBtn>
      </div>

      <div className="w-[88px]">
        <Select
          value={String(pageSize)}
          onChange={e => onPageSize(Number(e.target.value))}
          options={[20, 50, 100].map(n => ({ value: String(n), label: `${n} / trang` }))}
          className="h-6"
        />
      </div>
    </div>
  )
}

function PageBtn({ disabled, onClick, children }: { disabled: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      className="focus-ring flex h-6 w-6 items-center justify-center rounded text-ink-3 transition-colors hover:bg-line-soft disabled:cursor-not-allowed disabled:opacity-35"
    >
      {children}
    </button>
  )
}
