package com.example.cep_backend.admin.repository;

import com.example.cep_backend.admin.dto.AdminItemDto;
import com.example.cep_backend.admin.dto.AdminNoticeDto;
import com.example.cep_backend.admin.dto.AdminOrderDto;
import com.example.cep_backend.admin.dto.AdminSupportConversationDto;
import com.example.cep_backend.admin.dto.AdminSupportMessageDto;
import com.example.cep_backend.admin.dto.AdminUserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer countTodayNewUsers() {
        String sql = """
                SELECT COUNT(1)
                FROM users
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTotalUsers() {
        String sql = "SELECT COUNT(1) FROM users WHERE status <> 'DELETED'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTodayNewItems() {
        String sql = """
                SELECT COUNT(1)
                FROM items
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTotalItems() {
        String sql = "SELECT COUNT(1) FROM items WHERE status <> 'DELETED'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countTodayOrders() {
        String sql = """
                SELECT COUNT(1)
                FROM trade_orders
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public java.math.BigDecimal sumTodaySales() {
        String sql = """
                SELECT IFNULL(SUM(amount), 0)
                FROM trade_orders
                WHERE CAST(created_at AS DATE) = CAST(CURRENT_TIMESTAMP AS DATE)
                  AND status = 'COMPLETED'
                """;
        return jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class);
    }

    public Integer countPendingItems() {
        String sql = "SELECT COUNT(1) FROM items WHERE status = 'PENDING_REVIEW'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countAbnormalOrders() {
        String sql = "SELECT COUNT(1) FROM trade_orders WHERE status <> 'COMPLETED'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Integer countPendingConversations() {
        String sql = """
                SELECT COUNT(1)
                FROM admin_support_conversations
                WHERE status = 'OPEN'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public Map<String, Integer> countOrderStates() {
        String sql = """
                SELECT status, COUNT(1) AS count_value
                FROM trade_orders
                GROUP BY status
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get("status"));
            Number value = (Number) row.get("count_value");
            result.put(key, value == null ? 0 : value.intValue());
        }
        return result;
    }

    public List<AdminUserDto> listUsers(String keyword, String username, String phone, String email) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String sql = """
                SELECT
                    u.id,
                    COALESCE(NULLIF(u.username, ''), u.email) AS display_name,
                    COALESCE(p.phone, '') AS phone,
                    u.email,
                    u.created_at,
                    COALESCE(p.seller_credit_score, 100.0) AS seller_credit_score,
                    COALESCE(p.buyer_credit_score, 100.0) AS buyer_credit_score,
                    CASE WHEN u.status = 'DISABLED' THEN 1 ELSE 0 END AS disabled,
                    (
                        SELECT COUNT(1) FROM items i
                        WHERE COALESCE(i.publisher_user_id, (
                            SELECT d.publisher_user_id FROM item_details d WHERE d.item_id = i.id ORDER BY d.id DESC LIMIT 1
                        )) = u.id
                          AND i.status <> 'DELETED'
                    ) AS item_count,
                    (
                        SELECT COUNT(1) FROM trade_orders t
                        WHERE t.buyer_user_id = u.id OR t.seller_user_id = u.id
                    ) AS order_count
                FROM users u
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE u.status <> 'DELETED'
                  AND (? = '' OR u.email LIKE ? OR u.username LIKE ? OR p.phone LIKE ?)
                  AND (? = '' OR u.username LIKE ?)
                  AND (? = '' OR p.phone LIKE ?)
                  AND (? = '' OR u.email LIKE ?)
                ORDER BY u.created_at DESC, u.id DESC
                """;
        String like = "%" + normalized + "%";
        String likeUsername = "%" + normalizedUsername + "%";
        String likePhone = "%" + normalizedPhone + "%";
        String likeEmail = "%" + normalizedEmail + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminUserDto(
                rs.getLong("id"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBigDecimal("seller_credit_score"),
                rs.getBigDecimal("buyer_credit_score"),
                rs.getBoolean("disabled"),
                rs.getInt("item_count"),
                rs.getInt("order_count")),
                normalized,
                like,
                like,
                like,
                normalizedUsername,
                likeUsername,
                normalizedPhone,
                likePhone,
                normalizedEmail,
                likeEmail);
    }

    public BigDecimal findUserCreditScore(Long userId, String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        String column = "SELLER".equals(normalizedRole) ? "seller_credit_score" : "buyer_credit_score";
        String sql = """
                SELECT COALESCE(%s, 100.0)
                FROM user_profiles
                WHERE user_id = ?
                """.formatted(column);
        List<BigDecimal> scores = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBigDecimal(1), userId);
        return scores.isEmpty() ? null : scores.getFirst();
    }

    public int upsertUserCreditScore(Long userId, String role, BigDecimal creditScore, LocalDateTime now) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        if (!"SELLER".equals(normalizedRole) && !"BUYER".equals(normalizedRole)) {
            return 0;
        }
        String column = "SELLER".equals(normalizedRole) ? "seller_credit_score" : "buyer_credit_score";
        String sql = """
                INSERT INTO user_profiles (user_id, seller_credit_score, buyer_credit_score, created_at, updated_at)
                VALUES (?, 100.0, 100.0, ?, ?)
                ON DUPLICATE KEY UPDATE
                    %s = ?,
                    updated_at = VALUES(updated_at)
                """.formatted(column);
        Timestamp ts = Timestamp.valueOf(now);
        return jdbcTemplate.update(sql, userId, ts, ts, creditScore);
    }

    public int updateUserStatus(Long userId, String status, LocalDateTime now) {
        String sql = """
                UPDATE users
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), userId);
    }

    public int deleteUser(Long userId, LocalDateTime now) {
        String sql = """
                UPDATE users
                SET status = 'DELETED',
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), userId);
    }

    public List<AdminItemDto> listItems(
            String keyword,
            String title,
            String category,
            String price,
            String publisher,
            String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedCategory = category == null ? "" : category.trim();
        String normalizedPrice = price == null ? "" : price.trim();
        String normalizedPublisher = publisher == null ? "" : publisher.trim();
        String normalizedStatus = status == null ? "all" : status.trim().toLowerCase();
        String itemStatus = mapItemStatus(normalizedStatus);
        String sql = """
                SELECT
                    i.id,
                    i.title,
                    c.name AS category_name,
                    i.price,
                    COALESCE(NULLIF(u.username, ''), u.email, '未知用户') AS owner_name,
                    i.status
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN users u ON u.id = COALESCE(i.publisher_user_id, (
                    SELECT d.publisher_user_id
                    FROM item_details d
                    WHERE d.item_id = i.id
                    ORDER BY d.id DESC
                    LIMIT 1
                ))
                WHERE i.status <> 'DELETED'
                  AND (? = '' OR i.title LIKE ? OR c.name LIKE ? OR COALESCE(u.username, '') LIKE ? OR COALESCE(u.email, '') LIKE ?)
                  AND (? = '' OR i.title LIKE ?)
                  AND (? = '' OR c.name LIKE ?)
                  AND (? = '' OR CAST(i.price AS CHAR) LIKE ?)
                  AND (? = '' OR COALESCE(u.username, '') LIKE ? OR COALESCE(u.email, '') LIKE ?)
                  AND (? = 'ALL' OR i.status = ?)
                ORDER BY i.created_at DESC, i.id DESC
                """;
        String like = "%" + normalizedKeyword + "%";
        String likeTitle = "%" + normalizedTitle + "%";
        String likeCategory = "%" + normalizedCategory + "%";
        String likePrice = "%" + normalizedPrice + "%";
        String likePublisher = "%" + normalizedPublisher + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminItemDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                rs.getString("owner_name"),
                rs.getString("status")),
                normalizedKeyword,
                like,
                like,
                like,
                like,
                normalizedTitle,
                likeTitle,
                normalizedCategory,
                likeCategory,
                normalizedPrice,
                likePrice,
                normalizedPublisher,
                likePublisher,
                likePublisher,
                itemStatus,
                itemStatus);
    }

    public int updateItemStatus(Long itemId, String status, LocalDateTime now) {
        String sql = """
                UPDATE items
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), itemId);
    }

    public int deleteItem(Long itemId, LocalDateTime now) {
        String sql = """
                UPDATE items
                SET status = 'DELETED',
                    updated_at = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;
        return jdbcTemplate.update(sql, Timestamp.valueOf(now), itemId);
    }

    public List<AdminOrderDto> listOrders(
            String keyword,
            String orderNo,
            String buyer,
            String seller,
            String itemTitle,
            String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedOrderNo = orderNo == null ? "" : orderNo.trim();
        String normalizedBuyer = buyer == null ? "" : buyer.trim();
        String normalizedSeller = seller == null ? "" : seller.trim();
        String normalizedItemTitle = itemTitle == null ? "" : itemTitle.trim();
        String normalizedStatus = status == null ? "all" : status.trim().toLowerCase();
        String orderStatus = mapOrderStatus(normalizedStatus);
        String sql = """
                SELECT
                    o.order_no,
                    o.item_title,
                    COALESCE(NULLIF(b.username, ''), b.email, '未知买家') AS buyer_name,
                    COALESCE(NULLIF(s.username, ''), s.email, '未知卖家') AS seller_name,
                    o.amount,
                    o.status
                FROM trade_orders o
                LEFT JOIN users b ON b.id = o.buyer_user_id
                LEFT JOIN users s ON s.id = o.seller_user_id
                WHERE (? = '' OR o.order_no LIKE ? OR o.item_title LIKE ?
                    OR b.email LIKE ? OR b.username LIKE ?
                    OR s.email LIKE ? OR s.username LIKE ?)
                  AND (? = '' OR o.order_no LIKE ?)
                  AND (? = '' OR b.email LIKE ? OR b.username LIKE ?)
                  AND (? = '' OR s.email LIKE ? OR s.username LIKE ?)
                  AND (? = '' OR o.item_title LIKE ?)
                  AND (? = 'ALL' OR o.status = ?)
                ORDER BY o.created_at DESC, o.id DESC
                """;
        String like = "%" + normalizedKeyword + "%";
        String likeOrderNo = "%" + normalizedOrderNo + "%";
        String likeBuyer = "%" + normalizedBuyer + "%";
        String likeSeller = "%" + normalizedSeller + "%";
        String likeItemTitle = "%" + normalizedItemTitle + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminOrderDto(
                rs.getString("order_no"),
                rs.getString("item_title"),
                rs.getString("buyer_name"),
                rs.getString("seller_name"),
                rs.getBigDecimal("amount"),
                rs.getString("status")),
                normalizedKeyword,
                like,
                like,
                like,
                like,
                like,
                like,
                normalizedOrderNo,
                likeOrderNo,
                normalizedBuyer,
                likeBuyer,
                likeBuyer,
                normalizedSeller,
                likeSeller,
                likeSeller,
                normalizedItemTitle,
                likeItemTitle,
                orderStatus,
                orderStatus);
    }

    public int markOrderHandled(String orderNo, LocalDateTime now) {
        String sql = """
                UPDATE trade_orders
                SET status = 'COMPLETED',
                    paid_at = COALESCE(paid_at, ?),
                    updated_at = ?
                WHERE order_no = ?
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        return jdbcTemplate.update(sql, timestamp, timestamp, orderNo);
    }

    public List<AdminSupportConversationDto> listSupportConversations() {
        String sql = """
                SELECT
                    c.id AS conversation_id,
                    c.title,
                    c.report_type,
                    c.reporter_user_id,
                    c.item_id,
                    c.report_content,
                    c.status,
                    c.preview,
                    m.id AS message_id,
                    m.sender_type,
                    m.content,
                    m.image_url,
                    m.created_at,
                    u.email AS reporter_email,
                    u.username AS reporter_username,
                    i.title AS item_title
                FROM admin_support_conversations c
                LEFT JOIN admin_support_messages m ON m.conversation_id = c.id
                LEFT JOIN users u ON u.id = c.reporter_user_id
                LEFT JOIN items i ON i.id = c.item_id
                ORDER BY c.updated_at DESC, c.id DESC, m.created_at ASC, m.id ASC
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Long, ConversationAccumulator> accumulators = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long conversationId = ((Number) row.get("conversation_id")).longValue();
            ConversationAccumulator accumulator = accumulators.computeIfAbsent(
                    conversationId,
                    id -> new ConversationAccumulator(
                            id,
                            String.valueOf(row.get("title")),
                            toNullableString(row.get("report_type")),
                            resolveReporterName(row.get("reporter_username"), row.get("reporter_email")),
                            toNullableLong(row.get("item_id")),
                            toNullableString(row.get("item_title")),
                            toNullableString(row.get("report_content")),
                            toNullableString(row.get("status")),
                            String.valueOf(row.get("preview")),
                            new ArrayList<>()));

            Number messageId = (Number) row.get("message_id");
            if (messageId != null) {
                String senderType = String.valueOf(row.get("sender_type"));
                String from = "ADMIN".equalsIgnoreCase(senderType) ? "管理员" : "用户";
                LocalDateTime createdAt = toLocalDateTime(row.get("created_at"));
                accumulator.messages().add(new AdminSupportMessageDto(
                        messageId.longValue(),
                        from,
                        String.valueOf(row.get("content")),
                        toNullableString(row.get("image_url")),
                        createdAt));
            }
        }

        return accumulators.values().stream()
                .map(item -> new AdminSupportConversationDto(
                        item.id(),
                        item.title(),
                        item.reportType(),
                        item.reporterName(),
                        item.itemId(),
                        item.itemTitle(),
                        item.reportContent(),
                        item.status(),
                        item.preview(),
                        item.messages()))
                .toList();
    }

    public boolean existsSupportConversation(Long conversationId) {
        String sql = "SELECT COUNT(1) FROM admin_support_conversations WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId);
        return count != null && count > 0;
    }

    public Long findReporterUserIdByConversationId(Long conversationId) {
        String sql = "SELECT reporter_user_id FROM admin_support_conversations WHERE id = ? LIMIT 1";
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("reporter_user_id", Long.class),
                conversationId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public int insertSupportMessage(Long conversationId, String senderType, String content, String imageUrl,
            LocalDateTime now) {
        String sql = """
                INSERT INTO admin_support_messages (
                    conversation_id,
                    sender_type,
                    content,
                    image_url,
                    created_at
                ) VALUES (?, ?, ?, ?, ?)
                """;
        return jdbcTemplate.update(sql, conversationId, senderType, content, imageUrl, Timestamp.valueOf(now));
    }

    public int touchConversation(Long conversationId, String preview, LocalDateTime now) {
        String sql = """
                UPDATE admin_support_conversations
                SET preview = ?,
                    status = CASE WHEN status = 'OPEN' THEN 'PROCESSING' ELSE status END,
                    updated_at = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, preview, Timestamp.valueOf(now), conversationId);
    }

    public int updateSupportConversationStatus(Long conversationId, String status, LocalDateTime now) {
        String sql = """
                UPDATE admin_support_conversations
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, status, Timestamp.valueOf(now), conversationId);
    }

    public Long findUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), email);
        return list.isEmpty() ? null : list.getFirst();
    }

    public Long findLatestConversationIdByReporter(Long reporterUserId) {
        String sql = """
                SELECT id
                FROM admin_support_conversations
                WHERE reporter_user_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """;
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), reporterUserId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public Long findActiveConversationIdByReporter(Long reporterUserId) {
        String sql = """
                SELECT id
                FROM admin_support_conversations
                WHERE reporter_user_id = ?
                  AND status IN ('OPEN', 'PROCESSING')
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """;
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), reporterUserId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public Long createSupportConversationForUser(
            Long reporterUserId,
            String title,
            String reportType,
            String reportContent,
            String preview,
            String status,
            LocalDateTime now) {
        String sql = """
                INSERT INTO admin_support_conversations (
                    title,
                    report_type,
                    reporter_user_id,
                    report_content,
                    preview,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, reportType);
            statement.setLong(3, reporterUserId);
            statement.setString(4, reportContent);
            statement.setString(5, preview);
            statement.setString(6, status);
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public List<AdminSupportMessageDto> listSupportMessages(Long conversationId) {
        String sql = """
                SELECT id, sender_type, content, image_url, created_at
                FROM admin_support_messages
                WHERE conversation_id = ?
                ORDER BY created_at ASC, id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String senderType = rs.getString("sender_type");
            String from = "ADMIN".equalsIgnoreCase(senderType) ? "管理员" : "用户";
            return new AdminSupportMessageDto(
                    rs.getLong("id"),
                    from,
                    rs.getString("content"),
                    rs.getString("image_url"),
                    rs.getTimestamp("created_at").toLocalDateTime());
        }, conversationId);
    }

    public List<AdminNoticeDto> listNotices() {
        String sql = """
                SELECT id, content, created_at
                FROM admin_notices
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminNoticeDto(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public int createNotice(String content, LocalDateTime now) {
        String sql = """
                INSERT INTO admin_notices (content, created_at, updated_at)
                VALUES (?, ?, ?)
                """;
        Timestamp timestamp = Timestamp.valueOf(now);
        return jdbcTemplate.update(sql, content, timestamp, timestamp);
    }

    public int deleteNotice(Long noticeId) {
        String sql = "DELETE FROM admin_notices WHERE id = ?";
        return jdbcTemplate.update(sql, noticeId);
    }

    private String mapItemStatus(String status) {
        return switch (status) {
            case "pending" -> "PENDING_REVIEW";
            case "online" -> "PUBLISHED";
            case "offline" -> "OFF_SHELF";
            default -> "ALL";
        };
    }

    private String mapOrderStatus(String status) {
        return switch (status) {
            case "pending-pay" -> "PENDING_PAYMENT";
            case "pending-confirm" -> "PENDING_CONFIRMATION";
            case "completed" -> "COMPLETED";
            case "cancelled" -> "CANCELLED";
            default -> "ALL";
        };
    }

    private record ConversationAccumulator(
            Long id,
            String title,
            String reportType,
            String reporterName,
            Long itemId,
            String itemTitle,
            String reportContent,
            String status,
            String preview,
            List<AdminSupportMessageDto> messages) {
    }

    private String resolveReporterName(Object username, Object email) {
        String u = toNullableString(username);
        if (u != null && !u.isBlank()) {
            return u;
        }
        String e = toNullableString(email);
        if (e == null || e.isBlank()) {
            return "用户";
        }
        return e;
    }

    private Long toNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }

    private String toNullableString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDateTime();
        }
        if (value instanceof CharSequence charSequence) {
            String text = charSequence.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(text.replace(' ', 'T'));
        }
        throw new IllegalStateException("不支持的时间类型: " + value.getClass().getName());
    }
}
