import { api } from '@/shared/api/client'

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
 * Lấy deep-link từ backend rồi mở tab mới với rel="noopener,noreferrer".
 */
export async function openStudyViewer(studyId: number): Promise<void> {
  try {
    const data = await fetchViewerLink(studyId)
    if (data.viewer_url) {
      window.open(data.viewer_url, '_blank', 'noopener,noreferrer')
    } else {
      alert('Không nhận được đường dẫn xem ảnh từ máy chủ.')
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Không thể mở viewer xem ảnh.'
    alert(message)
  }
}

