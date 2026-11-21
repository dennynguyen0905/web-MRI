import { describe, expect, it } from 'vitest'
import { ApiError } from '@/shared/api/http'
import type { StudyStatus } from '@/shared/lib/types'
import {
  type ReportDraftLike, type ReportStepsApi, ReportStepError, performSaveDraft, performTransition,
} from './reportSteps'

const EMPTY: ReportDraftLike = { technique: '', findings: '', diagnosis: '', conclusion: '', recommendation: '' }
const TYPED: ReportDraftLike = { ...EMPTY, technique: 'KT', conclusion: 'KL' }

/** API giả ghi lại thứ tự gọi; `failOn` cho một bước cụ thể ném lỗi. */
function fakeApi(failOn?: 'save' | `transition:${StudyStatus}`) {
  const calls: string[] = []
  const conflict = new ApiError(409, 'STUDY_NOT_READING', 'Ca chụp không ở trạng thái READING.')
  const api: ReportStepsApi = {
    async transitionStudy(_id, to) {
      calls.push(`transition:${to}`)
      if (failOn === `transition:${to}`) throw conflict
    },
    async saveReportDraft() {
      calls.push('save')
      if (failOn === 'save') throw conflict
    },
  }
  return { api, calls, conflict }
}

async function caught(promise: Promise<unknown>): Promise<unknown> {
  return promise.then(() => null, (e: unknown) => e)
}

describe('performSaveDraft', () => {
  it('ca READING: chỉ lưu nháp, giữ trạng thái', async () => {
    const { api, calls } = fakeApi()
    expect(await performSaveDraft(api, 1, TYPED, 'READING')).toBe('READING')
    expect(calls).toEqual(['save'])
  })

  it('ca UNREAD: mở ca (T3) rồi lưu, trả READING', async () => {
    const { api, calls } = fakeApi()
    expect(await performSaveDraft(api, 1, TYPED, 'UNREAD')).toBe('READING')
    expect(calls).toEqual(['transition:READING', 'save'])
  })

  it('T3 xong mà lưu lỗi: ném ReportStepError mang trạng thái thật READING và lỗi gốc', async () => {
    const { api, conflict } = fakeApi('save')
    const err = await caught(performSaveDraft(api, 1, TYPED, 'UNREAD'))
    expect(err).toBeInstanceOf(ReportStepError)
    expect((err as ReportStepError).reachedStatus).toBe('READING')
    expect((err as ReportStepError).original).toBe(conflict)
  })

  it('lỗi ngay ở bước đầu (trạng thái chưa đổi): ném nguyên lỗi gốc', async () => {
    const { api, conflict } = fakeApi('transition:READING')
    expect(await caught(performSaveDraft(api, 1, TYPED, 'UNREAD'))).toBe(conflict)
  })
})

describe('performTransition', () => {
  it('T3 từ UNREAD có nội dung đã gõ: mở ca rồi lưu', async () => {
    const { api, calls } = fakeApi()
    expect(await performTransition(api, 1, 'READING', TYPED, null, 'UNREAD')).toBe('READING')
    expect(calls).toEqual(['transition:READING', 'save'])
  })

  it('T3 từ UNREAD form trống: không gọi lưu', async () => {
    const { api, calls } = fakeApi()
    await performTransition(api, 1, 'READING', EMPTY, null, 'UNREAD')
    expect(calls).toEqual(['transition:READING'])
  })

  it('T5 từ READING: lưu trước rồi chốt', async () => {
    const { api, calls } = fakeApi()
    expect(await performTransition(api, 1, 'READ', TYPED, null, 'READING')).toBe('READ')
    expect(calls).toEqual(['save', 'transition:READ'])
  })

  it('T5: lưu lỗi thì KHÔNG chốt (không chốt bản chính thức với nội dung cũ)', async () => {
    const { api, calls, conflict } = fakeApi('save')
    expect(await caught(performTransition(api, 1, 'READ', TYPED, null, 'READING'))).toBe(conflict)
    expect(calls).toEqual(['save'])
  })

  it('T5 từ UNREAD: T3 → lưu → T5; lưu lỗi thì báo trạng thái thật READING', async () => {
    const ok = fakeApi()
    expect(await performTransition(ok.api, 1, 'READ', TYPED, null, 'UNREAD')).toBe('READ')
    expect(ok.calls).toEqual(['transition:READING', 'save', 'transition:READ'])

    const failing = fakeApi('save')
    const err = await caught(performTransition(failing.api, 1, 'READ', TYPED, null, 'UNREAD'))
    expect((err as ReportStepError).reachedStatus).toBe('READING')
    expect(failing.calls).toEqual(['transition:READING', 'save'])
  })

  it('bước chuyển đơn (T6, T8…) chỉ gọi một lần, truyền lý do', async () => {
    const calls: [StudyStatus, string | null][] = []
    const api: ReportStepsApi = {
      async transitionStudy(_id, to, reason) { calls.push([to, reason]) },
      async saveReportDraft() { throw new Error('không được gọi') },
    }
    expect(await performTransition(api, 1, 'READING', TYPED, 'Trả lại', 'APPROVING')).toBe('READING')
    expect(calls).toEqual([['READING', 'Trả lại']])
  })
})
