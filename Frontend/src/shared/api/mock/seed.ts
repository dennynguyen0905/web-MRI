/**
 * Dữ liệu giả cho mock server, **chép đúng** `database/migrations/V7__seed_demo_data.sql`
 * và `V8__rbac_and_demo_extras.sql` để số liệu trên giao diện khớp với DB thật khi nối API.
 *
 * Trả về DTO snake_case y như backend để đường đi dto → mapper → kiểu miền được chạy thật,
 * đổi sang API thật thì không còn gì phải sửa.
 *
 * Ca `CANCELLED` (study id = 10 của V8) **không** có ở đây: `CANCELLED` còn ❓, chưa nằm trong
 * `enu.StudyStatus` của backend lẫn `shared/lib/types.ts` (ghi nợ kỹ thuật, `doc/12` TC-16).
 * R-03: toàn bộ là dữ liệu giả.
 */
import type {
  CurrentUserDto, DepartmentDto, DeviceDto, ImagingOrderDto, PatientDto, ReportDto,
  ServiceItemDto, StatusHistoryEntryDto, StudyDto, UserRefDto,
} from '../dto'

/** Mật khẩu chung của 7 tài khoản demo (`doc/12` mục 3.2). Chỉ dùng cho dev. */
export const DEMO_PASSWORD = '123456'

export const USERS: CurrentUserDto[] = [
  { id: 1, username: 'admin', full_name: 'Quản trị viên (demo)', email: 'admin@vr-ris.local', department_id: 1, roles: ['ADMIN'] },
  { id: 2, username: 'bslamsang', full_name: 'BS. Lâm Sàng (demo)', email: 'bslamsang@vr-ris.local', department_id: 7, roles: ['CLINICIAN'] },
  { id: 3, username: 'ktv', full_name: 'KTV. Kỹ Thuật (demo)', email: 'ktv@vr-ris.local', department_id: 1, roles: ['TECHNICIAN'] },
  { id: 4, username: 'bscdha', full_name: 'BS. CĐHA (demo)', email: 'bscdha@vr-ris.local', department_id: 1, roles: ['RADIOLOGIST'] },
  { id: 6, username: 'bscdha2', full_name: 'BS. CĐHA Hai (demo)', email: 'bscdha2@vr-ris.local', department_id: 1, roles: ['RADIOLOGIST'] },
  { id: 5, username: 'bsduyet', full_name: 'BS. Duyệt (demo)', email: 'bsduyet@vr-ris.local', department_id: 1, roles: ['APPROVER'] },
  { id: 7, username: 'bsduyet2', full_name: 'BS. Duyệt Hai (demo)', email: 'bsduyet2@vr-ris.local', department_id: 1, roles: ['APPROVER'] },
]

export const userRef = (id: number): UserRefDto => {
  const u = USERS.find(x => x.id === id)
  return { id, username: u?.username, full_name: u?.full_name ?? `Người dùng ${id}` }
}

export const DEPARTMENTS: DepartmentDto[] = [
  { id: 1, code: 'CDHA', name: 'Chẩn đoán hình ảnh', parent_id: null, type: 'KHOA', sort_order: 1 },
  { id: 2, code: 'CDHA_XQ', name: 'X-quang', parent_id: 1, type: 'NHOM', sort_order: 1 },
  { id: 3, code: 'CDHA_CT', name: 'CT', parent_id: 1, type: 'NHOM', sort_order: 2 },
  { id: 4, code: 'CDHA_MR', name: 'MRI', parent_id: 1, type: 'NHOM', sort_order: 3 },
  { id: 5, code: 'CDHA_SA', name: 'Siêu âm', parent_id: 1, type: 'NHOM', sort_order: 4 },
  { id: 6, code: 'CC', name: 'Cấp cứu', parent_id: null, type: 'KHOA', sort_order: 2 },
  { id: 7, code: 'NOI', name: 'Nội tổng hợp', parent_id: null, type: 'KHOA', sort_order: 3 },
  { id: 8, code: 'NGOAI', name: 'Ngoại tổng hợp', parent_id: null, type: 'KHOA', sort_order: 4 },
]

