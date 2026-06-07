package sneak_shop.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sneak_shop.enums.CustomerTier;

public record GuestClassRequest(
        @NotNull Integer userId,
        @NotNull CustomerTier rankName,
        @NotNull Integer productId,
        @Min(0) int totalAmount,
        @NotBlank @Size(max = 25) String description,
        @NotBlank @Size(max = 100) String color
) {}
