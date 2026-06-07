package cep_backend.service;

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
public class PaymentSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(PaymentSchemaInitializer.class);
    private static final String TABLE_NAME = "trade_orders";

    private final JdbcTemplate jdbcTemplate;

    public PaymentSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTradeOrdersSchema() {
        try {
            if (!tableExists(TABLE_NAME)) {
                createTradeOrdersTable();
                log.info("Created table: {}", TABLE_NAME);
            }

            createIndexIfMissing("idx_trade_orders_item",
                    "CREATE INDEX idx_trade_orders_item ON trade_orders (item_id, created_at DESC)");
            createIndexIfMissing("idx_trade_orders_status",
                    "CREATE INDEX idx_trade_orders_status ON trade_orders (status, created_at DESC)");

            ensureColumnExists("buyer_confirmed",
                    "ALTER TABLE trade_orders ADD COLUMN buyer_confirmed TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumnExists("seller_confirmed",
                    "ALTER TABLE trade_orders ADD COLUMN seller_confirmed TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumnExists("refund_status",
                    "ALTER TABLE trade_orders ADD COLUMN refund_status VARCHAR(30) NOT NULL DEFAULT 'NONE'");
            ensureColumnExists("refund_type", "ALTER TABLE trade_orders ADD COLUMN refund_type VARCHAR(40) NULL");
            ensureColumnExists("completed_at", "ALTER TABLE trade_orders ADD COLUMN completed_at DATETIME(6) NULL");
            ensureColumnExists("pending_confirmation_at",
                    "ALTER TABLE trade_orders ADD COLUMN pending_confirmation_at DATETIME(6) NULL");
            ensureColumnExists("refund_applied_at",
                    "ALTER TABLE trade_orders ADD COLUMN refund_applied_at DATETIME(6) NULL");
            ensureColumnExists("cancelled_at", "ALTER TABLE trade_orders ADD COLUMN cancelled_at DATETIME(6) NULL");
            ensureColumnExists("seller_name",
                    "ALTER TABLE trade_orders ADD COLUMN seller_name VARCHAR(50) NULL");
            ensureColumnExists("payment_channel",
                    "ALTER TABLE trade_orders ADD COLUMN payment_channel VARCHAR(20) NULL");
            ensureColumnExists("payment_url",
                    "ALTER TABLE trade_orders ADD COLUMN payment_url VARCHAR(300) NULL");
            ensureColumnExists("payment_expire_at",
                    "ALTER TABLE trade_orders ADD COLUMN payment_expire_at DATETIME(6) NULL");
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化支付表结构失败", ex);
        }
    }

    private void createTradeOrdersTable() {
        String ddl = """
                CREATE TABLE trade_orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_no VARCHAR(40) NOT NULL UNIQUE,
                    item_id BIGINT NOT NULL,
                    buyer_user_id BIGINT NULL,
                    seller_user_id BIGINT NULL,
                    item_title VARCHAR(120) NOT NULL,
                    amount DECIMAL(10, 2) NOT NULL,
                    cover_photo_url VARCHAR(500) NULL,
                    receiver_name VARCHAR(50) NOT NULL,
                    receiver_phone VARCHAR(30) NOT NULL,
                    receiver_address VARCHAR(200) NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
                    paid_at DATETIME(6) NULL,
                    seller_name VARCHAR(50) NULL,
                    payment_channel VARCHAR(20) NULL,
                    payment_url VARCHAR(300) NULL,
                    payment_expire_at DATETIME(6) NULL,
                    buyer_confirmed TINYINT(1) NOT NULL DEFAULT 0,
                    seller_confirmed TINYINT(1) NOT NULL DEFAULT 0,
                    refund_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
                    refund_type VARCHAR(40) NULL,
                    pending_confirmation_at DATETIME(6) NULL,
                    refund_applied_at DATETIME(6) NULL,
                    cancelled_at DATETIME(6) NULL,
                    completed_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_trade_orders_item FOREIGN KEY (item_id) REFERENCES items (id),
                    CONSTRAINT fk_trade_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
                    CONSTRAINT fk_trade_orders_seller FOREIGN KEY (seller_user_id) REFERENCES users (id)
                )
                """;
        jdbcTemplate.execute(ddl);
    }

    private void createIndexIfMissing(String indexName, String ddl) throws SQLException {
        if (indexExists(TABLE_NAME, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private void ensureColumnExists(String columnName, String ddl) throws SQLException {
        if (columnExists(TABLE_NAME, columnName)) {
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
