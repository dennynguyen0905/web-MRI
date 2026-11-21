package com.vrris.pacs.enu;

/**
 * Loai doi tuong bi tac dong trong mot dong audit_log (cot "audit_log.entity_type", doc/02-erd.md
 * muc 4.11, database/migrations/V5__audit_and_conflict.sql). Dung cho audit.Audited#entityType()
 * thay cho chuoi viet tay ("STUDY", "PATIENT"...), de sai chinh ta la loi bien dich.
 *
 * Java tu danh so thu tu (ordinal) theo vi tri khai bao: PATIENT = 0, IMAGING_ORDER = 1, ...
 * CSDL van luu TEN (name()), KHONG luu so thu tu - nen chi THEM gia tri moi o CUOI danh sach, khong
 * doi ten/xoa gia tri da co (du lieu audit cu va bo loc entity_type cua GET /audit-logs dung ten).
 */
public enum AuditEntityType {
    PATIENT,            // 0
    IMAGING_ORDER,      // 1
    STUDY,              // 2
    REPORT,             // 3
    CLINICAL_SNAPSHOT,  // 4
    IDENTITY_CONFLICT,  // 5
    APP_USER,           // 6
    AUDIT_LOG           // 7
}
