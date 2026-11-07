import { formatTimeShort } from '@/shared/lib/format'
import type { WorklistRow } from '@/shared/lib/types'
import { StatusChip } from '@/shared/ui/StatusChip'
import { EmptyState, cx } from '@/shared/ui/controls'
import { Pin, Settings } from '@/shared/ui/Icon'

const HEADERS = [
  { label: 'Mã phiếu', w: '110px' },
  { label: 'Mã BN', w: '100px' },
  { label: 'Họ tên', w: '190px' },
  { label: 'Trạng thái', w: '130px' },
  { label: 'Máy chụp', w: '150px' },
  { label: 'TG chỉ định', w: '130px' },
  { label: 'TG chụp', w: '130px' },
]

/** Bảng các ca chụp khác của cùng bệnh nhân (IN-08). */
export function RelatedOrders({ rows, selectedId, onSelect, loading }: {
  rows: WorklistRow[]
  selectedId: number | null
  onSelect: (row: WorklistRow) => void
  loading: boolean
}) {
  return (
    <div className="flex h-[150px] shrink-0 flex-col overflow-hidden border-t border-line bg-white">
      <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full border-collapse text-[12px]">
          <thead className="sticky top-0 z-10">
            <tr className="bg-[#e7eefb]">
              <th className="w-[46px] border-b border-brand-100 px-1.5 py-0 text-left">
                <div className="flex h-[28px] items-center gap-0.5 text-brand-700">
                  <Settings size={12} />
                  <Pin size={12} />
                </div>
              </th>
              {HEADERS.map(h => (
                <th
                  key={h.label}
                  style={{ width: h.w }}
                  className="border-b border-brand-100 px-2 py-0 text-left text-[11.5px] font-semibold text-[#1e3a6b]"
                >
                  <div className="flex h-[28px] items-center">{h.label}</div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr
                key={r.study.id}
                onClick={() => onSelect(r)}
                className={cx(
                  'cursor-pointer border-b border-line-soft transition-colors',
                  r.study.id === selectedId ? 'bg-[#e8f0fe]' : 'hover:bg-[#f6f9ff]',
                )}
              >
                <td className="px-2 py-1.5 text-[11px] tabular-nums text-ink-4">{i + 1}</td>
                <td className="px-2 py-1.5 tabular-nums">{r.order.orderCode}</td>
                <td className="px-2 py-1.5 tabular-nums">{r.patient.patientCode}</td>
                <td className="px-2 py-1.5 font-semibold text-ink">{r.patient.fullName}</td>
                <td className="px-2 py-1.5"><StatusChip status={r.study.status} /></td>
                <td className="px-2 py-1.5 text-ink-3">{r.device?.name ?? '—'}</td>
                <td className="px-2 py-1.5 tabular-nums text-ink-3">{formatTimeShort(r.order.orderedAt)}</td>
                <td className="px-2 py-1.5 tabular-nums text-ink-3">{formatTimeShort(r.study.completedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {rows.length === 0 && !loading && (
          <EmptyState title="Chưa chọn ca chụp" hint="Các ca chụp của cùng bệnh nhân sẽ hiển thị tại đây" />
        )}
      </div>
    </div>
  )
}
