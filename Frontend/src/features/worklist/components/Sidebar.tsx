import { useState } from 'react'
import { compactNumber, formatThousands } from '@/shared/lib/format'
import { DONE_STATUSES, STATUS_META } from '@/shared/lib/status'
import type {
  Department, Device, Modality, PatientClass, ServiceItem, StatusCounts, StudyStatus,
} from '@/shared/lib/types'
import { StatusGlyph } from '@/shared/ui/StatusChip'
import { Checkbox, Select } from '@/shared/ui/controls'
import { ChevronDown, ChevronRight, Folder, Lock, Settings } from '@/shared/ui/Icon'

const MODALITIES: Modality[] = ['CT', 'MR', 'CR', 'DX', 'US']

export interface SidebarFilters {
  modality: Modality | ''
  deviceIds: number[]
  statuses: StudyStatus[]
  patientClass: PatientClass | ''
  serviceItemId: string
  bodyPart: string
}

export const DEFAULT_SIDEBAR_FILTERS: SidebarFilters = {
  modality: '',
  deviceIds: [],
  statuses: [...DONE_STATUSES],
  patientClass: '',
  serviceItemId: '',
  bodyPart: '',
}

interface Props {
  filters: SidebarFilters
  onChange: (next: Partial<SidebarFilters>) => void
  departments: Department[]
  devices: Device[]
  serviceItems: ServiceItem[]
  counts: StatusCounts | null
  visibleCount: number
}

/**
 * Bộ lọc trái. Các nhóm lọc đã [HOÃN] ở `doc/01-scope.md` mục 3.2 (trạng thái đồng bộ HIS,
 * vi phạm BHYT, danh sách ca quá hạn) **không** dựng ở đây: backend không có tham số tương ứng
 * trong `GET /studies`, dựng ra sẽ là nút bấm không làm gì.
 */
export function Sidebar({ filters, onChange, departments, devices, serviceItems, counts, visibleCount }: Props) {
  return (
    <aside className="flex w-[232px] shrink-0 flex-col overflow-hidden border-r border-line bg-white">
      <DeviceTree
        filters={filters}
        onChange={onChange}
        departments={departments}
        devices={devices}
        total={counts?.total ?? 0}
        visibleCount={visibleCount}
      />

      <div className="min-h-0 flex-1 overflow-y-auto px-2.5 pb-3">
        <StatusFilter filters={filters} onChange={onChange} counts={counts} />

        <div className="mt-3 mb-1.5 flex items-center justify-between">
          <span className="section-label">Thông tin ca chụp</span>
          <Settings size={12} className="text-brand-500" />
        </div>

        <div className="space-y-1.5">
          <Select
            placeholder="Chọn loại bệnh nhân"
            value={filters.patientClass}
            onChange={e => onChange({ patientClass: e.target.value as PatientClass | '' })}
            options={[
              { value: 'INPATIENT', label: 'Nội trú' },
              { value: 'OUTPATIENT', label: 'Ngoại trú' },
            ]}
          />
          <Select
            placeholder="Chỉ định"
            value={filters.serviceItemId}
            onChange={e => onChange({ serviceItemId: e.target.value })}
            options={serviceItems.map(s => ({ value: String(s.id), label: s.name }))}
          />
          <Select
            placeholder="Bộ phận chụp"
            value={filters.bodyPart}
            onChange={e => onChange({ bodyPart: e.target.value })}
            options={[...new Set(serviceItems.map(s => s.bodyPart))].map(b => ({ value: b, label: b }))}
          />
        </div>
      </div>
    </aside>
  )
}

/* ------------------------------------------------------------- Cây máy chụp */

