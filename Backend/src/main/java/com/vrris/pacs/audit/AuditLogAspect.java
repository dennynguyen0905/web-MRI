package com.vrris.pacs.audit;

import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.model.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Ghi audit_log bang AOP, DONG BO (xac nhan 2026-11-05, doc/15-kien-truc-backend.md muc 10) -
 * chay trong cung thread/transaction voi method nghiep vu, truoc khi request tra ve. Khong goi tay
 * trong service.impl (quy uoc 7 muc 5 cua doc/15-kien-truc-backend.md).
 *
 * No ky thuat: entity_id / patient_id / before_value / after_value chua duoc trich tu tham so hay
 * ket qua method - can bo sung khi co tinh nang doc tren patient/study (gan annotation @Audited
 * dau tien).
 */
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogDao auditLogDao;

    public AuditLogAspect(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        AuditLog entry = new AuditLog();
        entry.setActorUsername(currentUsername());
        entry.setAction(audited.action().name());
        entry.setEntityType(audited.entityType());
        entry.setOccurredAt(LocalDateTime.now());
        auditLogDao.save(entry);

        return result;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }
}
