import { useState } from 'react'
import { formatDateTime } from '../../lib/format'
import type { WorklistRow } from '../../lib/types'
import { Button, Input, Modal, Radio } from '../ui/controls'
import { Search, Sync } from '../ui/Icon'

/** Ảnh 12 — "Cập nhật chỉ định từ HIS". */
export function UpdateFromHisModal({ open, onClose, row }: {
  open: boolean
  onClose: () => void
  row: WorklistRow | null
}) {
  const [mode, setMode] = useState<'ORDER' | 'PATIENT'>('ORDER')
  const [picked, setPicked] = useState(0)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Cập nhật chỉ định từ HIS"
      width={540}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Đóng</Button>
          <Button icon={<Sync size={12} />} onClick={onClose}>Cập nhật</Button>
        </>
      }
    >
      <div className="mb-2.5 flex items-center gap-5">
        <Radio checked={mode === 'ORDER'} onChange={() => setMode('ORDER')} label="Mã chỉ định" />
        <Radio checked={mode === 'PATIENT'} onChange={() => setMode('PATIENT')} label="Mã bệnh nhân" />
      </div>

      <div className="mb-3 flex gap-2">
        <Input
          withSearchIcon
          defaultValue={mode === 'ORDER' ? row?.order.orderCode ?? '' : row?.patient.patientCode ?? ''}
          placeholder={mode === 'ORDER' ? 'Nhập mã chỉ định' : 'Nhập mã bệnh nhân'}
        />
        <Button icon={<Search size={12} />} className="shrink-0 px-3">Tìm kiếm</Button>
      </div>

      <div className="mb-1.5 section-label">Danh sách chỉ định</div>

      <div className="space-y-1.5">
        {row && (
          <label
            onClick={() => setPicked(0)}
            className="flex cursor-pointer items-start gap-2.5 rounded-md border border-brand-200 bg-brand-50 px-2.5 py-2"
          >
            <span className="pt-0.5">
              <Radio checked={picked === 0} onChange={() => setPicked(0)} label="" />
            </span>
            <span className="leading-snug">
              <span className="block text-[12px] font-medium text-brand-700">{row.service.name}</span>
              <span className="mt-0.5 block text-[11px] text-ink-4">
                Bệnh nhân {row.patient.fullName} · Ngày chỉ định {formatDateTime(row.order.orderedAt)}
              </span>
            </span>
          </label>
        )}
      </div>
    </Modal>
  )
}
