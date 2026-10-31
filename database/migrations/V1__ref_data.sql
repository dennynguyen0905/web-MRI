-- V1: dữ liệu tham chiếu — department, service_item, device.
-- Nguồn: doc/02-erd.md (v0.9) và doc/diagrams/02-erd.mmd. Thời gian lưu theo UTC (ERD 4.7).
-- QUY TẮC: file đã vào main thì KHÔNG sửa nữa. Thay đổi schema = thêm file V8, V9, ... mới.

CREATE TABLE department (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    parent_id   BIGINT       NULL COMMENT 'cây khoa -> nhóm',
    `type`      VARCHAR(10)  NOT NULL COMMENT 'KHOA|NHOM',
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code),
    KEY idx_department_parent (parent_id),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE service_item (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL COMMENT 'tên chỉ định',
    modality   VARCHAR(10)  NOT NULL COMMENT 'CT|MR|CR|DX|US',
    body_part  VARCHAR(100) NULL COMMENT 'bộ phận chụp',
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_item_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE device (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(255) NOT NULL COMMENT 'tên máy',
    modality      VARCHAR(10)  NOT NULL,
    department_id BIGINT       NOT NULL,
    ae_title      VARCHAR(16)  NULL COMMENT 'để trống trong 8 tuần, dùng cho MWL (H-01)',
    station_name  VARCHAR(64)  NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (code),
    KEY idx_device_department (department_id),
    CONSTRAINT fk_device_department FOREIGN KEY (department_id) REFERENCES department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
