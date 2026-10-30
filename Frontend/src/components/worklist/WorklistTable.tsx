import { type ReactNode, useState } from 'react'
import { DOCTORS } from '../../data/mock'
import { calcAge, dash, formatDob, formatTimeShort, genderLabel } from '../../lib/format'
import type { WorklistRow } from '../../lib/types'
import { StatusChip } from '../ui/StatusChip'
import { Checkbox, EmptyState, Input, Popover, Select, Tag, cx, usePopover } from '../ui/controls'
import { Pin, Settings, Sort } from '../ui/Icon'

export type ColumnKey =
  | 'index' | 'status' | 'patientCode' | 'patientInfo' | 'fullName' | 'dob' | 'age' | 'gender'
  | 'service' | 'orderingDoctor' | 'bodyPart' | 'conclusion' | 'scannedAt' | 'startedAt' | 'completedAt'
  | 'orderCode' | 'groupCode' | 'groupName' | 'recordCode' | 'specimenCode' | 'archiveCode'

export interface Column {
  key: ColumnKey
  label: string
  width?: string
  align?: 'left' | 'center'
  sortable?: boolean
  filterable?: boolean
  render: (row: WorklistRow, index: number) => ReactNode
}

/** Các cột bật/tắt được trong popover "Cột hiển thị" (ảnh 08). */
export const OPTIONAL_COLUMNS: { key: ColumnKey; label: string; defaultOn: boolean }[] = [
  { key: 'orderingDoctor', label: 'Bác sĩ chỉ định', defaultOn: true },
  { key: 'service', label: 'Chỉ định', defaultOn: true },
  { key: 'bodyPart', label: 'Bộ phận chụp', defaultOn: true },
  { key: 'conclusion', label: 'Kết luận', defaultOn: true },
  { key: 'groupCode', label: 'Mã đoàn khám', defaultOn: false },
  { key: 'groupName', label: 'Tên đoàn khám', defaultOn: false },
  { key: 'recordCode', label: 'Mã hồ sơ', defaultOn: false },
  { key: 'specimenCode', label: 'Mã bệnh phẩm', defaultOn: false },
  { key: 'archiveCode', label: 'Mã lưu trữ', defaultOn: false },
  { key: 'startedAt', label: 'Bắt đầu', defaultOn: false },
  { key: 'completedAt', label: 'Kết thúc', defaultOn: false },
]

/* ------------------------------------------------------------ Định nghĩa cột */

const cellMuted = 'text-ink-3'

const doctorDept = (name: string) =>
  DOCTORS.find(d => d.name === name)?.dept ?? 'Trung tâm khám sức khoẻ'

