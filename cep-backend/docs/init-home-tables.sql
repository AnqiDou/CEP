IF OBJECT_ID ('dbo.item_categories', 'U') IS NULL BEGIN
CREATE TABLE item_categories (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    code NVARCHAR (30) NOT NULL UNIQUE,
    name NVARCHAR (50) NOT NULL,
    description NVARCHAR (200) NOT NULL,
    tags NVARCHAR (300) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

END;
GO

IF OBJECT_ID ('dbo.items', 'U') IS NULL BEGIN
CREATE TABLE items (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title NVARCHAR (120) NOT NULL,
    description NVARCHAR (500) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    campus NVARCHAR (50) NOT NULL,
    status NVARCHAR (20) NOT NULL DEFAULT 'PUBLISHED',
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories (id)
);

END;
GO

IF OBJECT_ID ('dbo.search_keywords', 'U') IS NULL BEGIN
CREATE TABLE search_keywords (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    keyword NVARCHAR (100) NOT NULL UNIQUE,
    search_count BIGINT NOT NULL DEFAULT 0,
    last_searched_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_item_categories_sort'
        AND object_id = OBJECT_ID ('dbo.item_categories')
) BEGIN
CREATE INDEX idx_item_categories_sort ON item_categories (sort_order ASC);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_items_filter'
        AND object_id = OBJECT_ID ('dbo.items')
) BEGIN
CREATE INDEX idx_items_filter ON items (
    status,
    category_id,
    created_at DESC,
    price DESC
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_items_hot'
        AND object_id = OBJECT_ID ('dbo.items')
) BEGIN
CREATE INDEX idx_items_hot ON items (
    status,
    favorite_count DESC,
    view_count DESC,
    created_at DESC
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_search_keywords_hot'
        AND object_id = OBJECT_ID ('dbo.search_keywords')
) BEGIN
CREATE INDEX idx_search_keywords_hot ON search_keywords (
    search_count DESC,
    updated_at DESC
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM item_categories
) BEGIN
INSERT INTO
    item_categories (
        code,
        name,
        description,
        tags,
        sort_order,
        created_at,
        updated_at
    )
VALUES (
        'digital',
        '数码产品',
        '手机、电脑、平板、耳机、充电器等',
        '二手手机,笔记本,平板,耳机,充电器',
        1,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'book',
        '图书教材',
        '课本、考研考公资料、小说、专业书',
        '课本,考研资料,考公资料,小说,专业书',
        2,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'clothes',
        '服饰鞋包',
        '衣服、鞋子、包包、配饰',
        '外套,球鞋,双肩包,配饰',
        3,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'beauty',
        '美妆护肤',
        '化妆品、护肤品、香水',
        '口红,面霜,防晒,香水',
        4,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'sports',
        '运动器材',
        '篮球、羽毛球拍、瑜伽垫、自行车',
        '篮球,羽毛球拍,瑜伽垫,自行车',
        5,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'daily',
        '生活用品',
        '收纳、小家电、锅碗瓢盆、寝室用品',
        '收纳盒,小家电,锅具,寝室用品',
        6,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'stationery',
        '文具办公',
        '笔、本、计算器、文件夹等',
        '中性笔,笔记本,计算器,文件夹',
        7,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'other',
        '其他',
        '不好归类的都放这（默认）',
        '手办,乐器,票券,其他',
        8,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM items
) BEGIN
INSERT INTO
    items (
        category_id,
        title,
        description,
        price,
        campus,
        status,
        view_count,
        favorite_count,
        created_at,
        updated_at
    )
VALUES (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'book'
        ),
        '22级高数教材+辅导书全套',
        '九成新，无笔记，适合低年级同学使用。',
        35.00,
        '东校区',
        'PUBLISHED',
        142,
        31,
        DATEADD (HOUR, -1, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -1, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'digital'
        ),
        'iPad 9 64G + 原装笔',
        '用于记笔记，电池健康良好，附带原装包装盒。',
        1200.00,
        '本部',
        'PUBLISHED',
        286,
        57,
        DATEADD (HOUR, -2, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -2, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'book'
        ),
        '考研政治核心考点精讲',
        '基本全新，少量划线，已顺利上岸转让。',
        20.00,
        '南校区',
        'PUBLISHED',
        98,
        22,
        DATEADD (HOUR, -3, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -3, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'digital'
        ),
        '降噪无线蓝牙耳机',
        '音质不错，适合通勤与自习使用。',
        80.00,
        '本部',
        'PUBLISHED',
        167,
        38,
        DATEADD (
            MINUTE,
            -30,
            CURRENT_TIMESTAMP
        ),
        DATEADD (
            MINUTE,
            -30,
            CURRENT_TIMESTAMP
        )
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'daily'
        ),
        '宿舍护眼台灯（可调色温）',
        '灯光柔和，支持三档调节，自习必备。',
        28.00,
        '东校区',
        'PUBLISHED',
        123,
        19,
        DATEADD (
            MINUTE,
            -15,
            CURRENT_TIMESTAMP
        ),
        DATEADD (
            MINUTE,
            -15,
            CURRENT_TIMESTAMP
        )
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'sports'
        ),
        '九成新山地自行车',
        '通勤代步稳定，车况良好，可当面试骑。',
        360.00,
        '本部',
        'PUBLISHED',
        209,
        41,
        DATEADD (
            MINUTE,
            -45,
            CURRENT_TIMESTAMP
        ),
        DATEADD (
            MINUTE,
            -45,
            CURRENT_TIMESTAMP
        )
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'digital'
        ),
        '机械键盘青轴 87键',
        '手感清脆，带灯效，送拔键器。',
        99.00,
        '南校区',
        'PUBLISHED',
        88,
        16,
        DATEADD (HOUR, -4, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -4, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'other'
        ),
        '吉他入门教程 + 民谣谱合集',
        '包含多本教材，适合零基础上手。',
        26.00,
        '本部',
        'PUBLISHED',
        76,
        11,
        DATEADD (DAY, -1, CURRENT_TIMESTAMP),
        DATEADD (DAY, -1, CURRENT_TIMESTAMP)
    );

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM search_keywords
) BEGIN
INSERT INTO
    search_keywords (
        keyword,
        search_count,
        last_searched_at,
        created_at,
        updated_at
    )
VALUES (
        '考研资料',
        126,
        DATEADD (
            MINUTE,
            -20,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '四六级',
        89,
        DATEADD (
            MINUTE,
            -38,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '平板',
        95,
        DATEADD (
            MINUTE,
            -12,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '耳机',
        87,
        DATEADD (
            MINUTE,
            -15,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '自行车',
        73,
        DATEADD (
            MINUTE,
            -30,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '台灯',
        64,
        DATEADD (
            MINUTE,
            -24,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '二手手机',
        110,
        DATEADD (MINUTE, -8, CURRENT_TIMESTAMP),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '雅思',
        58,
        DATEADD (
            MINUTE,
            -50,
            CURRENT_TIMESTAMP
        ),
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

END;
GO