-- V9: khóa lạc quan + bộ sinh số tuần tự theo ngày (review 2026-11-21).
--
-- 1) Cột row_version cho study / report / imaging_order: Hibernate @Version dùng cột này để hai
--    yêu cầu đồng thời (ví dụ hai bác sĩ cùng mở đọc một ca - T3, doc/03-state-machine.md mục 2.4
--    điều 5) không cùng thành công. Đặt tên row_version (KHÔNG phải "version") vì report.version
--    đã là số phiên bản báo cáo (doc/02-erd.md mục 4.10).
--
-- 2) Bảng daily_sequence: sinh accession_number (YYYYMMDD + 5 chữ số, doc/02-erd.md mục 4.3) và
--    order_code (PH + YYYYMMDD + 3 chữ số) NGUYÊN TỬ bằng
--    INSERT ... ON DUPLICATE KEY UPDATE seq_value = LAST_INSERT_ID(seq_value + 1),
--    thay cho cách cũ "đếm số bản ghi trong ngày + 1" (hai request đồng thời sinh trùng số).
--    seq_key: 'ACC:<YYYYMMDD>' cho accession_number, 'ORD:PH<YYYYMMDD>' cho order_code.
--    Cột tên seq_value (LAST_VALUE là từ khóa dành riêng của MySQL 8).
--    Khởi tạo từ dữ liệu đang có để không sinh lại số đã dùng (seed V7/V8 hoặc dữ liệu cũ).

ALTER TABLE study         ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0 COMMENT 'khóa lạc quan (Hibernate @Version)';
ALTER TABLE report        ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0 COMMENT 'khóa lạc quan (Hibernate @Version)';
ALTER TABLE imaging_order ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0 COMMENT 'khóa lạc quan (Hibernate @Version)';

CREATE TABLE daily_sequence (
    seq_key    VARCHAR(32) NOT NULL COMMENT 'ACC:<YYYYMMDD> | ORD:PH<YYYYMMDD>',
    seq_value  INT         NOT NULL COMMENT 'số cuối cùng đã cấp trong ngày',
    PRIMARY KEY (seq_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Bảng dẫn xuất để khớp sql_mode=only_full_group_by (mặc định MySQL 8).
INSERT INTO daily_sequence (seq_key, seq_value)
SELECT t.seq_key, MAX(t.seq_value)
FROM (SELECT CONCAT('ACC:', LEFT(accession_number, 8))          AS seq_key,
             CAST(SUBSTRING(accession_number, 9) AS UNSIGNED)  AS seq_value
      FROM study
      WHERE accession_number REGEXP '^[0-9]{13}$') t
GROUP BY t.seq_key;

INSERT INTO daily_sequence (seq_key, seq_value)
SELECT t.seq_key, MAX(t.seq_value)
FROM (SELECT CONCAT('ORD:', LEFT(order_code, 10))         AS seq_key,
             CAST(SUBSTRING(order_code, 11) AS UNSIGNED)  AS seq_value
      FROM imaging_order
      WHERE order_code REGEXP '^PH[0-9]{11}$') t
GROUP BY t.seq_key;
