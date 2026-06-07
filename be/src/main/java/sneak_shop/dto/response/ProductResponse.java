package sneak_shop.dto.response;

import sneak_shop.dto.request.MediaItem;
import sneak_shop.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Integer id,
        Integer shopId,
        String shopName,
        String name,
        String slug,
        String description,
        BigDecimal price,
        Integer discountPercent,
        Integer stockQuantity,
        String coverImageUrl,
        String sizeGuideNote,
        List<MediaItem> media,
        List<String> colors,
        ProductStatus status,
        Instant createdAt,
        String createdBy,
        String updatedBy,
        List<CategorySummary> categories,
        List<VariantSummary> variants,
        Double avgRating,
        Long reviewCount,
        Long soldCount,
        boolean deleted
) {
    public record CategorySummary(Integer id, String name, String slug) {}
    public record ColorSummary(Integer id, String color, Integer stockQuantity, String imageUrl) {}
    public record VariantSummary(Integer id, String size, List<ColorSummary> colors, BigDecimal price, String sku) {}
}
