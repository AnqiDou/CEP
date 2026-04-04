package com.example.cep_backend.message.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class MessageNotificationSchemaInitializer {
    private static final String TABLE_MESSAGE_NOTIFICATIONS = "message_notifications";

    private final JdbcTemplate jdbcTemplate;

    public MessageNotificationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        try {
            ensureMessageNotificationsTable();
            createIndexIfMissing(
                    TABLE_MESSAGE_NOTIFICATIONS,
                    "idx_message_notifications_user",
                    "CREATE INDEX idx_message_notifications_user ON message_notifications (user_id, is_read, created_at DESC)");
            createIndexIfMissing(
                    TABLE_MESSAGE_NOTIFICATIONS,
                    "idx_message_notifications_item",
                    "CREATE INDEX idx_message_notifications_item ON message_notifications (related_item_id, created_at DESC)");
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化通知消息表失败", ex);
        }
    }

    private void ensureMessageNotificationsTable() throws SQLException {
        if (tableExists(TABLE_MESSAGE_NOTIFICATIONS)) {
            return;
        }
        String ddl = """
                CREATE TABLE message_notifications (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    notification_type VARCHAR(40) NOT NULL,
                    title VARCHAR(120) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    related_item_id BIGINT NULL,
                    related_user_id BIGINT NULL,
                    is_read TINYINT(1) NOT NULL DEFAULT 0,
                    read_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    CONSTRAINT fk_message_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
                    CONSTRAINT fk_message_notifications_item FOREIGN KEY (related_item_id) REFERENCES items (id),
                    CONSTRAINT fk_message_notifications_related_user FOREIGN KEY (related_user_id) REFERENCES users (id)
                )
                """;
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

    private void createIndexIfMissing(String tableName, String indexName, String ddl) throws SQLException {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
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
