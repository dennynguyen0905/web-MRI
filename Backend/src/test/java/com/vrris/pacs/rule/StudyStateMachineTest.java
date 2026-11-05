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
}
