-- 评价模块手动建表脚本（SQL Server）

IF OBJECT_ID('trade_review_tasks', 'U') IS NULL
BEGIN
    CREATE TABLE trade_review_tasks (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        order_id BIGINT NOT NULL,
        reviewer_user_id BIGINT NOT NULL,
        target_user_id BIGINT NOT NULL,
        target_role NVARCHAR(20) NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
        reviewed_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT uq_trade_review_tasks UNIQUE (order_id, reviewer_user_id),
        CONSTRAINT fk_trade_review_tasks_order FOREIGN KEY (order_id) REFERENCES trade_orders (id),
        CONSTRAINT fk_trade_review_tasks_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users (id),
        CONSTRAINT fk_trade_review_tasks_target FOREIGN KEY (target_user_id) REFERENCES users (id)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_trade_review_tasks_reviewer'
      AND object_id = OBJECT_ID('trade_review_tasks')
)
BEGIN
    CREATE INDEX idx_trade_review_tasks_reviewer
        ON trade_review_tasks (reviewer_user_id, status, created_at DESC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_trade_review_tasks_order'
      AND object_id = OBJECT_ID('trade_review_tasks')
)
BEGIN
    CREATE INDEX idx_trade_review_tasks_order
        ON trade_review_tasks (order_id, created_at DESC);
END;
GO

-- 聊天消息扩展字段（用于评价邀请透出）
IF COL_LENGTH('message_records', 'biz_type') IS NULL
BEGIN
    ALTER TABLE message_records ADD biz_type NVARCHAR(40) NULL;
END;
GO

IF COL_LENGTH('message_records', 'biz_id') IS NULL
BEGIN
    ALTER TABLE message_records ADD biz_id BIGINT NULL;
END;
GO

-- 建表后核验（应返回 3 行）
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('trade_review_tasks', 'message_conversations', 'message_records');
GO

