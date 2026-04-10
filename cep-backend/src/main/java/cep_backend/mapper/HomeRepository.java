package cep_backend.mapper;
import cep_backend.entity.po.HomeCategoryRecord;
import cep_backend.entity.po.HomeItemRecord;
import cep_backend.entity.po.HotKeywordRecord;
import cep_backend.dto.HomeNoticeDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HomeRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<HomeCategoryRecord> categoryRowMapper = (rs, rowNum) -> new HomeCategoryRecord(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("tags"));

    private final RowMapper<HomeItemRecord> itemRowMapper = (rs, rowNum) -> new HomeItemRecord(
            rs.getLong("id"),
            rs.getLong("category_id"),
            rs.getLong("publisher_user_id"),
            rs.getBoolean("is_self"),
            rs.getString("seller_name"),
            rs.getString("seller_avatar_url"),
            rs.getBigDecimal("seller_credit_score"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getString("badge"),
            rs.getString("ops_columns"),
            rs.getString("photo_url"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final RowMapper<HotKeywordRecord> hotKeywordRowMapper = (rs, rowNum) -> new HotKeywordRecord(
            rs.getString("keyword"),
            rs.getLong("search_count"));

    private final RowMapper<HomeNoticeDto> homeNoticeRowMapper = (rs, rowNum) -> new HomeNoticeDto(
            rs.getLong("id"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toLocalDateTime());

    public HomeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HomeCategoryRecord> findAllCategories() {
        String sql = """
                SELECT id, code, name, description, tags
                FROM item_categories
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbcTemplate.query(sql, categoryRowMapper);
    }

    public long countItems(String keyword,
            Long categoryId,
            String opsColumn,
            Long currentUserId,
            boolean selfOnly,
            boolean othersOnly) {
        List<Object> args = new ArrayList<>();
        String sql = buildBaseItemSql(keyword, categoryId, opsColumn, currentUserId, selfOnly, othersOnly, args, true);
        Long total = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    public List<HomeItemRecord> findItems(String keyword,
            Long categoryId,
            String opsColumn,
            Long currentUserId,
            boolean selfOnly,
            boolean othersOnly,
            String orderBy,
            String order,
            int offset,
            int limit) {
        List<Object> args = new ArrayList<>();
        String sql = buildBaseItemSql(keyword, categoryId, opsColumn, currentUserId, selfOnly, othersOnly, args, false)
                + " ORDER BY is_self ASC, " + orderBy + " " + order + " LIMIT ?, ?";
        args.add(offset);
        args.add(limit);
        return jdbcTemplate.query(sql, itemRowMapper, args.toArray());
    }

    public List<HomeItemRecord> findItemsByHotPriority(String keyword,
            Long categoryId,
            String opsColumn,
            Long currentUserId,
            boolean selfOnly,
            boolean othersOnly,
            int offset,
            int limit) {
        List<Object> args = new ArrayList<>();
        String sql = buildBaseItemSql(keyword, categoryId, opsColumn, currentUserId, selfOnly, othersOnly, args, false)
                + " ORDER BY is_self ASC, "
                + "CASE "
                + "WHEN c.code = 'daily' THEN 1 "
                + "WHEN c.code IN ('book', 'stationery') THEN 2 "
                + "WHEN c.code = 'digital' THEN 3 "
                + "WHEN c.code = 'clothes' THEN 4 "
                + "WHEN c.code = 'beauty' THEN 5 "
                + "ELSE 6 END ASC, "
                + "i.created_at DESC LIMIT ?, ?";
        args.add(offset);
        args.add(limit);
        return jdbcTemplate.query(sql, itemRowMapper, args.toArray());
    }

    public List<HomeItemRecord> findHotItems(int limit, Long currentUserId) {
        String isSelfExpr = currentUserId == null
                ? "0"
                : "CASE WHEN COALESCE(i.publisher_user_id, d.publisher_user_id) = ? THEN 1 ELSE 0 END";
        String sql = """
                SELECT
                    i.id,
                    i.category_id,
                    COALESCE(i.publisher_user_id, d.publisher_user_id) AS publisher_user_id,
                    %s AS is_self,
                    COALESCE(NULLIF(u.username, ''), '校园用户') AS seller_name,
                    up.avatar_url AS seller_avatar_url,
                    COALESCE(up.seller_credit_score, 100.0) AS seller_credit_score,
                    c.code AS category_code,
                    c.name AS category_name,
                    i.title,
                    i.description,
                    i.price,
                    i.badge,
                    (
                        SELECT GROUP_CONCAT(ioc.column_code ORDER BY ioc.column_code SEPARATOR ',')
                        FROM item_ops_columns ioc
                        WHERE ioc.item_id = i.id
                    ) AS ops_columns,
                    (
                        SELECT ip.photo_url
                        FROM item_photos ip
                        WHERE ip.item_id = i.id
                        ORDER BY ip.sort_order ASC, ip.id ASC
                        LIMIT 1
                    ) AS photo_url,
                    i.created_at
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN item_details d ON d.item_id = i.id
                LEFT JOIN users u ON u.id = COALESCE(i.publisher_user_id, d.publisher_user_id)
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE i.status = 'PUBLISHED'
                ORDER BY is_self ASC, (i.favorite_count * 6 + i.view_count) DESC, i.created_at DESC
                LIMIT ?
                """.formatted(isSelfExpr);
        if (currentUserId == null) {
            return jdbcTemplate.query(sql, itemRowMapper, limit);
        }
        return jdbcTemplate.query(sql, itemRowMapper, currentUserId, limit);
    }

    public List<HotKeywordRecord> findHotKeywords(int limit) {
        String sql = """
                SELECT keyword, search_count
                FROM search_keywords
                ORDER BY search_count DESC, updated_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, hotKeywordRowMapper, limit);
    }

    public List<HomeNoticeDto> findHomeNotices(int limit) {
        String sql = """
                SELECT id, content, created_at
                FROM admin_notices
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, homeNoticeRowMapper, limit);
    }

    public void recordSearchKeyword(String keyword, LocalDateTime now) {
        String updateSql = "UPDATE search_keywords SET search_count = search_count + 1, last_searched_at = ?, updated_at = ? WHERE keyword = ?";
        int updatedRows = jdbcTemplate.update(updateSql, now, now, keyword);
        if (updatedRows > 0) {
            return;
        }

        String insertSql = "INSERT INTO search_keywords (keyword, search_count, last_searched_at, created_at, updated_at) VALUES (?, 1, ?, ?, ?)";
        jdbcTemplate.update(insertSql, keyword, now, now, now);
    }

    private String buildBaseItemSql(String keyword,
            Long categoryId,
            String opsColumn,
            Long currentUserId,
            boolean selfOnly,
            boolean othersOnly,
            List<Object> args,
            boolean countOnly) {
        StringBuilder sql = new StringBuilder();
        String isSelfExpr = currentUserId == null
                ? "0"
                : "CASE WHEN COALESCE(i.publisher_user_id, d.publisher_user_id) = ? THEN 1 ELSE 0 END";
        if (countOnly) {
            sql.append("SELECT COUNT(1) ");
        } else {
            sql.append("""
                    SELECT
                        i.id,
                        i.category_id,
                        COALESCE(i.publisher_user_id, d.publisher_user_id) AS publisher_user_id,
                        %s AS is_self,
                        COALESCE(NULLIF(u.username, ''), '校园用户') AS seller_name,
                        up.avatar_url AS seller_avatar_url,
                        COALESCE(up.seller_credit_score, 100.0) AS seller_credit_score,
                        c.code AS category_code,
                        c.name AS category_name,
                        i.title,
                        i.description,
                        i.price,
                        i.badge,
                        (
                            SELECT GROUP_CONCAT(ioc.column_code ORDER BY ioc.column_code SEPARATOR ',')
                            FROM item_ops_columns ioc
                            WHERE ioc.item_id = i.id
                        ) AS ops_columns,
                        (
                            SELECT ip.photo_url
                            FROM item_photos ip
                            WHERE ip.item_id = i.id
                            ORDER BY ip.sort_order ASC, ip.id ASC
                            LIMIT 1
                        ) AS photo_url,
                        i.created_at
                    """.formatted(isSelfExpr));
            if (currentUserId != null) {
                args.add(currentUserId);
            }
        }

        sql.append("""
                FROM items i
                INNER JOIN item_categories c ON c.id = i.category_id
                LEFT JOIN item_details d ON d.item_id = i.id
                LEFT JOIN users u ON u.id = COALESCE(i.publisher_user_id, d.publisher_user_id)
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE i.status = 'PUBLISHED'
                """);

        if (categoryId != null) {
            sql.append(" AND i.category_id = ?");
            args.add(categoryId);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (i.title LIKE ? OR i.description LIKE ? OR c.name LIKE ?)");
            String likeValue = "%" + keyword + "%";
            args.add(likeValue);
            args.add(likeValue);
            args.add(likeValue);
        }

        if (opsColumn != null && !opsColumn.isBlank()) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM item_ops_columns ioc WHERE ioc.item_id = i.id AND ioc.column_code = ?)");
            args.add(opsColumn);
            if ("campus-bargain".equals(opsColumn)) {
                sql.append(" AND i.price < ?");
                args.add(15);
            }
        }

        if (selfOnly && currentUserId != null) {
            sql.append(" AND COALESCE(i.publisher_user_id, d.publisher_user_id) = ?");
            args.add(currentUserId);
        } else if (othersOnly && currentUserId != null) {
            sql.append(" AND COALESCE(i.publisher_user_id, d.publisher_user_id) <> ?");
            args.add(currentUserId);
        }
        return sql.toString();
    }
}
