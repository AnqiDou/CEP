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
        jdbcTemplate.execute("ALTER TABLE items ADD COLUMN IF NOT EXISTS publisher_user_id BIGINT NULL");

        jdbcTemplate.execute("""
                UPDATE i
                SET i.publisher_user_id = d.publisher_user_id
                FROM items i
                INNER JOIN item_details d ON d.item_id = i.id
                WHERE i.publisher_user_id IS NULL
                  AND d.publisher_user_id IS NOT NULL;
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

        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_items_publisher ON items (publisher_user_id, created_at DESC)");

        log.info("Publish schema ensured: items.publisher_user_id");
    }
}
