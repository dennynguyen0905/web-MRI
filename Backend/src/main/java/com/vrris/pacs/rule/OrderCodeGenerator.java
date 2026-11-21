package com.vrris.pacs.rule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Sinh order_code cho imaging_order ("Filler Order Number", ORC-3 - doc/02-erd.md muc 4.9). Chua co
 * quy uoc chinh thuc trong doc/02-erd.md (chi accession_number cua study la co, muc 4.3) - ap dung
 * CUNG kieu quy uoc, suy ra tu vi du "PH20261031001" o schema ImagingOrder trong doc/api/openapi.yaml:
 * tien to "PH" + YYYYMMDD + 3 chu so tang dan trong ngay. ❓ Chua co nguoi xac nhan rieng cho dinh
 * dang nay - neu nhom chot khac thi sua lai o day va o service.impl.ImagingOrderServiceImpl.
 *
 * Thuan Java, khong dung DB (giong rule.AccessionNumberGenerator) - cap so tiep theo trong ngay
 * thuoc ve service (dao.DailySequenceDao, bang daily_sequence V9).
 */
public final class OrderCodeGenerator {

    private static final String PREFIX = "PH";
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.BASIC_ISO_DATE;
    public static final int MAX_SEQUENCE_PER_DAY = 999;

    private OrderCodeGenerator() {
    }

    public static String datePrefix(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return PREFIX + date.format(DATE_PREFIX);
    }

    /**
     * Khoa trong bang daily_sequence (V9) cho ngay nay - dao.DailySequenceDao cap so tiep theo
     * nguyen tu theo khoa nay (khop cach khoi tao trong database/migrations/V9).
     */
    public static String sequenceKey(LocalDate date) {
        return "ORD:" + datePrefix(date);
    }

    public static String format(LocalDate date, int sequenceOfDay) {
        if (sequenceOfDay < 1 || sequenceOfDay > MAX_SEQUENCE_PER_DAY) {
            throw new IllegalArgumentException(
                    "sequenceOfDay phai trong khoang 1.." + MAX_SEQUENCE_PER_DAY + ", nhan duoc " + sequenceOfDay);
        }
        return datePrefix(date) + String.format("%03d", sequenceOfDay);
    }
}
