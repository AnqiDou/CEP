package com.example.cep_backend.publish.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublishSchemaInitializer {
        private static final Logger log = LoggerFactory.getLogger(PublishSchemaInitializer.class);

        private final JdbcTemplate jdbcTemplate;

        public PublishSchemaInitializer(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        @PostConstruct
        public void ensurePublishSchema() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.tables
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'item_ops_columns'
                                                """,
                                Integer.class);
                if (tableCount == null || tableCount == 0) {
                        jdbcTemplate.execute(
                                        """
                                                        CREATE TABLE item_ops_columns (
                                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                            item_id BIGINT NOT NULL,
                                                            column_code VARCHAR(40) NOT NULL,
                                                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                            CONSTRAINT uq_item_ops_columns UNIQUE (item_id, column_code),
                                                            CONSTRAINT fk_item_ops_columns_item FOREIGN KEY (item_id) REFERENCES items (id)
                                                        )
                                                        """);
                }

                Integer tableIndexCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.statistics
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'item_ops_columns'
                                                  AND index_name = 'idx_item_ops_columns_code'
                                                """,
                                Integer.class);
                if (tableIndexCount == null || tableIndexCount == 0) {
                        jdbcTemplate.execute(
                                        "CREATE INDEX idx_item_ops_columns_code ON item_ops_columns (column_code, item_id)");
                }

                Integer columnCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.columns
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'items'
                                                  AND column_name = 'publisher_user_id'
                                                """,
                                Integer.class);
                if (columnCount == null || columnCount == 0) {
                        jdbcTemplate.execute("ALTER TABLE items ADD COLUMN publisher_user_id BIGINT NULL");
                }

                Integer campusColumnCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.columns
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'items'
                                                  AND column_name = 'campus'
                                                """,
                                Integer.class);
                if (campusColumnCount != null && campusColumnCount > 0) {
                        jdbcTemplate.execute("ALTER TABLE items DROP COLUMN campus");
                }

                jdbcTemplate.execute("""
                                UPDATE items i
                                INNER JOIN item_details d ON d.item_id = i.id
                                SET i.publisher_user_id = d.publisher_user_id
                                WHERE i.publisher_user_id IS NULL
                                  AND d.publisher_user_id IS NOT NULL
                                """);

                Integer fkCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.table_constraints
                                                WHERE constraint_schema = DATABASE()
                                                  AND table_name = 'items'
                                                  AND constraint_name = 'fk_items_publisher'
                                                  AND constraint_type = 'FOREIGN KEY'
                                                """,
                                Integer.class);
                if (fkCount == null || fkCount == 0) {
                        jdbcTemplate.execute(
                                        "ALTER TABLE items ADD CONSTRAINT fk_items_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id)");
                }

                Integer indexCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.statistics
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'items'
                                                  AND index_name = 'idx_items_publisher'
                                                """,
                                Integer.class);
                if (indexCount == null || indexCount == 0) {
                        jdbcTemplate.execute(
                                        "CREATE INDEX idx_items_publisher ON items (publisher_user_id, created_at)");
                }

                log.info("Publish schema ensured: items.publisher_user_id");
        }
}
