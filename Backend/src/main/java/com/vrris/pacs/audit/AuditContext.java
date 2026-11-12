package com.vrris.pacs.audit;

/**
 * Kenh phu de service.impl truyen them du lieu audit ma AuditLogAspect khong tu suy ra duoc tu
 * tham so/ket qua method: before_value/after_value (snapshot truoc/sau khi sua - tra mot phan no
 * ky thuat da ghi o Audited/AuditLogAspect, lam dau tien cho IN-10), va entity_id/patient_id khi
 * entity bi sua KHONG phai chinh resource tren URL (vi du PUT .../reports/current nhan studyId
 * tren path nhung audit_log.entity_id phai la report.id, entity_type = REPORT).
 *
 * Dung ThreadLocal vi AuditLogAspect.around() doc gia tri NGAY SAU joinPoint.proceed() tra ve,
 * CUNG thread/transaction (dong bo, xem Javadoc AuditLogAspect) - khong di qua request/thread khac.
 * AuditLogAspect LUON doc (consume*) dung mot lan cho moi key, ca khi method @Audited nem exception,
 * de khong de sot gia tri sang request sau tren thread bi servlet container tai su dung.
 *
 * service.impl KHONG tu ghi audit_log qua class nay (van la AOP, dong bo, dong quy uoc
 * doc/15-kien-truc-backend.md muc 10) - chi cung cap du lieu cho AuditLogAspect dung.
 */
public final class AuditContext {

    private static final ThreadLocal<Long> PATIENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ENTITY_ID = new ThreadLocal<>();
    private static final ThreadLocal<Object> BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Object> AFTER = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void recordPatientId(Long patientId) {
        PATIENT_ID.set(patientId);
    }

    public static void recordEntityId(Long entityId) {
        ENTITY_ID.set(entityId);
    }

    /** snapshot truoc khi sua - vi du Map&lt;String,Object&gt; cac truong bi anh huong. */
    public static void recordBefore(Object snapshot) {
        BEFORE.set(snapshot);
    }

    /** snapshot sau khi sua - cung dang voi recordBefore. */
    public static void recordAfter(Object snapshot) {
        AFTER.set(snapshot);
    }

    static Long consumePatientId() {
        return consume(PATIENT_ID);
    }

    static Long consumeEntityId() {
        return consume(ENTITY_ID);
    }

    static Object consumeBefore() {
        return consume(BEFORE);
    }

    static Object consumeAfter() {
        return consume(AFTER);
    }

    private static <T> T consume(ThreadLocal<T> threadLocal) {
        T value = threadLocal.get();
        threadLocal.remove();
        return value;
    }
}
