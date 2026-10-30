import { useState } from 'react'
import { ROLE_META, can, deniedReason } from '../../lib/auth'
import type { AppUser } from '../../lib/types'
import { Button, IconButton, Input, Popover, Toggle2, cx, usePopover } from '../ui/controls'
import {
  Check, ChevronDown, Download, Lock, Monitor, Plus, Printer, Refresh, Settings, User, UserPlus, X,
} from '../ui/Icon'

export type DateRangeKey = 'TODAY' | 'YESTERDAY' | 'D3' | 'D7' | 'ALL'

export const DATE_RANGES: { key: DateRangeKey; label: string }[] = [
  { key: 'TODAY', label: 'Hôm nay' },
  { key: 'YESTERDAY', label: 'Hôm qua' },
  { key: 'D3', label: '-3 ngày' },
  { key: 'D7', label: '-7 ngày' },
  { key: 'ALL', label: 'Tất cả' },
]

export interface TopBarState {
  patientCode: string
  patientName: string
  visitCode: string
  range: DateRangeKey
  from: string
  to: string
}

interface Props {
  state: TopBarState
  onChange: (next: Partial<TopBarState>) => void
  onReset: () => void
  onAddPatient: () => void
  user: AppUser
  onLogout: () => void
}

export function TopBar({ state, onChange, onReset, onAddPatient, user, onLogout }: Props) {
  const config = usePopover()
  const account = usePopover()

  return (
    <header className="flex h-[52px] shrink-0 items-center gap-2 border-b border-line bg-white px-3">
      {/* Thương hiệu */}
      <div className="flex shrink-0 items-center gap-2 pr-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white">
          <Plus size={17} strokeWidth={2.4} />
        </div>
        <div className="leading-tight">
          <div className="text-[13px] font-bold tracking-tight text-ink">VR-RIS PACS</div>
          <div className="text-[9px] font-medium uppercase tracking-wider text-ink-4">RIS tích hợp PACS</div>
        </div>
      </div>

      {/* Ô tìm kiếm nhanh */}
      <div className="flex shrink-0 items-center gap-1.5">
        <div className="w-[118px]">
          <Input
            placeholder="Mã bệnh nhân"
            value={state.patientCode}
            onChange={e => onChange({ patientCode: e.target.value })}
          />
        </div>
        <div className="w-[132px]">
          <Input
            placeholder="Họ tên bệnh nhân"
            value={state.patientName}
            onChange={e => onChange({ patientName: e.target.value })}
          />
        </div>
        <div className="w-[96px]">
          <Input
            placeholder="Mã LSVV"
            value={state.visitCode}
            onChange={e => onChange({ visitCode: e.target.value })}
          />
        </div>
      </div>

      {/* Khoảng thời gian */}
      <div className="flex shrink-0 items-center gap-1">
        {DATE_RANGES.map(r => (
          <button
            key={r.key}
            onClick={() => onChange({ range: r.key })}
            className={cx(
              'focus-ring h-7 rounded-md px-2.5 text-[12px] font-medium transition-colors',
              state.range === r.key
                ? 'bg-brand-600 text-white'
                : 'text-ink-3 hover:bg-line-soft hover:text-ink',
            )}
          >
            {r.label}
          </button>
        ))}
      </div>

      <div className="flex shrink-0 items-center gap-1.5">
        <input
          type="date"
          value={state.from}
          onChange={e => onChange({ from: e.target.value })}
          className="focus-ring h-7 rounded-md border border-line bg-white px-2 text-[12px] text-ink"
        />
        <span className="text-ink-4">—</span>
        <input
          type="date"
          value={state.to}
          onChange={e => onChange({ to: e.target.value })}
          className="focus-ring h-7 rounded-md border border-line bg-white px-2 text-[12px] text-ink"
        />
        <IconButton title="Xóa bộ lọc" onClick={onReset}><X size={14} /></IconButton>
        <IconButton title="Tải lại"><Refresh size={14} /></IconButton>
        <IconButton
          title={can(user, 'patient.create') ? 'Thêm bệnh nhân' : deniedReason('patient.create')}
          disabled={!can(user, 'patient.create')}
          onClick={onAddPatient}
          className={!can(user, 'patient.create') ? 'cursor-not-allowed opacity-40' : undefined}
        >
          <UserPlus size={14} />
        </IconButton>
        <div className="relative">
          <IconButton
            title={can(user, 'config.system') ? 'Cấu hình hệ thống' : deniedReason('config.system')}
            disabled={!can(user, 'config.system')}
            onClick={config.toggle}
            className={cx(
              config.open && 'border-brand-300 bg-brand-50 text-brand-600',
              !can(user, 'config.system') && 'cursor-not-allowed opacity-40',
            )}
          >
            <Settings size={14} />
          </IconButton>
          <SystemConfigMenu open={config.open} onClose={config.close} onAddPatient={onAddPatient} user={user} />
        </div>
      </div>

      <div className="flex-1" />

      <div className="relative">
        <button
          onClick={account.toggle}
          className="focus-ring flex h-7 items-center gap-1.5 rounded-md border border-line px-1.5 pr-2 transition-colors hover:bg-line-soft"
        >
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-brand-600 text-[10px] font-bold text-white">
            {user.fullName.slice(0, 1)}
          </span>
          <span className="max-w-[130px] truncate text-[12px] font-medium text-ink-2">{user.username}</span>
          <ChevronDown size={12} className="text-ink-4" />
        </button>

        <Popover open={account.open} onClose={account.close} className="w-[216px] p-2.5">
          <div className="mb-2 flex items-center gap-2 border-b border-line pb-2.5">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand-600 text-[12px] font-bold text-white">
              {user.fullName.slice(0, 1)}
            </span>
            <span className="min-w-0 leading-tight">
              <span className="block truncate text-[12px] font-semibold text-ink">{user.fullName}</span>
              <span className="block truncate text-[10.5px] text-ink-4">{user.email}</span>
            </span>
          </div>

          <div className="mb-2 section-label">Vai trò</div>
          <div className="mb-2.5 space-y-1">
            {user.roles.map(r => (
              <div key={r} className="flex items-center gap-1.5 text-[11.5px] text-ink-2">
                <User size={11} className="text-brand-500" />
                {ROLE_META[r].label}
              </div>
            ))}
          </div>

          <Button variant="ghost" block icon={<Lock size={12} />} onClick={onLogout}>Đăng xuất</Button>
        </Popover>
      </div>
    </header>
  )
}

