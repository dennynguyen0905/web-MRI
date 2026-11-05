package com.vrris.pacs.enu;

/**
 * 6 trang thai ca chup theo README Phan 4 / doc/02-erd.md muc 4.5. KHONG co CANCELLED o day -
 * trang thai do van la ❓ chua duoc nhom xac nhan (doc/02-erd.md muc 9). Them vao khi co quyet dinh,
 * cung luc cap nhat rule.StudyStateMachine.
 */
public enum StudyStatus {
    SCHEDULED, UNREAD, READING, READ, APPROVING, APPROVED
}
