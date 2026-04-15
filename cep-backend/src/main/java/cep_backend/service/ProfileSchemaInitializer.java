package cep_backend.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class ProfileSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(ProfileSchemaInitializer.class);
    private static final String TABLE_USER_PROFILES = "user_profiles";
    private static final String TABLE_USER_FOLLOWS = "user_follows";
    private static final String TABLE_USER_FAVORITES = "user_favorites";
    private static final String TABLE_USER_CREDIT_REVIEWS = "user_credit_reviews";
    private static final String TABLE_REVIEW_SENSITIVE_WORDS = "review_sensitive_words";
    private static final String TABLE_TRADE_ORDERS = "trade_orders";

    private final JdbcTemplate jdbcTemplate;

    public ProfileSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureProfileSchema() {
        try {
            ensureUserProfilesTable();
            ensureUserProfilesColumns();
            ensureUserFollowsTable();
            ensureUserFavoritesTable();
            ensureUserCreditReviewsTable();
            ensureReviewSensitiveWordsTable();
            seedReviewSensitiveWords();
            ensureTradeOrdersColumns();

            createIndexIfMissing(TABLE_USER_FOLLOWS, "idx_user_follows_target",
                    "CREATE INDEX idx_user_follows_target ON user_follows (target_user_id, created_at DESC)");
            createIndexIfMissing(TABLE_USER_FAVORITES, "idx_user_favorites_user",
                    "CREATE INDEX idx_user_favorites_user ON user_favorites (user_id, created_at DESC)");
            createIndexIfMissing(TABLE_USER_CREDIT_REVIEWS, "idx_user_credit_reviews_target",
                    "CREATE INDEX idx_user_credit_reviews_target ON user_credit_reviews (target_user_id, created_at DESC)");
            createIndexIfMissing(TABLE_REVIEW_SENSITIVE_WORDS, "idx_review_sensitive_words_category",
                    "CREATE INDEX idx_review_sensitive_words_category ON review_sensitive_words (category, enabled)");
            createIndexIfMissing(TABLE_TRADE_ORDERS, "idx_trade_orders_buyer",
                    "CREATE INDEX idx_trade_orders_buyer ON trade_orders (buyer_user_id, created_at DESC)");
            createIndexIfMissing(TABLE_TRADE_ORDERS, "idx_trade_orders_seller",
                    "CREATE INDEX idx_trade_orders_seller ON trade_orders (seller_user_id, created_at DESC)");
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化个人中心表结构失败", ex);
        }
    }

    private void ensureUserProfilesTable() throws SQLException {
        if (tableExists(TABLE_USER_PROFILES)) {
            return;
        }
        String ddl = """
                CREATE TABLE user_profiles (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL UNIQUE,
                    name VARCHAR(50) NULL,
                    phone VARCHAR(30) NULL,
                    address VARCHAR(200) NULL,
                    seller_credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0,
                    buyer_credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0,
                    avatar_url VARCHAR(500) NULL,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
                )
                """;
        jdbcTemplate.execute(ddl);
        log.info("Created table: {}", TABLE_USER_PROFILES);
    }

    private void ensureUserProfilesColumns() throws SQLException {
        dropColumnIfExists(TABLE_USER_PROFILES, "college");
        dropColumnIfExists(TABLE_USER_PROFILES, "campus");
        dropColumnIfExists(TABLE_USER_PROFILES, "note");
        migrateCreditScoreColumnsSafely();
        if (!columnExists(TABLE_USER_PROFILES, "avatar_url")) {
            jdbcTemplate.execute("ALTER TABLE user_profiles ADD COLUMN avatar_url VARCHAR(500) NULL");
        }
        if (!columnExists(TABLE_USER_PROFILES, "phone")) {
            jdbcTemplate.execute("ALTER TABLE user_profiles ADD COLUMN phone VARCHAR(30) NULL");
        }
        if (!columnExists(TABLE_USER_PROFILES, "name")) {
            jdbcTemplate.execute("ALTER TABLE user_profiles ADD COLUMN name VARCHAR(50) NULL");
        }
        if (!columnExists(TABLE_USER_PROFILES, "address")) {
            jdbcTemplate.execute("ALTER TABLE user_profiles ADD COLUMN address VARCHAR(200) NULL");
        }
    }

    private void migrateCreditScoreColumnsSafely() {
        try {
            if (!columnExists(TABLE_USER_PROFILES, "seller_credit_score")) {
                jdbcTemplate
                        .execute(
                                "ALTER TABLE user_profiles ADD COLUMN seller_credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0");
            }
            if (!columnExists(TABLE_USER_PROFILES, "buyer_credit_score")) {
                jdbcTemplate
                        .execute(
                                "ALTER TABLE user_profiles ADD COLUMN buyer_credit_score DECIMAL(10, 1) NOT NULL DEFAULT 100.0");
            }

            if (columnExists(TABLE_USER_PROFILES, "credit_score")) {
                jdbcTemplate.execute("""
                        UPDATE user_profiles
                        SET seller_credit_score = COALESCE(credit_score, seller_credit_score, 100.0),
                            buyer_credit_score = COALESCE(credit_score, buyer_credit_score, 100.0)
                        """);
                dropColumnIfExists(TABLE_USER_PROFILES, "credit_score");
            }
        } catch (SQLException | DataAccessException ex) {
            log.warn("Skip migrating user_profiles credit columns: {}", ex.getMessage());
        }
    }

    private void dropColumnIfExists(String tableName, String columnName) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
        } catch (org.springframework.jdbc.BadSqlGrammarException ex) {
            if (isDropMissingColumnError(ex, columnName)) {
                return;
            }
            throw ex;
        }
    }

    private boolean isDropMissingColumnError(Throwable throwable, String columnName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.sql.SQLSyntaxErrorException sqlException) {
                String message = sqlException.getMessage();
                if (message != null
                        && message.contains("Can't DROP")
                        && message.toLowerCase(Locale.ROOT).contains(columnName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void ensureUserFollowsTable() throws SQLException {
        if (tableExists(TABLE_USER_FOLLOWS)) {
            return;
        }
        String ddl = """
                CREATE TABLE user_follows (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    target_user_id BIGINT NOT NULL,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uq_user_follows UNIQUE (user_id, target_user_id),
                    CONSTRAINT fk_user_follows_user FOREIGN KEY (user_id) REFERENCES users (id),
                    CONSTRAINT fk_user_follows_target FOREIGN KEY (target_user_id) REFERENCES users (id)
                )
                """;
        jdbcTemplate.execute(ddl);
        log.info("Created table: {}", TABLE_USER_FOLLOWS);
    }

    private void ensureUserFavoritesTable() throws SQLException {
        if (tableExists(TABLE_USER_FAVORITES)) {
            return;
        }
        String ddl = """
                CREATE TABLE user_favorites (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    item_id BIGINT NOT NULL,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uq_user_favorites UNIQUE (user_id, item_id),
                    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
                    CONSTRAINT fk_user_favorites_item FOREIGN KEY (item_id) REFERENCES items (id)
                )
                """;
        jdbcTemplate.execute(ddl);
        log.info("Created table: {}", TABLE_USER_FAVORITES);
    }

    private void ensureUserCreditReviewsTable() throws SQLException {
        if (tableExists(TABLE_USER_CREDIT_REVIEWS)) {
            return;
        }
        String ddl = """
                CREATE TABLE user_credit_reviews (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_id BIGINT NULL,
                    rater_user_id BIGINT NOT NULL,
                    target_user_id BIGINT NOT NULL,
                    target_role VARCHAR(20) NOT NULL,
                    rating VARCHAR(10) NOT NULL,
                    content VARCHAR(300) NULL,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_user_credit_reviews_order FOREIGN KEY (order_id) REFERENCES trade_orders (id),
                    CONSTRAINT fk_user_credit_reviews_rater FOREIGN KEY (rater_user_id) REFERENCES users (id),
                    CONSTRAINT fk_user_credit_reviews_target FOREIGN KEY (target_user_id) REFERENCES users (id)
                )
                """;
        jdbcTemplate.execute(ddl);
        log.info("Created table: {}", TABLE_USER_CREDIT_REVIEWS);
    }

    private void ensureReviewSensitiveWordsTable() throws SQLException {
        if (tableExists(TABLE_REVIEW_SENSITIVE_WORDS)) {
            return;
        }
        String ddl = """
                CREATE TABLE review_sensitive_words (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    category VARCHAR(50) NOT NULL,
                    word VARCHAR(50) NOT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    CONSTRAINT uq_review_sensitive_words_word UNIQUE (word)
                )
                """;
        jdbcTemplate.execute(ddl);
        log.info("Created table: {}", TABLE_REVIEW_SENSITIVE_WORDS);
    }

    private void seedReviewSensitiveWords() {
        insertSensitiveWordIfMissing("辱骂人身攻击类", "傻逼");
        insertSensitiveWordIfMissing("辱骂人身攻击类", "垃圾");
        insertSensitiveWordIfMissing("辱骂人身攻击类", "废物");
        insertSensitiveWordIfMissing("辱骂人身攻击类", "脑残");
        insertSensitiveWordIfMissing("辱骂人身攻击类", "滚");

        insertSensitiveWordIfMissing("广告导流类", "微信");
        insertSensitiveWordIfMissing("广告导流类", "wechat");
        insertSensitiveWordIfMissing("广告导流类", "vx");
        insertSensitiveWordIfMissing("广告导流类", "qq");
        insertSensitiveWordIfMissing("广告导流类", "电话");
        insertSensitiveWordIfMissing("广告导流类", "加好友");
        insertSensitiveWordIfMissing("广告导流类", "私聊");

        insertSensitiveWordIfMissing("违规承诺类", "包过");
        insertSensitiveWordIfMissing("违规承诺类", "代写");
        insertSensitiveWordIfMissing("违规承诺类", "作弊");
        insertSensitiveWordIfMissing("违规承诺类", "代考");
        insertSensitiveWordIfMissing("违规承诺类", "刷分");

        insertSensitiveWordIfMissing("涉政涉黄暴力类", "约炮");
        insertSensitiveWordIfMissing("涉政涉黄暴力类", "嫖娼");
        insertSensitiveWordIfMissing("涉政涉黄暴力类", "枪支");
        insertSensitiveWordIfMissing("涉政涉黄暴力类", "爆炸物");
        insertSensitiveWordIfMissing("涉政涉黄暴力类", "恐袭");

        insertSensitiveWordIfMissing("隐私信息类", "手机号");
        insertSensitiveWordIfMissing("隐私信息类", "身份证号");
        insertSensitiveWordIfMissing("隐私信息类", "住址");
        insertSensitiveWordIfMissing("隐私信息类", "银行卡号");
        insertSensitiveWordIfMissing("隐私信息类", "门牌号");
    }

    private void insertSensitiveWordIfMissing(String category, String word) {
        String sql = """
                INSERT INTO review_sensitive_words (category, word, enabled, created_at, updated_at)
                SELECT ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM review_sensitive_words
                    WHERE word = ?
                )
                """;
        jdbcTemplate.update(sql, category, word, word);
    }

    private void ensureTradeOrdersColumns() throws SQLException {
        if (!tableExists(TABLE_TRADE_ORDERS)) {
            return;
        }
        if (!columnExists(TABLE_TRADE_ORDERS, "buyer_user_id")) {
            jdbcTemplate.execute("ALTER TABLE trade_orders ADD buyer_user_id BIGINT NULL");
        }
        if (!columnExists(TABLE_TRADE_ORDERS, "seller_user_id")) {
            jdbcTemplate.execute("ALTER TABLE trade_orders ADD COLUMN seller_user_id BIGINT NULL");
            jdbcTemplate.execute("""
                    UPDATE trade_orders o
                    INNER JOIN item_details d ON d.item_id = o.item_id
                    SET o.seller_user_id = d.publisher_user_id
                    WHERE o.seller_user_id IS NULL
                    """);
        }
    }

    private void createIndexIfMissing(String tableName, String indexName, String ddl) throws SQLException {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private boolean tableExists(String tableName) throws SQLException {
        return matchMetadataTableName(tableName, (metaData, normalizedName) -> {
            try (ResultSet tables = metaData.getTables(null, null, normalizedName, new String[] { "TABLE" })) {
                return tables.next();
            }
        });
    }

    private boolean indexExists(String tableName, String indexName) throws SQLException {
        return matchMetadataTableName(tableName, (metaData, normalizedTableName) -> {
            try (ResultSet indexes = metaData.getIndexInfo(null, null, normalizedTableName, false, false)) {
                while (indexes.next()) {
                    String current = indexes.getString("INDEX_NAME");
                    if (current != null && current.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        return matchMetadataTableName(tableName, (metaData, normalizedTableName) -> {
            try (ResultSet columns = metaData.getColumns(null, null, normalizedTableName, columnName)) {
                if (columns.next()) {
                    return true;
                }
            }
            try (ResultSet columns = metaData.getColumns(null, null, normalizedTableName,
                    columnName.toUpperCase(Locale.ROOT))) {
                if (columns.next()) {
                    return true;
                }
            }
            try (ResultSet columns = metaData.getColumns(null, null, normalizedTableName,
                    columnName.toLowerCase(Locale.ROOT))) {
                return columns.next();
            }
        });
    }

    private boolean matchMetadataTableName(String tableName, MetadataMatcher matcher) throws SQLException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not configured");
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String normalized = isStoresLowerCase(metaData)
                    ? tableName.toLowerCase(Locale.ROOT)
                    : tableName.toUpperCase(Locale.ROOT);

            if (matcher.match(metaData, normalized)) {
                return true;
            }

            return matcher.match(metaData, tableName);
        }
    }

    private boolean isStoresLowerCase(DatabaseMetaData metaData) throws SQLException {
        return metaData.storesLowerCaseIdentifiers() && !metaData.storesUpperCaseIdentifiers();
    }

    @FunctionalInterface
    private interface MetadataMatcher {
        boolean match(DatabaseMetaData metaData, String tableName) throws SQLException;
    }
}
