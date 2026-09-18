-- OceanBase MySQL 模式：已有库升级时执行一次；新建库不需要执行。
-- 先升级数据库，再部署新版 JAR。历史工单保留 NULL，不猜测审批人。
ALTER TABLE kb_ticket
    ADD COLUMN leader_id VARCHAR(64) NULL COMMENT '部门领导用户ID',
    ADD COLUMN knowledge_base_owner_id VARCHAR(64) NULL COMMENT '目标知识库拥有者用户ID';
