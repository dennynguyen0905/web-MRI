import { useState } from 'react'
import type { AiAnalysis } from '@/types/aiAnalysis'
import { primaryDraftOf } from '@/types/reportDraft'
import type { RisBridge } from '@/integration/risBridge'
import { formatDateTime } from '@/lib/format'
import { StatusBadge } from '../StatusBadge'
import { OverlaySection } from './OverlaySection'
import { FindingsSection } from './FindingsSection'
import { ReportDraftPreview } from './ReportDraftPreview'
import { WarningBanner } from './WarningBanner'
import { ActionButtons } from './ActionButtons'
import { PriorReadingNotice } from './PriorReadingNotice'

/**
 * `ai_status = READY` — doc/11-ai-design.md mục 5.2.5. Gộp 4 khối con theo đúng thứ tự tài
 * liệu: overlay → gợi ý chẩn đoán → cảnh báo cố định → hành động.
 */
export function ReadyState({
  analysis,
  risBridge,
  previouslyReviewed,
}: {
  analysis: AiAnalysis
  risBridge: RisBridge
  /** T9 (mục 5.3) — xem integration/deepLinkContext.ts cho nguồn giá trị này. */
  previouslyReviewed: boolean
}) {
  const [revealed, setRevealed] = useState(!previouslyReviewed)
  const [overlayOn, setOverlayOn] = useState(false) // mặc định TẮT — mục 7
  const [opacity, setOpacity] = useState(65)
  const [suggestionOpen, setSuggestionOpen] = useState(false)
  const [suggestionViewedLogged, setSuggestionViewedLogged] = useState(false)
  const [pending, setPending] = useState(false)
  const [resultMessage, setResultMessage] = useState<string | null>(null)

  const primary = primaryDraftOf(analysis.findings)

  const toggleSuggestion = () => {
    if (!suggestionOpen && !suggestionViewedLogged) {
      risBridge.logAiAction('AI_SUGGESTION_VIEWED', {
        study_instance_uid: analysis.study_instance_uid,
        model_name: analysis.model_name,
        model_version: analysis.model_version,
      })
      setSuggestionViewedLogged(true)
    }
    setSuggestionOpen(o => !o)
  }

  async function handoff(action: 'USE_AS_IS' | 'EDIT_BEFORE_USE' | 'DISMISS') {
    setPending(true)
    setResultMessage(null)
    try {
      const result = await risBridge.sendDraftToReportForm({
        study_instance_uid: analysis.study_instance_uid,
        action,
        draft: action === 'DISMISS' ? null : primary?.draft ?? null,
        source_finding_label: action === 'DISMISS' ? null : primary?.source.label ?? null,
        model_name: analysis.model_name,
        model_version: analysis.model_version,
      })
      setResultMessage(result.message)
      const auditAction =
        action === 'USE_AS_IS' ? 'AI_SUGGESTION_ACCEPTED' : action === 'EDIT_BEFORE_USE' ? 'AI_SUGGESTION_EDITED' : 'AI_SUGGESTION_REJECTED'
      // Ghi chú: audit THẬT theo origin (AI_DRAFT/AI_DRAFT_EDITED/MANUAL) chỉ chốt được lúc bác
      // sĩ LƯU báo cáo ở RIS (mục 4.2) — event ở đây chỉ ghi lại Ý ĐỊNH bác sĩ chọn trong AI
      // Panel, không thay cho audit lúc lưu.
      risBridge.logAiAction(auditAction, {
        study_instance_uid: analysis.study_instance_uid,
        model_name: analysis.model_name,
        model_version: analysis.model_version,
      })
    } finally {
      setPending(false)
    }
  }

  if (!revealed) {
    return <PriorReadingNotice onReveal={() => setRevealed(true)} />
  }

  return (
    <>
      <StatusBadge kind="ready" />
      <div className="meta-line">
        {analysis.model_name} v{analysis.model_version} · hoàn tất {formatDateTime(analysis.completed_at)}
      </div>
      <div className="divider" />

      <OverlaySection
        findings={analysis.findings}
        on={overlayOn}
        onToggle={() => setOverlayOn(v => !v)}
        opacity={opacity}
        onOpacityChange={setOpacity}
      />

      <div className="divider" />

      <FindingsSection findings={analysis.findings} open={suggestionOpen} onToggle={toggleSuggestion} />
      {suggestionOpen && primary && (
        <>
          <div className="divider" />
          <ReportDraftPreview draft={primary.draft} />
        </>
      )}

      <WarningBanner />

      <ActionButtons
        pending={pending}
        resultMessage={resultMessage}
        onUseAsIs={() => handoff('USE_AS_IS')}
        onEditBeforeUse={() => handoff('EDIT_BEFORE_USE')}
        onDismiss={() => handoff('DISMISS')}
      />
    </>
  )
}
