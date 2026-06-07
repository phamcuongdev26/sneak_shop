package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.UpdateOrderStatusRequest;
import sneak_shop.dto.response.OrderResponse;
import sneak_shop.enums.OrderStatus;
import sneak_shop.service.OrderService;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(orderService
                .adminGetAll(status, keyword, page, size));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<PageResponse<OrderResponse>> getUserOrders(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(orderService.adminGetUserOrders(userId, page, size));
    }

    @PatchMapping("/{orderCode}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable String orderCode,
                                                   @Valid @RequestBody UpdateOrderStatusRequest req) {
        return ApiResponse.ok(orderService.adminUpdateStatus(orderCode, req));
    }
}
