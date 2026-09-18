ALTER TABLE kb_ticket
    ADD COLUMN attachment_id VARCHAR(128) NULL COMMENT '附件服务返回的附件ID' AFTER supplement_type;
