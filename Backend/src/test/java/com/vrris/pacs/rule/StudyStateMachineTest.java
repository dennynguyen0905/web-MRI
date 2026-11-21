package com.vrris.pacs.rule;

import com.vrris.pacs.enu.StudyStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vet can ma tran 7 x 7 (from, to): dung 9 cap hop le (T2..T10, doc/03-state-machine.md muc 2.2),
 * 40 cap con lai tra null.
 */
class StudyStateMachineTest {

    private static final Map<String, TransitionCode> EXPECTED = Map.of(
            "SCHEDULED>UNREAD", TransitionCode.T2,
            "UNREAD>READING", TransitionCode.T3,
            "READING>UNREAD", TransitionCode.T4,
            "READING>READ", TransitionCode.T5,
            "READ>APPROVING", TransitionCode.T6,
            "APPROVING>APPROVED", TransitionCode.T7,
            "APPROVING>READING", TransitionCode.T8,
            "APPROVED>READING", TransitionCode.T9,
            "SCHEDULED>CANCELLED", TransitionCode.T10
    );

    static Stream<Arguments> allPairs() {
        return Arrays.stream(StudyStatus.values())
                .flatMap(from -> Arrays.stream(StudyStatus.values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    void ma_tran_7x7_chi_9_cap_hop_le(StudyStatus from, StudyStatus to) {
        TransitionCode expected = EXPECTED.get(from.name() + ">" + to.name());
        assertThat(StudyStateMachine.resolveTransitionCode(from, to)).isEqualTo(expected);
    }

    @Test
    void moi_TransitionCode_khop_dung_cap_from_to_cua_no() {
        for (TransitionCode code : TransitionCode.values()) {
            assertThat(StudyStateMachine.resolveTransitionCode(code.from(), code.to())).isEqualTo(code);
        }
        assertThat(TransitionCode.values()).hasSize(EXPECTED.size());
    }

    @Test
    void tham_so_null_tra_null() {
        assertThat(StudyStateMachine.resolveTransitionCode(null, StudyStatus.UNREAD)).isNull();
        assertThat(StudyStateMachine.resolveTransitionCode(StudyStatus.SCHEDULED, null)).isNull();
    }

    @Test
    void chi_nhanh_ngoai_le_T4_T8_T9_T10_bat_buoc_ly_do() {
        assertThat(Arrays.stream(TransitionCode.values()).filter(TransitionCode::reasonRequired))
                .containsExactlyInAnyOrder(TransitionCode.T4, TransitionCode.T8, TransitionCode.T9, TransitionCode.T10);
    }

    @ParameterizedTest
    @EnumSource(StudyStatus.class)
    void transitionsFrom_khop_ma_tran(StudyStatus from) {
        assertThat(StudyStateMachine.transitionsFrom(from))
                .allSatisfy(code -> assertThat(code.from()).isEqualTo(from))
                .hasSize((int) EXPECTED.keySet().stream().filter(k -> k.startsWith(from.name() + ">")).count());
    }

    @Test
    void CANCELLED_la_trang_thai_cuoi() {
        assertThat(StudyStateMachine.transitionsFrom(StudyStatus.CANCELLED)).isEmpty();
    }
}
