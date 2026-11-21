package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;

import java.util.Arrays;
import java.util.List;

/**
 * State machine ca chup (doc/03-state-machine.md muc 2.2, Frontend/src/shared/lib/stateMachine.ts).
 * Nguon duy nhat cho "cap (from, to) nao hop le" la enum TransitionCode; lop nay chi tra cuu.
 * "Ai duoc lam va can dieu kien gi" nam o rule.TransitionPolicy.
 *
 * R-05: rule/ chi phu thuoc enu va JDK.
 */
public final class StudyStateMachine {

    private StudyStateMachine() {
    }

    /** Ma buoc chuyen cho cap (from, to); null neu cap nay khong hop le (hoac tham so null). */
    public static TransitionCode resolveTransitionCode(StudyStatus from, StudyStatus to) {
        if (from == null || to == null) {
            return null;
        }
        for (TransitionCode code : TransitionCode.values()) {
            if (code.from() == from && code.to() == to) {
                return code;
            }
        }
        return null;
    }

    /** Cac buoc chuyen co the di tu mot trang thai (rong voi CANCELLED - trang thai cuoi). */
    public static List<TransitionCode> transitionsFrom(StudyStatus from) {
        return Arrays.stream(TransitionCode.values())
                .filter(code -> code.from() == from)
                .toList();
    }
}
