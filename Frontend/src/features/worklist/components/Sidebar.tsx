import { useState } from 'react'
import { formatThousands } from '@/shared/lib/format'
import { DONE_STATUSES } from '@/shared/lib/status'
import type {
  Department, Device, Modality, PatientClass, ServiceItem, StatusCounts, StudyStatus,
} from '@/shared/lib/types'
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
 * Bộ lọc trái (Danh sách máy + Thông tin ca chụp).
 * Bộ lọc "Trạng thái ca chụp" đã được chuyển lên thanh công cụ phía trên bảng worklist
 * cạnh hai tab "Chờ" và "Đã thực hiện".
 */
export function Sidebar({ filters, onChange, departments, devices, serviceItems, counts, visibleCount }: Props) {
  return (
    <aside className="flex w-[240px] shrink-0 flex-col overflow-hidden border-r border-line bg-white desktop:w-[300px]">
      <DeviceTree
        filters={filters}
        onChange={onChange}
        departments={departments}
        devices={devices}
        total={counts?.total ?? 0}
        visibleCount={visibleCount}
      />

      <div className="min-h-0 flex-1 overflow-y-auto px-2.5 pb-3 desktop:px-3 desktop:pb-4">
        <div className="mt-3 mb-1.5 flex items-center justify-between desktop:mt-4">
          <span className="section-label">Thông tin ca chụp</span>
          <Settings size={12} className="text-brand-500" />
        </div>

        <div className="space-y-1.5 desktop:space-y-2">
          <Select
            placeholder="Chọn loại bệnh nhân"
            value={filters.patientClass}
            onChange={e => onChange({ patientClass: e.target.value as PatientClass | '' })}
            options={[
              { value: 'INPATIENT', label: 'Nội trú' },
              { value: 'OUTPATIENT', label: 'Ngoại trú' },
            ]}
            className="desktop:h-8 desktop:text-[12.5px]"
          />
          <Select
            placeholder="Chỉ định"
            value={filters.serviceItemId}
            onChange={e => onChange({ serviceItemId: e.target.value })}
            options={serviceItems.map(s => ({ value: String(s.id), label: s.name }))}
            className="desktop:h-8 desktop:text-[12.5px]"
          />
          <Select
            placeholder="Bộ phận chụp"
            value={filters.bodyPart}
            onChange={e => onChange({ bodyPart: e.target.value })}
            options={[...new Set(serviceItems.map(s => s.bodyPart))].map(b => ({ value: b, label: b }))}
            className="desktop:h-8 desktop:text-[12.5px]"
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
        <span className="text-[10.5px] font-semibold text-ink-3 desktop:text-[11.5px]">
          {visibleCount} <span className="text-ink-4">| {formatThousands(total)}</span>
        </span>
      </div>

      <div className="mb-2 flex items-center gap-1.5 desktop:gap-2">
        <span className="flex h-6 items-center rounded-md border border-line bg-line-soft px-1.5 text-[10.5px] font-semibold text-ink-3 desktop:h-7 desktop:px-2 desktop:text-[11.5px]">
          ALL
        </span>
        <Select
          placeholder="Modality"
          value={filters.modality}
          onChange={e => onChange({ modality: e.target.value as Modality | '' })}
          options={MODALITIES.map(m => ({ value: m, label: m }))}
          className="h-6 desktop:h-7"
        />
      </div>

      <div className="max-h-[260px] space-y-px overflow-y-auto pr-0.5 desktop:max-h-[340px]">
        {roots.map(khoa => {
          const groups = departments.filter(d => d.parentId === khoa.id)
          const open = !collapsed.has(khoa.id)
          const direct = devices.filter(d => d.departmentId === khoa.id)
          return (
            <div key={khoa.id}>
              <button
                onClick={() => toggleNode(khoa.id)}
                className="flex h-[22px] w-full items-center gap-1 rounded px-0.5 text-left transition-colors hover:bg-line-soft desktop:h-[26px] desktop:gap-1.5"
              >
                {open ? <ChevronDown size={11} className="text-ink-4" /> : <ChevronRight size={11} className="text-ink-4" />}
                <Lock size={11} className="text-brand-500" />
                <span className="text-[11px] font-semibold uppercase tracking-wide text-ink desktop:text-[12px]">{khoa.name}</span>
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
                      className="flex h-[22px] w-full items-center gap-1 rounded px-0.5 text-left transition-colors hover:bg-line-soft desktop:h-[26px] desktop:gap-1.5"
                    >
                      {gopen ? <ChevronDown size={11} className="text-ink-4" /> : <ChevronRight size={11} className="text-ink-4" />}
                      <Folder size={11} className="text-ink-4" />
                      <span className="text-[11px] text-ink-2 desktop:text-[12px]">{nhom.name}</span>
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
    <div className={`flex h-[22px] items-center desktop:h-[26px] ${indent}`}>
      <Checkbox
        checked={checked}
        onChange={() => onToggle(device.id)}
        label={<span className="text-[10.5px] text-ink-3 desktop:text-[11.5px]">{device.name}</span>}
      />
    </div>
  )
}
