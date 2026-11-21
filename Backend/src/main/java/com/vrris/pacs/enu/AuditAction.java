package com.vrris.pacs.enu;

/**
 * Khop cot "audit_log.action" (doc/02-erd.md muc 4.11, database/migrations/V5__audit_and_conflict.sql).
 *
 * LOGIN_FAILED / ACCESS_DENIED: ghi khi hanh vi @Audited THAT BAI vi ly do bao mat (dang nhap sai,
 * service tu choi quyen 403) - xem audit.AuditLogAspect. Cot action la VARCHAR(20) khong co rang
 * buoc ENUM/CHECK nen khong can migration; ❓ doc/02-erd.md muc 4.11 chua liet ke hai gia tri nay.
 */
public enum AuditAction {
    READ,           // 0
    VIEW_IMAGE,     // 1
    CREATE,         // 2
    UPDATE,         // 3
    DELETE,         // 4
    STATUS_CHANGE,  // 5
    LOGIN,          // 6
    PRINT,          // 7
    EXPORT,         // 8
    LOGIN_FAILED,   // 9
    ACCESS_DENIED   // 10
}
