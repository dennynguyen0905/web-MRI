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
 * Chua co method nghiep vu nao dung annotation nay trong lan refactor nay (3 endpoint
 * reference-data la danh muc, khong phai du lieu benh nhan -> khong audit). Dung khi co tinh nang
 * dong tren patient/imaging_order/study.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    AuditAction action();

    String entityType();
}
