/**
 * Dữ liệu giả cho frontend. Hình dạng bám đúng ERD backend (diagrams/02-erd.mmd),
 * nên khi API Spring Boot sẵn sàng chỉ cần thay `buildWorklist()` bằng lời gọi HTTP.
 * Scope doc R-03: không dùng dữ liệu bệnh nhân thật ở bất kỳ đâu.
 */
import type {
  AppUser, Department, Device, ImagingOrder, Modality, Patient, ServiceItem, Study, StudyStatus, WorklistRow,
} from '../lib/types'

/* ------------------------------------------------- Tài khoản demo (IN-13/14) */

/** Mật khẩu dùng chung cho toàn bộ tài khoản seed — dữ liệu giả, không phải thật. */
export const DEMO_PASSWORD = '123456'

export const USERS: AppUser[] = [
  { id: 1, username: 'admin', fullName: 'Nguyễn Quản Trị', email: 'admin@vju.local', departmentId: null, roles: ['ADMIN'], isActive: true },
  { id: 2, username: 'bslamsang', fullName: 'BS. Phạm Thu Hà', email: 'thuha@vju.local', departmentId: 1, roles: ['CLINICIAN'], isActive: true },
  { id: 3, username: 'ktv', fullName: 'KTV. Trần Văn Duy', email: 'duy@vju.local', departmentId: 2, roles: ['TECHNICIAN'], isActive: true },
  { id: 4, username: 'bscdha', fullName: 'BS. Lê Anh Tuấn', email: 'tuan@vju.local', departmentId: 4, roles: ['RADIOLOGIST'], isActive: true },
  { id: 5, username: 'bsduyet', fullName: 'BS. Võ Thái Dương', email: 'duong@vju.local', departmentId: 4, roles: ['APPROVER'], isActive: true },
]

export function authenticate(username: string, password: string): AppUser | null {
  if (password !== DEMO_PASSWORD) return null
  return USERS.find(u => u.username === username.trim().toLowerCase() && u.isActive) ?? null
}

/* ------------------------------------------------------- Bộ sinh tất định */

function makeRng(seed: number) {
  let s = seed >>> 0
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0
    return s / 4294967296
  }
}
const rng = makeRng(20261030)
const pick = <T,>(arr: readonly T[]): T => arr[Math.floor(rng() * arr.length)]
const int = (min: number, max: number) => min + Math.floor(rng() * (max - min + 1))

/* ---------------------------------------------------------------- Danh mục */

export const DEPARTMENTS: Department[] = [
  { id: 1, code: 'TRUYEN_NHIEM', name: 'TRUYỀN NHIỄM', parentId: null, type: 'KHOA', sortOrder: 1 },
  { id: 2, code: 'GTKTN', name: 'GTKTN', parentId: 1, type: 'NHOM', sortOrder: 1 },
  { id: 3, code: 'SAKTN', name: 'SAKTN', parentId: 1, type: 'NHOM', sortOrder: 2 },
  { id: 4, code: 'CAP_CUU', name: 'CẤP CỨU', parentId: null, type: 'KHOA', sortOrder: 2 },
  { id: 5, code: 'CC_DIEN_TIM', name: 'CẤP CỨU-ĐIỆN TIM', parentId: 4, type: 'NHOM', sortOrder: 1 },
]

export const DEVICES: Device[] = [
  { id: 1, code: 'TT_DT_TRUYEN_NHIEM', name: 'TT_DT_TRUYEN_NHIEM', modality: 'CR', departmentId: 2 },
  { id: 2, code: 'TS_A84_DIENTIM', name: 'TS_A84_DIENTIM', modality: 'US', departmentId: 2 },
  { id: 3, code: 'TS_A48_SACHSON', name: 'TS_A48_SACHSON', modality: 'US', departmentId: 3 },
  { id: 4, code: 'CT32_CAPCUU', name: 'CT32-CAPCUU', modality: 'CT', departmentId: 5 },
  { id: 5, code: 'MR15_CAPCUU', name: 'MR15-CAPCUU', modality: 'MR', departmentId: 5 },
  { id: 6, code: 'XQ_CT3', name: 'X-Quang-CT-3', modality: 'CR', departmentId: 5 },
]

