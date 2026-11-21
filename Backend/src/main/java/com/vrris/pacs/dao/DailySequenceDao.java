package com.vrris.pacs.dao;

/**
 * Bo dem tuan tu theo khoa (bang daily_sequence, V9) - dung de sinh accession_number va order_code
 * NGUYEN TU, thay cach "dem so ban ghi trong ngay + 1" (hai request dong thoi sinh trung so).
 *
 * Phai goi trong mot transaction dang mo (service.impl @Transactional): hai cau SQL ben duoi dung
 * cung connection.
 */
public interface DailySequenceDao {

    /** Tang va tra ve gia tri tiep theo cua khoa (bat dau tu 1 neu khoa chua co). */
    int next(String sequenceKey);
}
