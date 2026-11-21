package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;

/**
 * Cac buoc chuyen trang thai ca chup T2..T10 (doc/03-state-machine.md muc 2.2). T1 (tao ca, trang
 * thai dau SCHEDULED) khong di qua POST /studies/{id}/transitions nen khong co o day - lich su ghi
 * from_status = NULL cho buoc nay.
 *
 * reasonRequired: cac nhanh ngoai le T4, T8, T9, T10 bat buoc co ly do (doc/03 muc 2.4 dieu 6).
 */
public enum TransitionCode {
    T2(StudyStatus.SCHEDULED, StudyStatus.UNREAD, false),
    T3(StudyStatus.UNREAD, StudyStatus.READING, false),
    T4(StudyStatus.READING, StudyStatus.UNREAD, true),
    T5(StudyStatus.READING, StudyStatus.READ, false),
    T6(StudyStatus.READ, StudyStatus.APPROVING, false),
    T7(StudyStatus.APPROVING, StudyStatus.APPROVED, false),
    T8(StudyStatus.APPROVING, StudyStatus.READING, true),
    T9(StudyStatus.APPROVED, StudyStatus.READING, true),
    T10(StudyStatus.SCHEDULED, StudyStatus.CANCELLED, true);

    private final StudyStatus from;
    private final StudyStatus to;
    private final boolean reasonRequired;

    TransitionCode(StudyStatus from, StudyStatus to, boolean reasonRequired) {
        this.from = from;
        this.to = to;
        this.reasonRequired = reasonRequired;
    }

    public StudyStatus from() {
        return from;
    }

    public StudyStatus to() {
        return to;
    }

    /** true voi nhanh ngoai le (T4, T8, T9, T10): bat buoc ly do khong rong. */
    public boolean reasonRequired() {
        return reasonRequired;
    }
}
