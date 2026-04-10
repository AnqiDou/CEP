package cep_backend.dto;
import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardDto(
        Integer todayNewUsers,
        Integer totalUsers,
        Integer todayNewItems,
        Integer totalItems,
        Integer todayOrders,
        BigDecimal todaySales,
        Integer pendingItemCount,
        Integer abnormalOrderCount,
        Integer pendingConversationCount,
        List<AdminOrderStateStatDto> orderStateStats) {
}