export const DEVICES: DeviceDto[] = [
  { id: 1, code: 'CR-01', name: 'Máy X-quang số hóa 01', modality: 'CR', department_id: 2, ae_title: null },
  { id: 2, code: 'CT-01', name: 'Máy CT 64 dãy 01', modality: 'CT', department_id: 3, ae_title: null },
  { id: 3, code: 'MR-01', name: 'Máy MRI 1.5T 01', modality: 'MR', department_id: 4, ae_title: null },
  { id: 4, code: 'US-01', name: 'Máy siêu âm 01', modality: 'US', department_id: 5, ae_title: null },
]

export const SERVICE_ITEMS: ServiceItemDto[] = [
  { id: 1, code: 'XQ-NGUC', name: 'Chụp X-quang ngực thẳng', modality: 'CR', body_part: 'Ngực' },
  { id: 2, code: 'XQ-CSTL', name: 'Chụp X-quang cột sống thắt lưng', modality: 'CR', body_part: 'Cột sống' },
  { id: 3, code: 'CT-SN', name: 'Chụp CT sọ não không tiêm thuốc cản quang', modality: 'CT', body_part: 'Sọ não' },
  { id: 4, code: 'CT-NGUC', name: 'Chụp CT ngực', modality: 'CT', body_part: 'Ngực' },
  { id: 5, code: 'MR-SN', name: 'Chụp MRI sọ não', modality: 'MR', body_part: 'Sọ não' },
  { id: 6, code: 'US-BUNG', name: 'Siêu âm ổ bụng', modality: 'US', body_part: 'Bụng' },
]

/** Cố ý đủ 4 trường hợp `dob_precision` để kiểm cột Tuổi (`doc/12` TC-09). */
export const PATIENTS: PatientDto[] = [
  { id: 1, patient_code: 'TEST0001', his_patient_id: null, full_name: 'Nguyễn Văn Giả A', date_of_birth: '1958-03-15', dob_precision: 'DAY', gender: 'M', social_insurance_no: 'GIA0000000001', phone: '0900000001', address: 'Địa chỉ giả 1', health_check_code: null },
  { id: 2, patient_code: 'TEST0002', his_patient_id: null, full_name: 'Lê Thị Giả B', date_of_birth: '1958-01-01', dob_precision: 'YEAR', gender: 'F', social_insurance_no: 'GIA0000000002', phone: '0900000002', address: 'Địa chỉ giả 2', health_check_code: null },
  { id: 3, patient_code: 'TEST0003', his_patient_id: null, full_name: 'Trần Văn Giả C', date_of_birth: '1975-06-01', dob_precision: 'MONTH', gender: 'M', social_insurance_no: 'GIA0000000003', phone: '0900000003', address: 'Địa chỉ giả 3', health_check_code: null },
  { id: 4, patient_code: 'TEST0004', his_patient_id: null, full_name: 'Phạm Thị Giả D', date_of_birth: '1990-11-20', dob_precision: 'DAY', gender: 'F', social_insurance_no: null, phone: '0900000004', address: 'Địa chỉ giả 4', health_check_code: null },
  { id: 5, patient_code: 'TEST0005', his_patient_id: null, full_name: 'Hoàng Văn Giả E', date_of_birth: null, dob_precision: 'DAY', gender: 'M', social_insurance_no: null, phone: null, address: null, health_check_code: null },
  { id: 6, patient_code: 'TEST0006', his_patient_id: null, full_name: 'Vũ Thị Giả F', date_of_birth: '1983-09-09', dob_precision: 'DAY', gender: 'F', social_insurance_no: 'GIA0000000006', phone: '0900000006', address: 'Địa chỉ giả 6', health_check_code: null },
  { id: 7, patient_code: 'TEST0007', his_patient_id: null, full_name: 'Đặng Văn Giả G', date_of_birth: '2001-02-28', dob_precision: 'DAY', gender: 'M', social_insurance_no: 'GIA0000000007', phone: '0900000007', address: 'Địa chỉ giả 7', health_check_code: null },
  { id: 8, patient_code: 'TEST0008', his_patient_id: null, full_name: 'Bùi Thị Giả H', date_of_birth: '1949-12-01', dob_precision: 'YEAR', gender: 'F', social_insurance_no: 'GIA0000000008', phone: '0900000008', address: 'Địa chỉ giả 8', health_check_code: null },
]

