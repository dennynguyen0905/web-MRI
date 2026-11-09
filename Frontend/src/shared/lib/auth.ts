import type { AppUser, RoleCode } from './types'

/**
 * RBAC 5 vai trò (IN-14). Frontend chỉ ẩn/khóa nút cho gọn mắt;
 * việc chặn thật nằm ở tầng API backend — scope doc ghi rõ "chặn ở tầng API chứ không chỉ ẩn nút".
 * Quy tắc gán quyền dưới đây là suy luận từ UI tham chiếu và state machine (giả định G-04).
 */

export const ROLE_META: Record<RoleCode, { label: string; short: string }> = {
  ADMIN: { label: 'Quản trị hệ thống', short: 'Admin' },
  CLINICIAN: { label: 'Bác sĩ lâm sàng', short: 'Lâm sàng' },
  TECHNICIAN: { label: 'Kỹ thuật viên', short: 'KTV' },
  RADIOLOGIST: { label: 'Bác sĩ chẩn đoán hình ảnh', short: 'BS CĐHA' },
  APPROVER: { label: 'Bác sĩ duyệt', short: 'BS duyệt' },
}

export type Permission =
  | 'patient.create'      // thêm bệnh nhân / ca chụp
  | 'patient.update'      // cập nhật ca chụp
  | 'order.sync'          // cập nhật chỉ định từ HIS
  | 'clinical.view'       // mở modal thông tin lâm sàng
  | 'report.write'        // nhập, sửa, lưu báo cáo
  | 'report.approve'      // duyệt / trả lại báo cáo
  | 'study.markReceived'  // xác nhận PACS đã nhận đủ ảnh
  | 'study.reopen'        // mở lại ca đã duyệt xong (T9)
  | 'study.cancel'        // hủy ca chưa có ảnh (T10, doc/06-rbac-matrix.md mục 6)
  | 'study.release'       // giải phóng khóa mềm của ca đang đọc/duyệt (T4 do ADMIN thực hiện, D20)
  | 'conflict.resolve'    // xử lý ca lệch dữ liệu HIS/RIS (identity_conflict, D22)
  | 'audit.view'          // xem audit log (IN-15)
  | 'list.print'          // in / tải danh sách
  | 'report.print'        // in phiếu kết quả của một bệnh nhân (khác list.print)
  | 'study.viewImage'     // xem ảnh DICOM trong OHIF Viewer (IN-19, IN-23, doc/06-rbac-matrix.md)
  | 'config.system'       // popover cấu hình hệ thống

/**
 * `report.print` là **đề xuất mới** kèm Task 5, chưa có trong `doc/06-rbac-matrix.md` mục 3.
 * Gán cho cả 5 vai trò vì ai đọc được báo cáo trên màn hình thì in ra không lộ thêm dữ liệu nào;
 * đổi lại mỗi lần in **bắt buộc** ghi `audit_log` action `PRINT` kèm `patient_id` (`doc/09` mục 2.2).
 * Cần nhóm xác nhận rồi bổ sung vào `openapi.yaml` (`x-allowed-roles`) và `doc/06` mục 3.
 *
 * Đã đồng bộ theo `doc/06-rbac-matrix.md` mục 6 (bảng đối chiếu, 2026-11-08):
 * - `report.write`, `report.approve`: bỏ `ADMIN` — `ADMIN` không đọc và không duyệt báo cáo (D15).
 * - `study.reopen`: thêm `APPROVER` (T9 cho phép cả `ADMIN`, `APPROVER`, D16).
 * - `study.cancel`, `study.release`, `conflict.resolve`, `audit.view`: quyền mới, chưa có nút/màn
 *   nào trong Frontend dùng tới — xem `doc/06b-ui-permission-spec.md` cột "Trạng thái".
 */
const PERMISSIONS: Record<RoleCode, Permission[]> = {
  ADMIN: [
    'patient.create', 'patient.update', 'order.sync', 'clinical.view',
    'study.markReceived', 'study.reopen', 'study.cancel', 'study.release', 'conflict.resolve', 'audit.view',
    'list.print', 'report.print', 'study.viewImage', 'config.system',
  ],
  CLINICIAN: ['patient.create', 'patient.update', 'order.sync', 'clinical.view', 'list.print', 'report.print', 'study.viewImage'],
  TECHNICIAN: [
    'patient.create', 'patient.update', 'order.sync', 'clinical.view', 'study.markReceived',
    'study.cancel', 'conflict.resolve', 'list.print', 'report.print', 'study.viewImage',
  ],
  RADIOLOGIST: ['clinical.view', 'report.write', 'list.print', 'report.print', 'study.viewImage'],
  APPROVER: ['clinical.view', 'report.approve', 'study.reopen', 'list.print', 'report.print', 'study.viewImage'],
}

export function can(user: AppUser | null, permission: Permission): boolean {
  if (!user) return false
  return user.roles.some(r => PERMISSIONS[r].includes(permission))
}

/** Nhãn vai trò hiển thị cạnh tên người dùng, ví dụ "Admin · BS CĐHA". */
export function roleLabel(user: AppUser): string {
  return user.roles.map(r => ROLE_META[r].short).join(' · ')
}

/** Lý do một nút bị khóa — hiện trên tooltip để người dùng biết cần vai trò nào. */
export function deniedReason(permission: Permission): string {
  const roles = (Object.keys(PERMISSIONS) as RoleCode[])
    .filter(r => PERMISSIONS[r].includes(permission))
    .map(r => ROLE_META[r].short)
    .join(', ')
  return `Cần vai trò: ${roles}`
}
