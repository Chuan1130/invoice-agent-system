CREATE DATABASE IF NOT EXISTS invoice_agent;
USE invoice_agent;

CREATE TABLE IF NOT EXISTS audit_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    original_file_path VARCHAR(255) NULL,
    ocr_raw_text TEXT NULL,
    final_decision VARCHAR(64) NULL,
    need_human_review TINYINT(1) DEFAULT 0,
    report_path VARCHAR(255) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoice_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    invoice_no VARCHAR(128) NULL,
    invoice_date DATETIME NULL,
    amount DECIMAL(12,2) NULL,
    tax_amount DECIMAL(12,2) NULL,
    buyer_name VARCHAR(255) NULL,
    seller_name VARCHAR(255) NULL,
    invoice_type VARCHAR(64) NULL,
    raw_json TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_rule_hit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    hit_result VARCHAR(32) NOT NULL,
    rule_message VARCHAR(500) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    step_status VARCHAR(32) NOT NULL,
    message VARCHAR(1000) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