/** `ordering_doctor_name` của đơn 4 và `his_order_id` 5 đơn đã áp đúng V8. */
export const IMAGING_ORDERS: ImagingOrderDto[] = [
  { id: 1, order_code: 'PH20261029001', patient_id: 1, visit_code: 'VV0001', source: 'MANUAL', his_order_id: 'HISO2026110500007', sync_status: 'NOT_SYNCED', ordering_department_id: 6, ordering_doctor_name: 'BS. Chỉ Định A (giả)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Đau đầu đột ngột (dữ liệu giả)', ordered_at: '2026-10-29T02:10:00Z' },
  { id: 2, order_code: 'PH20261030001', patient_id: 2, visit_code: 'VV0002', source: 'MANUAL', his_order_id: 'HISO2026110500008', sync_status: 'NOT_SYNCED', ordering_department_id: 7, ordering_doctor_name: 'BS. Chỉ Định B (giả)', patient_class: 'INPATIENT', room: 'P101', bed: '3', clinical_diagnosis: 'Ho kéo dài (dữ liệu giả)', ordered_at: '2026-10-30T00:40:00Z' },
  { id: 3, order_code: 'PH20261030002', patient_id: 3, visit_code: 'VV0003', source: 'MANUAL', his_order_id: 'HISO2026110500011', sync_status: 'NOT_SYNCED', ordering_department_id: 7, ordering_doctor_name: 'BS. Chỉ Định B (giả)', patient_class: 'INPATIENT', room: 'P102', bed: '1', clinical_diagnosis: 'Chóng mặt (dữ liệu giả)', ordered_at: '2026-10-30T01:30:00Z' },
  { id: 4, order_code: 'PH20261030003', patient_id: 4, visit_code: 'VV0004', source: 'MANUAL', his_order_id: null, sync_status: 'NOT_SYNCED', ordering_department_id: 8, ordering_doctor_name: 'BS. Lâm Sàng (demo)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Đau bụng (dữ liệu giả)', ordered_at: '2026-10-30T02:40:00Z' },
  { id: 5, order_code: 'PH20261031001', patient_id: 8, visit_code: 'VV0005', source: 'MANUAL', his_order_id: null, sync_status: 'NOT_SYNCED', ordering_department_id: 6, ordering_doctor_name: 'BS. Chỉ Định A (giả)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Ngã, chấn thương đầu (dữ liệu giả)', ordered_at: '2026-10-31T01:10:00Z' },
  { id: 6, order_code: 'PH20261031002', patient_id: 5, visit_code: 'VV0006', source: 'MANUAL', his_order_id: null, sync_status: 'NOT_SYNCED', ordering_department_id: 8, ordering_doctor_name: 'BS. Chỉ Định C (giả)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Đau lưng (dữ liệu giả)', ordered_at: '2026-10-31T04:30:00Z' },
  { id: 7, order_code: 'PH20261031003', patient_id: 6, visit_code: 'VV0007', source: 'MANUAL', his_order_id: 'HISO2026110500014', sync_status: 'NOT_SYNCED', ordering_department_id: 7, ordering_doctor_name: 'BS. Chỉ Định B (giả)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Khó thở (dữ liệu giả)', ordered_at: '2026-10-31T05:30:00Z' },
  { id: 8, order_code: 'PH20261031004', patient_id: 7, visit_code: 'VV0008', source: 'MANUAL', his_order_id: 'HISO2026110500013', sync_status: 'NOT_SYNCED', ordering_department_id: 6, ordering_doctor_name: 'BS. Chỉ Định A (giả)', patient_class: 'OUTPATIENT', room: null, bed: null, clinical_diagnosis: 'Kiểm tra sức khỏe (dữ liệu giả)', ordered_at: '2026-10-31T06:30:00Z' },
]

