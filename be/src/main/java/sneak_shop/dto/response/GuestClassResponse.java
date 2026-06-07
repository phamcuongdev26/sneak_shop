package sneak_shop.dto.response;

import sneak_shop.entity.GuestClassEntity;
import sneak_shop.enums.CustomerTier;

public record GuestClassResponse(
        Integer id,
        Integer userId,
        String userName,
        CustomerTier rankName,
        Integer productId,
        String productName,
        Integer totalAmount,
        String description,
        String color
) {
    public static GuestClassResponse from(GuestClassEntity e) {
        return new GuestClassResponse(
                e.getId(),
                e.getUser().getId(),
                e.getUser().getFullName(),
                e.getRankName(),
                e.getProduct().getId(),
                e.getProduct().getName(),
                e.getTotalAmount(),
                e.getDescription(),
                e.getColor()
        );
    }
}
