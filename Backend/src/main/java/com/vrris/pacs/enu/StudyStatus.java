package com.vrris.pacs.enu;

/**
 * 7 trang thai ca chup theo README Phan 4 / doc/02-erd.md muc 4.5 / doc/03-state-machine.md muc 2.1.
 *
 * Cac buoc chuyen hop le (T2..T10) dinh nghia o rule.TransitionCode / rule.StudyStateMachine, ke ca
 * T10 (SCHEDULED -> CANCELLED) da duoc cai o backend tu 2026-11-12. Vai tro/dieu kien cua cac nhanh
 * ngoai le T4, T8, T9, T10 van la DE XUAT ❓ chua duoc nhom xac nhan (doc/03 muc 2.3) - neu nhom
 * chot khac thi sua o rule.TransitionPolicy. CANCELLED la trang thai cuoi, khong co buoc chuyen di.
 */
public enum StudyStatus {
    SCHEDULED, UNREAD, READING, READ, APPROVING, APPROVED, CANCELLED
}
