import { describe, expect, it } from 'vitest'
import {
  DONE_STATUSES, PENDING_STATUSES, STATUS_META, STATUS_ORDER, isStudyStatus,
} from '@/shared/lib/status'
import type { StudyStatus } from '@/shared/lib/types'

/**
 * TC-16 (`doc/12` muc 3.1): worklist phai dem du 7 nhom trang thai, ke ca `CANCELLED`, de ca da
 * huy van tra cuu duoc chu khong bien mat khoi giao dien.
 *
 * LUU Y VE TAI LIEU: doc/12 phien ban 0.1 (2026-11-05) ghi TC-16 bi chan vi "Frontend chua co
 * CANCELLED trong src/lib/types.ts". Dieu nay DA LOI THOI — tu 2026-11-12 Frontend co du 7 gia
 * tri tai `src/shared/lib/types.ts`. Test duoi day chot lai dieu do.
 */
describe('TC-16: du 7 nhom trang thai', () => {
  const EXPECTED: StudyStatus[] = [
    'SCHEDULED', 'UNREAD', 'READING', 'READ', 'APPROVING', 'APPROVED', 'CANCELLED',
  ]

  it('STATUS_ORDER co dung 7 trang thai, dung thu tu vong doi', () => {
    expect(STATUS_ORDER).toEqual(EXPECTED)
    expect(STATUS_ORDER).toHaveLength(7)
  })

  it('STATUS_META phu du 7 trang thai, moi trang thai co nhan tieng Viet khong rong', () => {
    expect(Object.keys(STATUS_META)).toHaveLength(7)
    for (const s of EXPECTED) {
      expect(STATUS_META[s], `thieu meta cho ${s}`).toBeDefined()
      expect(STATUS_META[s].key).toBe(s)
      expect(STATUS_META[s].label.trim().length).toBeGreaterThan(0)
      expect(STATUS_META[s].color.trim().length).toBeGreaterThan(0)
      expect(STATUS_META[s].shape.trim().length).toBeGreaterThan(0)
    }
  })

  it('CANCELLED ton tai va co nhan "Đã hủy" (chot lai dong lo thoi cua doc/12)', () => {
    expect(isStudyStatus('CANCELLED')).toBe(true)
    expect(STATUS_META.CANCELLED.label).toBe('Đã hủy')
  })

  it('hinh dang chot chi dung trong tap da dinh nghia', () => {
    const allowed = ['clock', 'dot', 'ring', 'check', 'square', 'cross']
    for (const s of EXPECTED) expect(allowed).toContain(STATUS_META[s].shape)
  })

  it('READING va READ dung chung shape "dot", phan biet bang mau (co chu y)', () => {
    // Khong phai loi: 6 shape cho 7 trang thai. Hai trang thai "dang doc"/"doc xong" cung la
    // chot tron, khac mau. Chot lai de ai doi shape thi biet minh dang doi quy uoc hien thi.
    expect(STATUS_META.READING.shape).toBe('dot')
    expect(STATUS_META.READ.shape).toBe('dot')
    expect(STATUS_META.READING.color).not.toBe(STATUS_META.READ.color)
  })
})

describe('quy tac phan tab Cho / Da thuc hien', () => {
  it('tab "Cho" chi gom SCHEDULED', () => {
    expect(PENDING_STATUSES).toEqual(['SCHEDULED'])
  })

  it('tab "Da thuc hien" gom 6 trang thai con lai, CO CANCELLED', () => {
    expect(DONE_STATUSES).toHaveLength(6)
    expect(DONE_STATUSES).toContain('CANCELLED')
    expect(DONE_STATUSES).not.toContain('SCHEDULED')
  })

  it('hai tab phu kin 7 trang thai va khong chong lan nhau', () => {
    const union = [...PENDING_STATUSES, ...DONE_STATUSES]
    expect(new Set(union).size).toBe(7)
    expect(union).toHaveLength(7)
    for (const s of PENDING_STATUSES) expect(DONE_STATUSES).not.toContain(s)
  })

  it('DONE_STATUSES giu dung thu tu cua STATUS_ORDER', () => {
    expect(DONE_STATUSES).toEqual(STATUS_ORDER.filter(s => s !== 'SCHEDULED'))
  })
})

describe('isStudyStatus chan gia tri la', () => {
  it('nhan dung 7 ma hop le', () => {
    for (const s of STATUS_ORDER) expect(isStudyStatus(s)).toBe(true)
  })

  it('tu choi ma khong thuoc ERD', () => {
    expect(isStudyStatus('COMPLETED')).toBe(false)
    expect(isStudyStatus('')).toBe(false)
    expect(isStudyStatus('scheduled')).toBe(false)
  })
})
