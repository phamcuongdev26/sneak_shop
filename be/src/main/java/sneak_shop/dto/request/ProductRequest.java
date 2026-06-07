package sneak_shop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import sneak_shop.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        Integer shopId,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        Integer discountPercent,
        Integer stockQuantity,
        String coverImageUrl,
        String sizeGuideNote,
        List<MediaItem> media,
        List<@Valid ProductVariantRequest> variants,
        ProductStatus status,
        List<Integer> categoryIds
) {}
