-- 说明：本项目已通过后端启动自动建表（ProfileSchemaInitializer）保证表存在。
-- 本文件仅用于结构说明和排查，不要求手动执行。

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

-- 评价敏感词库（词条级，不是整句）
CREATE TABLE review_sensitive_words (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    category NVARCHAR (50) NOT NULL,
    word NVARCHAR (50) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_sensitive_words_word UNIQUE (word)
);

INSERT INTO
    review_sensitive_words (category, word, enabled)
VALUES (N'辱骂人身攻击类', N'傻逼', 1),
    (N'辱骂人身攻击类', N'垃圾', 1),
    (N'辱骂人身攻击类', N'废物', 1),
    (N'辱骂人身攻击类', N'脑残', 1),
    (N'辱骂人身攻击类', N'滚', 1),
    (N'广告导流类', N'微信', 1),
    (N'广告导流类', N'wechat', 1),
    (N'广告导流类', N'vx', 1),
    (N'广告导流类', N'qq', 1),
    (N'广告导流类', N'电话', 1),
    (N'广告导流类', N'加好友', 1),
    (N'广告导流类', N'私聊', 1),
    (N'违规承诺类', N'包过', 1),
    (N'违规承诺类', N'代写', 1),
    (N'违规承诺类', N'作弊', 1),
    (N'违规承诺类', N'代考', 1),
    (N'违规承诺类', N'刷分', 1),
    (N'涉政涉黄暴力类', N'约炮', 1),
    (N'涉政涉黄暴力类', N'嫖娼', 1),
    (N'涉政涉黄暴力类', N'枪支', 1),
    (N'涉政涉黄暴力类', N'爆炸物', 1),
    (N'涉政涉黄暴力类', N'恐袭', 1),
    (N'隐私信息类', N'手机号', 1),
    (N'隐私信息类', N'身份证号', 1),
    (N'隐私信息类', N'住址', 1),
    (N'隐私信息类', N'银行卡号', 1),
    (N'隐私信息类', N'门牌号', 1);

ALTER TABLE user_profiles ADD avatar_url NVARCHAR (500) NULL;

ALTER TABLE trade_orders ADD buyer_user_id BIGINT NULL;

ALTER TABLE trade_orders ADD seller_user_id BIGINT NULL;

CREATE INDEX idx_user_follows_target ON user_follows (
    target_user_id,
    created_at DESC
);

CREATE INDEX idx_user_favorites_user ON user_favorites (user_id, created_at DESC);

CREATE INDEX idx_user_credit_reviews_target ON user_credit_reviews (
    target_user_id,
    created_at DESC
);

CREATE INDEX idx_review_sensitive_words_category ON review_sensitive_words (category, enabled);

CREATE INDEX idx_trade_orders_buyer ON trade_orders (
    buyer_user_id,
    created_at DESC
);

CREATE INDEX idx_trade_orders_seller ON trade_orders (
    seller_user_id,
    created_at DESC
);