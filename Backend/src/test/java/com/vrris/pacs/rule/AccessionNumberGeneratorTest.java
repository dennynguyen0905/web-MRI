package com.vrris.pacs.rule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessionNumberGeneratorTest {

    @Test
    void format_ghep_dung_ngay_va_5_chu_so() {
        assertThat(AccessionNumberGenerator.format(LocalDate.of(2026, 10, 31), 1))
                .isEqualTo("2026103100001");
        assertThat(AccessionNumberGenerator.format(LocalDate.of(2026, 10, 31), 42))
                .isEqualTo("2026103100042");
    }

    @Test
    void datePrefix_dung_dinh_dang_YYYYMMDD() {
        assertThat(AccessionNumberGenerator.datePrefix(LocalDate.of(2026, 1, 5))).isEqualTo("20260105");
    }

    @Test
    void format_nem_loi_khi_sequence_ngoai_khoang() {
        assertThatThrownBy(() -> AccessionNumberGenerator.format(LocalDate.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessionNumberGenerator.format(LocalDate.now(), 100_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
