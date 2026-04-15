-- MySQL：评价敏感词表初始化脚本

CREATE TABLE IF NOT EXISTS review_sensitive_words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    word VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_review_sensitive_words_word UNIQUE (word)
);

CREATE INDEX idx_review_sensitive_words_category ON review_sensitive_words (category, enabled);

INSERT IGNORE INTO
    review_sensitive_words (category, word, enabled)
VALUES ('辱骂人身攻击类', '傻逼', 1),
    ('辱骂人身攻击类', '垃圾', 1),
    ('辱骂人身攻击类', '废物', 1),
    ('辱骂人身攻击类', '脑残', 1),
    ('辱骂人身攻击类', '滚', 1),
    ('广告导流类', '微信', 1),
    ('广告导流类', 'wechat', 1),
    ('广告导流类', 'vx', 1),
    ('广告导流类', 'qq', 1),
    ('广告导流类', '电话', 1),
    ('广告导流类', '加好友', 1),
    ('广告导流类', '私聊', 1),
    ('违规承诺类', '包过', 1),
    ('违规承诺类', '代写', 1),
    ('违规承诺类', '作弊', 1),
    ('违规承诺类', '代考', 1),
    ('违规承诺类', '刷分', 1),
    ('涉政涉黄暴力类', '约炮', 1),
    ('涉政涉黄暴力类', '嫖娼', 1),
    ('涉政涉黄暴力类', '枪支', 1),
    ('涉政涉黄暴力类', '爆炸物', 1),
    ('涉政涉黄暴力类', '恐袭', 1),
    ('隐私信息类', '手机号', 1),
    ('隐私信息类', '身份证号', 1),
    ('隐私信息类', '住址', 1),
    ('隐私信息类', '银行卡号', 1),
    ('隐私信息类', '门牌号', 1);