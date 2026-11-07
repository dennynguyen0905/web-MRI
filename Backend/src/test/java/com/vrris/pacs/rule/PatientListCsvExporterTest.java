package com.vrris.pacs.rule;

import com.vrris.pacs.enu.DobPrecision;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientListCsvExporterTest {

    @Test
    void dat_ten_file_theo_ngay_xuat() {
        LocalDate date = LocalDate.of(2026, 11, 7);
        assertThat(PatientListCsvExporter.patientListFileName(date))
                .isEqualTo("danh-sach-benh-nhan_20261107.csv");
        assertThat(PatientListCsvExporter.studyListFileName(date))
                .isEqualTo("danh-sach-ca-chup_20261107.csv");
    }

    @Test
    void xuat_benh_nhan_co_utf8_bom_va_tinh_tuoi_dung_quy_tac_TC09() {
        LocalDate asOf = LocalDate.of(2026, 11, 7);
        List<PatientListCsvExporter.PatientRow> rows = List.of(
                new PatientListCsvExporter.PatientRow(
                        "TEST0001", "HIS0001", "Nguyễn Văn Giả A",
                        LocalDate.of(1958, 1, 1), DobPrecision.YEAR, "M",
                        "GIA0000000001", "0900000001", "Quận 1, TP.HCM",
                        LocalDateTime.of(2026, 10, 31, 8, 30)),
                new PatientListCsvExporter.PatientRow(
                        "TEST0002", null, "Lê Thị Giả B",
                        null, DobPrecision.YEAR, "F",
                        null, null, null,
                        LocalDateTime.of(2026, 10, 31, 9, 0))
        );

        byte[] bytes = PatientListCsvExporter.exportPatients(rows, asOf);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(PatientListCsvExporter.UTF8_BOM + "STT,Mã bệnh nhân,Mã HIS,Họ tên,");
        // Benh nhan chi co nam sinh -> tuoi xap xi ~68, ngay sinh hien thi 1958
        assertThat(csv).contains("1,TEST0001,HIS0001,Nguyễn Văn Giả A,1958,YEAR,~68,M,GIA0000000001,0900000001,\"Quận 1, TP.HCM\",2026-10-31 08:30");
        // Benh nhan chua co ngay sinh -> tuoi la "—", KHONG bao gio la "0"
        assertThat(csv).contains("2,TEST0002,,Lê Thị Giả B,,YEAR,—,F,,,,2026-10-31 09:00");
    }

    @Test
    void escape_dau_phay_ngoac_kep_va_chan_csv_formula_injection() {
        assertThat(PatientListCsvExporter.escapeCsvCell("Bình thường")).isEqualTo("Bình thường");
        assertThat(PatientListCsvExporter.escapeCsvCell("Khoa CĐHA, Phòng 2"))
                .isEqualTo("\"Khoa CĐHA, Phòng 2\"");
        assertThat(PatientListCsvExporter.escapeCsvCell("Ghi chú \"khẩn\""))
                .isEqualTo("\"Ghi chú \"\"khẩn\"\"\"");
        assertThat(PatientListCsvExporter.escapeCsvCell("=CMD|' /C calc'!A0"))
                .isEqualTo("'=CMD|' /C calc'!A0");
        assertThat(PatientListCsvExporter.escapeCsvCell("+1+1,test"))
                .isEqualTo("\"'+1+1,test\"");
    }

    @Test
    void xuat_danh_sach_ca_chup_worklist_day_du_cot() {
        LocalDate asOf = LocalDate.of(2026, 11, 7);
        List<PatientListCsvExporter.StudyWorklistRow> rows = List.of(
                new PatientListCsvExporter.StudyWorklistRow(
                        "2026103100001", "APPROVED", "PH20261031001", "LSVV001", "OUTPATIENT",
                        "TEST0001", "Nguyễn Văn Giả A", LocalDate.of(1990, 5, 10), DobPrecision.DAY,
                        "M", "GIA0000000001", "Chụp cộng hưởng từ sọ não", "MR", "Sọ não",
                        "MRI 1.5T Phòng 1", "BS. Chỉ Định A (giả)", "Đau đầu kéo dài",
                        "Chưa phát hiện bất thường (dữ liệu giả).",
                        LocalDateTime.of(2026, 10, 31, 8, 0),
                        LocalDateTime.of(2026, 10, 31, 8, 15),
                        LocalDateTime.of(2026, 10, 31, 8, 45))
        );

        String csv = new String(PatientListCsvExporter.exportStudies(rows, asOf), StandardCharsets.UTF_8);
        assertThat(csv).startsWith(PatientListCsvExporter.UTF8_BOM + "STT,Mã hồ sơ,Trạng thái,");
        assertThat(csv).contains("2026103100001,APPROVED,PH20261031001,LSVV001,OUTPATIENT,TEST0001,Nguyễn Văn Giả A,1990-05-10,36,M");
        assertThat(csv).contains("Chụp cộng hưởng từ sọ não,MR,Sọ não,MRI 1.5T Phòng 1");
    }
}