export const SERVICES: ServiceItem[] = [
  { id: 1, code: 'CR_CHEST', name: 'Chụp X-quang ngực thẳng [số hóa 1 phim] (Áp dụng cho 01 vị trí)', modality: 'CR', bodyPart: 'Ngực' },
  { id: 2, code: 'CT_HEAD_CE', name: 'Chụp CLVT sọ não có tiêm thuốc cản quang (Từ 1-32 dãy) [Chưa bao gồm thuốc cản quang]', modality: 'CT', bodyPart: 'Sọ não' },
  { id: 3, code: 'US_ABD', name: 'Siêu âm ổ bụng (gan mật, tụy, lách, thận, tiết niệu, tiền liệt tuyến)', modality: 'US', bodyPart: 'Ổ bụng' },
  { id: 4, code: 'US_ECHO', name: 'Siêu âm Doppler tim, van tim', modality: 'US', bodyPart: 'Tim' },
  { id: 5, code: 'US_RENAL', name: 'Siêu âm Doppler động mạch thận', modality: 'US', bodyPart: 'Thận' },
  { id: 6, code: 'CR_LSPINE', name: 'Chụp X-quang cột sống thắt lưng thẳng, nghiêng hoặc chếch', modality: 'CR', bodyPart: 'Cột sống thắt lưng' },
  { id: 7, code: 'CR_CSPINE', name: 'Chụp X-quang cột sống cổ thẳng, nghiêng hoặc chếch', modality: 'CR', bodyPart: 'Cột sống cổ' },
  { id: 8, code: 'DX_BMD', name: 'Đo mật độ xương [sàng lọc bằng siêu âm]', modality: 'DX', bodyPart: 'Toàn thân' },
  { id: 9, code: 'CR_MAMMO', name: 'XQ tuyến vú (Mammography)', modality: 'CR', bodyPart: 'Tuyến vú' },
  { id: 10, code: 'MR_BRAIN', name: 'Chụp cộng hưởng từ sọ não không tiêm thuốc đối quang', modality: 'MR', bodyPart: 'Sọ não' },
  { id: 11, code: 'US_GI', name: 'Siêu âm ống tiêu hóa (dạ dày, ruột non, đại tràng)', modality: 'US', bodyPart: 'Tiêu hóa' },
  { id: 12, code: 'CT_ABD', name: 'Chụp CLVT ổ bụng - tiểu khung có tiêm thuốc cản quang (Từ 1-32 dãy)', modality: 'CT', bodyPart: 'Ổ bụng' },
]

export const DOCTORS = [
  { name: 'BS. Nguyễn Minh Khoa', dept: 'Trung tâm khám sức khoẻ' },
  { name: 'BS. Lê Anh Tuấn', dept: 'Khoa Cấp cứu C1.3' },
  { name: 'BS. Võ Thái Dương', dept: 'Khoa Cấp cứu C1.3' },
  { name: 'BS. Trương Anh Quân', dept: 'Khoa Chống nhiễm khuẩn (BI-6)' },
  { name: 'BS. Phạm Thu Hà', dept: 'Khoa Nội tổng hợp B2.1' },
] as const

const SURNAMES = ['TRẦN', 'LÊ THỊ', 'PHAN', 'ĐỖ THỊ', 'VŨ THỊ', 'BÙI THỊ', 'NGÔ THỊ', 'ĐINH', 'CAO THỊ', 'DƯƠNG THỊ', 'ĐẶNG', 'KIỀU', 'HOÀNG THỊ', 'MAI THỊ', 'NGUYỄN', 'PHẠM THỊ', 'LƯƠNG', 'TRỊNH THỊ'] as const
const GIVEN = ['VĂN HẢI', 'MINH ANH', 'QUỐC BẢO', 'NGỌC LAN', 'THU HÀ', 'XUÂN MAI', 'QUANG HƯNG', 'THANH THỦY', 'THỊ HẠNH', 'VĂN LỘC', 'VĂN MINH', 'THẾ LIÊN', 'THU PHƯỢNG', 'KIM OANH', 'HỮU PHÚC', 'BÍCH NGỌC', 'TRUNG KIÊN', 'HẢI YẾN'] as const

