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
  | 'study.reopen'        // mở lại ca đã duyệt xong
  | 'list.print'          // in / tải danh sách
  | 'config.system'       // popover cấu hình hệ thống

const PERMISSIONS: Record<RoleCode, Permission[]> = {
  ADMIN: [
    'patient.create', 'patient.update', 'order.sync', 'clinical.view',
    'report.write', 'report.approve', 'study.markReceived', 'study.reopen',
    'list.print', 'config.system',
  ],
  CLINICIAN: ['patient.create', 'patient.update', 'order.sync', 'clinical.view', 'list.print'],
  TECHNICIAN: ['patient.create', 'patient.update', 'order.sync', 'clinical.view', 'study.markReceived', 'list.print'],
  RADIOLOGIST: ['clinical.view', 'report.write', 'list.print'],
  APPROVER: ['clinical.view', 'report.approve', 'list.print'],
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
