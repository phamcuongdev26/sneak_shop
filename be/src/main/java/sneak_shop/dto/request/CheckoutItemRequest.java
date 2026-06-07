package sneak_shop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
        @NotNull Integer productId,
        Integer variantId,
        Integer colorId,
        @NotNull @Min(1) Integer quantity
) {}
