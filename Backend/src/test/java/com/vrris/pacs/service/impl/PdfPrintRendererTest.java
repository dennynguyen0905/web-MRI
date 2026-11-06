package com.vrris.pacs.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiem tra bo dung PDF ma khong can Spring hay Docker.
 *
 * Can mot font TTF Unicode tren may chay test (xem PdfPrintRenderer). Khong co thi bo qua test
 * thay vi bao do - may dev Windows co segoeui.ttf, image/CI Linux co DejaVu sau khi cai goi font.
 */
class PdfPrintRendererTest {

    private static final List<String> FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf");

    private static String availableFont() {
        return FONT_CANDIDATES.stream().filter(p -> Files.isReadable(Path.of(p))).findFirst().orElse(null);
    }

    @Test
    void dung_duoc_mot_file_pdf_hop_le() {
        String font = availableFont();
        assumeTrue(font != null, "May chay test chua co font TTF Unicode - bo qua");

        PdfPrintRenderer renderer = new PdfPrintRenderer(font);

        byte[] pdf = renderer.render(
                "Ket qua chan doan chuc nang chinh",
                List.of(new PdfPrintRenderer.Line("Ho ten", "Nguyen Van Gia A"),
                        new PdfPrintRenderer.Line("Ma benh nhan", "TEST0001")),
                List.of(new PdfPrintRenderer.Line("Ket luan", "Chua phat hien bat thuong.")),
                null,
                "Chua co du lieu.",
                "Nguoi in: ktv");

        assertThat(pdf).isNotEmpty();
        // Chu ky cua file PDF.
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void khong_co_du_lieu_thi_van_ra_file_kem_ghi_chu() {
        String font = availableFont();
        assumeTrue(font != null, "May chay test chua co font TTF Unicode - bo qua");

        PdfPrintRenderer renderer = new PdfPrintRenderer(font);

        byte[] pdf = renderer.render(
                "Don thuoc/vat tu trong phieu kham",
                List.of(new PdfPrintRenderer.Line("Ho ten", "Nguyen Van Gia A")),
                List.of(),
                new PdfPrintRenderer.Table(List.of("Ten thuoc"), List.of()),
                "Chua co du lieu — cho tich hop HIS.",
                "Nguoi in: ktv");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void khong_tim_thay_font_thi_bao_loi_ro_rang_thay_vi_xuat_pdf_mat_dau() {
        PdfPrintRenderer renderer = new PdfPrintRenderer("/khong/ton/tai/font.ttf");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> renderer.render(
                        "Ket qua", List.of(), List.of(), null, "", ""))
                .isInstanceOf(com.vrris.pacs.exception.ApiException.class)
                .hasMessageContaining("font");
    }
}
