package com.example.cep_backend.profile.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            ensureTradeOrdersColumns();

            createIndexIfMissing(TABLE_USER_FOLLOWS, "idx_user_follows_target",
                    "CREATE INDEX idx_user_follows_target ON user_follows (target_user_id, created_at DESC)");
            createIndexIfMissing(TABLE_USER_FAVORITES, "idx_user_favorites_user",
                    "CREATE INDEX idx_user_favorites_user ON user_favorites (user_id, created_at DESC)");
            createIndexIfMissing(TABLE_USER_CREDIT_REVIEWS, "idx_user_credit_reviews_target",
                    "CREATE INDEX idx_user_credit_reviews_target ON user_credit_reviews (target_user_id, created_at DESC)");
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
                    college VARCHAR(80) NULL,
                    credit_score DECIMAL(3, 1) NULL,
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
        dropColumnIfExists(TABLE_USER_PROFILES, "campus");
        dropColumnIfExists(TABLE_USER_PROFILES, "note");
        if (!columnExists(TABLE_USER_PROFILES, "avatar_url")) {
            jdbcTemplate.execute("ALTER TABLE user_profiles ADD COLUMN avatar_url VARCHAR(500) NULL");
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
