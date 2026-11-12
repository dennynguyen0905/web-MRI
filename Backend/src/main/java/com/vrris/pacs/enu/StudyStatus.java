package com.vrris.pacs.enu;

/**
 * 7 trang thai ca chup theo README Phan 4 / doc/02-erd.md muc 4.5. CANCELLED them vao cho IN-09
 * (GET /studies/status-counts phai dem du 7 nhom, khop doc/api/openapi.yaml schema StatusCounts) -
 * pham vi them nay CHI la gia tri enum de nhan dien/dem, KHONG dong nghia voi T10 (SCHEDULED ->
 * CANCELLED) da duoc xac nhan: rule.StudyStateMachine.LINEAR_FLOW chua co nhanh nao di den/di tu
 * CANCELLED, van ❓ cho toi khi nhom xac nhan dieu kien/vai trò (doc/03-state-machine.md muc 2.2 T10).
 */
public enum StudyStatus {
    SCHEDULED, UNREAD, READING, READ, APPROVING, APPROVED, CANCELLED
}