/* ------------------------------------------------ Popover Cấu hình hệ thống */

const CONFIG_ROWS: { label: string; options: [string, string]; initial: string }[] = [
  { label: 'Loại màn', options: ['Ngang', 'Dọc'], initial: 'Ngang' },
  { label: 'Màn hình', options: ['Một', 'Nhiều'], initial: 'Một' },
  { label: 'Double Click', options: ['Viewer', 'Chi tiết'], initial: 'Viewer' },
  { label: 'Tổng hợp', options: ['Đơn', 'Gộp'], initial: 'Đơn' },
  { label: 'Giải mã tải về', options: ['Không', 'Có'], initial: 'Không' },
  { label: 'Ẩn bộ lọc', options: ['Không', 'Có'], initial: 'Không' },
  { label: 'Ẩn ĐS máy', options: ['Không', 'Có'], initial: 'Không' },
  { label: 'Giao diện xổ', options: ['Tắt', 'Bật'], initial: 'Tắt' },
]

function SystemConfigMenu({ open, onClose, onAddPatient, user }: {
  open: boolean
  onClose: () => void
  onAddPatient: () => void
  user: AppUser
}) {
  return (
    <Popover open={open} onClose={onClose} className="w-[300px] p-3">
      <div className="mb-2.5 flex items-center gap-1.5 text-[12px] font-semibold text-brand-700">
        <Settings size={13} />
        Cấu hình hệ thống
      </div>

      <div className="space-y-1">
        {CONFIG_ROWS.map(row => <ConfigRow key={row.label} {...row} />)}
      </div>

      <div className="mt-3 space-y-1.5 border-t border-line pt-3">
        <Button variant="outline" block icon={<Printer size={13} />} disabled={!can(user, 'list.print')}>
          In toàn bộ danh sách
        </Button>
        <Button
          variant="outline" block icon={<Check size={13} />}
          disabled={!can(user, 'report.approve')}
          title={can(user, 'report.approve') ? undefined : deniedReason('report.approve')}
        >
          Duyệt toàn bộ danh sách
        </Button>
        <Button
          variant="outline" block icon={<UserPlus size={13} />}
          disabled={!can(user, 'patient.create')}
          title={can(user, 'patient.create') ? undefined : deniedReason('patient.create')}
          onClick={() => { onAddPatient(); onClose() }}
        >
          Thêm bệnh nhân
        </Button>
        <Button variant="outline" block icon={<Download size={13} />}>Tải toàn bộ danh sách</Button>
        <Button variant="outline" block icon={<Monitor size={13} />}>Chọn Modality</Button>
      </div>
    </Popover>
  )
}

function ConfigRow({ label, options, initial }: { label: string; options: [string, string]; initial: string }) {
  const [value, setValue] = useState(initial)
  return (
    <div className="flex items-center justify-between gap-2 py-0.5">
      <span className="text-[12px] text-ink-3">{label}</span>
      <Toggle2 value={value} options={options} onChange={setValue} />
    </div>
  )
}
