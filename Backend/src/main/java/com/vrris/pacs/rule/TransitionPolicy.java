package com.vrris.pacs.rule;

import com.vrris.pacs.enu.UserRole;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Luat "ai duoc thuc hien buoc chuyen Tx va can dieu kien gi" (doc/03-state-machine.md muc 2.2,
 * 2.4, 2.5; doc/06-rbac-matrix.md muc 4). Thuan Java (R-05 L5: chi phu thuoc enu va JDK) de unit
 * test truc tiep, khong can Spring/DB.
 *
 * Thu tu kiem theo doc/06 muc 4: vai tro (403) -> dieu kien nghiep vu (422 hoac 409). Tra ve vi
 * pham DAU TIEN gap phai, hoac Optional.empty() neu duoc phep. Service.impl doi Kind sang HTTP
 * status va nem ApiException; viec thay doi du lieu (effect cua tung buoc) van nam o service.impl.
 *
 * Cac diem dang la DE XUAT ❓ trong doc/03 (chua duoc nhom xac nhan) nhung da duoc cai o day:
 * ADMIN giai phong khoa o T4, ADMIN/APPROVER o T9, ADMIN/TECHNICIAN o T10, nguoi duyet khac nguoi
 * doc (T6, T7), technique + conclusion bat buoc o T5.
 */
public final class TransitionPolicy {

    private TransitionPolicy() {
    }

    /** Loai vi pham - service.impl doi sang HTTP status (403 / 409 / 422). */
    public enum Kind {
        FORBIDDEN, CONFLICT, UNPROCESSABLE
    }

    public record Violation(Kind kind, String code, String message) {
    }

    /** Nguoi goi: id app_user va cac vai tro (lay tu JWT). */
    public record Actor(Long userId, Set<UserRole> roles) {

        public Actor {
            Objects.requireNonNull(userId, "userId");
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }

        boolean has(UserRole role) {
            return roles.contains(role);
        }
    }

    /** Cac truong cua ca chup ma luat can doc. */
    public record StudyFacts(String studyInstanceUid, Long readingUserId, Long approvingUserId) {

        boolean hasImage() {
            return studyInstanceUid != null && !studyInstanceUid.isBlank();
        }
    }

    /** Bao cao hien hanh (null neu ca chua co bao cao). */
    public record ReportFacts(String technique, String conclusion, Long createdByUserId) {
    }

