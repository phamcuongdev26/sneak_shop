package sneak_shop.controller.admin;

import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.ChatMessageEntity;
import sneak_shop.entity.OrderEntity;
import sneak_shop.repository.ChatRepository;
import sneak_shop.repository.OrderRepository;
import sneak_shop.websocket.RealtimeSocketHub;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    record ChatMessageResponse(
            Integer id,
            String orderCode,
            String senderRole,
            String senderName,
            String content,
            Instant createdAt,
            Boolean isRead
    ) {
        static ChatMessageResponse from(ChatMessageEntity e) {
            return new ChatMessageResponse(
                    e.getId(),
                    e.getOrderCode(),
                    e.getSenderRole(),
                    e.getSenderName(),
                    e.getContent(),
                    e.getCreatedAt(),
                    e.getIsRead()
            );
        }
    }

    record ConversationResponse(
            String orderCode,
            String displayName,
            String lastContent,
            String lastSenderRole,
            Long unreadCount,
            Instant lastTime
    ) {}

    private final ChatRepository chatRepository;
    private final OrderRepository orderRepository;
    private final RealtimeSocketHub realtimeSocketHub;

    public AdminChatController(ChatRepository chatRepository,
                               OrderRepository orderRepository,
                               RealtimeSocketHub realtimeSocketHub) {
        this.chatRepository = chatRepository;
        this.orderRepository = orderRepository;
        this.realtimeSocketHub = realtimeSocketHub;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations() {
        List<ConversationResponse> conversations = chatRepository.findConversations()
                .stream()
                .map(row -> new ConversationResponse(
                        (String) row[0],
                        row[1] == null ? (String) row[0] : row[1].toString(),
                        (String) row[4],
                        (String) row[5],
                        row[3] instanceof Number n ? n.longValue() : Long.parseLong(row[3].toString()),
                        row[2] instanceof Instant i ? i : ((java.sql.Timestamp) row[2]).toInstant()
                ))
                .toList();
        return ApiResponse.ok(conversations);
    }

    @GetMapping("/{orderCode}")
    @Transactional
    public ApiResponse<List<ChatMessageResponse>> getMessages(@PathVariable String orderCode) {
        chatRepository.markUserMessagesAsRead(orderCode);
        List<ChatMessageResponse> messages = chatRepository
                .findByOrderCodeOrderByCreatedAtAsc(orderCode)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
        return ApiResponse.ok(messages);
    }

    @PostMapping("/{orderCode}/send")
    @Transactional
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable String orderCode,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Noi dung tin nhan khong duoc de trong");
        }
        ChatMessageEntity msg = ChatMessageEntity.builder()
                .orderCode(orderCode)
                .senderRole("ADMIN")
                .senderName("Sneak Shop")
                .content(content.trim())
                .build();

        ChatMessageEntity saved = chatRepository.save(msg);
        Integer targetUserId = resolveTargetUserId(orderCode);
        realtimeSocketHub.afterCommit(() -> {
            if (targetUserId != null) {
                realtimeSocketHub.pushChatMessageToUser(targetUserId, saved);
            }
            realtimeSocketHub.pushChatMessageToAdmins(saved);
        });
        return ApiResponse.ok("Gui tin nhan thanh cong", ChatMessageResponse.from(saved));
    }

    private Integer resolveTargetUserId(String orderCode) {
        if (orderCode != null && orderCode.startsWith("SUPPORT-")) {
            try {
                return Integer.valueOf(orderCode.substring("SUPPORT-".length()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        OrderEntity order = orderRepository.findByOrderCode(orderCode).orElse(null);
        return order != null ? order.getUser().getId() : null;
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(chatRepository.countByIsReadFalseAndSenderRole("USER"));
    }
}
