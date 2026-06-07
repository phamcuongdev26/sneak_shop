package sneak_shop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantRequest(
        @NotBlank String size,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        String sku,
        List<@jakarta.validation.Valid ColorRequest> colors
) {
    public record ColorRequest(
            @NotBlank String color,
            Integer stockQuantity,
            String imageUrl
    ) {}
}
