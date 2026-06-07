package sneak_shop.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.security.UserContext;
import sneak_shop.dto.response.NotificationResponse;
import sneak_shop.service.impl.NotificationServiceImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    public NotificationController(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getAll(
            @AuthenticationPrincipal UserContext ctx,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(notificationService.getAll(ctx.id(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserContext ctx) {
        return ApiResponse.ok(Map.of("count", notificationService.countUnread(ctx.id())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal UserContext ctx, @PathVariable Integer id) {
        notificationService.markRead(ctx.id(), id);
        return ApiResponse.ok("Da danh dau da doc");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserContext ctx) {
        notificationService.markAllRead(ctx.id());
        return ApiResponse.ok("Da danh dau tat ca da doc");
    }
}
