package sneak_shop.dto.response;

import sneak_shop.entity.ProductShopEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductShopResponse(
        Integer id,
        String name,
        String avatar,
        Integer productQuantity,
        BigDecimal ratingAverage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductShopResponse from(ProductShopEntity e) {
        return new ProductShopResponse(
                e.getId(),
                e.getName(),
                e.getAvatar(),
                e.getProductQuantity(),
                e.getRatingAverage(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
