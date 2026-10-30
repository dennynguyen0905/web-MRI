import type { DobPrecision, Gender } from './types'

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

/** Ngày sinh hiển thị theo độ chính xác đã lưu: chỉ có năm thì chỉ in năm. */
export function formatDob(iso: string | null, precision: DobPrecision): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (precision === 'YEAR') return String(d.getFullYear())
  if (precision === 'MONTH') return `${pad(d.getMonth() + 1)}/${d.getFullYear()}`
  return formatDate(iso)
}

/**
 * Tuổi tính theo độ chính xác ngày sinh — bệnh nhân chỉ có năm sinh vẫn ra tuổi đúng,
 * không rơi về 0 (kịch bản nghiệm thu bước 10 của scope doc).
 */
export function calcAge(iso: string | null, precision: DobPrecision, now = new Date()): string {
  if (!iso) return '—'
  const d = new Date(iso)
  let age = now.getFullYear() - d.getFullYear()
  if (precision === 'DAY' || precision === 'MONTH') {
    const beforeBirthday =
      now.getMonth() < d.getMonth() ||
      (precision === 'DAY' && now.getMonth() === d.getMonth() && now.getDate() < d.getDate())
    if (beforeBirthday) age -= 1
  }
  return age < 0 ? '—' : String(age)
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
