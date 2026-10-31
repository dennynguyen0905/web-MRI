-- V4: luồng việc — study_status_history, report.
-- study_status_history là append-only (ERD mục 2): backend chỉ INSERT, không UPDATE/DELETE.
-- report có version (ERD 4.10): mỗi version là một bản ghi riêng, UK(study_id, version).

CREATE TABLE study_status_history (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    study_id            BIGINT       NOT NULL,
    from_status         VARCHAR(20)  NULL COMMENT 'NULL khi khởi tạo',
    to_status           VARCHAR(20)  NOT NULL,
    changed_by_user_id  BIGINT       NOT NULL,
    changed_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason              VARCHAR(500) NULL,
    note                TEXT         NULL,
    PRIMARY KEY (id),
    KEY idx_status_history_study_changed (study_id, changed_at),
    KEY idx_status_history_changed_by (changed_by_user_id),
    CONSTRAINT fk_status_history_study FOREIGN KEY (study_id) REFERENCES study (id),
    CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE report (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    study_id             BIGINT      NOT NULL,
    version              INT         NOT NULL DEFAULT 1 COMMENT 'tăng khi trả lại rồi chốt lại, hoặc mở lại ca đã duyệt',
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|FINAL|APPROVED|RETURNED',
    origin               VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL|AI_DRAFT|AI_DRAFT_EDITED',
    technique            TEXT        NULL COMMENT 'kỹ thuật',
    findings             TEXT        NULL COMMENT 'mô tả',
    diagnosis            TEXT        NULL COMMENT 'chẩn đoán',
    conclusion           TEXT        NULL COMMENT 'kết luận',
    recommendation       TEXT        NULL COMMENT 'khuyến nghị',
    created_by_user_id   BIGINT      NOT NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by_user_id  BIGINT      NULL,
    approved_at          DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_study_version (study_id, version),
    KEY idx_report_created_by (created_by_user_id),
    KEY idx_report_approved_by (approved_by_user_id),
    CONSTRAINT fk_report_study FOREIGN KEY (study_id) REFERENCES study (id),
    CONSTRAINT fk_report_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_report_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
