-- V2: người dùng và vai trò — app_user, role, user_role.
-- 5 vai trò theo README Phần 5: ADMIN | CLINICIAN | TECHNICIAN | RADIOLOGIST | APPROVER.

CREATE TABLE app_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt, không bao giờ lưu mật khẩu thô',
    full_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NULL,
    department_id  BIGINT       NULL,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username),
    KEY idx_app_user_department (department_id),
    CONSTRAINT fk_app_user_department FOREIGN KEY (department_id) REFERENCES department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `role` (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    code  VARCHAR(30)  NOT NULL COMMENT 'ADMIN|CLINICIAN|TECHNICIAN|RADIOLOGIST|APPROVER',
    name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_role (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES `role` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
