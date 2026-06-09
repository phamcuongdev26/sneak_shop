package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.NotificationResponse;

public interface NotificationService {
    PageResponse<NotificationResponse> getAll(Integer userId, int page, int size);
    long countUnread(Integer userId);
    void markRead(Integer userId, Integer notifId);
    void markAllRead(Integer userId);
    void notifyUser(Integer userId, String title, String body, String type, String imageUrl);
    void notifyAdmins(String title, String body, String type, String imageUrl);
}
