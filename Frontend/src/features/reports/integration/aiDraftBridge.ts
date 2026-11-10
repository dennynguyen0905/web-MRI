/**
 * Kênh postMessage OHIF (AI Panel) → RIS (`ReportModal`) — doc/11-ai-design.md mục 4, 5.2.5.d.
 * Phía OHIF: deploy/ohif-extensions/vr-ris-demo-panel/src/risBridge.js (schema phải khớp tay đôi,
 * hai package build riêng biệt — không có cách chia sẻ type/module giữa chúng).
 *
 * Nguyên tắc cứng (xem báo cáo cuối task nối kênh):
 *   - KHÔNG BAO GIỜ dùng targetOrigin "*".
 *   - OHIF_ORIGIN lấy từ cấu hình (`VITE_OHIF_BASE_URL`, đã có sẵn cho deep-link — tái dùng, không
 *     thêm biến mới), KHÔNG hard-code.
 *   - Chỉ tin message khi cả `event.origin` khớp allowlist VÀ `event.source` là một cửa sổ do
 *     chính RIS mở (`ohifWindowRegistry`) — sai thì bỏ qua im lặng (console.debug).
 *   - Không log nội dung nháp đầy đủ ra console (doc/11 mục 6.2) — chỉ log khoá trường.
 */
import { isKnownOhifWindow } from '@/features/studies/lib/ohifWindowRegistry'
import type { ReportDraft } from '../hooks/useStudyReport'

export const AI_DRAFT_SCHEMA_VERSION = 1
export const ALLOWED_DRAFT_KEYS = ['technique', 'findings', 'diagnosis', 'conclusion', 'recommendation'] as const
export type AiDraftFieldKey = (typeof ALLOWED_DRAFT_KEYS)[number]
const MAX_FIELD_LENGTH = 5000

export type AiDraftAckReason = 'NO_OPEN_REPORT' | 'STUDY_MISMATCH' | 'USER_DECLINED' | 'INVALID'

export interface AiDraftApplyMessage {
  v: 1
  type: 'AI_DRAFT_APPLY'
  mode: 'use' | 'edit'
  studyInstanceUID: string
  fields: Partial<Record<AiDraftFieldKey, string>>
}

export interface AiDraftDismissMessage {
  v: 1
  type: 'AI_DRAFT_DISMISS'
  studyInstanceUID: string
}

/** Lấy origin OHIF từ cấu hình đã có (`VITE_OHIF_BASE_URL`, dùng chung với deep-link viewer-link). */
export function getOhifOrigin(): string | null {
  const base = import.meta.env.VITE_OHIF_BASE_URL || 'http://localhost:8043'
  try {
    return new URL(base).origin
  } catch {
    return null
  }
}

function sanitizeFields(raw: unknown): Partial<Record<AiDraftFieldKey, string>> {
  const out: Partial<Record<AiDraftFieldKey, string>> = {}
  if (!raw || typeof raw !== 'object') return out
  const r = raw as Record<string, unknown>
  for (const key of ALLOWED_DRAFT_KEYS) {
    const v = r[key]
    if (typeof v === 'string' && v.length > 0 && v.length <= MAX_FIELD_LENGTH) out[key] = v
  }
  return out
}

export function parseAiDraftApply(data: unknown): AiDraftApplyMessage | null {
  if (!data || typeof data !== 'object') return null
  const d = data as Record<string, unknown>
  if (d.v !== AI_DRAFT_SCHEMA_VERSION) return null
  if (d.type !== 'AI_DRAFT_APPLY') return null
  if (d.mode !== 'use' && d.mode !== 'edit') return null
  if (typeof d.studyInstanceUID !== 'string' || d.studyInstanceUID === '') return null
  const fields = sanitizeFields(d.fields)
  if (Object.keys(fields).length === 0) return null
  return { v: 1, type: 'AI_DRAFT_APPLY', mode: d.mode, studyInstanceUID: d.studyInstanceUID, fields }
}

export function parseAiDraftDismiss(data: unknown): AiDraftDismissMessage | null {
  if (!data || typeof data !== 'object') return null
  const d = data as Record<string, unknown>
  if (d.v !== AI_DRAFT_SCHEMA_VERSION) return null
  if (d.type !== 'AI_DRAFT_DISMISS') return null
  if (typeof d.studyInstanceUID !== 'string' || d.studyInstanceUID === '') return null
  return { v: 1, type: 'AI_DRAFT_DISMISS', studyInstanceUID: d.studyInstanceUID }
}