function DeviceTree({ filters, onChange, departments, devices, total, visibleCount }: {
  filters: SidebarFilters
  onChange: (next: Partial<SidebarFilters>) => void
  departments: Department[]
  devices: Device[]
  total: number
  visibleCount: number
}) {
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set())
  const roots = departments.filter(d => d.parentId === null)

  const toggleNode = (id: number) =>
    setCollapsed(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const toggleDevice = (id: number) =>
    onChange({
      deviceIds: filters.deviceIds.includes(id)
        ? filters.deviceIds.filter(d => d !== id)
        : [...filters.deviceIds, id],
    })

  return (
    <div className="shrink-0 border-b border-line px-2.5 pb-2.5 pt-3">
      <div className="mb-1.5 flex items-center justify-between">
        <span className="section-label">Danh sách máy</span>
        <span className="text-[10.5px] font-semibold text-ink-3">
          {visibleCount} <span className="text-ink-4">| {formatThousands(total)}</span>
        </span>
      </div>

      <div className="mb-2 flex items-center gap-1.5">
        <span className="flex h-6 items-center rounded-md border border-line bg-line-soft px-1.5 text-[10.5px] font-semibold text-ink-3">
          ALL
        </span>
        <Select
          placeholder="Modality"
          value={filters.modality}
          onChange={e => onChange({ modality: e.target.value as Modality | '' })}
          options={MODALITIES.map(m => ({ value: m, label: m }))}
          className="h-6"
        />
      </div>

      <div className="max-h-[164px] space-y-px overflow-y-auto pr-0.5">
        {roots.map(khoa => {
          const groups = departments.filter(d => d.parentId === khoa.id)
          const open = !collapsed.has(khoa.id)
          const direct = devices.filter(d => d.departmentId === khoa.id)
          return (
            <div key={khoa.id}>
              <button
                onClick={() => toggleNode(khoa.id)}
                className="flex h-[22px] w-full items-center gap-1 rounded px-0.5 text-left transition-colors hover:bg-line-soft"
              >
                {open ? <ChevronDown size={11} className="text-ink-4" /> : <ChevronRight size={11} className="text-ink-4" />}
                <Lock size={11} className="text-brand-500" />
                <span className="text-[11px] font-semibold uppercase tracking-wide text-ink">{khoa.name}</span>
              </button>

              {open && direct.map(dev => (
                <DeviceRow key={dev.id} device={dev} checked={filters.deviceIds.includes(dev.id)} onToggle={toggleDevice} indent="pl-6" />
              ))}

              {open && groups.map(nhom => {
                const groupDevices = devices.filter(d => d.departmentId === nhom.id)
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
                    {gopen && groupDevices.map(dev => (
                      <DeviceRow key={dev.id} device={dev} checked={filters.deviceIds.includes(dev.id)} onToggle={toggleDevice} indent="pl-6" />
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

function DeviceRow({ device, checked, onToggle, indent }: {
  device: Device
  checked: boolean
  onToggle: (id: number) => void
  indent: string
}) {
  return (
    <div className={`flex h-[22px] items-center ${indent}`}>
      <Checkbox
        checked={checked}
        onChange={() => onToggle(device.id)}
        label={<span className="text-[10.5px] text-ink-3">{device.name}</span>}
      />
    </div>
  )
}

/* ------------------------------------------------------ Lọc theo trạng thái */

function StatusFilter({ filters, onChange, counts }: {
  filters: SidebarFilters
  onChange: (next: Partial<SidebarFilters>) => void
  counts: StatusCounts | null
}) {
  const toggle = (s: StudyStatus) =>
    onChange({
      statuses: filters.statuses.includes(s)
        ? filters.statuses.filter(x => x !== s)
        : [...filters.statuses, s],
    })

  const allOn = DONE_STATUSES.every(s => filters.statuses.includes(s))

  return (
    <>
      <div className="mt-3 mb-1 section-label">Trạng thái ca chụp</div>

      {/* Tab "Chờ" đã là SCHEDULED nên dòng này chỉ để đối chiếu, không bật/tắt được. */}
      <div className="flex h-[23px] items-center gap-2 rounded px-0.5">
        <span className="w-3.5" />
        <StatusGlyph status="SCHEDULED" />
        <span className="flex-1 text-[11.5px] text-ink-2">{STATUS_META.SCHEDULED.label}</span>
        <span className="text-[10px] tabular-nums text-ink-4">({compactNumber(counts?.byStatus.SCHEDULED ?? 0)})</span>
      </div>

      {DONE_STATUSES.map(s => (
        <div key={s} className="flex h-[23px] items-center gap-2 rounded px-0.5 transition-colors hover:bg-line-soft">
          <Checkbox checked={filters.statuses.includes(s)} onChange={() => toggle(s)} />
          <StatusGlyph status={s} />
          <span className="flex-1 text-[11.5px] text-ink-2">{STATUS_META[s].label}</span>
          <span className="text-[10px] tabular-nums text-ink-4">({compactNumber(counts?.byStatus[s] ?? 0)})</span>
        </div>
      ))}

      <div className="flex h-[23px] items-center gap-2 rounded px-0.5 transition-colors hover:bg-line-soft">
        <Checkbox
          checked={allOn}
          onChange={v => onChange({ statuses: v ? [...DONE_STATUSES] : [] })}
        />
        <span className="w-3" />
        <span className="flex-1 text-[11.5px] text-ink-2">Tất cả</span>
      </div>
    </>
  )
}
