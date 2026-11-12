/**
 * Schema theo doc/11-ai-design.md mục 3.2 (`ai_analysis` + `ai_finding`, đề xuất kho dữ liệu
 * riêng của AI Backend — KHÔNG phải bảng của RIS, xem `02-erd.md` mục 8).
 *
 * `ai_status` phía RIS có 5 giá trị (doc/11 mục 0 quyết định 3, mục 5.2.1):
 * NOT_TRIGGERED | PENDING | READY | FAILED | TIMEOUT. `NOT_TRIGGERED` nghĩa là "chưa có lượt
 * xử lý AI nào cho study này" — không có bản ghi `ai_analysis` nào, nên ở đây được biểu diễn
 * bằng `AiService.getAnalysis()` trả về `null`, không phải một giá trị status riêng.
 */
export type AiAnalysisStatus = 'PENDING' | 'READY' | 'FAILED' | 'TIMEOUT'

/**
 * Lý do lỗi — các giá trị đã liệt kê ở doc/11-ai-design.md mục 2 (bước 2–5). Giữ kiểu mở
 * (`| string`) vì AI Backend thật (H-04, chưa làm) có thể trả lý do khác chưa liệt kê ở đây;
 * KHÔNG hiện chuỗi này cho bác sĩ dưới mọi hình thức (mục 5.2.4 — không hiện mã lỗi kỹ thuật).
 */
export type AiErrorReason =
  | 'KHONG_LAY_DUOC_ANH'
  | 'TIEN_XU_LY_LOI'
  | 'SUY_LUAN_LOI'
  | 'LUU_PACS_LOI'
  | string

export interface AiFinding {
  label: string
  /**
   * Xác suất [0,1] do model trả — CHƯA được hiệu chỉnh/đánh giá trong đồ án (doc/11 mục 1.2,
   * mục 7, mục 12 câu 5). Không hiển thị số thập phân thô cho bác sĩ — quy về mức qua
   * `lib/confidence.ts`.
   */
  confidence: number
  /** Tham chiếu DICOM SEG tương ứng trong PACS — OHIF tự vẽ, AI Panel không đọc ảnh. */
  seg_sop_instance_uid: string | null
  draft_technique: string
  draft_findings: string
  draft_diagnosis: string
  draft_conclusion: string
  draft_recommendation: string
}

export interface AiAnalysis {
  study_instance_uid: string
  model_name: string
  model_version: string
  status: AiAnalysisStatus
  triggered_at: string | null
  completed_at: string | null
  error_reason: AiErrorReason | null
  findings: AiFinding[]
}
