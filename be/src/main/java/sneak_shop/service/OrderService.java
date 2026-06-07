package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.CheckoutRequest;
import sneak_shop.dto.request.UpdateOrderStatusRequest;
import sneak_shop.dto.response.CheckoutResponse;
import sneak_shop.dto.response.OrderResponse;
import sneak_shop.enums.OrderStatus;

public interface OrderService {
    CheckoutResponse checkout(Integer userId, CheckoutRequest req);
    PageResponse<OrderResponse> getMyOrders(Integer userId, OrderStatus status, int page, int size);
    OrderResponse getMyOrder(Integer userId, String orderCode);
    OrderResponse cancelOrder(Integer userId, String orderCode, String reason);
    OrderResponse confirmReceived(Integer userId, String orderCode);
    PageResponse<OrderResponse> adminGetUserOrders(Integer userId, int page, int size);
    PageResponse<OrderResponse> adminGetAll(OrderStatus status, String keyword, int page, int size);
    OrderResponse adminUpdateStatus(String orderCode, UpdateOrderStatusRequest req);
}
