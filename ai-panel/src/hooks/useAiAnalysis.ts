import { useCallback, useEffect, useRef, useState } from 'react'
import type { AiService } from '@/services/aiService'
import type { AiAnalysis } from '@/types/aiAnalysis'

export interface UseAiAnalysisResult {
  /** `undefined` = đang tải lần đầu, `null` = NOT_TRIGGERED, object = có bản ghi ai_analysis. */
  analysis: AiAnalysis | null | undefined
  loading: boolean
  /** Gọi lại trạng thái — nút "Làm mới" ở PENDING. KHÔNG tự poll liên tục (doc/11 mục 5.2.3). */
  refresh: () => void
}

/** Lấy kết quả AI theo StudyInstanceUID đang mở trong OHIF (mục B của nhiệm vụ). */
export function useAiAnalysis(studyInstanceUid: string, aiService: AiService): UseAiAnalysisResult {
  const [analysis, setAnalysis] = useState<AiAnalysis | null | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [nonce, setNonce] = useState(0)
  const abortRef = useRef<AbortController | null>(null)

  const refresh = useCallback(() => setNonce(n => n + 1), [])

  useEffect(() => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setLoading(true)
    aiService
      .getAnalysis(studyInstanceUid, controller.signal)
      .then(result => {
        if (controller.signal.aborted) return
        setAnalysis(result)
      })
      .catch(err => {
        if (controller.signal.aborted) return
        // AiService không throw cho NOT_TRIGGERED (trả null) — lỗi ở đây là lỗi tải/kết nối tới
        // AI Backend, không phải ai_status = FAILED (đó nằm trong `analysis.status`). Xử lý như
        // "không tải được" chung, không phải một trạng thái ai_status riêng (doc/11 mục 2 bước 6).
        console.error('useAiAnalysis: không tải được kết quả AI', err)
        setAnalysis(null)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [studyInstanceUid, aiService, nonce])

  return { analysis, loading, refresh }
}
