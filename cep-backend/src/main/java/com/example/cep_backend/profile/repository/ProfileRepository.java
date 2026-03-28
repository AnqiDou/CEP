package com.example.cep_backend.profile.repository;

import com.example.cep_backend.profile.dto.ProfilePendingTradeDto;
import com.example.cep_backend.profile.dto.OtherProfileItemDto;
import com.example.cep_backend.profile.dto.ProfileReviewItemDto;
import com.example.cep_backend.profile.dto.ProfileTradeItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class ProfileRepository {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ProfileTradeItemDto> tradeItemRowMapper = (rs, rowNum) -> {
        Timestamp timestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
        return new ProfileTradeItemDto(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getString("title"),
                rs.getBigDecimal("price"),
                rs.getString("campus"),
                createdAt.format(DATE_TIME_FORMATTER),
                rs.getString("photo_url"));
    };

    private final RowMapper<ProfilePendingTradeDto> pendingTradeRowMapper = (rs, rowNum) -> {
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new ProfilePendingTradeDto(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getLong("item_id"),
                rs.getString("title"),
                rs.getString("partner"),
                rs.getString("location"),
                createdAt.format(DATE_TIME_FORMATTER),
                rs.getString("status"),
                "待付款");
    };

    private final RowMapper<ProfileReviewItemDto> reviewRowMapper = (rs, rowNum) -> {
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new ProfileReviewItemDto(
                rs.getLong("id"),
                rs.getString("rater_name"),
                rs.getString("rater_avatar"),
                rs.getString("rating"),
                rs.getString("content"),
                createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE));
    };

    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProfileBaseInfo findBaseInfo(Long userId) {
        String sql = """
                SELECT
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS username,
                    up.avatar_url,
                    (SELECT COUNT(1) FROM user_follows uf WHERE uf.target_user_id = u.id) AS fans,
                    (SELECT COUNT(1) FROM user_follows uf WHERE uf.user_id = u.id) AS following
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE u.id = ?
                """;
        List<ProfileBaseInfo> list = jdbcTemplate.query(sql, (rs, rowNum) -> new ProfileBaseInfo(
                rs.getString("username"),
                rs.getString("avatar_url"),
                rs.getLong("fans"),
                rs.getLong("following")), userId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public void ensureUserProfile(Long userId) {
        String sql = """
                IF NOT EXISTS (SELECT 1 FROM user_profiles WHERE user_id = ?)
                BEGIN
                    INSERT INTO user_profiles (user_id, created_at, updated_at)
                    VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                END
                """;
        jdbcTemplate.update(sql, userId, userId);
    }

    public CreditStats findCreditStats(Long userId, String role) {
        String sql = """
                SELECT
                    SUM(CASE WHEN rating = 'good' THEN 1 ELSE 0 END) AS good_count,
                    SUM(CASE WHEN rating = 'bad' THEN 1 ELSE 0 END) AS bad_count
                FROM user_credit_reviews
                WHERE target_user_id = ? AND target_role = ?
                """;
        List<CreditStats> stats = jdbcTemplate.query(sql, (rs, rowNum) -> new CreditStats(
                rs.getInt("good_count"),
                rs.getInt("bad_count")), userId, role);
        if (stats.isEmpty()) {
            return new CreditStats(0, 0);
        }
        return stats.getFirst();
    }

    public List<ProfileReviewItemDto> findReviews(Long userId, String rating) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.id,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS rater_name,
                    up.avatar_url AS rater_avatar,
                    r.rating,
                    r.content,
                    r.created_at
                FROM user_credit_reviews r
                LEFT JOIN users u ON u.id = r.rater_user_id
                LEFT JOIN user_profiles up ON up.user_id = r.rater_user_id
                WHERE r.target_user_id = ?
                """);
        if (!"all".equals(rating)) {
            sql.append(" AND r.rating = ? ");
        }
        sql.append(" ORDER BY r.created_at DESC, r.id DESC ");
        if ("all".equals(rating)) {
            return jdbcTemplate.query(sql.toString(), reviewRowMapper, userId);
        }
        return jdbcTemplate.query(sql.toString(), reviewRowMapper, userId, rating);
    }

    public List<ProfileTradeItemDto> findPublishedItems(Long userId) {
        String sql = """
                SELECT
                    i.id,
                    i.id AS item_id,
                    i.title,
                    i.price,
                    i.campus,
                    i.created_at,
                    (
                        SELECT TOP 1 p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = i.id
                        ORDER BY p.sort_order ASC, p.id ASC
                    ) AS photo_url
                FROM items i
                INNER JOIN item_details d ON d.item_id = i.id
                WHERE d.publisher_user_id = ?
                ORDER BY i.created_at DESC, i.id DESC
                """;
        return jdbcTemplate.query(sql, tradeItemRowMapper, userId);
    }

    public List<ProfileTradeItemDto> findSoldItems(Long userId) {
        String sql = """
                SELECT
                    o.id,
                    o.item_id,
                    o.item_title AS title,
                    o.amount AS price,
                    '' AS campus,
                    o.paid_at AS created_at,
                    o.cover_photo_url AS photo_url
                FROM trade_orders o
                WHERE o.seller_user_id = ? AND o.status = 'PAID'
                ORDER BY o.paid_at DESC, o.id DESC
                """;
        return jdbcTemplate.query(sql, tradeItemRowMapper, userId);
    }

    public List<ProfileTradeItemDto> findBoughtItems(Long userId) {
        String sql = """
                SELECT
                    o.id,
                    o.item_id,
                    o.item_title AS title,
                    o.amount AS price,
                    '' AS campus,
                    o.paid_at AS created_at,
                    o.cover_photo_url AS photo_url
                FROM trade_orders o
                WHERE o.buyer_user_id = ? AND o.status = 'PAID'
                ORDER BY o.paid_at DESC, o.id DESC
                """;
        return jdbcTemplate.query(sql, tradeItemRowMapper, userId);
    }

    public List<ProfileTradeItemDto> findFavoriteItems(Long userId) {
        String sql = """
                SELECT
                    i.id,
                    i.id AS item_id,
                    i.title,
                    i.price,
                    i.campus,
                    f.created_at,
                    (
                        SELECT TOP 1 p.photo_url
                        FROM item_photos p
                        WHERE p.item_id = i.id
                        ORDER BY p.sort_order ASC, p.id ASC
                    ) AS photo_url
                FROM user_favorites f
                INNER JOIN items i ON i.id = f.item_id
                WHERE f.user_id = ?
                ORDER BY f.created_at DESC, f.id DESC
                """;
        return jdbcTemplate.query(sql, tradeItemRowMapper, userId);
    }

    public List<ProfilePendingTradeDto> findPendingPaymentTrades(Long userId) {
        String sql = """
                SELECT
                    o.id,
                    o.id AS order_id,
                    o.item_id,
                    o.item_title AS title,
                    COALESCE(NULLIF(s.username, ''), '校园用户') AS partner,
                    o.receiver_address AS location,
                    o.status,
                    o.created_at
                FROM trade_orders o
                LEFT JOIN users s ON s.id = o.seller_user_id
                WHERE o.buyer_user_id = ? AND o.status = 'PENDING_PAYMENT'
                ORDER BY o.created_at DESC, o.id DESC
                """;
        return jdbcTemplate.query(sql, pendingTradeRowMapper, userId);
    }

    public void updateAvatar(Long userId, String avatar, LocalDateTime now) {
        String sql = "UPDATE user_profiles SET avatar_url = ?, updated_at = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, avatar, Timestamp.valueOf(now), userId);
    }

    public Long findUserIdByUsername(String username) {
        String sql = "SELECT TOP 1 id FROM users WHERE username = ?";
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), username);
        return list.isEmpty() ? null : list.getFirst();
    }

    public boolean userExists(Long userId) {
        String sql = "SELECT COUNT(1) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    public OtherProfileBaseInfo findOtherProfileBase(Long userId) {
        String sql = """
                SELECT
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS username,
                    up.avatar_url,
                    COALESCE(NULLIF(up.campus, ''), '未填写校区') AS city,
                    COALESCE(NULLIF(up.note, ''), '该用户暂未填写简介') AS bio,
                    (SELECT COUNT(1) FROM user_follows uf WHERE uf.target_user_id = u.id) AS fans,
                    (SELECT COUNT(1) FROM user_follows uf WHERE uf.user_id = u.id) AS following
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE u.id = ?
                """;
        List<OtherProfileBaseInfo> list = jdbcTemplate.query(sql, (rs, rowNum) -> new OtherProfileBaseInfo(
                rs.getString("username"),
                rs.getString("avatar_url"),
                rs.getString("city"),
                rs.getString("bio"),
                rs.getLong("fans"),
                rs.getLong("following")), userId);
        return list.isEmpty() ? null : list.getFirst();
    }

    public List<OtherProfileItemDto> findOtherItems(Long userId, String status, String sort) {
        String priceOrder = "price-desc".equals(sort) ? "DESC" : "ASC";
        StringBuilder sql = new StringBuilder();
        if ("onsale".equals(status)) {
            sql.append("""
                    SELECT
                        i.id,
                        i.id AS item_id,
                        i.title,
                        i.price,
                        'onsale' AS status,
                        (
                            SELECT TOP 1 p.photo_url
                            FROM item_photos p
                            WHERE p.item_id = i.id
                            ORDER BY p.sort_order ASC, p.id ASC
                        ) AS image,
                        i.created_at
                    FROM items i
                    INNER JOIN item_details d ON d.item_id = i.id
                    WHERE d.publisher_user_id = ?
                      AND i.status = 'PUBLISHED'
                    """);
        } else if ("sold".equals(status)) {
            sql.append("""
                    SELECT
                        o.id,
                        o.item_id,
                        o.item_title AS title,
                        o.amount AS price,
                        'sold' AS status,
                        o.cover_photo_url AS image,
                        o.paid_at AS created_at
                    FROM trade_orders o
                    WHERE o.seller_user_id = ?
                      AND o.status = 'PAID'
                    """);
        } else {
            sql.append("""
                    SELECT * FROM (
                        SELECT
                            i.id,
                            i.id AS item_id,
                            i.title,
                            i.price,
                            'onsale' AS status,
                            (
                                SELECT TOP 1 p.photo_url
                                FROM item_photos p
                                WHERE p.item_id = i.id
                                ORDER BY p.sort_order ASC, p.id ASC
                            ) AS image,
                            i.created_at
                        FROM items i
                        INNER JOIN item_details d ON d.item_id = i.id
                        WHERE d.publisher_user_id = ?
                          AND i.status = 'PUBLISHED'
                        UNION ALL
                        SELECT
                            o.id,
                            o.item_id,
                            o.item_title AS title,
                            o.amount AS price,
                            'sold' AS status,
                            o.cover_photo_url AS image,
                            o.paid_at AS created_at
                        FROM trade_orders o
                        WHERE o.seller_user_id = ?
                          AND o.status = 'PAID'
                    ) t
                    """);
        }
        sql.append(" ORDER BY price ").append(priceOrder).append(", created_at DESC, id DESC ");

        if ("all".equals(status)) {
            return jdbcTemplate.query(sql.toString(), this::mapOtherItem, userId, userId);
        }
        return jdbcTemplate.query(sql.toString(), this::mapOtherItem, userId);
    }

    public boolean isFollowing(Long userId, Long targetUserId) {
        String sql = "SELECT COUNT(1) FROM user_follows WHERE user_id = ? AND target_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, targetUserId);
        return count != null && count > 0;
    }

    public void followUser(Long userId, Long targetUserId) {
        String sql = """
                IF NOT EXISTS (
                    SELECT 1 FROM user_follows WHERE user_id = ? AND target_user_id = ?
                )
                BEGIN
                    INSERT INTO user_follows (user_id, target_user_id, created_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                END
                """;
        jdbcTemplate.update(sql, userId, targetUserId, userId, targetUserId);
    }

    public void unfollowUser(Long userId, Long targetUserId) {
        String sql = "DELETE FROM user_follows WHERE user_id = ? AND target_user_id = ?";
        jdbcTemplate.update(sql, userId, targetUserId);
    }

    private OtherProfileItemDto mapOtherItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
        return new OtherProfileItemDto(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getString("title"),
                rs.getBigDecimal("price"),
                rs.getString("status"),
                rs.getString("image"),
                createdAt.format(DATE_TIME_FORMATTER));
    }

    public record ProfileBaseInfo(String username, String avatar, long fans, long following) {
    }

    public record OtherProfileBaseInfo(String username, String avatar, String city, String bio, long fans,
            long following) {
    }

    public record CreditStats(int goodCount, int badCount) {
        public int total() {
            return goodCount + badCount;
        }
    }
}
