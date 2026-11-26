/**
 * 7 tai khoan seed V7/V8 (`doc/12` muc 3.2.1, doi chieu
 * `Frontend/src/shared/api/mock/seed.ts`). Mat khau chung la mat khau demo cua du lieu gia,
 * KHONG phai bi mat that — no nam san trong ma nguon mock va hien ngay tren man dang nhap.
 */
export const DEMO_PASSWORD = '123456'

export type RoleKey = 'ADMIN' | 'CLINICIAN' | 'TECHNICIAN' | 'RADIOLOGIST' | 'APPROVER'

export interface Account {
  username: string
  role: RoleKey
  fullName: string
}

export const ACCOUNTS: Record<string, Account> = {
  admin: { username: 'admin', role: 'ADMIN', fullName: 'Quản trị viên (demo)' },
  bslamsang: { username: 'bslamsang', role: 'CLINICIAN', fullName: 'BS. Lâm Sàng (demo)' },
  ktv: { username: 'ktv', role: 'TECHNICIAN', fullName: 'KTV. Kỹ Thuật (demo)' },
  bscdha: { username: 'bscdha', role: 'RADIOLOGIST', fullName: 'BS. CĐHA (demo)' },
  bscdha2: { username: 'bscdha2', role: 'RADIOLOGIST', fullName: 'BS. CĐHA Hai (demo)' },
  bsduyet: { username: 'bsduyet', role: 'APPROVER', fullName: 'BS. Duyệt (demo)' },
  bsduyet2: { username: 'bsduyet2', role: 'APPROVER', fullName: 'BS. Duyệt Hai (demo)' },
}

/** Ca chup seed theo trang thai (`doc/12` muc 3.2.2). */
export const SEED_STUDIES = {
  approved: 1, read: 2, approving: 3, reading: 4,
  unread: [5, 6], scheduled: [7, 8, 9], cancelled: 10,
} as const

/** Benh nhan seed theo do chinh xac ngay sinh (TC-09). */
export const SEED_PATIENTS = {
  dobDay: 'TEST0001', dobMonth: 'TEST0003', dobYear: ['TEST0002', 'TEST0008'], dobNull: 'TEST0005',
} as const
