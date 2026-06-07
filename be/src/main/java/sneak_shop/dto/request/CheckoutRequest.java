package sneak_shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sneak_shop.enums.PaymentMethod;

import java.util.List;

public record CheckoutRequest(
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @NotBlank String shippingAddress,
        String shippingWard,
        String shippingDistrict,
        @NotBlank String shippingCity,
        @NotNull PaymentMethod paymentMethod,
        Integer addressId,
        String note,
        List<CheckoutItemRequest> items
) {}
