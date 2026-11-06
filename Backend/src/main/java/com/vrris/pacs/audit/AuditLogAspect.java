package com.vrris.pacs.audit;

import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Ghi audit_log bang AOP, DONG BO (xac nhan 2026-11-05, doc/15-kien-truc-backend.md muc 10) -
 * chay trong cung thread/transaction voi method nghiep vu, truoc khi request tra ve. Khong goi tay
 * trong service.impl (quy uoc 7 muc 5 cua doc/15-kien-truc-backend.md).
 *
 * patient_id / entity_id / ip_address: da trich duoc (patient_id va entity_id lay tu tham so
 * method theo Audited.patientIdArg/entityIdArg).
 * No ky thuat con lai: before_value / after_value chua trich - can khi lam cac luong sua du lieu.
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
        entry.setPatientId(longArg(joinPoint, audited.patientIdArg()));
        entry.setEntityId(longArg(joinPoint, audited.entityIdArg()));
        entry.setIpAddress(currentIpAddress());
        entry.setOccurredAt(LocalDateTime.now());
        auditLogDao.save(entry);

        return result;
    }

    /** Lay tham so thu `index` cua method duoi dang Long; null khi khong co hoac khong phai so. */
    private Long longArg(ProceedingJoinPoint joinPoint, int index) {
        if (index < 0) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        if (index >= args.length || !(args[index] instanceof Number number)) {
            return null;
        }
        return number.longValue();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : String.valueOf(authentication.getPrincipal());
    }

    private String currentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Dung IP dau tien: client that; cac IP sau la proxy (nginx cua ris-frontend, ingress).
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
