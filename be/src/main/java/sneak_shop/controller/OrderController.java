package sneak_shop.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.CheckoutRequest;
import sneak_shop.dto.response.CheckoutResponse;
import sneak_shop.dto.response.OrderResponse;
import sneak_shop.enums.OrderStatus;
import sneak_shop.security.UserContext;
import sneak_shop.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal UserContext ctx,
                                                  @Valid @RequestBody CheckoutRequest req) {
        return ApiResponse.ok("Dat hang thanh cong", orderService.checkout(ctx.id(), req));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserContext ctx,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(orderService.getMyOrders(ctx.id(), status, page, size));
    }

    @GetMapping("/{orderCode}")
    public ApiResponse<OrderResponse> getOrder(@AuthenticationPrincipal UserContext ctx,
                                               @PathVariable String orderCode) {
        return ApiResponse.ok(orderService.getMyOrder(ctx.id(), orderCode));
    }

    @PostMapping("/{orderCode}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@AuthenticationPrincipal UserContext ctx,
                                                  @PathVariable String orderCode,
                                                  @RequestParam(required = false) String reason) {
        return ApiResponse.ok("Huy don hang thanh cong", orderService.cancelOrder(ctx.id(), orderCode, reason));
    }

    @PostMapping("/{orderCode}/received")
    public ApiResponse<OrderResponse> confirmReceived(@AuthenticationPrincipal UserContext ctx,
                                                      @PathVariable String orderCode) {
        return ApiResponse.ok("Da xac nhan nhan hang", orderService.confirmReceived(ctx.id(), orderCode));
    }
}
