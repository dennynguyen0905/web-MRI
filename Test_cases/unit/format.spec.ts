import { describe, expect, it } from 'vitest'
import {
  calcAge, dash, formatAge, formatDate, formatDob, formatThousands, genderLabel, toDateInput,
} from '@/shared/lib/format'

/**
 * TC-09 (`doc/12-seed-data-va-kich-ban-demo.md` muc 3.1) + `doc/02-erd.md` muc 4.2.
 *
 * Yeu cau nghiep vu: tuoi hien thi dung theo `dob_precision`, dang xap xi co tien to "~" khi
 * khong biet chac da qua sinh nhat, va KHONG BAO GIO ra chuoi "0" cho benh nhan chi co nam sinh
 * (`01-scope.md` muc 6 buoc 10). Du lieu seed tuong ung: TEST0001=DAY, TEST0003=MONTH,
 * TEST0002/TEST0008=YEAR, TEST0005=ngay sinh NULL.
 */
describe('TC-09: tuoi va ngay sinh theo dob_precision', () => {
  it('TEST0001 (DAY): tuoi chinh xac, khong co tien to ~', () => {
    expect(formatAge({ age: 68, ageApproximate: false })).toBe('68')
  })

  it('TEST0003 (MONTH) va TEST0002/TEST0008 (YEAR): tuoi xap xi, co tien to ~', () => {
    expect(formatAge({ age: 68, ageApproximate: true })).toBe('~68')
  })

  it('TEST0005 (ngay sinh NULL): hien thi "—", khong phai "0"', () => {
    expect(formatAge({ age: null, ageApproximate: false })).toBe('—')
    expect(formatAge({ age: null, ageApproximate: true })).toBe('—')
  })

  it('khong bao gio tra ve dung chuoi "0" khi chua co ngay sinh', () => {
    // Chot yeu cau TC-09: nhanh age == null phai ra "—". Neu ai doi formatAge tra "0"
    // cho age null thi test nay do.
    expect(formatAge({ age: null, ageApproximate: false })).not.toBe('0')
  })

  it('formatDob in theo dung do chinh xac da luu', () => {
    expect(formatDob('1958-03-15T00:00:00Z', 'DAY')).toBe('15/03/1958')
    expect(formatDob('1958-03-01T00:00:00Z', 'MONTH')).toBe('03/1958')
    expect(formatDob('1958-01-01T00:00:00Z', 'YEAR')).toBe('1958')
    expect(formatDob(null, 'DAY')).toBe('—')
  })
})

describe('calcAge (chi dung cho du lieu gia)', () => {
  const now = new Date('2026-11-26T00:00:00Z')

  it('YEAR: lay hieu nam, khong tru sinh nhat', () => {
    expect(calcAge('1958-01-01T00:00:00Z', 'YEAR', now)).toBe(68)
  })

  it('DAY: tru 1 khi chua qua sinh nhat trong nam', () => {
    expect(calcAge('1958-12-31T00:00:00Z', 'DAY', now)).toBe(67)
    expect(calcAge('1958-01-01T00:00:00Z', 'DAY', now)).toBe(68)
  })

  it('ngay sinh o tuong lai tra null (khong tra so am)', () => {
    // Lien quan loi L1: backend nem IllegalArgumentException cho truong hop nay (xem
    // Test_cases/reports/bug-report.md). Frontend thi xu ly dung.
    expect(calcAge('2030-01-01T00:00:00Z', 'DAY', now)).toBeNull()
  })

  it('khong co ngay sinh tra null', () => {
    expect(calcAge(null, 'DAY', now)).toBeNull()
  })
})

describe('cac ham dinh dang khac', () => {
  it('formatDate / toDateInput', () => {
    expect(formatDate('2026-08-09T10:30:00Z')).toBe('09/08/2026')
    expect(formatDate(null)).toBe('—')
    expect(toDateInput('2026-08-09T10:30:00Z')).toBe('2026-08-09')
    expect(toDateInput(null)).toBe('')
  })

  it('genderLabel phu du 3 gia tri + nhanh con lai', () => {
    expect(genderLabel('M')).toBe('Nam')
    expect(genderLabel('F')).toBe('Nữ')
    expect(genderLabel('O')).toBe('Khác')
  })

  it('formatThousands dung dau phan cach kieu Viet Nam', () => {
    expect(formatThousands(3397)).toBe('3.397')
  })

  it('dash thay rong bang "—"', () => {
    expect(dash(null)).toBe('—')
    expect(dash('')).toBe('—')
    expect(dash('CT-01')).toBe('CT-01')
  })
})
