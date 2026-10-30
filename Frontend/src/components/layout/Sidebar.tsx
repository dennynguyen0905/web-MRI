import { useState } from 'react'
import { DEPARTMENTS, DEVICES, DOCTORS, MODALITIES, OVERDUE_COUNT, SERVICES, STATUS_COUNTS, TOTAL_STUDIES } from '../../data/mock'
import { compactNumber, formatThousands } from '../../lib/format'
import { STATUS_META, STATUS_ORDER } from '../../lib/status'
import type { Modality, StudyStatus } from '../../lib/types'
import { StatusGlyph } from '../ui/StatusChip'
import { Checkbox, Select, cx } from '../ui/controls'
import { ChevronDown, ChevronRight, Folder, Lock, Settings, Warning } from '../ui/Icon'

export interface SidebarFilters {
  modality: string
  devices: number[]
  statuses: StudyStatus[]
  allStatuses: boolean
  pendingOnly: boolean
  hisSync: string
  bhyt: string
  patientClass: string
  orderingDoctor: string
  serviceId: string
  bodyPart: string
  conclusion: string
}

export const DEFAULT_SIDEBAR_FILTERS: SidebarFilters = {
  modality: '',
  devices: [1],
  statuses: [...STATUS_ORDER].filter(s => s !== 'CHO_THUC_HIEN'),
  allStatuses: true,
  pendingOnly: false,
  hisSync: '',
  bhyt: '',
  patientClass: '',
  orderingDoctor: '',
  serviceId: '',
  bodyPart: '',
  conclusion: '',
}

interface Props {
  filters: SidebarFilters
  onChange: (next: Partial<SidebarFilters>) => void
  visibleCount: number
}

export function Sidebar({ filters, onChange, visibleCount }: Props) {
  return (
    <aside className="flex w-[232px] shrink-0 flex-col overflow-hidden border-r border-line bg-white">
      <DeviceTree filters={filters} onChange={onChange} visibleCount={visibleCount} />

      <div className="min-h-0 flex-1 overflow-y-auto px-2.5 pb-3">
        <StatusFilter filters={filters} onChange={onChange} />

        <button className="focus-ring mt-2 flex h-7 w-full items-center justify-between gap-2 rounded-md border border-[#f7cfcf] bg-white px-2.5 text-[11.5px] font-medium text-[#e02424] transition-colors hover:bg-[#fdf2f2]">
          <span className="flex items-center gap-1.5">
            <Warning size={12} />
            Danh sách ca chụp quá hạn
          </span>
          <span className="flex h-[15px] min-w-[15px] items-center justify-center rounded-full bg-[#e02424] px-1 text-[9.5px] font-bold text-white">
            {OVERDUE_COUNT}
          </span>
        </button>

        <div className="mt-2 space-y-1.5">
          <Select
            placeholder="Trạng thái đồng bộ HIS"
            value={filters.hisSync}
            onChange={e => onChange({ hisSync: e.target.value })}
            options={[
              { value: 'ALL', label: 'Tất cả' },
              { value: 'SYNCED', label: 'Đồng bộ HIS' },
              { value: 'NOT_SYNCED', label: 'Chưa đồng bộ HIS' },
              { value: 'RESULT_SENT', label: 'Đã gửi kết quả' },
              { value: 'FAILED', label: 'Chưa gửi kết quả' },
            ]}
          />
          <Select
            placeholder="Trạng thái BHYT"
            value={filters.bhyt}
            onChange={e => onChange({ bhyt: e.target.value })}
            options={[
              { value: 'VIOLATION', label: 'Vi phạm BHYT' },
              { value: 'OK', label: 'Không vi phạm BHYT' },
            ]}
          />
        </div>

        <div className="mt-3 mb-1.5 flex items-center justify-between">
          <span className="section-label">Thông tin ca chụp</span>
          <Settings size={12} className="text-brand-500" />
        </div>

        <div className="space-y-1.5">
          <Select
            placeholder="Chọn loại bệnh nhân"
            value={filters.patientClass}
            onChange={e => onChange({ patientClass: e.target.value })}
            options={[
              { value: 'INPATIENT', label: 'Nội trú' },
              { value: 'OUTPATIENT', label: 'Ngoại trú' },
            ]}
          />
          <Select
            placeholder="Bác sĩ chỉ định"
            value={filters.orderingDoctor}
            onChange={e => onChange({ orderingDoctor: e.target.value })}
            options={DOCTORS.map(d => ({ value: d.name, label: d.name }))}
          />
          <Select
            placeholder="Chỉ định"
            value={filters.serviceId}
            onChange={e => onChange({ serviceId: e.target.value })}
            options={SERVICES.map(s => ({ value: String(s.id), label: s.name }))}
          />
          <Select
            placeholder="Bộ phận chụp"
            value={filters.bodyPart}
            onChange={e => onChange({ bodyPart: e.target.value })}
            options={[...new Set(SERVICES.map(s => s.bodyPart))].map(b => ({ value: b, label: b }))}
          />
          <Select
            placeholder="Kết luận"
            value={filters.conclusion}
            onChange={e => onChange({ conclusion: e.target.value })}
            options={[
              { value: 'HAS', label: 'Đã có kết luận' },
              { value: 'NONE', label: 'Chưa có kết luận' },
            ]}
          />
        </div>
      </div>
    </aside>
  )
}

