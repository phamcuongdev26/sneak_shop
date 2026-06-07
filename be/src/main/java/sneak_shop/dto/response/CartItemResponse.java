package sneak_shop.dto.response;

import sneak_shop.entity.CartItemEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CartItemResponse(
        Integer id,
        Integer productId,
        String productName,
        String productImage,
        Integer variantId,
        String variantName,
        String size,
        Integer colorId,
        String colorName,
        BigDecimal originalPrice,
        Integer discountPercent,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItemEntity e) {
        BigDecimal originalPrice = e.getVariant() != null ? e.getVariant().getPrice() : e.getProduct().getPrice();
        int discount = e.getProduct().getDiscountPercent() != null ? e.getProduct().getDiscountPercent() : 0;
        BigDecimal price = discount > 0
                ? originalPrice.multiply(BigDecimal.ONE.subtract(
                        BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(0, RoundingMode.HALF_UP)
                : originalPrice;
        return new CartItemResponse(
                e.getId(),
                e.getProduct().getId(),
                e.getProduct().getName(),
                e.getProductImage() != null ? e.getProductImage() : e.getProduct().getCoverImageUrl(),
                e.getVariant() != null ? e.getVariant().getId() : null,
                e.getVariant() != null && e.getVariant().getSize() != null ? "Size " + e.getVariant().getSize() : null,
                e.getVariant() != null ? e.getVariant().getSize() : null,
                e.getColor() != null ? e.getColor().getId() : null,
                e.getColor() != null ? e.getColor().getColor() : null,
                originalPrice,
                discount,
                price,
                e.getQuantity(),
                price.multiply(BigDecimal.valueOf(e.getQuantity()))
        );
    }
}
