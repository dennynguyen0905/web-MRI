import { useReferenceData } from '@/features/worklist/hooks/useWorklist'
import { useState } from 'react'
import { Button, Checkbox, Input, Modal, Select } from '@/shared/ui/controls'
import { Plus } from '@/shared/ui/Icon'

/**
 * "THÊM CA CHỤP" (IN-01/02/03). Form đã đủ trường; việc gửi lên
 * `POST /imaging-orders` và `POST /imaging-orders/{id}/studies` nằm ở task riêng.
 * Danh mục máy và dịch vụ lấy từ `/devices`, `/service-items` qua hook, không còn dữ liệu cứng.
 */
export function AddStudyModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [autoCode, setAutoCode] = useState(true)
  const reference = useReferenceData()
  const devices = reference.data?.devices ?? []
  const serviceItems = reference.data?.serviceItems ?? []

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
        <Field label="Mã bệnh nhân" span={2}>
          <Input placeholder="Tự động sinh khi tạo" disabled={autoCode} />
        </Field>
      </Row>

      <Row>
        <Field label="Họ tên"><Input placeholder="Nhập họ tên" /></Field>
        <Field label="Ngày sinh"><Input type="date" /></Field>
        <Field label="Giới tính">
          <Select placeholder="Chọn" options={[{ value: 'M', label: 'Nam' }, { value: 'F', label: 'Nữ' }]} />
        </Field>
      </Row>

      <Row>
        <Field label="Độ chính xác ngày sinh">
          <Select
            placeholder="Chọn"
            options={[
              { value: 'DAY', label: 'Đủ ngày' },
              { value: 'MONTH', label: 'Chỉ tháng' },
              { value: 'YEAR', label: 'Chỉ năm' },
            ]}
          />
        </Field>
        <Field label="Mã BHXH"><Input placeholder="Số BHXH" /></Field>
        <Field label="Điện thoại"><Input placeholder="Số điện thoại" /></Field>
      </Row>

      <Row>
        <Field label="Địa chỉ" span={3}><Input placeholder="Nhập địa chỉ" /></Field>
      </Row>

      <Row>
        <Field label="Loại bệnh nhân">
          <Select
            placeholder="Chọn"
            options={[
              { value: 'INPATIENT', label: 'Nội trú' },
              { value: 'OUTPATIENT', label: 'Ngoại trú' },
            ]}
          />
        </Field>
        <Field label="Chẩn đoán ban đầu" span={2}><Input placeholder="Nhập chẩn đoán" /></Field>
      </Row>

      <Row>
        <Field label="Khoa chỉ định"><Input placeholder="Chọn khoa" /></Field>
        <Field label="Bác sĩ chỉ định" span={2}><Input placeholder="Nhập tên bác sĩ" /></Field>
      </Row>

      <Row>
        <Field label="Buồng"><Input placeholder="Buồng" /></Field>
        <Field label="Giường"><Input placeholder="Giường" /></Field>
        <Field label="Mã LSVV"><Input placeholder="Mã lịch sử vào viện" /></Field>
      </Row>

      <Row>
        <Field label="Dịch vụ chụp" span={3}>
          <Select
            placeholder={reference.loading ? 'Đang tải danh mục…' : 'Chọn dịch vụ'}
            options={serviceItems.map(s => ({ value: String(s.id), label: `${s.name} (${s.modality})` }))}
          />
        </Field>
      </Row>

      <Row>
        <Field label="Máy chụp" span={3}>
          <Select
            placeholder={reference.loading ? 'Đang tải danh mục…' : 'Chọn máy chụp'}
            options={devices.map(d => ({ value: String(d.id), label: `${d.name} (${d.modality})` }))}
          />
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
