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
        entry.setPatientId(resolvePatientId(joinPoint, audited, result));
        entry.setEntityId(resolveEntityId(joinPoint, audited, result));
        entry.setIpAddress(currentIpAddress());
        entry.setOccurredAt(LocalDateTime.now());
        auditLogDao.save(entry);

        return result;
    }

    private Long resolvePatientId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        Long id = longArg(joinPoint, audited.patientIdArg());
        if (id != null) {
            return id;
        }
        return extractLongProperty(result, "patientId");
    }

    private Long resolveEntityId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        Long id = longArg(joinPoint, audited.entityIdArg());
        if (id != null) {
            return id;
        }
        return extractLongProperty(result, "entityId", "studyId", "id");
    }

    private Long extractLongProperty(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        Class<?> clazz = target.getClass();
        for (String name : methodNames) {
            try {
                var method = clazz.getMethod(name);
                Object val = method.invoke(target);
                if (val instanceof Number n) {
                    return n.longValue();
                }
            } catch (NoSuchMethodException ignored) {
                String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                try {
                    var method = clazz.getMethod(getter);
                    Object val = method.invoke(target);
                    if (val instanceof Number n) {
                        return n.longValue();
                    }
                } catch (Exception ignored2) {
                }
            } catch (Exception ignored) {
            }
        }
        return null;
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
