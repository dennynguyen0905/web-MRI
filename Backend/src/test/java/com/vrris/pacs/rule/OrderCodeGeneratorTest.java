package com.vrris.pacs.rule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCodeGeneratorTest {

    @Test
    void format_ghep_dung_tien_to_PH_ngay_va_3_chu_so() {
        assertThat(OrderCodeGenerator.format(LocalDate.of(2026, 10, 31), 1))
                .isEqualTo("PH20261031001");
    }

    @Test
    void format_nem_loi_khi_sequence_ngoai_khoang() {
        assertThatThrownBy(() -> OrderCodeGenerator.format(LocalDate.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderCodeGenerator.format(LocalDate.now(), 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
