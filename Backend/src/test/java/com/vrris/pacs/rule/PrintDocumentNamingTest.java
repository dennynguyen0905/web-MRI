package com.vrris.pacs.rule;

import com.vrris.pacs.enu.PrintSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thuan Java, khong can Spring hay Docker - chay duoc ca khi may dev chua co Testcontainers.
 */
class PrintDocumentNamingTest {

    @Test
    void ten_file_gom_ma_benh_nhan_ma_ho_so_phan_in_va_ngay() {
        String name = PrintDocumentNaming.fileName(
                "TEST0001", "2026102900001", PrintSection.FUNCTIONAL, LocalDate.of(2026, 11, 6));

        assertThat(name).isEqualTo("phieu_TEST0001_2026102900001_chan-doan-chuc-nang_20261106.pdf");
    }

    @ParameterizedTest
    @EnumSource(PrintSection.class)
    void moi_phan_in_co_slug_va_tieu_de_rieng(PrintSection section) {
        assertThat(PrintDocumentNaming.slug(section)).isNotBlank();
        assertThat(PrintDocumentNaming.title(section)).isNotBlank();
    }

    @Test
    void hai_phan_in_khac_nhau_cho_ra_hai_ten_file_khac_nhau() {
        LocalDate day = LocalDate.of(2026, 11, 6);
        String a = PrintDocumentNaming.fileName("TEST0001", "2026102900001", PrintSection.LABS, day);
        String b = PrintDocumentNaming.fileName("TEST0001", "2026102900001", PrintSection.DRUGS, day);

        // Moi phan la mot file rieng - tai ca hai cung luc khong duoc de len nhau.
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void ky_tu_khong_an_toan_trong_ten_file_bi_thay_bang_gach_ngang() {
        assertThat(PrintDocumentNaming.sanitize("TEST/0001 A")).isEqualTo("TEST-0001-A");
        assertThat(PrintDocumentNaming.sanitize("  ")).isEqualTo("khong-ro");
        assertThat(PrintDocumentNaming.sanitize(null)).isEqualTo("khong-ro");
    }
}
