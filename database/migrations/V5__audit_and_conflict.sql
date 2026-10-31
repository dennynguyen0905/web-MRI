-- V5: audit_log, identity_conflict.
-- audit_log là append-only (không UPDATE/DELETE). actor_username là snapshot (ERD 4.8), patient_id không có
-- khóa ngoại để bản ghi audit còn nguyên kể cả khi dữ liệu liên quan thay đổi.

CREATE TABLE audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id   BIGINT       NULL COMMENT 'NULL khi do hệ thống thực hiện',
    actor_username  VARCHAR(100) NOT NULL COMMENT 'snapshot, không join',
    action          VARCHAR(20)  NOT NULL COMMENT 'READ|CREATE|UPDATE|DELETE|STATUS_CHANGE|LOGIN|PRINT|EXPORT',
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       BIGINT       NULL,
    patient_id      BIGINT       NULL COMMENT 'truy vết theo bệnh nhân',
    before_value    JSON         NULL,
    after_value     JSON         NULL,
    ip_address      VARCHAR(45)  NULL,
    occurred_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_patient_occurred (patient_id, occurred_at),
    KEY idx_audit_actor_occurred (actor_user_id, occurred_at),
    KEY idx_audit_entity (entity_type, entity_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE identity_conflict (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    patient_id           BIGINT       NULL COMMENT 'NULL nếu chưa khớp được bệnh nhân',
    order_id             BIGINT       NULL,
    source               VARCHAR(10)  NOT NULL COMMENT 'HIS|MANUAL',
    field_name           VARCHAR(100) NOT NULL,
    ris_value            VARCHAR(500) NULL,
    his_value            VARCHAR(500) NULL,
    detected_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolution           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|KEEP_RIS|TAKE_HIS|MANUAL_EDIT',
    resolved_by_user_id  BIGINT       NULL,
    resolved_at          DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_conflict_resolution_detected (resolution, detected_at),
    KEY idx_conflict_patient (patient_id),
    KEY idx_conflict_order (order_id),
    KEY idx_conflict_resolved_by (resolved_by_user_id),
    CONSTRAINT fk_conflict_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_conflict_order FOREIGN KEY (order_id) REFERENCES imaging_order (id),
    CONSTRAINT fk_conflict_resolved_by FOREIGN KEY (resolved_by_user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
