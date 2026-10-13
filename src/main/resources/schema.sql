CREATE TABLE IF NOT EXISTS sys_user (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    name      VARCHAR(50)  NOT NULL,
    role      VARCHAR(20)   NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    campus   VARCHAR(20)  NOT NULL,
    building VARCHAR(100),
    department_head VARCHAR(50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS work_order (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    department_id  BIGINT,
    department_name VARCHAR(100),
    campus         VARCHAR(20),
    location       VARCHAR(200),
    contact_person VARCHAR(50),
    contact_phone  VARCHAR(30),
    priority       VARCHAR(10) DEFAULT 'MEDIUM',
    status         VARCHAR(20) NOT NULL,
    assignee_id    BIGINT,
    creator_id    BIGINT,
    created_at    DATETIME,
    completed_at  DATETIME,
    repair_record TEXT,
    report_id     BIGINT,
    sign_status   VARCHAR(20) DEFAULT 'UNSIGNED',
    signed_by     VARCHAR(50),
    signed_at     DATETIME,
    signature_url VARCHAR(255)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department_binding (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    device_code   VARCHAR(100) NOT NULL UNIQUE,
    bind_at       DATETIME,
    active        TINYINT DEFAULT 1
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_no       VARCHAR(32),
    department_id   BIGINT NOT NULL,
    department_name VARCHAR(100),
    reporter_name   VARCHAR(50) NOT NULL,
    device_name     VARCHAR(100) NOT NULL,
    device_photo    VARCHAR(255),
    problem_desc    VARCHAR(500),
    department_head VARCHAR(50),
    status          VARCHAR(20) DEFAULT 'PENDING',
    creator_id      BIGINT,
    binding_id      BIGINT,
    created_at      DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consumable (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    name          VARCHAR(100),
    quantity      INT,
    unit_price    DECIMAL(10,2),
    is_iou        TINYINT DEFAULT 0,
    iou_no        VARCHAR(50)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reimbursement (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    month         VARCHAR(7) NOT NULL,
    department_id BIGINT,
    total_amount  DECIMAL(12,2),
    status        VARCHAR(20) DEFAULT 'PENDING_SIGN',
    signed_by     VARCHAR(50),
    signed_at     DATETIME,
    reimbursed_at DATETIME,
    created_at    DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reimbursement_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reimbursement_id BIGINT NOT NULL,
    work_order_id   BIGINT NOT NULL,
    created_at      DATETIME
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;