package sneak_shop.dto.response;

import sneak_shop.entity.NotificationEntity;

import java.time.Instant;

public record NotificationResponse(
        Integer id,
        String title,
        String body,
        String imageUrl,
        String type,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationEntity e) {
        return new NotificationResponse(e.getId(), e.getTitle(), e.getBody(),
                e.getImageUrl(), e.getType(), e.getIsRead(), e.getCreatedAt());
    }
}
