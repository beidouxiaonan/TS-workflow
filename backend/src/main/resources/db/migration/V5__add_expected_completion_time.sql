-- OceanBase MySQL 模式；已有库执行一次，先升级表结构再部署新 JAR。
-- 全新库使用最新 init-oceanbase.sql，不重复执行此迁移。
ALTER TABLE kb_ticket
    ADD COLUMN expected_completion_time DATETIME(3) NULL COMMENT '期望完成时间';
