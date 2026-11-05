package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;

import java.util.Map;
import java.util.Set;

/**
 * Luong chinh (thang hang) theo README Phan 4 - CHUA dua cac mui ten (?) vao (bo do khi dang doc,
 * tra lai de sua khi dang duyet, mo lai khi da duyet xong, huy chi dinh) vi chua duoc nhom xac
 * nhan (README Phan 9 cau 1, doc/03-state-machine.md). Khi co cau tra loi, cap nhat LINEAR_FLOW
 * (hoac them cau truc rieng cho chuyen lui) va StudyStateMachineTest tuong ung.
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
}
