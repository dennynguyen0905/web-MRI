import { api } from '@/shared/api/client'
import { registerOhifWindow } from '../lib/ohifWindowRegistry'

export interface ViewerLinkDto {
  study_id: number
  patient_id?: number
  study_instance_uid: string
  viewer_url: string
}

/**
 * Gọi backend RIS lấy deep-link OHIF Viewer cho một study (IN-19, IN-23).
 * Backend ghi audit_log VIEW_IMAGE đồng bộ trước khi trả link.
 */
export async function fetchViewerLink(studyId: number): Promise<ViewerLinkDto> {
  return api.request<ViewerLinkDto>({
    method: 'GET',
    path: `/studies/${studyId}/viewer-link`,
  })
}

/**
 * Hàm dùng chung cho cả hai cửa vào (a - worklist và b - tab 3 modal lâm sàng).
 * Lấy deep-link từ backend rồi mở tab mới.
 *
 * KHÔNG dùng "noopener"/"noreferrer" ở đây (khác mọi link khác trong RIS, vốn luôn giữ hai thuộc
 * tính này): RIS cần giữ được tham chiếu `window.open` trả về, và OHIF cần `window.opener` hoạt
 * động, để nối kênh postMessage AI_DRAFT_APPLY/AI_DRAFT_DISMISS (AI Panel OHIF → ReportModal RIS,
 * `doc/11-ai-design.md` mục 4, 5.2.5.d). Trước khi có kênh này, `noopener` luôn khiến
 * `window.opener` rỗng phía OHIF — đây là đánh đổi bảo mật (mất chống "reverse tabnabbing") được
 * chấp nhận vì OHIF là viewer do chính hệ thống kiểm soát, không phải trang ngoài — xem báo cáo
 * cuối của task nối kênh, mục "quyết định cần nhóm duyệt".
 */
export async function openStudyViewer(studyId: number): Promise<void> {
  try {
    const data = await fetchViewerLink(studyId)
    if (data.viewer_url) {
      registerOhifWindow(window.open(data.viewer_url, '_blank'))
    } else {
      alert('Không nhận được đường dẫn xem ảnh từ máy chủ.')
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Không thể mở viewer xem ảnh.'
    alert(message)
  }
}

