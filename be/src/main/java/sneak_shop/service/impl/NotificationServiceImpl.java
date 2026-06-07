package sneak_shop.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.NotificationResponse;
import sneak_shop.repository.NotificationRepository;
import sneak_shop.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public PageResponse<NotificationResponse> getAll(Integer userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(NotificationResponse::from));
    }

    public long countUnread(Integer userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Integer userId, Integer notifId) {
        notificationRepository.findById(notifId)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(n -> { n.setIsRead(true); notificationRepository.save(n); });
    }

    @Transactional
    public void markAllRead(Integer userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
