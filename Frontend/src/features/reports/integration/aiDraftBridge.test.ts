import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { registerOhifWindow } from '@/features/studies/lib/ohifWindowRegistry'
import type { ReportDraft } from '../hooks/useStudyReport'
import { attachAiDraftBridge, getOhifOrigin, type AiDraftBridgeState } from './aiDraftBridge'

const EMPTY_DRAFT: ReportDraft = {
  technique: '', findings: '', diagnosis: '', conclusion: '', recommendation: '',
}

function fakeOhifWindow(): Window {
  // Chỉ cần đủ để làm `event.source` và `registerOhifWindow` — không cần Window thật.
  return { closed: false } as unknown as Window
}

function dispatch(data: unknown, origin: string, source: Window | object | null) {
  window.dispatchEvent(new MessageEvent('message', { data, origin, source: source as Window }))
}

describe('getOhifOrigin', () => {
  it('suy ra origin từ VITE_OHIF_BASE_URL (mặc định localhost:8043)', () => {
    expect(getOhifOrigin()).toBe('http://localhost:8043')
  })
})

describe('attachAiDraftBridge', () => {
  let detach: () => void
  let ohifWin: Window
  let state: AiDraftBridgeState
  let applyFields: ReturnType<typeof vi.fn>
  let confirmOverwrite: ReturnType<typeof vi.fn>
  const origin = getOhifOrigin() as string

  beforeEach(() => {
    ohifWin = fakeOhifWindow()
    registerOhifWindow(ohifWin)
    state = { open: true, studyInstanceUid: '1.2.3.4', draft: { ...EMPTY_DRAFT } }
    applyFields = vi.fn()
    confirmOverwrite = vi.fn(() => true)
    detach = attachAiDraftBridge({
      getState: () => state,
      applyFields,
      confirmOverwrite,
    })
  })

  afterEach(() => detach())

  it('bỏ qua message từ origin lạ (không gọi applyFields, không gửi ACK)', () => {
    const postMessage = vi.fn()
    const source = { ...fakeOhifWindow(), postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      'http://evil.example',
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).not.toHaveBeenCalled()
  })

  it('bỏ qua message từ window lạ (event.source không phải cửa sổ RIS đã mở)', () => {
    const postMessage = vi.fn()
    const strangerSource = { postMessage } as unknown as Window
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      strangerSource,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).not.toHaveBeenCalled()
  })

  it('từ chối message sai version', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 2, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
  })

  it('từ chối fields không phải chuỗi hoặc quá dài, kèm ACK INVALID', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 12345 } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: false, reason: 'INVALID' },
      origin,
    )

    postMessage.mockClear()
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'x'.repeat(5001) } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: false, reason: 'INVALID' },
      origin,
    )
  })

  it('bỏ trường lạ không nằm trong 5 key cho phép', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      {
        v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4',
        fields: { technique: 'A', not_a_real_field: 'B' },
      },
      origin,
      source,
    )
    expect(applyFields).toHaveBeenCalledWith({ technique: 'A' }, false)
  })

  it('ACK ok:false NO_OPEN_REPORT khi không có ReportModal nào đang mở', () => {
    state.open = false
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: false, reason: 'NO_OPEN_REPORT' },
      origin,
    )
  })

  it('ACK ok:false STUDY_MISMATCH khi ca đang mở khác ca trong message', () => {
    state.studyInstanceUid = '9.9.9.9'
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: false, reason: 'STUDY_MISMATCH' },
      origin,
    )
  })

  it('mode "use": applyFields(fields, focusFirst=false), ACK ok:true', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A', findings: 'B' } },
      origin,
      source,
    )
    expect(applyFields).toHaveBeenCalledWith({ technique: 'A', findings: 'B' }, false)
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: true },
      origin,
    )
  })

  it('mode "edit": applyFields(fields, focusFirst=true)', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'edit', studyInstanceUID: '1.2.3.4', fields: { conclusion: 'C' } },
      origin,
      source,
    )
    expect(applyFields).toHaveBeenCalledWith({ conclusion: 'C' }, true)
  })

  it('trường đã có nội dung bác sĩ nhập -> hỏi xác nhận; đồng ý thì ghi đè', () => {
    state.draft = { ...EMPTY_DRAFT, technique: 'Bác sĩ đã gõ sẵn' }
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      source,
    )
    expect(confirmOverwrite).toHaveBeenCalledWith(['technique'])
    expect(applyFields).toHaveBeenCalledWith({ technique: 'A' }, false)
  })

  it('trường đã có nội dung, bác sĩ từ chối ghi đè -> ACK USER_DECLINED, không đổi draft', () => {
    state.draft = { ...EMPTY_DRAFT, technique: 'Bác sĩ đã gõ sẵn' }
    confirmOverwrite.mockReturnValue(false)
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch(
      { v: 1, type: 'AI_DRAFT_APPLY', mode: 'use', studyInstanceUID: '1.2.3.4', fields: { technique: 'A' } },
      origin,
      source,
    )
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).toHaveBeenCalledWith(
      { v: 1, type: 'AI_DRAFT_ACK', studyInstanceUID: '1.2.3.4', ok: false, reason: 'USER_DECLINED' },
      origin,
    )
  })

  it('AI_DRAFT_DISMISS hợp lệ: không gọi applyFields, không gửi ACK (stub cục bộ)', () => {
    const postMessage = vi.fn()
    const source = { postMessage } as unknown as Window
    registerOhifWindow(source)
    dispatch({ v: 1, type: 'AI_DRAFT_DISMISS', studyInstanceUID: '1.2.3.4' }, origin, source)
    expect(applyFields).not.toHaveBeenCalled()
    expect(postMessage).not.toHaveBeenCalled()
  })
})
