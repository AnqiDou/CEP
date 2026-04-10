package cep_backend.service;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminSchemaInitializer {
        private static final Logger log = LoggerFactory.getLogger(AdminSchemaInitializer.class);

        private final JdbcTemplate jdbcTemplate;

        public AdminSchemaInitializer(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        @PostConstruct
        public void ensureAdminSchema() {
                ensureAdminSupportConversationsTable();
                ensureAdminSupportMessagesTable();
                ensureAdminNoticesTable();
                ensureIndexes();
                log.info("Admin schema ensured: support conversations/messages and notices");
        }

        private void ensureAdminSupportConversationsTable() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.tables
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'admin_support_conversations'
                                                """,
                                Integer.class);
                if (tableCount != null && tableCount > 0) {
                        return;
                }
                jdbcTemplate.execute(
                                """
                                                CREATE TABLE admin_support_conversations (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    title VARCHAR(120) NOT NULL,
                                                    report_type VARCHAR(30) NULL,
                                                    reporter_user_id BIGINT NULL,
                                                    item_id BIGINT NULL,
                                                    report_content VARCHAR(500) NULL,
                                                    preview VARCHAR(200) NOT NULL DEFAULT '',
                                                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                    CONSTRAINT fk_admin_support_conversations_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id),
                                                    CONSTRAINT fk_admin_support_conversations_item FOREIGN KEY (item_id) REFERENCES items (id)
                                                )
                                                """);
        }

        private void ensureAdminSupportMessagesTable() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.tables
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'admin_support_messages'
                                                """,
                                Integer.class);
                if (tableCount != null && tableCount > 0) {
                        ensureAdminSupportMessagesColumns();
                        return;
                }
                jdbcTemplate.execute(
                                """
                                                CREATE TABLE admin_support_messages (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    conversation_id BIGINT NOT NULL,
                                                    sender_type VARCHAR(20) NOT NULL,
                                                    content VARCHAR(500) NOT NULL,
                                                    image_url VARCHAR(500) NULL,
                                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                    CONSTRAINT fk_admin_support_messages_conversation FOREIGN KEY (conversation_id) REFERENCES admin_support_conversations (id)
                                                )
                                                """);
        }

        private void ensureAdminSupportMessagesColumns() {
                Integer imageColumnCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.columns
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'admin_support_messages'
                                                  AND column_name = 'image_url'
                                                """,
                                Integer.class);
                if (imageColumnCount == null || imageColumnCount == 0) {
                        jdbcTemplate.execute(
                                        "ALTER TABLE admin_support_messages ADD COLUMN image_url VARCHAR(500) NULL");
                }
        }

        private void ensureAdminNoticesTable() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.tables
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = 'admin_notices'
                                                """,
                                Integer.class);
                if (tableCount != null && tableCount > 0) {
                        return;
                }
                jdbcTemplate.execute(
                                """
                                                CREATE TABLE admin_notices (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    content VARCHAR(500) NOT NULL,
                                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                                )
                                                """);
        }

        private void ensureIndexes() {
                createIndexIfMissing("admin_support_conversations", "idx_admin_support_conversations_status",
                                "CREATE INDEX idx_admin_support_conversations_status ON admin_support_conversations (status, updated_at DESC)");
                createIndexIfMissing("admin_support_conversations", "idx_admin_support_conversations_reporter",
                                "CREATE INDEX idx_admin_support_conversations_reporter ON admin_support_conversations (reporter_user_id, created_at DESC)");
                createIndexIfMissing("admin_support_conversations", "idx_admin_support_conversations_item",
                                "CREATE INDEX idx_admin_support_conversations_item ON admin_support_conversations (item_id, created_at DESC)");
                createIndexIfMissing("admin_support_messages", "idx_admin_support_messages_conversation",
                                "CREATE INDEX idx_admin_support_messages_conversation ON admin_support_messages (conversation_id, created_at ASC)");
                createIndexIfMissing("admin_notices", "idx_admin_notices_created_at",
                                "CREATE INDEX idx_admin_notices_created_at ON admin_notices (created_at DESC)");
        }

        private void createIndexIfMissing(String tableName, String indexName, String ddl) {
                Integer count = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM information_schema.statistics
                                                WHERE table_schema = DATABASE()
                                                  AND table_name = ?
                                                  AND index_name = ?
                                                """,
                                Integer.class,
                                tableName,
                                indexName);
                if (count == null || count == 0) {
                        jdbcTemplate.execute(ddl);
                }
        }
}
