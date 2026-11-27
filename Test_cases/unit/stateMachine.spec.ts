import { describe, expect, it } from 'vitest'
import { TRANSITIONS, canEditReport, transitionsFrom } from '@/shared/lib/stateMachine'
import { STATUS_ORDER } from '@/shared/lib/status'
import type { StudyStatus } from '@/shared/lib/types'

/**
 * State machine ca chup — `doc/03-state-machine.md` muc 2.2/2.5, doi chieu
 * `doc/06-rbac-matrix.md` muc 4 (T1-T10).
 *
 * `Frontend/src/shared/lib/auth.test.ts` da chot bang TRANSITIONS (from/to/permission/
 * exceptional). Phan CHUA duoc phu la hai ham dan xuat `transitionsFrom()` va `canEditReport()`
 * — do la phan test nay nham vao.
 */
describe('transitionsFrom', () => {
  it('SCHEDULED co dung 2 nhanh: T2 (nhan anh) va T10 (huy ca)', () => {
    const codes = transitionsFrom('SCHEDULED').map(t => t.code).sort()
    expect(codes).toEqual(['T10', 'T2'])
  })

  it('UNREAD chi co T3 (mo ca de doc)', () => {
    expect(transitionsFrom('UNREAD').map(t => t.code)).toEqual(['T3'])
  })

  it('READING co T5 (chot bao cao) va T4 (bo do)', () => {
    const codes = transitionsFrom('READING').map(t => t.code).sort()
    expect(codes).toEqual(['T4', 'T5'])
  })

  it('READ chi co T6; APPROVING co T7 va T8; APPROVED chi co T9', () => {
    expect(transitionsFrom('READ').map(t => t.code)).toEqual(['T6'])
    expect(transitionsFrom('APPROVING').map(t => t.code).sort()).toEqual(['T7', 'T8'])
    expect(transitionsFrom('APPROVED').map(t => t.code)).toEqual(['T9'])
  })

  it('CANCELLED la trang thai cuoi: khong co buoc chuyen nao di ra', () => {
    expect(transitionsFrom('CANCELLED')).toEqual([])
  })

  it('moi buoc tra ve dung co `from` khop trang thai duoc hoi', () => {
    for (const s of STATUS_ORDER) {
      for (const t of transitionsFrom(s)) expect(t.from).toBe(s)
    }
  })

  it('tong cac nhanh tren 7 trang thai dung bang so buoc trong TRANSITIONS', () => {
    const total = STATUS_ORDER.reduce((n, s) => n + transitionsFrom(s).length, 0)
    expect(total).toBe(TRANSITIONS.length)
    expect(TRANSITIONS).toHaveLength(9)
  })

  it('moi dich den deu la trang thai hop le cua ERD', () => {
    for (const t of TRANSITIONS) {
      expect(STATUS_ORDER).toContain(t.from)
      expect(STATUS_ORDER).toContain(t.to)
    }
  })
})

describe('canEditReport', () => {
  it('cho go khi ca dang READING', () => {
    expect(canEditReport('READING')).toBe(true)
  })

  it('cho go khi ca con UNREAD — vi reportSteps tu mo ca (T3) truoc khi PUT', () => {
    expect(canEditReport('UNREAD')).toBe(true)
  })

  it('chi xem o cac trang thai con lai', () => {
    const readOnly: StudyStatus[] = ['SCHEDULED', 'READ', 'APPROVING', 'APPROVED', 'CANCELLED']
    for (const s of readOnly) {
      expect(canEditReport(s), `${s} phai la chi xem`).toBe(false)
    }
  })

  it('phu du 7 trang thai: dung 2 trang thai cho go', () => {
    expect(STATUS_ORDER.filter(canEditReport)).toEqual(['UNREAD', 'READING'])
  })
})

describe('nhanh ngoai le bat buoc nhap ly do', () => {
  it('dung 4 buoc ngoai le: T4, T8, T9, T10', () => {
    const codes = TRANSITIONS.filter(t => t.exceptional).map(t => t.code).sort()
    expect(codes).toEqual(['T10', 'T4', 'T8', 'T9'])
  })

  it('cac buoc thong thuong khong bat ly do', () => {
    const normal = TRANSITIONS.filter(t => !t.exceptional).map(t => t.code).sort()
    expect(normal).toEqual(['T2', 'T3', 'T5', 'T6', 'T7'])
  })
})
