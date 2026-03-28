-- 说明：本项目已通过后端启动自动建表（ProfileSchemaInitializer）保证表存在。
-- 本文件仅用于结构说明和排查，不要求手动执行。

CREATE TABLE user_follows (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_follows UNIQUE (user_id, target_user_id),
    CONSTRAINT fk_user_follows_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_follows_target FOREIGN KEY (target_user_id) REFERENCES users (id)
);

CREATE TABLE user_favorites (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_favorites UNIQUE (user_id, item_id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_favorites_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE TABLE user_credit_reviews (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    order_id BIGINT NULL,
    rater_user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    target_role NVARCHAR(20) NOT NULL,
    rating NVARCHAR(10) NOT NULL,
    content NVARCHAR(300) NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_credit_reviews_order FOREIGN KEY (order_id) REFERENCES trade_orders (id),
    CONSTRAINT fk_user_credit_reviews_rater FOREIGN KEY (rater_user_id) REFERENCES users (id),
    CONSTRAINT fk_user_credit_reviews_target FOREIGN KEY (target_user_id) REFERENCES users (id)
);

ALTER TABLE user_profiles ADD avatar_url NVARCHAR(500) NULL;
ALTER TABLE trade_orders ADD buyer_user_id BIGINT NULL;
ALTER TABLE trade_orders ADD seller_user_id BIGINT NULL;

CREATE INDEX idx_user_follows_target ON user_follows (target_user_id, created_at DESC);
CREATE INDEX idx_user_favorites_user ON user_favorites (user_id, created_at DESC);
CREATE INDEX idx_user_credit_reviews_target ON user_credit_reviews (target_user_id, created_at DESC);
CREATE INDEX idx_trade_orders_buyer ON trade_orders (buyer_user_id, created_at DESC);
CREATE INDEX idx_trade_orders_seller ON trade_orders (seller_user_id, created_at DESC);

