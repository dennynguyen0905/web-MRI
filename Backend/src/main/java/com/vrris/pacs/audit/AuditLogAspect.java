package com.vrris.pacs.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrris.pacs.dao.AuditLogDao;
import com.vrris.pacs.enu.AuditAction;
import com.vrris.pacs.enu.AuditEntityType;
import com.vrris.pacs.exception.ApiException;
import com.vrris.pacs.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ghi audit_log bang AOP, DONG BO (xac nhan 2026-11-05, doc/15-kien-truc-backend.md muc 10) -
 * chay trong cung thread voi method nghiep vu, truoc khi request tra ve. Khong goi tay trong
 * service.impl (quy uoc 7 muc 5 cua doc/15-kien-truc-backend.md).
 *
 * Transaction khi ghi dong audit thanh cong:
 * <ul>
 *   <li>method nghiep vu chay trong transaction GHI -> ghi cung transaction do (audit va du lieu
 *       cung commit/rollback, nhu quyet dinh doc/15 muc 10);</li>
 *   <li>method chay trong transaction readOnly (moi method doc @Transactional(readOnly = true), ke
 *       ca login) -> ghi bang transaction rieng REQUIRES_NEW, van dong bo truoc khi tra ket qua.
 *       Ly do: Spring dat connection JDBC o che do read-only, MySQL Connector/J tu choi INSERT
 *       ("Connection is read-only. Queries leading to data modification are not allowed") -> moi
 *       endpoint doc co @Audited va POST /auth/login tung tra 500 khi chay that (test @Transactional
 *       khong phat hien duoc, xem AuditLogCommittedTransactionTest). Method doc khong co du lieu
 *       nghiep vu nao can commit chung, nen tach transaction khong lam mat tinh nguyen tu.</li>
 * </ul>
 *
 * Hanh vi that bai: chi ghi khi that bai vi ly do BAO MAT - dang nhap sai (ApiException 401 tren
 * method action=LOGIN -> LOGIN_FAILED) va bi tu choi quyen o tang service (ApiException 403 hoac
 * AccessDeniedException -> ACCESS_DENIED). Dong nay ghi bang REQUIRES_NEW vi transaction nghiep vu
 * se rollback cung exception; loi khi ghi chi log canh bao, khong che exception goc. Cac that bai
 * khac (404, 409, 422...) khong ghi, giu hanh vi cu. Tu choi boi @PreAuthorize o controller xay ra
 * TRUOC khi vao service nen khong di qua aspect nay (chua ghi - no ky thuat).
 *
 * patient_id / entity_id: lay theo thu tu tham so method (Audited.patientIdArg/entityIdArg) ->
 * audit.AuditContext -> thuoc tinh cua ket qua. before_value / after_value (IN-10): lay tu
 * AuditContext.recordBefore/recordAfter, serialize JSON bang ObjectMapper cua Spring Boot.
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    /** Khop do dai cot audit_log (V5__audit_and_conflict.sql). */
    private static final int MAX_USERNAME_LENGTH = 100;
    private static final int MAX_IP_LENGTH = 45;

    private final AuditLogDao auditLogDao;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate requiresNew;

    public AuditLogAspect(AuditLogDao auditLogDao, ObjectMapper objectMapper,
                          PlatformTransactionManager transactionManager) {
        this.auditLogDao = auditLogDao;
        this.objectMapper = objectMapper;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        AuditContext.Scope outer = AuditContext.suspend();
        try {
            Object result;
            try {
                result = joinPoint.proceed();
            } catch (Throwable ex) {
                recordSecurityFailure(joinPoint, audited, ex);
                throw ex;
            }

            AuditLog entry = baseEntry(audited.action(), audited.entityType());
            entry.setPatientId(resolvePatientId(joinPoint, audited, result));
            entry.setEntityId(resolveEntityId(joinPoint, audited, result));
            entry.setBeforeValue(toJson(AuditContext.before()));
            entry.setAfterValue(toJson(AuditContext.after()));
            persistSuccess(entry);
            return result;
        } finally {
            AuditContext.restore(outer);
        }
    }

    private void persistSuccess(AuditLog entry) {
        boolean writableTx = TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if (writableTx) {
            auditLogDao.save(entry);
        } else {
            requiresNew.executeWithoutResult(status -> auditLogDao.save(entry));
        }
    }

    private void recordSecurityFailure(ProceedingJoinPoint joinPoint, Audited audited, Throwable ex) {
        AuditAction failureAction = classifySecurityFailure(audited, ex);
        if (failureAction == null) {
            return;
        }
        try {
            AuditLog entry = baseEntry(failureAction, audited.entityType());
            Long patientId = longArg(joinPoint, audited.patientIdArg());
            entry.setPatientId(patientId != null ? patientId : AuditContext.patientId());
            Long entityId = longArg(joinPoint, audited.entityIdArg());
            entry.setEntityId(entityId != null ? entityId : AuditContext.entityId());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("attempted_action", audited.action().name());
            detail.put("error_code", ex instanceof ApiException api ? api.getCode() : "FORBIDDEN");
            entry.setAfterValue(toJson(detail));
            requiresNew.executeWithoutResult(status -> auditLogDao.save(entry));
        } catch (RuntimeException auditEx) {
            log.warn("Khong ghi duoc audit {} cho {}: {}", failureAction, audited.entityType(), auditEx.getMessage());
        }
    }

    private static AuditAction classifySecurityFailure(Audited audited, Throwable ex) {
        if (ex instanceof AccessDeniedException) {
            return AuditAction.ACCESS_DENIED;
        }
        if (ex instanceof ApiException api) {
            if (api.getStatus() == HttpStatus.UNAUTHORIZED && audited.action() == AuditAction.LOGIN) {
                return AuditAction.LOGIN_FAILED;
            }
            if (api.getStatus() == HttpStatus.FORBIDDEN) {
                return AuditAction.ACCESS_DENIED;
            }
        }
        return null;
    }

    private AuditLog baseEntry(AuditAction action, AuditEntityType entityType) {
        AuditLog entry = new AuditLog();
        entry.setActorUsername(truncate(currentUsername(), MAX_USERNAME_LENGTH));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setIpAddress(truncate(currentIpAddress(), MAX_IP_LENGTH));
        entry.setOccurredAt(LocalDateTime.now());
        return entry;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Khong serialize duoc snapshot audit: {}", ex.getMessage());
            return null;
        }
    }

    private Long resolvePatientId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        Long id = longArg(joinPoint, audited.patientIdArg());
        if (id != null) {
            return id;
        }
        if (AuditContext.patientId() != null) {
            return AuditContext.patientId();
        }
        Long fromResult = extractLongProperty(result, "patientId");
        if (fromResult != null) {
            return fromResult;
        }
        for (Object arg : joinPoint.getArgs()) {
            Long fromArg = extractLongProperty(arg, "patientId");
            if (fromArg != null) {
                return fromArg;
            }
        }
        return null;
    }

    private Long resolveEntityId(ProceedingJoinPoint joinPoint, Audited audited, Object result) {
        Long id = longArg(joinPoint, audited.entityIdArg());
        if (id != null) {
            return id;
        }
        if (AuditContext.entityId() != null) {
            return AuditContext.entityId();
        }
        return extractLongProperty(result, "entityId", "studyId", "id");
    }

    /**
     * Doc thuoc tinh so dang record accessor (name()) hoac getter (getName()). Chi bo qua loi
     * "khong co method" / khong goi duoc qua reflection; loi that ben trong accessor van ghi log.
     */
    private static Long extractLongProperty(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            Method method = findAccessor(target.getClass(), name);
            if (method == null) {
                continue;
            }
            try {
                Object val = method.invoke(target);
                if (val instanceof Number n) {
                    return n.longValue();
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                log.debug("Khong doc duoc thuoc tinh audit '{}' cua {}: {}", name,
                        target.getClass().getSimpleName(), ex.getMessage());
            }
        }
        return null;
    }

    private static Method findAccessor(Class<?> clazz, String name) {
        String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (String candidate : new String[]{name, getter}) {
            try {
                Method method = clazz.getMethod(candidate);
                if (method.getParameterCount() == 0) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // thu ten tiep theo
            }
        }
        return null;
    }

    /** Lay tham so thu `index` cua method duoi dang Long; null khi khong co hoac khong phai so. */
    private static Long longArg(ProceedingJoinPoint joinPoint, int index) {
        if (index < 0) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        if (index >= args.length || !(args[index] instanceof Number number)) {
            return null;
        }
        return number.longValue();
    }

    private static String currentUsername() {
        String fromContext = AuditContext.actorUsername();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getPrincipal() == null
                ? "system" : String.valueOf(authentication.getPrincipal());
    }

    /**
     * X-Forwarded-For chi duoc tin khi request den tu proxy noi bo (loopback/dai IP rieng - nginx
     * cua ris-frontend, ingress trong cum k8s local). Request den thang tu IP cong khai co the tu
     * dat header nay de gia mao IP trong audit_log, nen khi do dung remoteAddr.
     */
    private static String currentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String remoteAddr = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isTrustedProxy(remoteAddr)) {
            // IP dau tien: client that; cac IP sau la proxy.
            return forwarded.split(",")[0].trim();
        }
        return remoteAddr;
    }

    static boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank() || !looksLikeIpLiteral(remoteAddr)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddr);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                return true;
            }
            // IPv6 unique local fc00::/7 (Java khong coi la site-local).
            return address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    /** Tranh InetAddress.getByName tra DNS khi remoteAddr la ten mien (chi chap nhan IP literal). */
    private static boolean looksLikeIpLiteral(String value) {
        return value.chars().allMatch(c -> Character.digit(c, 16) >= 0 || c == '.' || c == ':');
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
