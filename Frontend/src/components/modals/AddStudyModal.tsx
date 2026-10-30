import { useState } from 'react'
import { DEVICES, SERVICES } from '../../data/mock'
import { Button, Checkbox, Input, Modal, Select } from '../ui/controls'
import { Plus } from '../ui/Icon'

/** Ảnh 10 — "THÊM CA CHỤP". */
export function AddStudyModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [autoCode, setAutoCode] = useState(true)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="THÊM CA CHỤP"
      width={560}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Hủy</Button>
          <Button icon={<Plus size={12} />} onClick={onClose}>Tạo mới</Button>
        </>
      }
    >
      <Checkbox
        checked={autoCode}
        onChange={setAutoCode}
        label={<span className="font-medium text-ink-2">Thêm mới bệnh nhân</span>}
        className="mb-3"
      />

      <Row>
        <Field label="Mã" span={2}>
          <Input placeholder="Tự động tạo khi tạo" disabled={autoCode} />
        </Field>
      </Row>

      <Row>
        <Field label="Họ tên"><Input placeholder="Nhập họ tên" /></Field>
        <Field label="Tuổi"><Input placeholder="Tự tính" /></Field>
        <Field label="Giới tính">
          <Select placeholder="Chọn" options={[{ value: 'M', label: 'Nam' }, { value: 'F', label: 'Nữ' }]} />
        </Field>
      </Row>

      <Row>
        <Field label="BHXH"><Input placeholder="Số BHXH" /></Field>
        <Field label="Địa chỉ" span={1}><Input placeholder="Nhập địa chỉ" /></Field>
        <Field label="Điện thoại"><Input placeholder="Số điện thoại" /></Field>
      </Row>

      <Row>
        <Field label="Bộ phận chụp"><Input placeholder="Ví dụ: CHEST" /></Field>
        <Field label="Chẩn đoán ban đầu" span={2}><Input placeholder="Nhập chẩn đoán" /></Field>
      </Row>

      <Row>
        <Field label="Khoa chỉ định"><Input placeholder="Chọn khoa" /></Field>
        <Field label="Bác sĩ chỉ định" span={2}><Input placeholder="Chọn bác sĩ" /></Field>
      </Row>

      <Row>
        <Field label="Buồng"><Input placeholder="Buồng" /></Field>
        <Field label="Giường"><Input placeholder="Giường" /></Field>
        <Field label="Tên chỉ định"><Input placeholder="Nhập tên chỉ định" /></Field>
      </Row>

      <Row>
        <Field label="Máy chụp" span={3}>
          <Select
            placeholder="Chọn máy chụp"
            options={DEVICES.map(d => ({ value: String(d.id), label: `${d.name} (${d.modality})` }))}
          />
        </Field>
      </Row>

      <Row>
        <Field label="Dịch vụ chụp" span={3}>
          <Select placeholder="Chọn dịch vụ" options={SERVICES.map(s => ({ value: String(s.id), label: s.name }))} />
        </Field>
      </Row>
    </Modal>
  )
}

function Row({ children }: { children: React.ReactNode }) {
  return <div className="mb-2.5 grid grid-cols-3 gap-2.5">{children}</div>
}

function Field({ label, span = 1, children }: { label: string; span?: 1 | 2 | 3; children: React.ReactNode }) {
  const cls = span === 3 ? 'col-span-3' : span === 2 ? 'col-span-2' : 'col-span-1'
  return (
    <label className={cls}>
      <div className="mb-1 text-[11.5px] text-ink-3">{label}</div>
      {children}
    </label>
  )
}