    public static Optional<Violation> check(TransitionCode code, Actor actor, StudyFacts study,
                                            ReportFacts report, String reason) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(study, "study");
        return switch (code) {
            case T2 -> checkT2(actor, study);
            case T3 -> checkT3(actor, study);
            case T4 -> checkT4(actor, study, reason);
            case T5 -> checkT5(actor, study, report);
            case T6 -> checkT6(actor, study, report);
            case T7 -> checkT7(actor, study, report);
            case T8 -> checkT8(actor, study, reason);
            case T9 -> checkT9(actor, reason);
            case T10 -> checkT10(actor, study, reason);
        };
    }

    private static Optional<Violation> checkT2(Actor actor, StudyFacts study) {
        if (!actor.has(UserRole.ADMIN) && !actor.has(UserRole.TECHNICIAN)) {
            return forbidden("Chi TECHNICIAN va ADMIN duoc chuyen SCHEDULED -> UNREAD");
        }
        if (!study.hasImage()) {
            return violation(Kind.UNPROCESSABLE, "STUDY_NO_DICOM_IMAGE",
                    "Chua co study_instance_uid, khong the chuyen sang UNREAD");
        }
        return Optional.empty();
    }

    private static Optional<Violation> checkT3(Actor actor, StudyFacts study) {
        if (!actor.has(UserRole.RADIOLOGIST)) {
            return forbidden("Chi RADIOLOGIST duoc mo ca de doc");
        }
        if (study.readingUserId() != null && !study.readingUserId().equals(actor.userId())) {
            return violation(Kind.CONFLICT, "STUDY_LOCKED_BY_ANOTHER", "Ca chup dang duoc mo doc boi bac si khac");
        }
        return Optional.empty();
    }

    private static Optional<Violation> checkT4(Actor actor, StudyFacts study, String reason) {
        if (!actor.has(UserRole.ADMIN)) {
            if (!actor.has(UserRole.RADIOLOGIST)) {
                return forbidden("Khong co quyen giai phong khoa hoac bo do ca chup");
            }
            if (!actor.userId().equals(study.readingUserId())) {
                return violation(Kind.FORBIDDEN, "REPORT_NOT_HOLDER", "Chi nguoi dang giu ca duoc bo do");
            }
        }
        return requireReason(reason, "T4 (bo do) bat buoc co ly do");
    }

    private static Optional<Violation> checkT5(Actor actor, StudyFacts study, ReportFacts report) {
        if (!actor.has(UserRole.RADIOLOGIST)) {
            return forbidden("Chi RADIOLOGIST duoc chot doc ca chup");
        }
        if (!actor.userId().equals(study.readingUserId())) {
            return violation(Kind.FORBIDDEN, "REPORT_NOT_HOLDER", "Chi nguoi dang giu ca duoc chot doc");
        }
        if (report == null || isBlank(report.technique()) || isBlank(report.conclusion())) {
            return violation(Kind.UNPROCESSABLE, "REPORT_INCOMPLETE",
                    "Ky thuat va ket luan khong duoc de trong khi chot doc");
        }
        return Optional.empty();
    }

    private static Optional<Violation> checkT6(Actor actor, StudyFacts study, ReportFacts report) {
        if (!actor.has(UserRole.APPROVER)) {
            return forbidden("Chi APPROVER duoc mo ca de duyet");
        }
        return requireApproverNotReader(actor, study, report);
    }

    private static Optional<Violation> checkT7(Actor actor, StudyFacts study, ReportFacts report) {
        if (actor.has(UserRole.ADMIN) && !actor.has(UserRole.APPROVER)) {
            return violation(Kind.FORBIDDEN, "ADMIN_CANNOT_APPROVE", "ADMIN khong duoc duyet bao cao");
        }
        if (!actor.has(UserRole.APPROVER)) {
            return forbidden("Chi APPROVER duoc duyet ca chup");
        }
        if (!actor.userId().equals(study.approvingUserId())) {
            return violation(Kind.FORBIDDEN, "NOT_APPROVING_HOLDER", "Chi nguoi da mo duyet duoc duyet ca chup nay");
        }
        Optional<Violation> notReader = requireApproverNotReader(actor, study, report);
        if (notReader.isPresent()) {
            return notReader;
        }
        if (report == null) {
            return violation(Kind.UNPROCESSABLE, "REPORT_INCOMPLETE", "Ca chup chua co bao cao, khong the duyet");
        }
        return Optional.empty();
    }

    /** doc/03 muc 2.4 dieu 5: khi APPROVING, chi nguoi dang giu khoa duyet thao tac duoc tren ca. */
    private static Optional<Violation> checkT8(Actor actor, StudyFacts study, String reason) {
        if (!actor.has(UserRole.APPROVER)) {
            return forbidden("Chi APPROVER duoc tra lai ca chup de sua");
        }
        if (!actor.userId().equals(study.approvingUserId())) {
            return violation(Kind.FORBIDDEN, "NOT_APPROVING_HOLDER", "Chi nguoi da mo duyet duoc tra lai ca chup nay");
        }
        return requireReason(reason, "T8 (tra lai) bat buoc co ly do");
    }

    private static Optional<Violation> checkT9(Actor actor, String reason) {
        if (!actor.has(UserRole.ADMIN) && !actor.has(UserRole.APPROVER)) {
            return forbidden("Chi ADMIN hoac APPROVER duoc mo lai ca da duyet");
        }
        return requireReason(reason, "T9 (mo lai ca da duyet) bat buoc co ly do");
    }

    private static Optional<Violation> checkT10(Actor actor, StudyFacts study, String reason) {
        if (!actor.has(UserRole.ADMIN) && !actor.has(UserRole.TECHNICIAN)) {
            return forbidden("Chi ADMIN va TECHNICIAN duoc huy ca chup");
        }
        if (study.hasImage()) {
            return violation(Kind.UNPROCESSABLE, "CANNOT_CANCEL_STUDY_WITH_IMAGE",
                    "Khong the huy ca chup da co anh trong PACS (study_instance_uid khong null)");
        }
        return requireReason(reason, "T10 (huy ca chup) bat buoc co ly do");
    }

    private static Optional<Violation> requireApproverNotReader(Actor actor, StudyFacts study, ReportFacts report) {
        if (actor.userId().equals(study.readingUserId())
                || (report != null && actor.userId().equals(report.createdByUserId()))) {
            return violation(Kind.FORBIDDEN, "APPROVER_CANNOT_BE_READER", "Nguoi duyet khong duoc la nguoi doc bao cao");
        }
        return Optional.empty();
    }

    private static Optional<Violation> requireReason(String reason, String message) {
        return isBlank(reason)
                ? violation(Kind.UNPROCESSABLE, "TRANSITION_REASON_REQUIRED", message)
                : Optional.empty();
    }

    private static Optional<Violation> forbidden(String message) {
        return violation(Kind.FORBIDDEN, "FORBIDDEN", message);
    }

    private static Optional<Violation> violation(Kind kind, String code, String message) {
        return Optional.of(new Violation(kind, code, message));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
