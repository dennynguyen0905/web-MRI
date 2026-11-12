import type { AiAnalysis } from '@/types/aiAnalysis'

/**
 * Cổng đọc dữ liệu AI (doc/11-ai-design.md mục 1.1 bảng "AI ↔ RIS", mục 3.2). AI Panel đọc
 * dữ liệu phi ảnh (nhãn, điểm tin cậy, văn bản nháp) trực tiếp từ API đọc của AI Backend theo
 * `StudyInstanceUID` — KHÔNG đọc qua RIS, KHÔNG gọi thẳng MySQL của RIS.
 *
 * AI Backend là module thứ 4, hiện [HOÃN] H-04 (chưa có backend thật) — `MockAiClient` là
 * implementation duy nhất tồn tại hôm nay. Khi H-04 được làm, chỉ cần viết một implementation
 * mới (ví dụ `HttpAiClient`) gọi API thật của AI Backend và thay vào nơi khởi tạo
 * (`AiPanel` nhận `aiService` qua props, không tự new MockAiClient bên trong — xem
 * components/AiPanel.tsx) — không sửa UI.
 */
export interface AiService {
  /**
   * Trả `null` nghĩa là `ai_status = NOT_TRIGGERED` — chưa từng có lượt xử lý AI nào cho study
   * này (không có bản ghi `ai_analysis`, doc/11 mục 2 bước 1). Không throw cho trường hợp này.
   */
  getAnalysis(studyInstanceUid: string, signal?: AbortSignal): Promise<AiAnalysis | null>
}
