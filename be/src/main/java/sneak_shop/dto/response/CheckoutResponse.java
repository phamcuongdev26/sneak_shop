package sneak_shop.dto.response;

public record CheckoutResponse(
        OrderResponse order,
        String paymentUrl   // null nếu COD, có URL nếu VNPAY
) {}