export const COLUMNS: Record<ColumnKey, Column> = {
  index: {
    key: 'index', label: '#', width: '36px', align: 'center',
    render: (_r, i) => <span className="tabular-nums text-ink-4">{i + 1}</span>,
  },
  status: {
    key: 'status', label: 'Trạng thái', width: '128px', sortable: true, filterable: true,
    render: r => (
      <div className="leading-tight">
        <StatusChip status={r.study.status} />
        <div className="mt-0.5 pl-[18px] text-[10.5px] text-ink-4">{r.device?.name ?? '—'}</div>
      </div>
    ),
  },
  patientCode: {
    key: 'patientCode', label: 'Mã bệnh nhân', width: '108px', sortable: true, filterable: true,
    render: r => <span className="tabular-nums">{r.patient.patientCode}</span>,
  },
  patientInfo: {
    key: 'patientInfo', label: 'Thông tin bệnh nhân', width: '196px', sortable: true, filterable: true,
    render: r => (
      <div className="leading-tight">
        <div className="flex items-center gap-1.5">
          <span className="font-semibold text-ink">{r.patient.fullName}</span>
          {r.order.patientClass === 'INPATIENT' ? <Tag>DV</Tag> : <Tag tone="green">BHYT-X</Tag>}
        </div>
        <div className="mt-0.5 text-[10.5px] tabular-nums text-ink-4">
          {r.patient.patientCode} · {genderLabel(r.patient.gender).charAt(0)} ·{' '}
          {formatDob(r.patient.dateOfBirth, r.patient.dobPrecision)}
        </div>
      </div>
    ),
  },
  fullName: {
    key: 'fullName', label: 'Họ tên', width: '168px', sortable: true, filterable: true,
    render: r => (
      <div className="flex items-center gap-1.5">
        <span className="font-semibold text-ink">{r.patient.fullName}</span>
        {r.order.patientClass === 'INPATIENT' && <Tag>DV</Tag>}
      </div>
    ),
  },
  dob: {
    key: 'dob', label: 'Ngày sinh', width: '88px', sortable: true,
    render: r => <span className="tabular-nums">{formatDob(r.patient.dateOfBirth, r.patient.dobPrecision)}</span>,
  },
  age: {
    key: 'age', label: 'Tuổi', width: '48px', align: 'center', sortable: true,
    render: r => <span className="tabular-nums">{calcAge(r.patient.dateOfBirth, r.patient.dobPrecision)}</span>,
  },
  gender: {
    key: 'gender', label: 'Giới tính', width: '64px', sortable: true,
    render: r => genderLabel(r.patient.gender),
  },
  service: {
    key: 'service', label: 'Chỉ định', sortable: true, filterable: true,
    render: r => (
      <div className="leading-tight">
        <span className="line-clamp-2 text-ink-2">{r.service.name}</span>
      </div>
    ),
  },
  orderingDoctor: {
    key: 'orderingDoctor', label: 'Bác sĩ chỉ định', width: '176px', sortable: true, filterable: true,
    render: r => (
      <div className="leading-tight">
        <div className="font-medium text-ink-2">{r.order.orderingDoctorName}</div>
        <div className="mt-0.5 text-[10.5px] text-ink-4">{doctorDept(r.order.orderingDoctorName)}</div>
      </div>
    ),
  },
  bodyPart: {
    key: 'bodyPart', label: 'Bộ phận chụp', width: '112px', sortable: true,
    render: r => <span className={cellMuted}>{r.service.bodyPart}</span>,
  },
  conclusion: {
    key: 'conclusion', label: 'Chẩn đoán', width: '150px',
    render: r => <span className={cx(cellMuted, 'line-clamp-2')}>{dash(r.study.conclusion)}</span>,
  },
  scannedAt: {
    key: 'scannedAt', label: 'Thời gian chụp', width: '124px', sortable: true,
    render: r => (
      <div className="leading-tight tabular-nums">
        <div>{formatTimeShort(r.study.scheduledAt)}</div>
        <div className="mt-0.5 text-[10.5px] text-ink-4">{r.service.modality} · {r.service.code}</div>
      </div>
    ),
  },
  startedAt: {
    key: 'startedAt', label: 'Bắt đầu', width: '118px', sortable: true,
    render: r => <span className="tabular-nums">{formatTimeShort(r.study.startedAt)}</span>,
  },
  completedAt: {
    key: 'completedAt', label: 'Kết thúc', width: '118px', sortable: true,
    render: r => <span className="tabular-nums">{formatTimeShort(r.study.completedAt)}</span>,
  },
  orderCode: {
    key: 'orderCode', label: 'Mã phiếu', width: '96px', sortable: true, filterable: true,
    render: r => <span className="tabular-nums">{r.order.orderCode}</span>,
  },
  groupCode: { key: 'groupCode', label: 'Mã đoàn khám', width: '104px', render: () => <span className={cellMuted}>—</span> },
  groupName: { key: 'groupName', label: 'Tên đoàn khám', width: '120px', render: () => <span className={cellMuted}>—</span> },
  recordCode: { key: 'recordCode', label: 'Mã hồ sơ', width: '96px', render: r => <span className="tabular-nums">{r.study.accessionNumber}</span> },
  specimenCode: { key: 'specimenCode', label: 'Mã bệnh phẩm', width: '104px', render: () => <span className={cellMuted}>—</span> },
  archiveCode: { key: 'archiveCode', label: 'Mã lưu trữ', width: '96px', render: () => <span className={cellMuted}>—</span> },
}

/* ------------------------------------------------------------------- Bảng */

interface Props {
  rows: WorklistRow[]
  columns: ColumnKey[]
  selectedId: number | null
  onSelect: (row: WorklistRow) => void
  onOpen?: (row: WorklistRow) => void
  visibleOptional: Set<ColumnKey>
  onToggleOptional: (key: ColumnKey) => void
  columnFilterMode: boolean
  onToggleColumnFilterMode: () => void
  columnFilters: Partial<Record<ColumnKey, string>>
  onColumnFilter: (key: ColumnKey, value: string) => void
  sort: { key: ColumnKey; dir: 'asc' | 'desc' } | null
  onSort: (key: ColumnKey) => void
}

