ALTER TABLE kb_ticket
    CHANGE COLUMN attachment_id attachment_ids TEXT NULL COMMENT '附件ID列表，JSON数组格式';

UPDATE kb_ticket
SET attachment_ids = CONCAT('["', REPLACE(attachment_ids, '"', '\\"'), '"]')
WHERE attachment_ids IS NOT NULL
  AND attachment_ids <> ''
  AND LEFT(TRIM(attachment_ids), 1) <> '[';