const DIAGNOSES = [
  'Khối máu não, không xác định (I63.9)',
  'Xuất huyết dưới nhện từ động mạch não giữa (I60.1)',
  'Viêm phổi, tác nhân không xác định (J18.9)',
  'Đau vùng thắt lưng (M54.5)',
  'Tăng huyết áp vô căn nguyên phát (I10)',
  'Sỏi thận (N20.0)',
] as const

const CONCLUSIONS = [
  'Hình tim không to. Trung thất không rộng. Trường phổi hai bên sáng.',
  'Không thấy hình ảnh bất thường trên phim chụp.',
  'Theo dõi tổn thương nhu mô phổi phải, đề nghị chụp CLVT.',
  'Thoái hóa cột sống thắt lưng mức độ nhẹ.',
  'Nhu mô gan đều, không thấy khối khu trú.',
] as const

/* ------------------------------------------------------ Sinh dữ liệu chính */

const STATUS_PLAN: { status: StudyStatus; count: number }[] = [
  { status: 'CHO_THUC_HIEN', count: 66 },
  { status: 'CHUA_DOC', count: 24 },
  { status: 'DANG_DOC', count: 12 },
  { status: 'DOC_XONG', count: 8 },
  { status: 'DANG_DUYET', count: 5 },
  { status: 'DUYET_XONG', count: 31 },
]

/** Số đếm hiển thị trên bộ lọc — số thật của hệ thống, lớn hơn số dòng seed. */
export const STATUS_COUNTS: Record<StudyStatus, number> = {
  CHO_THUC_HIEN: 66,
  CHUA_DOC: 451,
  DANG_DOC: 28,
  DOC_XONG: 4,
  DANG_DUYET: 0,
  DUYET_XONG: 13320,
}
export const TOTAL_STUDIES = 3397
export const OVERDUE_COUNT = 41

const BASE_DAY = new Date(2026, 7, 9) // 09/08/2026 — khớp khoảng ngày trên thanh lọc

function at(hour: number, minute: number, dayOffset = 0): string {
  const d = new Date(BASE_DAY)
  d.setDate(d.getDate() + dayOffset)
  d.setHours(hour, minute, 0, 0)
  return d.toISOString()
}

const patients: Patient[] = []
const orders: ImagingOrder[] = []
const studies: Study[] = []

let pSeq = 0
let oSeq = 0
let sSeq = 0

function newPatient(): Patient {
  pSeq += 1
  // Một phần bệnh nhân chỉ có năm sinh — để kiểm chứng cột Tuổi không ra 0.
  const onlyYear = rng() < 0.28
  const year = int(1942, 2012)
  const dob = new Date(year, onlyYear ? 0 : int(0, 11), onlyYear ? 1 : int(1, 28))
  const p: Patient = {
    id: pSeq,
    patientCode: `P${String(10000000 + pSeq).slice(1)}`,
    hisPatientId: rng() < 0.6 ? `H${String(20000 + pSeq)}` : null,
    fullName: `${pick(SURNAMES)} ${pick(GIVEN)}`,
    dateOfBirth: dob.toISOString(),
    dobPrecision: onlyYear ? 'YEAR' : 'DAY',
    gender: rng() < 0.5 ? 'M' : 'F',
    socialInsuranceNo: rng() < 0.75 ? `CB${String(10 ** 11 + pSeq * 7).slice(0, 12)}` : null,
    phone: rng() < 0.5 ? `09${int(10000000, 99999999)}` : '',
    address: rng() < 0.5 ? 'Phường Cầu Giấy, Hà Nội' : '',
    healthCheckCode: rng() < 0.3 ? `CB000000000${int(100, 999)}` : null,
  }
  patients.push(p)
  return p
}

