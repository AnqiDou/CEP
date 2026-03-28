IF OBJECT_ID('dbo.idle_items', 'U') IS NOT NULL
BEGIN
    DROP TABLE idle_items;
END;
GO

IF OBJECT_ID('dbo.idle_item_photos', 'U') IS NOT NULL AND OBJECT_ID('dbo.item_photos', 'U') IS NULL
BEGIN
    EXEC sp_rename 'dbo.idle_item_photos', 'item_photos';
END;
GO

IF OBJECT_ID('dbo.idle_item_photos', 'U') IS NOT NULL AND OBJECT_ID('dbo.item_photos', 'U') IS NOT NULL
BEGIN
    DROP TABLE idle_item_photos;
END;
GO

IF OBJECT_ID('dbo.items', 'U') IS NULL
BEGIN
    CREATE TABLE items (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        category_id BIGINT NOT NULL,
        publisher_user_id BIGINT NULL,
        title NVARCHAR(120) NOT NULL,
        description NVARCHAR(500) NOT NULL,
        price DECIMAL(10, 2) NOT NULL,
        campus NVARCHAR(50) NOT NULL,
        badge NVARCHAR(20) NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
        view_count INT NOT NULL DEFAULT 0,
        favorite_count INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories (id),
        CONSTRAINT fk_items_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id)
    );
END;
GO

IF COL_LENGTH('dbo.items', 'publisher_user_id') IS NULL
BEGIN
    ALTER TABLE items ADD publisher_user_id BIGINT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_items_publisher')
BEGIN
    ALTER TABLE items
    ADD CONSTRAINT fk_items_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id);
END;
GO

UPDATE i
SET i.publisher_user_id = d.publisher_user_id
FROM items i
INNER JOIN item_details d ON d.item_id = i.id
WHERE i.publisher_user_id IS NULL
  AND d.publisher_user_id IS NOT NULL;
GO

IF OBJECT_ID('dbo.item_photos', 'U') IS NULL
BEGIN
    CREATE TABLE item_photos (
        id BIGINT IDENTITY(1, 1) PRIMARY KEY,
        item_id BIGINT NOT NULL,
        photo_url NVARCHAR(500) NOT NULL,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_item_photos_item FOREIGN KEY (item_id) REFERENCES items (id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_items_filter' AND object_id = OBJECT_ID('dbo.items'))
BEGIN
    CREATE INDEX idx_items_filter ON items (status, category_id, created_at DESC, price DESC);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_items_hot' AND object_id = OBJECT_ID('dbo.items'))
BEGIN
    CREATE INDEX idx_items_hot ON items (status, favorite_count DESC, view_count DESC, created_at DESC);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_items_publisher' AND object_id = OBJECT_ID('dbo.items'))
BEGIN
    CREATE INDEX idx_items_publisher ON items (publisher_user_id, created_at DESC);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_item_photos_item' AND object_id = OBJECT_ID('dbo.item_photos'))
BEGIN
    CREATE INDEX idx_item_photos_item ON item_photos (item_id, sort_order ASC);
END;
GO

SELECT
    OBJECT_ID('dbo.items', 'U') AS items_table_id,
    OBJECT_ID('dbo.item_photos', 'U') AS item_photos_table_id;
GO

