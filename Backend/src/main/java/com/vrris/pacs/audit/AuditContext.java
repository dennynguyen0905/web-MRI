package com.vrris.pacs.audit;

/**
 * Kenh phu de service.impl truyen them du lieu audit ma AuditLogAspect khong tu suy ra duoc tu
 * tham so/ket qua method: before_value/after_value (snapshot truoc/sau khi sua - tra mot phan no
 * ky thuat da ghi o Audited/AuditLogAspect, lam dau tien cho IN-10), va entity_id/patient_id khi
 * entity bi sua KHONG phai chinh resource tren URL (vi du PUT .../reports/current nhan studyId
 * tren path nhung audit_log.entity_id phai la report.id, entity_type = REPORT).
 *
 * Dung ThreadLocal vi AuditLogAspect.around() doc gia tri NGAY SAU joinPoint.proceed() tra ve,
 * CUNG thread (dong bo, xem Javadoc AuditLogAspect) - khong di qua request/thread khac.
 *
 * Pham vi: moi lan goi method @Audited co pham vi rieng. AuditLogAspect goi {@link #suspend()} khi
 * vao method (cat gia tri cua method @Audited ben ngoai, neu dang long nhau - vi du
 * ImagingOrderServiceImpl.create goi StudyService.addToOrder) va {@link #restore(Scope)} trong
 * finally khi ra - nen: (1) method trong khong doc nham/khong xoa mat gia tri cua method ngoai;
 * (2) sau method ngoai cung, moi key deu duoc xoa, ke ca khi method nem exception hay khi id da lay
 * duoc tu tham so (truoc day key khong duoc consume trong truong hop do -> ro sang request sau tren
 * thread bi servlet container tai su dung).
 *
 * service.impl KHONG tu ghi audit_log qua class nay (van la AOP, dong bo, dong quy uoc
 * doc/15-kien-truc-backend.md muc 10) - chi cung cap du lieu cho AuditLogAspect dung.
 */
public final class AuditContext {

    private static final ThreadLocal<Long> PATIENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ENTITY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ACTOR_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Object> BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Object> AFTER = new ThreadLocal<>();

    private AuditContext() {
    }

    /** Gia tri dang dat cua method @Audited ben ngoai, cat tam khi vao method @Audited long ben trong. */
    record Scope(Long patientId, Long entityId, String actorUsername, Object before, Object after) {
    }

    public static void recordActorUsername(String username) {
        ACTOR_USERNAME.set(username);
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

    static Long patientId() {
        return PATIENT_ID.get();
    }

    static Long entityId() {
        return ENTITY_ID.get();
    }

    static String actorUsername() {
        return ACTOR_USERNAME.get();
    }

    static Object before() {
        return BEFORE.get();
    }

    static Object after() {
        return AFTER.get();
    }

    /** Cat gia tri hien tai (cua method ngoai, neu co) va xoa sach cho method sap chay. */
    static Scope suspend() {
        Scope outer = new Scope(PATIENT_ID.get(), ENTITY_ID.get(), ACTOR_USERNAME.get(), BEFORE.get(), AFTER.get());
        clear();
        return outer;
    }

    /** Xoa gia tri cua method vua xong va tra lai gia tri cua method ngoai (null o tang ngoai cung). */
    static void restore(Scope outer) {
        clear();
        set(PATIENT_ID, outer.patientId());
        set(ENTITY_ID, outer.entityId());
        set(ACTOR_USERNAME, outer.actorUsername());
        set(BEFORE, outer.before());
        set(AFTER, outer.after());
    }

    private static void clear() {
        PATIENT_ID.remove();
        ENTITY_ID.remove();
        ACTOR_USERNAME.remove();
        BEFORE.remove();
        AFTER.remove();
    }

    private static <T> void set(ThreadLocal<T> threadLocal, T value) {
        if (value != null) {
            threadLocal.set(value);
        }
    }
}
