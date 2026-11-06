package com.vrris.pacs.rule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Sinh accession_number cho study, theo quy uoc doc/02-erd.md muc 4.3: YYYYMMDD + 5 chu so tang dan
 * trong ngay (vi du "2026103100001"). Day la khoa noi RIS - DICOM (0008,0050), sinh ngay khi tao
 * study, ke ca khi chua co anh.
 *
 * Lop nay chi format chuoi (thuan Java, khong dung DB) - viec dem "da co bao nhieu study trong ngay
 * hom nay" (de biet sequenceOfDay tiep theo) thuoc ve service (can query StudyDao), khong thuoc ve
 * day (ArchUnit L5: rule/ khong duoc phu thuoc dao/model/service).
 */
public final class AccessionNumberGenerator {

    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_SEQUENCE_PER_DAY = 99_999;

    private AccessionNumberGenerator() {
    }

    /** Tien to ngay (YYYYMMDD) dung de dem so study da sinh trong ngay do (StudyDao.countByAccessionNumberStartingWith). */
    public static String datePrefix(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return date.format(DATE_PREFIX);
    }

    public static String format(LocalDate date, int sequenceOfDay) {
        if (sequenceOfDay < 1 || sequenceOfDay > MAX_SEQUENCE_PER_DAY) {
            throw new IllegalArgumentException(
                    "sequenceOfDay phai trong khoang 1.." + MAX_SEQUENCE_PER_DAY + ", nhan duoc " + sequenceOfDay);
        }
        return datePrefix(date) + String.format("%05d", sequenceOfDay);
    }
}
