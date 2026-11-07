package com.vrris.pacs.rule;

import com.vrris.pacs.enu.PrintSection;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dat ten file PDF cua phieu in. Thuan Java (luat ArchUnit L5: rule/ chi phu thuoc enu + JDK).
 *
 * Moi phan in la MOT file rieng (yeu cau cua Task 5), nen ten file phai mang ca ma benh nhan,
 * ma ho so va phan in de nguoi dung khong nham khi tai nhieu file cung luc.
 */
public final class PrintDocumentNaming {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private PrintDocumentNaming() {
    }

    public static String slug(PrintSection section) {
        return switch (section) {
            case OVERVIEW -> "tong-quan-kham";
            case LABS -> "xet-nghiem";
            case FUNCTIONAL -> "chan-doan-chuc-nang";
            case DRUGS -> "don-thuoc-vat-tu";
            case SURGERY -> "phau-thuat-thu-thuat";
        };
    }

    public static String title(PrintSection section) {
        return switch (section) {
            case OVERVIEW -> "Ket qua tong quan kham";
            case LABS -> "Ket qua xet nghiem";
            case FUNCTIONAL -> "Ket qua chan doan chuc nang chinh";
            case DRUGS -> "Don thuoc/vat tu trong phieu kham";
            case SURGERY -> "Du lieu phau thuat thu thuat";
        };
    }

    /** Vi du: phieu_TEST0001_2026102900001_chan-doan-chuc-nang_20261106.pdf */
    public static String fileName(String patientCode, String accessionNumber, PrintSection section, LocalDate printedOn) {
        return "phieu_%s_%s_%s_%s.pdf".formatted(
                sanitize(patientCode), sanitize(accessionNumber), slug(section), DATE.format(printedOn));
    }

    /** Bo moi ky tu khong an toan cho ten file; giu chu, so, gach ngang, gach duoi. */
    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "khong-ro";
        }
        String cleaned = raw.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        return cleaned.isBlank() ? "khong-ro" : cleaned;
    }
}
