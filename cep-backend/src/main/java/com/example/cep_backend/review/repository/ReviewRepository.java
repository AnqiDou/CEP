package com.example.cep_backend.review.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReviewRepository {
    private static final String REVIEW_INVITE_TEXT = "交易已完成，邀请你进行本次交易评价。";

    private final JdbcTemplate jdbcTemplate;

    public ReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ReviewTaskDetail findReviewTaskDetail(Long orderId, Long reviewerUserId) {
        String sql = """
                SELECT TOP 1
                    t.order_id,
                    t.reviewer_user_id,
                    t.target_user_id,
                    t.target_role,
                    t.status,
                    o.item_id,
                    o.item_title,
                    o.cover_photo_url,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS target_user_name
                FROM trade_review_tasks t
                INNER JOIN trade_orders o ON o.id = t.order_id
                LEFT JOIN users u ON u.id = t.target_user_id
                WHERE t.order_id = ? AND t.reviewer_user_id = ?
                """;
        List<ReviewTaskDetail> list = jdbcTemplate.query(sql, (rs, rowNum) -> new ReviewTaskDetail(
                rs.getLong("order_id"),
                rs.getLong("reviewer_user_id"),
                rs.getLong("target_user_id"),
                rs.getString("target_role"),
                rs.getString("status"),
                rs.getLong("item_id"),
                rs.getString("item_title"),
                rs.getString("cover_photo_url"),
                rs.getString("target_user_name")), orderId, reviewerUserId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public void submitReview(Long orderId, Long reviewerUserId, Long targetUserId, String targetRole, String rating,
            String content, LocalDateTime now) {
        String insertSql = """
                INSERT INTO user_credit_reviews (
                    order_id,
                    rater_user_id,
                    target_user_id,
                    target_role,
                    rating,
                    content,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(insertSql,
                orderId,
                reviewerUserId,
                targetUserId,
                targetRole,
                rating,
                content,
                Timestamp.valueOf(now));

        String updateTaskSql = """
                UPDATE trade_review_tasks
                SET status = 'SUBMITTED',
                    reviewed_at = ?,
                    updated_at = ?
                WHERE order_id = ?
                  AND reviewer_user_id = ?
                  AND status = 'PENDING'
                """;
        jdbcTemplate.update(updateTaskSql, Timestamp.valueOf(now), Timestamp.valueOf(now), orderId, reviewerUserId);
    }

    public void ensureReviewInvite(Long orderId, Long itemId, Long buyerUserId, Long sellerUserId) {
        if (orderId == null || itemId == null || buyerUserId == null || sellerUserId == null) {
            return;
        }
        if (buyerUserId <= 0 || sellerUserId <= 0 || buyerUserId.equals(sellerUserId)) {
            return;
        }

        createReviewTaskIfMissing(orderId, buyerUserId, sellerUserId, "seller");
        createReviewTaskIfMissing(orderId, sellerUserId, buyerUserId, "buyer");

        Long conversationId = findConversationId(itemId, buyerUserId, sellerUserId);
        LocalDateTime now = LocalDateTime.now();
        if (conversationId == null) {
            conversationId = createConversation(itemId, buyerUserId, sellerUserId, now);
        } else {
            updateConversationLastInvite(conversationId, now);
        }

        if (conversationId != null && !existsInviteMessage(orderId)) {
            insertInviteMessage(conversationId, sellerUserId, orderId, now);
        }
    }

    private boolean existsInviteMessage(Long orderId) {
        String sql = """
                SELECT COUNT(1)
                FROM message_records
                WHERE message_type = 'REVIEW_INVITE'
                  AND biz_type = 'TRADE_ORDER_REVIEW'
                  AND biz_id = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, orderId);
        return count != null && count > 0;
    }

    private Long findConversationId(Long itemId, Long buyerUserId, Long sellerUserId) {
        String sql = """
                SELECT TOP 1 id
                FROM message_conversations
                WHERE item_id = ?
                  AND buyer_user_id = ?
                  AND seller_user_id = ?
                """;
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), itemId, buyerUserId, sellerUserId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long createConversation(Long itemId, Long buyerUserId, Long sellerUserId, LocalDateTime now) {
        String sql = """
                INSERT INTO message_conversations (
                    item_id,
                    buyer_user_id,
                    seller_user_id,
                    last_message,
                    last_message_type,
                    unread_buyer,
                    unread_seller,
                    last_message_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, 'REVIEW_INVITE', 1, 1, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, itemId);
            statement.setLong(2, buyerUserId);
            statement.setLong(3, sellerUserId);
            statement.setString(4, REVIEW_INVITE_TEXT);
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.setTimestamp(6, Timestamp.valueOf(now));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private void updateConversationLastInvite(Long conversationId, LocalDateTime now) {
        String sql = """
                UPDATE message_conversations
                SET last_message = ?,
                    last_message_type = 'REVIEW_INVITE',
                    unread_buyer = unread_buyer + 1,
                    unread_seller = unread_seller + 1,
                    last_message_at = ?,
                    updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                REVIEW_INVITE_TEXT,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                conversationId);
    }

    private void insertInviteMessage(Long conversationId, Long senderUserId, Long orderId, LocalDateTime now) {
        String sql = """
                INSERT INTO message_records (
                    conversation_id,
                    sender_user_id,
                    message_type,
                    text_content,
                    biz_type,
                    biz_id,
                    created_at
                )
                VALUES (?, ?, 'REVIEW_INVITE', ?, 'TRADE_ORDER_REVIEW', ?, ?)
                """;
        jdbcTemplate.update(sql,
                conversationId,
                senderUserId,
                REVIEW_INVITE_TEXT,
                orderId,
                Timestamp.valueOf(now));
    }

    private void createReviewTaskIfMissing(Long orderId, Long reviewerUserId, Long targetUserId, String targetRole) {
        String sql = """
                IF NOT EXISTS (
                    SELECT 1
                    FROM trade_review_tasks
                    WHERE order_id = ? AND reviewer_user_id = ?
                )
                BEGIN
                    INSERT INTO trade_review_tasks (
                        order_id,
                        reviewer_user_id,
                        target_user_id,
                        target_role,
                        status,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                END
                """;
        jdbcTemplate.update(sql,
                orderId,
                reviewerUserId,
                orderId,
                reviewerUserId,
                targetUserId,
                targetRole);
    }

    public record ReviewTaskDetail(
            Long orderId,
            Long reviewerUserId,
            Long targetUserId,
            String targetRole,
            String status,
            Long itemId,
            String itemTitle,
            String itemCover,
            String targetUserName) {
    }
}
