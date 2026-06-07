package sneak_shop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal revenueToday,
        BigDecimal revenueThisMonth,
        Long ordersToday,
        Long newUsersToday,
        Long pendingOrders,
        Long totalProducts,
        Double avgRating,
        Long totalReviews,
        List<DailyRevenue> revenueChart,
        List<OrderSummary> recentOrders
) {
    public record DailyRevenue(String date, BigDecimal revenue, Long orders) {}
    public record OrderSummary(Integer id, String orderCode, String recipientName,
                                BigDecimal totalAmount, String status, String createdAt) {}
}
