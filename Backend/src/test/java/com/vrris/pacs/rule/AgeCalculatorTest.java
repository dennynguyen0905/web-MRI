package com.vrris.pacs.rule;

import com.vrris.pacs.enu.DobPrecision;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Thuan JUnit 5, khong can Spring context - rule/ khong phu thuoc framework (R-05). */
class AgeCalculatorTest {

    @Test
    void chinh_xac_khi_biet_ngay_sinh_day_du() {
        AgeCalculator.Result result = AgeCalculator.calculate(
                LocalDate.of(1958, 3, 15), DobPrecision.DAY, LocalDate.of(2026, 11, 5));

        assertThat(result.years()).isEqualTo(68);
        assertThat(result.approximate()).isFalse();
        assertThat(result.format()).isEqualTo("68");
    }

    @Test
    void xap_xi_khi_chi_biet_nam_sinh() {
        // doc/02-erd.md muc 4.2: dob_precision = YEAR -> date_of_birth luu YYYY-01-01, tuoi xap xi.
        AgeCalculator.Result result = AgeCalculator.calculate(
                LocalDate.of(1958, 1, 1), DobPrecision.YEAR, LocalDate.of(2026, 11, 5));

        assertThat(result.approximate()).isTrue();
        assertThat(result.format()).isEqualTo("~" + result.years());
    }

    @Test
    void xap_xi_khi_chi_biet_thang_sinh_suy_luan_tu_quy_tac_nam() {
        // Suy luan (chua co nguoi xac nhan rieng cho MONTH) - xem Javadoc AgeCalculator.
        AgeCalculator.Result result = AgeCalculator.calculate(
                LocalDate.of(1958, 3, 1), DobPrecision.MONTH, LocalDate.of(2026, 11, 5));

        assertThat(result.approximate()).isTrue();
    }

    @Test
    void khong_cho_asOf_truoc_ngay_sinh() {
        assertThatThrownBy(() -> AgeCalculator.calculate(
                LocalDate.of(2026, 1, 1), DobPrecision.DAY, LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