export function WorklistTable({
  rows, columns, selectedId, onSelect, onOpen,
  visibleOptional, onToggleOptional,
  columnFilterMode, onToggleColumnFilterMode,
  columnFilters, onColumnFilter, sort, onSort,
}: Props) {
  const colMenu = usePopover()
  const cols = columns
    .filter(k => !OPTIONAL_COLUMNS.some(o => o.key === k) || visibleOptional.has(k))
    .map(k => COLUMNS[k])

  return (
    <div className="relative flex min-h-0 flex-1 flex-col">
      {/* Popover nằm ngoài vùng cuộn của bảng để không bị cắt. */}
      <div className="absolute left-2 top-7 z-40">
        <ColumnMenu open={colMenu.open} onClose={colMenu.close} visible={visibleOptional} onToggle={onToggleOptional} />
      </div>

      <div className="min-h-0 flex-1 overflow-auto">
        <table className="w-full min-w-[920px] border-collapse text-[12px]">
          <thead className="sticky top-0 z-20">
            <tr className="bg-[#e7eefb]">
              <th className="w-[46px] border-b border-brand-100 px-1.5 py-0 text-left">
                <div className="relative flex h-[30px] items-center gap-0.5">
                  <button
                    onClick={colMenu.toggle}
                    title="Cột hiển thị"
                    className="focus-ring rounded p-0.5 text-brand-700 transition-colors hover:bg-brand-100"
                  >
                    <Settings size={12} />
                  </button>
                  <button
                    onClick={onToggleColumnFilterMode}
                    title="Lọc theo cột"
                    className={cx(
                      'focus-ring rounded p-0.5 transition-colors hover:bg-brand-100',
                      columnFilterMode ? 'bg-brand-200 text-brand-800' : 'text-brand-700',
                    )}
                  >
                    <Pin size={12} />
                  </button>
                </div>
              </th>

              {cols.map(c => (
                <th
                  key={c.key}
                  style={{ width: c.width }}
                  className={cx(
                    'border-b border-brand-100 px-2 py-0 text-[11.5px] font-semibold text-[#1e3a6b]',
                    c.align === 'center' ? 'text-center' : 'text-left',
                  )}
                >
                  <button
                    disabled={!c.sortable}
                    onClick={() => c.sortable && onSort(c.key)}
                    className={cx(
                      'flex h-[30px] w-full items-center gap-1',
                      c.align === 'center' && 'justify-center',
                      c.sortable && 'cursor-pointer hover:text-brand-700',
                    )}
                  >
                    {c.label}
                    {c.sortable && (
                      <Sort
                        size={11}
                        className={cx(
                          'shrink-0 transition-colors',
                          sort?.key === c.key ? 'text-brand-600' : 'text-[#9fb4d8]',
                        )}
                      />
                    )}
                  </button>
                </th>
              ))}
            </tr>

            {columnFilterMode && (
              <tr className="bg-white">
                <td className="border-b border-line px-1.5 py-1" />
                {cols.map(c => (
                  <td key={c.key} className="border-b border-line px-1.5 py-1">
                    {c.key === 'status' ? (
                      <Select
                        value={columnFilters.status ?? ''}
                        onChange={e => onColumnFilter('status', e.target.value)}
                        placeholder="Tất cả"
                        options={[]}
                        className="h-6"
                      />
                    ) : c.filterable ? (
                      <Input
                        placeholder={c.label}
                        value={columnFilters[c.key] ?? ''}
                        onChange={e => onColumnFilter(c.key, e.target.value)}
                        className="h-6"
                      />
                    ) : null}
                  </td>
                ))}
              </tr>
            )}
          </thead>

          <tbody>
            {rows.map((row, i) => {
              const selected = row.study.id === selectedId
              return (
                <tr
                  key={row.study.id}
                  onClick={() => onSelect(row)}
                  onDoubleClick={() => onOpen?.(row)}
                  className={cx(
                    'cursor-pointer border-b border-line-soft transition-colors',
                    selected ? 'bg-[#e8f0fe]' : 'hover:bg-[#f6f9ff]',
                  )}
                >
                  <td className="relative px-1.5 py-1.5 align-top">
                    {selected && <span className="absolute inset-y-0 left-0 w-[3px] bg-brand-600" />}
                  </td>
                  {cols.map(c => (
                    <td
                      key={c.key}
                      className={cx('px-2 py-1.5 align-top', c.align === 'center' && 'text-center')}
                    >
                      {c.render(row, i)}
                    </td>
                  ))}
                </tr>
              )
            })}
          </tbody>
        </table>

        {rows.length === 0 && (
          <EmptyState
            title="Không có ca chụp nào"
            hint="Thay đổi khoảng thời gian hoặc bộ lọc để xem dữ liệu"
          />
        )}
      </div>
    </div>
  )
}

/* --------------------------------------------------- Popover Cột hiển thị */

function ColumnMenu({ open, onClose, visible, onToggle }: {
  open: boolean
  onClose: () => void
  visible: Set<ColumnKey>
  onToggle: (k: ColumnKey) => void
}) {
  return (
    <Popover open={open} onClose={onClose} align="left" className="w-[176px] p-2.5">
      <div className="mb-1.5 section-label">Cột hiển thị</div>
      <div className="space-y-1.5">
        {OPTIONAL_COLUMNS.map(c => (
          <div key={c.key}>
            <Checkbox
              checked={visible.has(c.key)}
              onChange={() => onToggle(c.key)}
              label={<span className="text-ink-2">{c.label}</span>}
            />
          </div>
        ))}
      </div>
    </Popover>
  )
}

export function useOptionalColumns() {
  const [visible, setVisible] = useState<Set<ColumnKey>>(
    () => new Set(OPTIONAL_COLUMNS.filter(c => c.defaultOn).map(c => c.key)),
  )
  const toggle = (k: ColumnKey) =>
    setVisible(prev => {
      const next = new Set(prev)
      if (next.has(k)) next.delete(k)
      else next.add(k)
      return next
    })
  return { visible, toggle }
}
