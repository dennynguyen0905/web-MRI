import type { DobPrecision, Gender, Patient } from './types'

const pad = (n: number) => String(n).padStart(2, '0')

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatTimeShort(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return `${pad(d.getDate())}-${pad(d.getMonth() + 1)}-${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** `2026-08-09T…` → `2026-08-09`, dạng mà input[type=date] và query `date_from`/`date_to` dùng. */
export function toDateInput(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Ngày sinh hiển thị theo độ chính xác đã lưu: chỉ có năm thì chỉ in năm. */
export function formatDob(iso: string | null, precision: DobPrecision): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (precision === 'YEAR') return String(d.getFullYear())
  if (precision === 'MONTH') return `${pad(d.getMonth() + 1)}/${d.getFullYear()}`
  return formatDate(iso)
}

/**
 * Tuổi hiển thị. Backend trả sẵn `age` và `age_approximate`; khi `age_approximate` là true
 * (ngày sinh chỉ có năm hoặc tháng) thì in dạng xấp xỉ "~68", không in như số chính xác
 * (`doc/02-erd.md` mục 4.2). Không bao giờ ra "0" cho bệnh nhân chỉ có năm sinh
 * (`01-scope.md` mục 6 bước 10).
 */
export function formatAge(patient: Pick<Patient, 'age' | 'ageApproximate'>): string {
  if (patient.age == null) return '—'
  return patient.ageApproximate ? `~${patient.age}` : String(patient.age)
}

/** Tính tuổi từ ngày sinh — chỉ dùng cho dữ liệu giả; bản thật lấy `age` từ API. */
export function calcAge(iso: string | null, precision: DobPrecision, now = new Date()): number | null {
  if (!iso) return null
  const d = new Date(iso)
  let age = now.getFullYear() - d.getFullYear()
  if (precision === 'DAY' || precision === 'MONTH') {
    const beforeBirthday =
      now.getMonth() < d.getMonth() ||
      (precision === 'DAY' && now.getMonth() === d.getMonth() && now.getDate() < d.getDate())
    if (beforeBirthday) age -= 1
  }
  return age < 0 ? null : age
}

export function genderLabel(g: Gender): string {
  return g === 'M' ? 'Nam' : g === 'F' ? 'Nữ' : g === 'O' ? 'Khác' : '—'
}

export function dash(v: string | null | undefined): string {
  return v == null || v === '' ? '—' : v
}

/** 3397 → "3.397" (dấu chấm phân cách nghìn kiểu Việt Nam). */
export function formatThousands(n: number): string {
  return n.toLocaleString('vi-VN')
}

export function compactNumber(n: number): string {
  if (n >= 1000) return `${(n / 1000).toFixed(n >= 10000 ? 2 : 1).replace(/\.0$/, '')}k`
  return String(n)
}
