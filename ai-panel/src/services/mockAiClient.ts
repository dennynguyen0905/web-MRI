import type { AiService } from './aiService'
import type { AiAnalysis } from '@/types/aiAnalysis'
import pendingFixture from './fixtures/pending.json'
import readyFixture from './fixtures/ready.json'
import failedFixture from './fixtures/failed.json'
import timeoutFixture from './fixtures/timeout.json'

const SIMULATED_LATENCY_MS = 300

const FIXTURES: Record<string, AiAnalysis> = {
  [pendingFixture.study_instance_uid]: pendingFixture as AiAnalysis,
  [readyFixture.study_instance_uid]: readyFixture as AiAnalysis,
  [failedFixture.study_instance_uid]: failedFixture as AiAnalysis,
  [timeoutFixture.study_instance_uid]: timeoutFixture as AiAnalysis,
}

/**
 * `study_instance_uid` demo cho mỗi trong 5 `ai_status` — dùng ở DemoApp.tsx để lắp đủ 5 trạng
 * thái. `NOT_TRIGGERED` không có fixture (đúng ngữ nghĩa "chưa có lượt xử lý AI nào") nên bất kỳ
 * UID không nằm trong FIXTURES đều trả `null`.
 */
export const DEMO_STUDY_UIDS = {
  NOT_TRIGGERED: '1.2.826.0.1.3680043.8.498.90000000',
  PENDING: pendingFixture.study_instance_uid,
  READY: readyFixture.study_instance_uid,
  FAILED: failedFixture.study_instance_uid,
  TIMEOUT: timeoutFixture.study_instance_uid,
} as const

/**
 * Đọc từ fixture JSON tĩnh — đứng vào vị trí API đọc thật của AI Backend (H-04, [HOÃN], chưa
 * có backend thật trong phạm vi đồ án). Độ trễ giả lập để UI loading có ý nghĩa khi demo.
 */
export class MockAiClient implements AiService {
  async getAnalysis(studyInstanceUid: string, signal?: AbortSignal): Promise<AiAnalysis | null> {
    await delay(SIMULATED_LATENCY_MS, signal)
    return FIXTURES[studyInstanceUid] ?? null
  }
}

function delay(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const id = setTimeout(resolve, ms)
    signal?.addEventListener('abort', () => {
      clearTimeout(id)
      reject(new DOMException('Aborted', 'AbortError'))
    })
  })
}
