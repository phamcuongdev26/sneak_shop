package sneak_shop.dto.request;

import jakarta.validation.constraints.NotNull;
import sneak_shop.enums.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        String cancelReason
) {}
