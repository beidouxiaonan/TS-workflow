-- 全新环境初始化：OceanBase MySQL 模式，需要建库权限。
-- 数据库名示例为 ekbdb，执行前与 deployment 中 JDBC URL 的数据库名保持一致。
-- 不删除现有数据；已有表升级请使用对应 migration，不重复执行已执行的升级脚本。
CREATE DATABASE IF NOT EXISTS ekbdb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ekbdb;

-- OceanBase MySQL 模式，最新完整表结构（包含 V2/V3/V4 字段）。
-- 在目标数据库中执行；已有表不会被覆盖，也不会自动补齐缺失列。
CREATE TABLE IF NOT EXISTS kb_ticket (
    id VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    applicant VARCHAR(64) NOT NULL,
    department VARCHAR(128),
    team VARCHAR(128),
    knowledge_base VARCHAR(200),
    file_type VARCHAR(64),
    supplement_type VARCHAR(64),
    attachment_ids TEXT COMMENT '附件ID字符串，多个ID可用逗号分隔，历史文本原样保留',
    reason TEXT NOT NULL,
    solution TEXT,
    priority VARCHAR(16) NOT NULL DEFAULT '普通',
    status VARCHAR(32) NOT NULL DEFAULT '草稿',
    current_node VARCHAR(64) NOT NULL DEFAULT '填写申请',
    leader_id VARCHAR(64) COMMENT '部门领导用户ID',
    knowledge_base_owner_id VARCHAR(64) COMMENT '目标知识库拥有者用户ID',
    owner VARCHAR(64) NOT NULL DEFAULT '待指派',
    expected_completion_time DATETIME(3) NULL COMMENT '期望完成时间',
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_kb_ticket_status (status),
    KEY idx_kb_ticket_applicant (applicant),
    KEY idx_kb_ticket_updated_at (updated_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS kb_ticket_process (
    process_id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id VARCHAR(32) NOT NULL,
    node_name VARCHAR(64) NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    action_name VARCHAR(32) NOT NULL,
    comment_text VARCHAR(1000),
    operated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (process_id),
    KEY idx_process_ticket_time (ticket_id, operated_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
