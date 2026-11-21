package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chi kiem luong chinh (thang hang) - cac mui ten (?) chua xac nhan la KHONG hop le o ban nay. */
class StudyStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, UNREAD",
            "UNREAD, READING",
            "READING, READ",
            "READ, APPROVING",
            "APPROVING, APPROVED"
    })
    void cho_phep_chuyen_dung_luong_chinh(StudyStatus from, StudyStatus to) {
        assertThat(StudyStateMachine.canTransition(from, to)).isTrue();
        assertThat(StudyStateMachine.transition(from, to)).isEqualTo(to);
    }

    @Test
    void khong_cho_bo_qua_buoc() {
        assertThat(StudyStateMachine.canTransition(StudyStatus.SCHEDULED, StudyStatus.READING)).isFalse();
        assertThatThrownBy(() -> StudyStateMachine.transition(StudyStatus.SCHEDULED, StudyStatus.READING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void khong_cho_chuyen_lui_vi_mui_ten_do_chua_xac_nhan() {
        // DANG_DOC -> CHUA_DOC ("bo do (?)") - README Phan 4, chua duoc nhom xac nhan.
        assertThat(StudyStateMachine.canTransition(StudyStatus.READING, StudyStatus.UNREAD)).isFalse();
    }

    @Test
    void khong_co_buoc_tiep_theo_sau_APPROVED() {
        assertThat(StudyStateMachine.allowedNextStates(StudyStatus.APPROVED)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, UNREAD, T2, false",
            "UNREAD, READING, T3, false",
            "READING, UNREAD, T4, true",
            "READING, READ, T5, false",
            "READ, APPROVING, T6, false",
            "APPROVING, APPROVED, T7, false",
            "APPROVING, READING, T8, true",
            "APPROVED, READING, T9, true",
            "SCHEDULED, CANCELLED, T10, true"
    })
    void resolveTransitionCode_va_isExceptional_khop_T2_den_T10(
            StudyStatus from, StudyStatus to, String expectedCode, boolean expectedExceptional) {
        assertThat(StudyStateMachine.resolveTransitionCode(from, to)).isEqualTo(expectedCode);
        assertThat(StudyStateMachine.isExceptionalTransition(from, to)).isEqualTo(expectedExceptional);
    }
}