export const STUDIES: StudyDto[] = [
  { id: 1, accession_number: '2026102900001', order_id: 1, service_item_id: 3, device_id: 2, status: 'APPROVED', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000001', scheduled_at: '2026-10-29T02:30:00Z', started_at: '2026-10-29T02:45:00Z', completed_at: '2026-10-29T03:00:00Z', due_at: null, performed_by: userRef(3), reading_user: userRef(4), approving_user: userRef(5) },
  { id: 2, accession_number: '2026102900002', order_id: 1, service_item_id: 1, device_id: 1, status: 'READ', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000002', scheduled_at: '2026-10-29T02:30:00Z', started_at: '2026-10-29T02:50:00Z', completed_at: '2026-10-29T02:55:00Z', due_at: null, performed_by: userRef(3), reading_user: userRef(4), approving_user: null },
  { id: 3, accession_number: '2026103000001', order_id: 2, service_item_id: 1, device_id: 1, status: 'APPROVING', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000003', scheduled_at: '2026-10-30T01:00:00Z', started_at: '2026-10-30T01:10:00Z', completed_at: '2026-10-30T01:20:00Z', due_at: null, performed_by: userRef(3), reading_user: userRef(4), approving_user: userRef(5) },
  { id: 4, accession_number: '2026103000002', order_id: 3, service_item_id: 5, device_id: 3, status: 'READING', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000004', scheduled_at: '2026-10-30T02:00:00Z', started_at: '2026-10-30T02:10:00Z', completed_at: '2026-10-30T02:40:00Z', due_at: null, performed_by: userRef(3), reading_user: userRef(4), approving_user: null },
  { id: 5, accession_number: '2026103000003', order_id: 4, service_item_id: 6, device_id: 4, status: 'UNREAD', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000005', scheduled_at: '2026-10-30T03:00:00Z', started_at: '2026-10-30T03:05:00Z', completed_at: '2026-10-30T03:20:00Z', due_at: null, performed_by: userRef(3), reading_user: null, approving_user: null },
  { id: 6, accession_number: '2026103100001', order_id: 5, service_item_id: 3, device_id: 2, status: 'UNREAD', study_instance_uid: '1.2.826.0.1.3680043.8.498.10000006', scheduled_at: '2026-10-31T01:30:00Z', started_at: '2026-10-31T01:35:00Z', completed_at: '2026-10-31T01:50:00Z', due_at: null, performed_by: userRef(3), reading_user: null, approving_user: null },
  { id: 7, accession_number: '2026103100002', order_id: 6, service_item_id: 2, device_id: 1, status: 'SCHEDULED', study_instance_uid: null, scheduled_at: '2026-10-31T05:00:00Z', started_at: null, completed_at: null, due_at: null, performed_by: null, reading_user: null, approving_user: null },
  { id: 8, accession_number: '2026103100003', order_id: 7, service_item_id: 4, device_id: 2, status: 'SCHEDULED', study_instance_uid: null, scheduled_at: '2026-10-31T06:00:00Z', started_at: null, completed_at: null, due_at: null, performed_by: null, reading_user: null, approving_user: null },
  { id: 9, accession_number: '2026103100004', order_id: 8, service_item_id: 1, device_id: null, status: 'SCHEDULED', study_instance_uid: null, scheduled_at: '2026-10-31T07:00:00Z', started_at: null, completed_at: null, due_at: null, performed_by: null, reading_user: null, approving_user: null },
]

export const REPORTS: ReportDto[] = [
  { id: 1, study_id: 1, version: 1, status: 'APPROVED', origin: 'MANUAL', technique: 'CT sọ não không tiêm thuốc cản quang (dữ liệu giả).', findings: 'Không thấy tổn thương khu trú (dữ liệu giả).', diagnosis: 'Chưa phát hiện bất thường.', conclusion: 'Chưa phát hiện bất thường trên CT sọ não (dữ liệu giả).', recommendation: 'Theo dõi lâm sàng.', created_by: userRef(4), created_at: '2026-10-29T03:30:00Z', approved_by: userRef(5), approved_at: '2026-10-29T04:40:00Z' },
  { id: 2, study_id: 2, version: 1, status: 'FINAL', origin: 'MANUAL', technique: 'X-quang ngực thẳng (dữ liệu giả).', findings: 'Hai trường phổi sáng, bóng tim không to (dữ liệu giả).', diagnosis: 'Chưa phát hiện bất thường.', conclusion: 'X-quang ngực chưa phát hiện bất thường (dữ liệu giả).', recommendation: null, created_by: userRef(4), created_at: '2026-10-29T05:00:00Z', approved_by: null, approved_at: null },
  { id: 3, study_id: 3, version: 1, status: 'FINAL', origin: 'MANUAL', technique: 'X-quang ngực thẳng (dữ liệu giả).', findings: 'Tăng đậm rốn phổi hai bên (dữ liệu giả).', diagnosis: 'Gợi ý viêm phế quản.', conclusion: 'Hình ảnh gợi ý viêm phế quản (dữ liệu giả).', recommendation: 'Đối chiếu lâm sàng.', created_by: userRef(4), created_at: '2026-10-30T02:00:00Z', approved_by: null, approved_at: null },
  { id: 4, study_id: 4, version: 1, status: 'DRAFT', origin: 'MANUAL', technique: 'MRI sọ não các chuỗi xung cơ bản (dữ liệu giả).', findings: null, diagnosis: null, conclusion: null, recommendation: null, created_by: userRef(4), created_at: '2026-10-30T04:00:00Z', approved_by: null, approved_at: null },
]

export const STATUS_HISTORY: StatusHistoryEntryDto[] = [
  { id: 1, study_id: 1, from_status: null, to_status: 'SCHEDULED', changed_by: userRef(2), changed_at: '2026-10-29T02:10:00Z', reason: null, note: null },
  { id: 2, study_id: 1, from_status: 'SCHEDULED', to_status: 'UNREAD', changed_by: userRef(3), changed_at: '2026-10-29T03:00:00Z', reason: null, note: null },
  { id: 3, study_id: 1, from_status: 'UNREAD', to_status: 'READING', changed_by: userRef(4), changed_at: '2026-10-29T03:30:00Z', reason: null, note: null },
  { id: 4, study_id: 1, from_status: 'READING', to_status: 'READ', changed_by: userRef(4), changed_at: '2026-10-29T04:00:00Z', reason: null, note: null },
  { id: 5, study_id: 1, from_status: 'READ', to_status: 'APPROVING', changed_by: userRef(5), changed_at: '2026-10-29T04:20:00Z', reason: null, note: null },
  { id: 6, study_id: 1, from_status: 'APPROVING', to_status: 'APPROVED', changed_by: userRef(5), changed_at: '2026-10-29T04:40:00Z', reason: null, note: null },
  { id: 7, study_id: 2, from_status: null, to_status: 'SCHEDULED', changed_by: userRef(2), changed_at: '2026-10-29T02:10:00Z', reason: null, note: null },
  { id: 8, study_id: 2, from_status: 'SCHEDULED', to_status: 'UNREAD', changed_by: userRef(3), changed_at: '2026-10-29T02:55:00Z', reason: null, note: null },
  { id: 9, study_id: 2, from_status: 'UNREAD', to_status: 'READING', changed_by: userRef(4), changed_at: '2026-10-29T05:00:00Z', reason: null, note: null },
  { id: 10, study_id: 2, from_status: 'READING', to_status: 'READ', changed_by: userRef(4), changed_at: '2026-10-29T05:20:00Z', reason: null, note: null },
]

/**
 * `clinical_snapshot.payload` — 5 tab lâm sàng. Chỉ đơn 1 và 2 có snapshot (V7).
 * Tab 3/4/5 là mảng rỗng, đúng `01-scope.md` mục 3.3 "chờ tích hợp HIS".
 */
export interface ClinicalPayload {
  overview: Record<string, string>
  labs: { stt: number; ten: string; trang_thai: string }[]
  functional: { ten_dich_vu: string; ket_qua: string }[]
  drugs: { ten: string; so_luong: number; don_vi: string }[]
  surgery: unknown[]
}

export const CLINICAL_SNAPSHOTS: Record<number, ClinicalPayload> = {
  1: {
    overview: {
      'Chẩn đoán phiếu khám': 'Đau đầu đột ngột (dữ liệu giả)',
      'Khoa khám': 'Cấp cứu',
      'Nhịp tim': '82',
      'Huyết áp': '120/80 mmHg',
    },
    labs: [{ stt: 1, ten: 'Huyết học (giả)', trang_thai: 'Đã có kết quả' }],
    functional: [],
    drugs: [],
    surgery: [],
  },
  2: {
    overview: {
      'Chẩn đoán phiếu khám': 'Ho kéo dài (dữ liệu giả)',
      'Khoa khám': 'Nội tổng hợp',
      'Nhịp tim': '76',
      'Huyết áp': '130/85 mmHg',
    },
    labs: [{ stt: 1, ten: 'Sinh hóa máu (giả)', trang_thai: 'Đã có kết quả' }],
    functional: [],
    drugs: [],
    surgery: [],
  },
}
