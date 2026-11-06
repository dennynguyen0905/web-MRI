package com.vrris.pacs.audit;

import com.vrris.pacs.enu.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Danh dau method trong service.impl can ghi audit_log (README nguyen tac bat bien: moi hanh vi
 * doc/sua/duyet du lieu benh nhan deu phai ghi audit log). AuditLogAspect doc annotation nay qua
 * AOP, dong bo (da xac nhan 2026-11-05, doc/15-kien-truc-backend.md muc 10) - khong goi tay trong
 * service.
 *
 * patientIdArg/entityIdArg tra loi mot phan no ky thuat da ghi o AuditLogAspect: patient_id va
 * entity_id duoc lay tu THAM SO cua method (theo chi so, dem tu 0). Dat -1 (mac dinh) khi method
 * khong co tham so tuong ung. before_value/after_value van chua trich - con no.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    AuditAction action();

    String entityType();

    /** Chi so tham so chua patient_id (kieu Long/Number); -1 = khong co. */
    int patientIdArg() default -1;

    /** Chi so tham so chua entity_id (kieu Long/Number); -1 = khong co. */
    int entityIdArg() default -1;
}