function sendAck(target: Window, studyInstanceUID: string, ok: boolean, reason?: AiDraftAckReason): void {
  const origin = getOhifOrigin()
  if (!origin) return
  target.postMessage(
    { v: AI_DRAFT_SCHEMA_VERSION, type: 'AI_DRAFT_ACK', studyInstanceUID, ok, ...(reason ? { reason } : {}) },
    origin,
  )
}

export interface AiDraftBridgeState {
  /** `ReportModal` đang mở (prop `open`) cho đúng ca này. */
  open: boolean
  studyInstanceUid: string | null
  draft: ReportDraft
}

export interface AiDraftBridgeHandlers {
  getState: () => AiDraftBridgeState
  /** Ghi fields vào draft hiện tại; `focusFirst` true khi mode là "edit" (mục 4.2). */
  applyFields: (fields: Partial<ReportDraft>, focusFirst: boolean) => void
  /** Hỏi xác nhận trước khi ghi đè các trường đã có nội dung; trả true nếu bác sĩ đồng ý. */
  confirmOverwrite: (keys: AiDraftFieldKey[]) => boolean
}

/** Gắn listener `message` cho kênh AI draft. Trả về hàm huỷ đăng ký. */
export function attachAiDraftBridge(handlers: AiDraftBridgeHandlers): () => void {
  const listener = (event: MessageEvent) => {
    const origin = getOhifOrigin()
    if (!origin || event.origin !== origin) {
      console.debug('[ai-draft-bridge] bỏ message từ origin lạ', event.origin)
      return
    }
    if (!isKnownOhifWindow(event.source)) {
      console.debug('[ai-draft-bridge] bỏ message từ window lạ (không phải cửa sổ OHIF do RIS mở)')
      return
    }

    const data = event.data
    if (!data || typeof data !== 'object') {
      console.debug('[ai-draft-bridge] bỏ message không đúng dạng object')
      return
    }
    const msgType = (data as Record<string, unknown>).type

    if (msgType === 'AI_DRAFT_DISMISS') {
      const dismiss = parseAiDraftDismiss(data)
      if (dismiss) {
        // Stub (doc/11-ai-design.md mục 4.2): chỉ ghi nhận cục bộ, không gọi API nào.
        console.info('[ai-draft-bridge] bác sĩ bỏ qua gợi ý AI (ghi nhận cục bộ)', { studyInstanceUID: dismiss.studyInstanceUID })
      } else {
        console.debug('[ai-draft-bridge] AI_DRAFT_DISMISS sai schema, bỏ qua')
      }
      return
    }

    if (msgType !== 'AI_DRAFT_APPLY') {
      console.debug('[ai-draft-bridge] bỏ message type lạ', msgType)
      return
    }

    const source = event.source as Window
    const apply = parseAiDraftApply(data)
    if (!apply) {
      const rawUid = (data as Record<string, unknown>).studyInstanceUID
      if (typeof rawUid === 'string' && rawUid) sendAck(source, rawUid, false, 'INVALID')
      console.debug('[ai-draft-bridge] AI_DRAFT_APPLY sai schema/quá dài/không phải chuỗi, bị từ chối')
      return
    }

    const state = handlers.getState()
    if (!state.open || !state.studyInstanceUid) {
      sendAck(source, apply.studyInstanceUID, false, 'NO_OPEN_REPORT')
      return
    }
    if (state.studyInstanceUid !== apply.studyInstanceUID) {
      sendAck(source, apply.studyInstanceUID, false, 'STUDY_MISMATCH')
      return
    }

    const overwritten = ALLOWED_DRAFT_KEYS.filter(
      key => apply.fields[key] !== undefined && state.draft[key].trim() !== '',
    )
    if (overwritten.length > 0 && !handlers.confirmOverwrite(overwritten)) {
      sendAck(source, apply.studyInstanceUID, false, 'USER_DECLINED')
      return
    }

    handlers.applyFields(apply.fields, apply.mode === 'edit')
    sendAck(source, apply.studyInstanceUID, true)
  }

  window.addEventListener('message', listener)
  return () => window.removeEventListener('message', listener)
}
