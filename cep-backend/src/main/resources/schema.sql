DROP TABLE IF EXISTS email_verification_codes;

DROP TABLE IF EXISTS auth_sessions;

DROP TABLE IF EXISTS search_keywords;

DROP TABLE IF EXISTS item_photos;

DROP TABLE IF EXISTS item_details;

DROP TABLE IF EXISTS user_credit_reviews;

DROP TABLE IF EXISTS user_favorites;

DROP TABLE IF EXISTS user_follows;

DROP TABLE IF EXISTS trade_orders;

DROP TABLE IF EXISTS idle_item_photos;

DROP TABLE IF EXISTS idle_items;

DROP TABLE IF EXISTS items;

DROP TABLE IF EXISTS item_categories;

DROP TABLE IF EXISTS user_profiles;

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    email NVARCHAR (100) NOT NULL UNIQUE,
    username NVARCHAR (50),
    password_hash NVARCHAR (100) NOT NULL,
    status NVARCHAR (20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME2 NULL
);

CREATE TABLE email_verification_codes (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    email NVARCHAR (100) NOT NULL,
    purpose NVARCHAR (30) NOT NULL,
    code NVARCHAR (6) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_sessions (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token_hash NVARCHAR (64) NOT NULL UNIQUE,
    access_token_hash NVARCHAR (64) NOT NULL UNIQUE,
    refresh_expires_at DATETIME2 NOT NULL,
    access_expires_at DATETIME2 NOT NULL,
    revoked BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_profiles (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    college NVARCHAR (80) NULL,
    credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0,
    avatar_url NVARCHAR (500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

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

CREATE TABLE items (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    publisher_user_id BIGINT NULL,
    title NVARCHAR (120) NOT NULL,
    description NVARCHAR (500) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    badge NVARCHAR (20) NULL,
    status NVARCHAR (20) NOT NULL DEFAULT 'PUBLISHED',
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories (id),
    CONSTRAINT fk_items_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id)
);

CREATE TABLE item_photos (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    item_id BIGINT NOT NULL,
    photo_url NVARCHAR (500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_photos_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE TABLE item_details (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    item_id BIGINT NOT NULL UNIQUE,
    publisher_user_id BIGINT NULL,
    purchase_date DATE NULL,
    usage_duration NVARCHAR (50) NULL,
    item_condition NVARCHAR (50) NULL,
    accessories NVARCHAR (200) NULL,
    detail_note NVARCHAR (300) NULL,
    trade_location NVARCHAR (80) NULL,
    original_price DECIMAL(10, 2) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_details_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_item_details_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id)
);

CREATE TABLE trade_orders (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    order_no NVARCHAR (40) NOT NULL UNIQUE,
    item_id BIGINT NOT NULL,
    buyer_user_id BIGINT NULL,
    seller_user_id BIGINT NULL,
    item_title NVARCHAR (120) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    cover_photo_url NVARCHAR (500) NULL,
    receiver_name NVARCHAR (50) NOT NULL,
    receiver_phone NVARCHAR (30) NOT NULL,
    receiver_address NVARCHAR (200) NOT NULL,
    status NVARCHAR (30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    paid_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_orders_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_trade_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
    CONSTRAINT fk_trade_orders_seller FOREIGN KEY (seller_user_id) REFERENCES users (id)
);

CREATE TABLE user_follows (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_follows UNIQUE (user_id, target_user_id),
    CONSTRAINT fk_user_follows_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_follows_target FOREIGN KEY (target_user_id) REFERENCES users (id)
);

CREATE TABLE user_favorites (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_favorites UNIQUE (user_id, item_id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_favorites_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE TABLE user_credit_reviews (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    order_id BIGINT NULL,
    rater_user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    target_role NVARCHAR (20) NOT NULL,
    rating NVARCHAR (10) NOT NULL,
    content NVARCHAR (300) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_credit_reviews_order FOREIGN KEY (order_id) REFERENCES trade_orders (id),
    CONSTRAINT fk_user_credit_reviews_rater FOREIGN KEY (rater_user_id) REFERENCES users (id),
    CONSTRAINT fk_user_credit_reviews_target FOREIGN KEY (target_user_id) REFERENCES users (id)
);

CREATE TABLE search_keywords (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    keyword NVARCHAR (100) NOT NULL UNIQUE,
    search_count BIGINT NOT NULL DEFAULT 0,
    last_searched_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verification_email_purpose ON email_verification_codes (
    email,
    purpose,
    created_at DESC
);

CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id, created_at DESC);

CREATE INDEX idx_user_profiles_user ON user_profiles (user_id);

CREATE INDEX idx_item_categories_sort ON item_categories (sort_order ASC);

CREATE INDEX idx_items_filter ON items (
    status,
    category_id,
    created_at DESC,
    price DESC
);

CREATE INDEX idx_items_hot ON items (
    status,
    favorite_count DESC,
    view_count DESC,
    created_at DESC
);

CREATE INDEX idx_items_publisher ON items (
    publisher_user_id,
    created_at DESC
);

CREATE INDEX idx_item_photos_item ON item_photos (item_id, sort_order ASC);

CREATE INDEX idx_item_details_item ON item_details (item_id, publisher_user_id);

CREATE INDEX idx_trade_orders_item ON trade_orders (item_id, created_at DESC);

CREATE INDEX idx_trade_orders_status ON trade_orders (status, created_at DESC);

CREATE INDEX idx_trade_orders_buyer ON trade_orders (
    buyer_user_id,
    created_at DESC
);

CREATE INDEX idx_trade_orders_seller ON trade_orders (
    seller_user_id,
    created_at DESC
);

CREATE INDEX idx_user_follows_target ON user_follows (
    target_user_id,
    created_at DESC
);

CREATE INDEX idx_user_favorites_user ON user_favorites (user_id, created_at DESC);

CREATE INDEX idx_user_credit_reviews_target ON user_credit_reviews (
    target_user_id,
    created_at DESC
);

CREATE INDEX idx_search_keywords_hot ON search_keywords (
    search_count DESC,
    updated_at DESC
);

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
        '手机、电脑、平板、耳机、充电器�?,
        '二手手机,笔记�?平板,耳机,充电�?,
        1,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'book',
        '图书教材',
        '课本、考研考公资料、小说、专业书',
        '课本,考研资料,考公资料,小说,专业�?,
        2,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'clothes',
        '服饰鞋包',
        '衣服、鞋子、包包、配�?,
        '外套,球鞋,双肩�?配饰',
        3,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'beauty',
        '美妆护肤',
        '化妆品、护肤品、香�?,
        '口红,面霜,防晒,香水',
        4,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'sports',
        '运动器材',
        '篮球、羽毛球拍、瑜伽垫、自行车',
        '篮球,羽毛球拍,瑜伽�?自行�?,
        5,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'daily',
        '生活用品',
        '收纳、小家电、锅碗瓢盆、寝室用�?,
        '收纳�?小家�?锅具,寝室用品',
        6,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'stationery',
        '文具办公',
        '笔、本、计算器、文件夹�?,
        '中性笔,笔记�?计算�?文件�?,
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

INSERT INTO
    items (
        category_id,
        title,
        description,
        price,
        badge,
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
        '22级高数教�?辅导书全�?,
        '九成新，无笔记，适合低年级同学使用�?,
        35.00,
        '东校�?,
        '热门',
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
        'iPad 9 64G + 原装�?,
        '用于记笔记，电池健康良好，附带原装包装盒�?,
        1200.00,
        '急售',
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
        '基本全新，少量划线，已顺利上岸转让�?,
        20.00,
        '南校�?,
        '',
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
        '音质不错，适合通勤与自习使用�?,
        80.00,
        '精�?,
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
        '灯光柔和，支持三档调节，自习必备�?,
        28.00,
        '东校�?,
        '新品',
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
        '通勤代步稳定，车况良好，可当面试骑�?,
        360.00,
        '热销',
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
        '机械键盘青轴 87�?,
        '手感清脆，带灯效，送拔键器�?,
        99.00,
        '南校�?,
        '精�?,
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
        '吉他入门教程 + 民谣谱合�?,
        '包含多本教材，适合零基础上手�?,
        26.00,
        '超�?,
        'PUBLISHED',
        76,
        11,
        DATEADD (DAY, -1, CURRENT_TIMESTAMP),
        DATEADD (DAY, -1, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'clothes'
        ),
        '冬季加绒外套',
        '尺码M，穿过两次，保暖效果好�?,
        59.00,
        '北校�?,
        '',
        'PUBLISHED',
        61,
        9,
        DATEADD (HOUR, -6, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -6, CURRENT_TIMESTAMP)
    ),
    (
        (
            SELECT id
            FROM item_categories
            WHERE
                code = 'stationery'
        ),
        '全新科学计算�?,
        '课程要求型号，现低价转让�?,
        48.00,
        '东校�?,
        '',
        'PUBLISHED',
        53,
        7,
        DATEADD (HOUR, -8, CURRENT_TIMESTAMP),
        DATEADD (HOUR, -8, CURRENT_TIMESTAMP)
    );

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
        '四六�?,
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
        '自行�?,
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
    );