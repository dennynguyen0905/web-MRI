package com.vrris.pacs.rule;

import com.vrris.pacs.enu.DobPrecision;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sinh file CSV (UTF-8 kem BOM) cho chuc nang "Tai danh sach" benh nhan va ca chup (IN-25).
 *
 * Thuan Java (luat ArchUnit L5: rule/ chi phu thuoc rule, enu va JDK):
 * - Them UTF-8 BOM (\uFEFF) o dau file de Microsoft Excel tren Windows mo tieng Viet khong loi dau.
 * - Cot Tuoi luon tinh qua AgeCalculator: "~68" khi dob_precision != DAY, "—" khi chua co ngay sinh,
 *   tuyet doi khong bao gio tra "0" vi thieu du lieu (doc/01-scope.md muc 6 buoc 10).
 * - Escape theo RFC 4180 va chan CSV Formula Injection khi chuoi bat dau bang '=', '+', '-', '@'.
 */
public final class PatientListCsvExporter {

    public static final String UTF8_BOM = "\uFEFF";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PatientListCsvExporter() {
    }

    public record PatientRow(
            String patientCode,
            String hisPatientId,
            String fullName,
            LocalDate dateOfBirth,
            DobPrecision dobPrecision,
            String gender,
            String socialInsuranceNo,
            String phone,
            String address,
            LocalDateTime createdAt
    ) {
    }

    public record StudyWorklistRow(
            String accessionNumber,
            String status,
            String orderCode,
            String visitCode,
            String patientClass,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            DobPrecision dobPrecision,
            String gender,
            String socialInsuranceNo,
            String serviceName,
            String modality,
            String bodyPart,
            String deviceName,
            String orderingDoctorName,
            String clinicalDiagnosis,
            String conclusion,
            LocalDateTime scheduledAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
    }

    public static String patientListFileName(LocalDate exportedOn) {
        return "danh-sach-benh-nhan_%s.csv".formatted(FILE_DATE.format(exportedOn));
    }

    public static String studyListFileName(LocalDate exportedOn) {
        return "danh-sach-ca-chup_%s.csv".formatted(FILE_DATE.format(exportedOn));
    }

    public static byte[] exportPatients(List<PatientRow> rows, LocalDate asOfDate) {
        StringBuilder sb = new StringBuilder(UTF8_BOM);
        appendRow(sb, List.of(
                "STT",
                "Mã bệnh nhân",
                "Mã HIS",
                "Họ tên",
                "Ngày sinh",
                "Độ chính xác NS",
                "Tuổi",
                "Giới tính",
                "Số BHXH",
                "Điện thoại",
                "Địa chỉ",
                "Ngày tạo"
        ));

        for (int i = 0; i < rows.size(); i++) {
            PatientRow r = rows.get(i);
            String ageText = formatAge(r.dateOfBirth(), r.dobPrecision(), asOfDate);
            appendRow(sb, List.of(
                    String.valueOf(i + 1),
                    orEmpty(r.patientCode()),
                    orEmpty(r.hisPatientId()),
                    orEmpty(r.fullName()),
                    formatDob(r.dateOfBirth(), r.dobPrecision()),
                    r.dobPrecision() == null ? "DAY" : r.dobPrecision().name(),
                    ageText,
                    orEmpty(r.gender()),
                    orEmpty(r.socialInsuranceNo()),
                    orEmpty(r.phone()),
                    orEmpty(r.address()),
                    formatDateTime(r.createdAt())
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] exportStudies(List<StudyWorklistRow> rows, LocalDate asOfDate) {
        StringBuilder sb = new StringBuilder(UTF8_BOM);
        appendRow(sb, List.of(
                "STT",
                "Mã hồ sơ",
                "Trạng thái",
                "Mã phiếu",
                "Mã LSVV",
                "Loại BN",
                "Mã bệnh nhân",
                "Họ tên",
                "Ngày sinh",
                "Tuổi",
                "Giới tính",
                "Số BHXH",
                "Dịch vụ chỉ định",
                "Modality",
                "Bộ phận chụp",
                "Máy chụp",
                "Bác sĩ chỉ định",
                "Chẩn đoán lâm sàng",
                "Kết luận",
                "Thời gian lên lịch",
                "Thời gian bắt đầu",
                "Thời gian hoàn thành"
        ));

        for (int i = 0; i < rows.size(); i++) {
            StudyWorklistRow r = rows.get(i);
            String ageText = formatAge(r.dateOfBirth(), r.dobPrecision(), asOfDate);
            appendRow(sb, List.of(
                    String.valueOf(i + 1),
                    orEmpty(r.accessionNumber()),
                    orEmpty(r.status()),
                    orEmpty(r.orderCode()),
                    orEmpty(r.visitCode()),
                    orEmpty(r.patientClass()),
                    orEmpty(r.patientCode()),
                    orEmpty(r.fullName()),
                    formatDob(r.dateOfBirth(), r.dobPrecision()),
                    ageText,
                    orEmpty(r.gender()),
                    orEmpty(r.socialInsuranceNo()),
                    orEmpty(r.serviceName()),
                    orEmpty(r.modality()),
                    orEmpty(r.bodyPart()),
                    orEmpty(r.deviceName()),
                    orEmpty(r.orderingDoctorName()),
                    orEmpty(r.clinicalDiagnosis()),
                    orEmpty(r.conclusion()),
                    formatDateTime(r.scheduledAt()),
                    formatDateTime(r.startedAt()),
                    formatDateTime(r.completedAt())
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String formatAge(LocalDate dob, DobPrecision precision, LocalDate asOfDate) {
        if (dob == null) {
            return "—";
        }
        DobPrecision safePrecision = precision == null ? DobPrecision.DAY : precision;
        return AgeCalculator.calculate(dob, safePrecision, asOfDate).format();
    }

    static String formatDob(LocalDate dob, DobPrecision precision) {
        if (dob == null) {
            return "";
        }
        DobPrecision safePrecision = precision == null ? DobPrecision.DAY : precision;
        return switch (safePrecision) {
            case YEAR -> String.valueOf(dob.getYear());
            case MONTH -> "%02d/%04d".formatted(dob.getMonthValue(), dob.getYear());
            case DAY -> dob.toString();
        };
    }

    static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "" : DATE_TIME_FMT.format(dt);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsvCell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    static String escapeCsvCell(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String value = raw;
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            value = "'" + value;
        }
        boolean needsQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

