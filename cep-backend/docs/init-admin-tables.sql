IF OBJECT_ID('admin_support_messages', 'U') IS NOT NULL
DROP TABLE admin_support_messages;

IF OBJECT_ID('admin_support_conversations', 'U') IS NOT NULL
DROP TABLE admin_support_conversations;

IF OBJECT_ID('admin_notices', 'U') IS NOT NULL
DROP TABLE admin_notices;

CREATE TABLE admin_support_conversations (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    title NVARCHAR(120) NOT NULL,
    preview NVARCHAR(200) NOT NULL DEFAULT '',
    status NVARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_support_messages (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_type NVARCHAR(20) NOT NULL,
    content NVARCHAR(500) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_support_messages_conversation FOREIGN KEY (conversation_id) REFERENCES admin_support_conversations (id)
);

CREATE TABLE admin_notices (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    content NVARCHAR(500) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_support_conversations_status ON admin_support_conversations (status, updated_at DESC);
CREATE INDEX idx_admin_support_messages_conversation ON admin_support_messages (conversation_id, created_at ASC);
CREATE INDEX idx_admin_notices_created_at ON admin_notices (created_at DESC);

INSERT INTO admin_support_conversations (title, preview, status, created_at, updated_at)
VALUES
    ('Order dispute: item mismatch', 'User reported item does not match description', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Report: prohibited listing', 'User reported prohibited contact info in detail page', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO admin_support_messages (conversation_id, sender_type, content, created_at)
VALUES
    (1, 'USER', 'The item model is different from the page description.', CURRENT_TIMESTAMP),
    (1, 'USER', 'Please ask platform support to intervene.', CURRENT_TIMESTAMP),
    (2, 'USER', 'The listing detail includes prohibited external contact info.', CURRENT_TIMESTAMP);

INSERT INTO admin_notices (content, created_at, updated_at)
VALUES
    ('Please do not bypass platform transactions offline. Beware of fraud.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('System maintenance will be performed this Sunday 02:00-03:00.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
