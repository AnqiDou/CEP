-- 消息模块手动建表脚本（SQL Server）

IF OBJECT_ID('message_conversations', 'U') IS NULL
BEGIN
    CREATE TABLE message_conversations (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        item_id BIGINT NOT NULL,
        buyer_user_id BIGINT NOT NULL,
        seller_user_id BIGINT NOT NULL,
        last_message NVARCHAR(1000) NULL,
        last_message_type NVARCHAR(20) NOT NULL DEFAULT 'TEXT',
        unread_buyer INT NOT NULL DEFAULT 0,
        unread_seller INT NOT NULL DEFAULT 0,
        last_message_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_message_conversations_item FOREIGN KEY (item_id) REFERENCES items (id),
        CONSTRAINT fk_message_conversations_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
        CONSTRAINT fk_message_conversations_seller FOREIGN KEY (seller_user_id) REFERENCES users (id),
        CONSTRAINT uq_message_conversations_item_pair UNIQUE (item_id, buyer_user_id, seller_user_id)
    );
END;
GO

IF OBJECT_ID('message_records', 'U') IS NULL
BEGIN
    CREATE TABLE message_records (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        conversation_id BIGINT NOT NULL,
        sender_user_id BIGINT NOT NULL,
        message_type NVARCHAR(20) NOT NULL DEFAULT 'TEXT',
        text_content NVARCHAR(2000) NULL,
        image_url NVARCHAR(500) NULL,
        biz_type NVARCHAR(40) NULL,
        biz_id BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        read_at DATETIME2 NULL,
        CONSTRAINT fk_message_records_conversation FOREIGN KEY (conversation_id) REFERENCES message_conversations (id),
        CONSTRAINT fk_message_records_sender FOREIGN KEY (sender_user_id) REFERENCES users (id)
    );
END;
GO

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

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_message_conversations_buyer'
      AND object_id = OBJECT_ID('message_conversations')
)
BEGIN
    CREATE INDEX idx_message_conversations_buyer
        ON message_conversations (buyer_user_id, last_message_at DESC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_message_conversations_seller'
      AND object_id = OBJECT_ID('message_conversations')
)
BEGIN
    CREATE INDEX idx_message_conversations_seller
        ON message_conversations (seller_user_id, last_message_at DESC);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_message_records_conversation'
      AND object_id = OBJECT_ID('message_records')
)
BEGIN
    CREATE INDEX idx_message_records_conversation
        ON message_records (conversation_id, created_at ASC, id ASC);
END;
GO

-- 建表后核验（应返回两行）
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('message_conversations', 'message_records');

