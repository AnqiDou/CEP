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
        jdbcTemplate.execute("""
                IF COL_LENGTH('dbo.items', 'publisher_user_id') IS NULL
                BEGIN
                    ALTER TABLE items ADD publisher_user_id BIGINT NULL;
                END;
                """);

        jdbcTemplate.execute("""
                UPDATE i
                SET i.publisher_user_id = d.publisher_user_id
                FROM items i
                INNER JOIN item_details d ON d.item_id = i.id
                WHERE i.publisher_user_id IS NULL
                  AND d.publisher_user_id IS NOT NULL;
                """);

        jdbcTemplate.execute("""
                IF NOT EXISTS (
                    SELECT 1
                    FROM sys.foreign_keys
                    WHERE name = 'fk_items_publisher'
                )
                BEGIN
                    ALTER TABLE items
                    ADD CONSTRAINT fk_items_publisher FOREIGN KEY (publisher_user_id) REFERENCES users (id);
                END;
                """);

        jdbcTemplate.execute("""
                IF NOT EXISTS (
                    SELECT 1
                    FROM sys.indexes
                    WHERE name = 'idx_items_publisher'
                      AND object_id = OBJECT_ID('dbo.items')
                )
                BEGIN
                    CREATE INDEX idx_items_publisher ON items (publisher_user_id, created_at DESC);
                END;
                """);

        log.info("Publish schema ensured: items.publisher_user_id");
    }
}
