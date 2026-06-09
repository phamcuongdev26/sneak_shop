package sneak_shop.controller.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.OrderEntity;
import sneak_shop.dto.response.DashboardResponse;
import sneak_shop.dto.response.DashboardResponse.*;
import sneak_shop.enums.OrderStatus;
import sneak_shop.repository.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public AdminDashboardController(OrderRepository orderRepository,
                                    UserRepository userRepository,
                                    ProductRepository productRepository,
                                    ReviewRepository reviewRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse> getDashboard(@RequestParam(defaultValue = "7") int days) {
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vn);
        Instant todayStart = today.atStartOfDay(vn).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(vn).toInstant();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(vn).toInstant();
        Instant chartFrom = today.minusDays(days - 1).atStartOfDay(vn).toInstant();

        BigDecimal revenueToday = orderRepository.sumRevenueBetween(todayStart, todayEnd, OrderStatus.cancelled);
        BigDecimal revenueMonth = orderRepository.sumRevenueBetween(monthStart, todayEnd, OrderStatus.cancelled);
        Long ordersToday = orderRepository.countOrdersBetween(todayStart, todayEnd);
        Long newUsers = userRepository.countNewBetween(todayStart, todayEnd);
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.pending);
        Long totalProducts = productRepository.count();

        Double avgRating = reviewRepository.avgRatingAll();
        Long totalReviews = reviewRepository.count();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<DailyRevenue> chart = new ArrayList<>();
        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        Map<LocalDate, Long> ordersByDay = new HashMap<>();
        for (OrderEntity order : orderRepository.findByCreatedAtGreaterThanEqual(chartFrom)) {
            LocalDate date = order.getCreatedAt().atZone(vn).toLocalDate();
            ordersByDay.merge(date, 1L, Long::sum);
            if (order.getStatus() != OrderStatus.cancelled) {
                revenueByDay.merge(date,
                        order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        }
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal rev = revenueByDay.getOrDefault(d, BigDecimal.ZERO);
            Long cnt = ordersByDay.getOrDefault(d, 0L);
            chart.add(new DailyRevenue(d.format(fmt), rev, cnt));
        }

        List<OrderSummary> recent = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8))
                .getContent().stream().map(o -> new OrderSummary(
                        o.getId(), o.getOrderCode(), o.getRecipientName(),
                        o.getTotalAmount(), o.getStatus().name(),
                        o.getCreatedAt().atZone(vn).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                )).toList();

        return ApiResponse.ok(new DashboardResponse(
                revenueToday, revenueMonth, ordersToday, newUsers,
                pendingOrders, totalProducts, avgRating, totalReviews,
                chart, recent
        ));
    }
}
