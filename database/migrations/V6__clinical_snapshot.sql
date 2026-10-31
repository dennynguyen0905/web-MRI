-- V6: clinical_snapshot — dữ liệu lâm sàng từ HIS lưu nguyên khối JSON (ERD mục 5), không mô hình hóa thành ~15 bảng.
-- Quan hệ với imaging_order là 1 - 0..1 nên order_id là UNIQUE.

CREATE TABLE clinical_snapshot (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_id    BIGINT      NOT NULL,
    payload     JSON        NOT NULL COMMENT '5 tab lâm sàng',
    fetched_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source      VARCHAR(10) NOT NULL DEFAULT 'SEED' COMMENT 'SEED|HIS',
    PRIMARY KEY (id),
    UNIQUE KEY uk_clinical_snapshot_order (order_id),
    CONSTRAINT fk_clinical_snapshot_order FOREIGN KEY (order_id) REFERENCES imaging_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
