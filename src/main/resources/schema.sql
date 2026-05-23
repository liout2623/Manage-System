-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    phone VARCHAR(30),
    occupation VARCHAR(255),
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 客户表
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(128),
    gender VARCHAR(16),
    tags VARCHAR(255),
    note TEXT,
    birthday DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 服务项目表
CREATE TABLE IF NOT EXISTS services (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    duration_minutes INT,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 健康档案表
CREATE TABLE IF NOT EXISTS health_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    assessment TEXT,
    recommendation TEXT,
    record_date DATE,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 预约表
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    service_id BIGINT,
    therapist_id BIGINT,
    appointment_time DATETIME NOT NULL COMMENT '预约开始时间',
    end_time DATETIME NOT NULL COMMENT '预约结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED / COMPLETED / CANCELLED',
    note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL,
    FOREIGN KEY (therapist_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_therapist_time (therapist_id, appointment_time),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 理疗师-服务项目关联表
CREATE TABLE IF NOT EXISTS therapist_services (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (therapist_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
    UNIQUE KEY uk_therapist_service (therapist_id, service_id)
);

-- 初始管理员账号（密码: admin123）
INSERT IGNORE INTO users (id, username, password_hash, role, display_name, phone, occupation, active, created_at, updated_at)
VALUES (
    1,
    'admin',
    '$2a$10$yN2tFWs8vG4xwmOPzP.FVu9/fOANyGplF7Lc/aQhpxwpcnYLm3oLq',
    'ADMIN',
    '系统管理员',
    '13800000000',
    '馆长',
    1,
    NOW(),
    NOW()
);