function newOrder(patient: Patient): ImagingOrder {
  oSeq += 1
  const doc = pick(DOCTORS)
  const o: ImagingOrder = {
    id: oSeq,
    orderCode: `9${String(1000000 + oSeq).slice(1)}`,
    patientId: patient.id,
    visitCode: rng() < 0.8 ? String(90000000 + oSeq) : null,
    source: rng() < 0.55 ? 'HIS' : 'MANUAL',
    syncStatus: pick(['NOT_SYNCED', 'SYNCED', 'RESULT_SENT', 'FAILED'] as const),
    orderingDepartmentId: pick(DEPARTMENTS).id,
    orderingDoctorName: doc.name,
    patientClass: rng() < 0.45 ? 'INPATIENT' : 'OUTPATIENT',
    room: rng() < 0.5 ? `B${int(1, 9)}.${int(1, 4)}` : '',
    bed: rng() < 0.5 ? String(int(1, 40)) : '',
    clinicalDiagnosis: pick(DIAGNOSES),
    orderedAt: at(int(7, 17), int(0, 59), -int(0, 2)),
  }
  orders.push(o)
  return o
}

function newStudy(order: ImagingOrder, status: StudyStatus): Study {
  sSeq += 1
  const svc = pick(SERVICES)
  const dev = DEVICES.filter(d => d.modality === svc.modality)[0] ?? pick(DEVICES)
  const done = status !== 'CHO_THUC_HIEN'
  const read = status === 'DOC_XONG' || status === 'DANG_DUYET' || status === 'DUYET_XONG'
  const h = int(8, 22)
  const m = int(0, 59)
  return {
    id: sSeq,
    accessionNumber: `${String(90000000 + sSeq)}`,
    orderId: order.id,
    serviceItemId: svc.id,
    deviceId: done ? dev.id : null,
    status,
    studyInstanceUid: done ? `1.2.826.0.1.3680043.8.498.${10000000 + sSeq}` : null,
    scheduledAt: at(h, m),
    startedAt: done ? at(h, m) : null,
    completedAt: done ? at(h, Math.min(59, m + int(2, 20))) : null,
    dueAt: at(h + 2 > 23 ? 23 : h + 2, m),
    performedByName: done ? pick(DOCTORS).name : null,
    readingUserName: read ? pick(DOCTORS).name : null,
    approvingUserName: status === 'DUYET_XONG' ? pick(DOCTORS).name : null,
    technique: read ? 'Chụp tư thế thẳng, khoảng cách 180cm, kVp 120.' : null,
    conclusion: read ? pick(CONCLUSIONS) : null,
    bhytViolation: rng() < 0.08,
  }
}

for (const { status, count } of STATUS_PLAN) {
  for (let i = 0; i < count; i++) {
    const patient = newPatient()
    const order = newOrder(patient)
    // Một chỉ định có thể gồm nhiều ca chụp (IN-02) — trừ tab Chờ, để số dòng khớp số đếm.
    const n = status !== 'CHO_THUC_HIEN' && rng() < 0.22 ? 2 : 1
    for (let k = 0; k < n; k++) studies.push(newStudy(order, status))
  }
}

export const PATIENTS = patients
export const ORDERS = orders
export const STUDIES = studies

const patientById = new Map(patients.map(p => [p.id, p]))
const orderById = new Map(orders.map(o => [o.id, o]))
const serviceById = new Map(SERVICES.map(s => [s.id, s]))
const deviceById = new Map(DEVICES.map(d => [d.id, d]))
const deptById = new Map(DEPARTMENTS.map(d => [d.id, d]))

export const WORKLIST: WorklistRow[] = studies.map(study => {
  const order = orderById.get(study.orderId)!
  return {
    study,
    order,
    patient: patientById.get(order.patientId)!,
    service: serviceById.get(study.serviceItemId)!,
    device: study.deviceId ? deviceById.get(study.deviceId)! : null,
    department: deptById.get(order.orderingDepartmentId) ?? null,
  }
})

export const MODALITIES: Modality[] = ['CT', 'MR', 'CR', 'DX', 'US']

/* ------------------------------------------- Snapshot lâm sàng (5 tab HIS) */

export interface ClinicalSnapshot {
  overview: Record<string, string>
  labs: { stt: number; code: string; name: string; status: string; createdBy: string; createdAt: string; performedAt: string }[]
  functional: { code: string; orderedAt: string; resultAt: string; groupCode: string; note: string; serviceName: string; result: string; codhaCode: string }[]
  drugs: { recordNo: string; formNo: string; seq: number; drugCode: string; drugName: string; ingredient: string; unit: string; qty: number }[]
  surgery: unknown[]
}

