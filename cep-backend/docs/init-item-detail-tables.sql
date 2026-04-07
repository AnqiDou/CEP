IF OBJECT_ID('dbo.user_profiles', 'U') IS NULL
BEGIN
    CREATE TABLE user_profiles (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        college NVARCHAR(80) NULL,
        campus NVARCHAR(50) NULL,
        credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0,
        note NVARCHAR(200) NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
    );
END;
GO

IF OBJECT_ID('dbo.item_details', 'U') IS NULL
BEGIN
    CREATE TABLE item_details (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        item_id BIGINT NOT NULL UNIQUE,
        publisher_user_id BIGINT NULL,
        purchase_date DATE NULL,
        usage_duration NVARCHAR(50) NULL,
        item_condition NVARCHAR(50) NULL,
        accessories NVARCHAR(200) NULL,
        detail_note NVARCHAR(300) NULL,
        trade_location NVARCHAR(80) NULL,
        original_price DECIMAL(10, 2) NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_item_details_item FOREIGN KEY (item_id) REFERENCES items (id),
        CONSTRAINT fk_item_details_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_user_profiles_user' AND object_id = OBJECT_ID('dbo.user_profiles'))
BEGIN
    CREATE INDEX idx_user_profiles_user ON user_profiles (user_id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_item_details_item' AND object_id = OBJECT_ID('dbo.item_details'))
BEGIN
    CREATE INDEX idx_item_details_item ON item_details (item_id, publisher_user_id);
END;
GO

IF COL_LENGTH('dbo.user_profiles', 'credit_score') IS NULL
BEGIN
    ALTER TABLE user_profiles ADD credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0;
END;
GO

ALTER TABLE user_profiles ALTER COLUMN credit_score DECIMAL(10, 1) NOT NULL;
GO

UPDATE user_profiles SET credit_score = 100.0 WHERE credit_score IS NULL;
GO

SELECT
    OBJECT_ID('dbo.user_profiles', 'U') AS user_profiles_table_id,
    OBJECT_ID('dbo.item_details', 'U') AS item_details_table_id;
GO

SELECT
    t.name AS table_name,
    c.name AS column_name,
    ty.name AS data_type
FROM sys.tables t
INNER JOIN sys.columns c ON c.object_id = t.object_id
INNER JOIN sys.types ty ON ty.user_type_id = c.user_type_id
WHERE t.name IN ('user_profiles', 'item_details')
ORDER BY t.name, c.column_id;
GO