/* ------------------------------------------------------------- Cây máy chụp */

function DeviceTree({ filters, onChange, visibleCount }: Props) {
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set())
  const roots = DEPARTMENTS.filter(d => d.parentId === null)

  const toggleNode = (id: number) =>
    setCollapsed(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const toggleDevice = (id: number) =>
    onChange({
      devices: filters.devices.includes(id)
        ? filters.devices.filter(d => d !== id)
        : [...filters.devices, id],
    })

  return (
    <div className="shrink-0 border-b border-line px-2.5 pb-2.5 pt-3">
      <div className="mb-1.5 flex items-center justify-between">
        <span className="section-label">Danh sách máy</span>
        <span className="text-[10.5px] font-semibold text-ink-3">
          {visibleCount} <span className="text-ink-4">| {formatThousands(TOTAL_STUDIES)}</span>
        </span>
      </div>

      <div className="mb-2 flex items-center gap-1.5">
        <span className="flex h-6 items-center rounded-md border border-line bg-line-soft px-1.5 text-[10.5px] font-semibold text-ink-3">
          ALL
        </span>
        <Select
          placeholder="Modality"
          value={filters.modality}
          onChange={e => onChange({ modality: e.target.value })}
          options={MODALITIES.map(m => ({ value: m, label: m as Modality }))}
          className="h-6"
        />
      </div>

      <div className="max-h-[164px] space-y-px overflow-y-auto pr-0.5">
        {roots.map(khoa => {
          const groups = DEPARTMENTS.filter(d => d.parentId === khoa.id)
          const open = !collapsed.has(khoa.id)
          return (
            <div key={khoa.id}>
              <button
                onClick={() => toggleNode(khoa.id)}
                className="flex h-[22px] w-full items-center gap-1 rounded px-0.5 text-left transition-colors hover:bg-line-soft"
              >
                {open ? <ChevronDown size={11} className="text-ink-4" /> : <ChevronRight size={11} className="text-ink-4" />}
                <Lock size={11} className="text-brand-500" />
                <span className="text-[11px] font-semibold tracking-wide text-ink">{khoa.name}</span>
              </button>

              {open && groups.map(nhom => {
                const devices = DEVICES.filter(d => d.departmentId === nhom.id)
                const gopen = !collapsed.has(nhom.id)
                return (
                  <div key={nhom.id} className="pl-3">
                    <button
                      onClick={() => toggleNode(nhom.id)}
                      className="flex h-[22px] w-full items-center gap-1 rounded px-0.5 text-left transition-colors hover:bg-line-soft"
                    >
                      {gopen ? <ChevronDown size={11} className="text-ink-4" /> : <ChevronRight size={11} className="text-ink-4" />}
                      <Folder size={11} className="text-ink-4" />
                      <span className="text-[11px] text-ink-2">{nhom.name}</span>
                    </button>

                    {gopen && devices.map(dev => (
                      <div key={dev.id} className="flex h-[22px] items-center pl-6">
                        <Checkbox
                          checked={filters.devices.includes(dev.id)}
                          onChange={() => toggleDevice(dev.id)}
                          label={<span className="text-[10.5px] text-ink-3">{dev.name}</span>}
                        />
                      </div>
                    ))}
                  </div>
                )
              })}
            </div>
          )
        })}
      </div>
    </div>
  )
}

/* ------------------------------------------------------ Lọc theo trạng thái */

function StatusFilter({ filters, onChange }: Pick<Props, 'filters' | 'onChange'>) {
  const toggle = (s: StudyStatus) =>
    onChange({
      statuses: filters.statuses.includes(s)
        ? filters.statuses.filter(x => x !== s)
        : [...filters.statuses, s],
    })

  const allOn = filters.statuses.length === STATUS_ORDER.length - 1 && !filters.statuses.includes('CHO_THUC_HIEN')

  return (
    <>
      <div className="mt-3 mb-1 section-label">Trạng thái ca chụp</div>

      <label className="flex h-[23px] cursor-pointer items-center gap-2 rounded px-0.5 transition-colors hover:bg-line-soft">
        <span className="w-3.5" />
        <StatusGlyph status="CHO_THUC_HIEN" />
        <span className="flex-1 text-[11.5px] text-ink-2">{STATUS_META.CHO_THUC_HIEN.label}</span>
      </label>

      {STATUS_ORDER.filter(s => s !== 'CHO_THUC_HIEN').map(s => (
        <div key={s} className="flex h-[23px] items-center gap-2 rounded px-0.5 transition-colors hover:bg-line-soft">
          <Checkbox checked={filters.statuses.includes(s)} onChange={() => toggle(s)} />
          <StatusGlyph status={s} />
          <span className="flex-1 text-[11.5px] text-ink-2">{STATUS_META[s].label}</span>
          <span className="text-[10px] tabular-nums text-ink-4">({compactNumber(STATUS_COUNTS[s])})</span>
        </div>
      ))}

      <div className={cx('flex h-[23px] items-center gap-2 rounded px-0.5 transition-colors hover:bg-line-soft')}>
        <Checkbox
          checked={allOn}
          onChange={v => onChange({ statuses: v ? STATUS_ORDER.filter(s => s !== 'CHO_THUC_HIEN') : [] })}
        />
        <span className="w-3" />
        <span className="flex-1 text-[11.5px] text-ink-2">Tất cả</span>
      </div>
    </>
  )
}
