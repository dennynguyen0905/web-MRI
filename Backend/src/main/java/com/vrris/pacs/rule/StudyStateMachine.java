package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;

import java.util.Map;
import java.util.Set;

/**
 * State machine ca chup (doc/03-state-machine.md, Frontend/src/shared/lib/stateMachine.ts).
 * R-05: rule/ chi phu thuoc enu va JDK.
 */
public final class StudyStateMachine {

    private static final Map<StudyStatus, StudyStatus> LINEAR_FLOW = Map.of(
            StudyStatus.SCHEDULED, StudyStatus.UNREAD,
            StudyStatus.UNREAD, StudyStatus.READING,
            StudyStatus.READING, StudyStatus.READ,
            StudyStatus.READ, StudyStatus.APPROVING,
            StudyStatus.APPROVING, StudyStatus.APPROVED
    );

    private StudyStateMachine() {
    }

    public static boolean canTransition(StudyStatus from, StudyStatus to) {
        return to.equals(LINEAR_FLOW.get(from));
    }

    /**
     * @throws IllegalStateException neu chuyen khong hop le - rule/ khong duoc phu thuoc
     *         exception.ApiException (R-05: rule chi phu thuoc enu va JDK), nen service.impl la
     *         noi bat IllegalStateException nay va chuyen thanh ApiException(409, ...).
     */
    public static StudyStatus transition(StudyStatus from, StudyStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                    "Khong cho phep chuyen " + from + " -> " + to + " (chua xac nhan hoac khong hop le)");
        }
        return to;
    }

    public static Set<StudyStatus> allowedNextStates(StudyStatus from) {
        StudyStatus next = LINEAR_FLOW.get(from);
        return next == null ? Set.of() : Set.of(next);
    }

    /**
     * Tra ma chuyen trang thai (T2..T10) cho moi buoc hop le trong vong doi ca chup
     * (bao gom luong chinh T2, T3, T5, T6, T7 va cac nhanh ngoai le T4, T8, T9, T10).
     * Tra null neu cap (from, to) khong hop le.
     */
    public static String resolveTransitionCode(StudyStatus from, StudyStatus to) {
        if (from == null || to == null) {
            return null;
        }
        return switch (from) {
            case SCHEDULED -> switch (to) {
                case UNREAD -> "T2";
                case CANCELLED -> "T10";
                default -> null;
            };
            case UNREAD -> to == StudyStatus.READING ? "T3" : null;
            case READING -> switch (to) {
                case UNREAD -> "T4";
                case READ -> "T5";
                default -> null;
            };
            case READ -> to == StudyStatus.APPROVING ? "T6" : null;
            case APPROVING -> switch (to) {
                case APPROVED -> "T7";
                case READING -> "T8";
                default -> null;
            };
            case APPROVED -> to == StudyStatus.READING ? "T9" : null;
            case CANCELLED -> null;
        };
    }

    public static boolean isExceptionalTransition(StudyStatus from, StudyStatus to) {
        String code = resolveTransitionCode(from, to);
        return "T4".equals(code) || "T8".equals(code) || "T9".equals(code) || "T10".equals(code);
    }
}