export const CLINICAL_SNAPSHOT: ClinicalSnapshot = {
  overview: {
    'Chẩn đoán phiếu khám': 'Xuất huyết dưới nhện từ động mạch não giữa',
    'Tiền sử bản thân': '—',
    'Nhịp tim': '100',
    'Nhiệt độ': '37,7',
    'Huyết áp': '150/86 mmHg',
    'Nhịp thở': '20 lần/phút',
    'Chiều cao': '155',
    'Cân nặng': '50',
    'Chẩn đoán KQ cận lâm sàng': 'Xuất huyết dưới nhện từ động mạch não giữa (I60.1)',
    'Tiền sử gia đình': '—',
    'Quá trình bệnh lý':
      'Khởi phát đột ngột đau đầu vùng chẩm, lan lên đỉnh đầu, không yếu liệt, không nôn khan, không méo miệng. Đi chụp CLVT phát hiện máu tụ nội sọ, nay chuyển khoa C1-3.',
    'Quá trình khám bệnh':
      'Đường thở thông thoáng · Tự thở, ran phế nang 2 phổi rõ, SpO2 98% · Nhịp tim đều, T1 T2 rõ · Tỉnh, Glasgow 15đ · Da niêm mạc bình thường · Bụng mềm, không chướng.',
    'Khoa khám': 'C1.3',
    'Phòng khám': '1',
    'Kết luận': 'Xuất huyết dưới nhện từ động mạch não giữa',
  },
  labs: [
    { stt: 1, code: '90000011', name: 'Huyết học', status: 'S', createdBy: 'BS. Lê Anh Tuấn', createdAt: '09/08/2026 16:06', performedAt: '09/08/2026 16:10' },
    { stt: 2, code: '90000012', name: 'Xét nghiệm đông máu', status: 'S', createdBy: 'BS. Lê Anh Tuấn', createdAt: '09/08/2026 16:06', performedAt: '09/08/2026 16:10' },
    { stt: 3, code: '90000013', name: 'Sinh hóa máu', status: 'S', createdBy: 'BS. Lê Anh Tuấn', createdAt: '09/08/2026 16:06', performedAt: '09/08/2026 16:10' },
  ],
  functional: [
    {
      code: '90000021', orderedAt: '2026-08-09 16:06:00', resultAt: '2026-08-09 17:01:29', groupCode: '82100', note: '—',
      serviceName: 'Chụp X-quang ngực thẳng [số hóa 1 phim] (Áp dụng cho 01 vị trí)',
      result: 'Hình tim không to. Trung thất không rộng. Trường phổi hai bên sáng.', codhaCode: '82',
    },
    {
      code: '90000022', orderedAt: '2026-08-09 16:06:00', resultAt: '—', groupCode: '82200', note: '—',
      serviceName: 'Chụp CLVT sọ não có tiêm thuốc cản quang (Từ 1-32 dãy) [Chưa bao gồm thuốc cản quang]',
      result: '—', codhaCode: '82',
    },
    {
      code: '90000023', orderedAt: '2026-08-09 16:06:00', resultAt: '—', groupCode: '83200', note: '—',
      serviceName: 'Siêu âm Doppler tim, van tim', result: '—', codhaCode: '83',
    },
  ],
  drugs: [
    { recordNo: '90000001', formNo: '90000031', seq: 1, drugCode: '95284', drugName: 'Midovicam 1g/10ml', ingredient: 'Paracetamol', unit: 'Ống', qty: 1 },
    { recordNo: '90000001', formNo: '90000031', seq: 3, drugCode: '47661', drugName: 'Nerisld 10mg/50ml', ingredient: 'Nimodipin', unit: 'Lọ', qty: 1 },
    { recordNo: '90000001', formNo: '90000031', seq: 4, drugCode: '87844', drugName: 'Sodium Chloride 0,9% 500ml', ingredient: 'Natri clorid', unit: 'Túi', qty: 1 },
    { recordNo: '90000001', formNo: '90000031', seq: 2, drugCode: '86559', drugName: 'Sodium Chloride Injection 0,9% 100ml', ingredient: 'Natri clorid', unit: 'Chai', qty: 1 },
  ],
  surgery: [],
}
