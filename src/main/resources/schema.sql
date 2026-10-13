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
    building VARCHAR(100)
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
    repair_record TEXT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;