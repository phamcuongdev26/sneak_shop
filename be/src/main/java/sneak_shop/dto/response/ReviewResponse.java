package sneak_shop.dto.response;

import sneak_shop.entity.ReviewEntity;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Integer id,
        Integer userId,
        String userName,
        Integer orderItemId,
        Integer productId,
        Integer shopId,
        String shopName,
        Integer rating,
        String comment,
        List<Integer> productImageIds,
        List<String> imageUrls,
        String shopReply,
        Instant shopReplyAt,
        String reply,
        Instant replyAt,
        Instant createdAt
) {
    public static ReviewResponse from(ReviewEntity e) {
        List<Integer> productImageIds = e.getImages() == null ? List.of() : e.getImages().stream()
                .filter(img -> img.getProductImage() != null)
                .map(img -> img.getProductImage().getId())
                .toList();
        List<String> imageUrls = e.getImages() == null ? List.of() : e.getImages().stream()
                .filter(img -> img.getProductImage() != null)
                .map(img -> img.getProductImage().getImageUrl())
                .toList();
        return new ReviewResponse(
                e.getId(),
                e.getUser().getId(),
                e.getUser().getFullName(),
                e.getOrderItem() != null ? e.getOrderItem().getId() : null,
                e.getProduct().getId(),
                e.getShop() != null ? e.getShop().getId() : null,
                e.getShop() != null ? e.getShop().getName() : null,
                e.getRating(),
                e.getComment(),
                productImageIds,
                imageUrls,
                e.getShopReply(),
                e.getShopReplyAt(),
                e.getReply(),
                e.getReplyAt(),
                e.getCreatedAt()
        );
    }
}
