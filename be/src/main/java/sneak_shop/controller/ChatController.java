package sneak_shop.controller;

import jakarta.transaction.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.ChatMessageEntity;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.ChatRepository;
import sneak_shop.repository.OrderRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.security.UserContext;
import sneak_shop.websocket.RealtimeSocketHub;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

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

    private final ChatRepository chatRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RealtimeSocketHub realtimeSocketHub;

    public ChatController(ChatRepository chatRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          RealtimeSocketHub realtimeSocketHub) {
        this.chatRepository = chatRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.realtimeSocketHub = realtimeSocketHub;
    }

    private void verifyAccess(String orderCode, Integer userId) {
        if (orderCode.startsWith("SUPPORT-")) {
            if (!orderCode.equals("SUPPORT-" + userId))
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong co quyen truy cap");
            return;
        }
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai"));
        if (!order.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Don hang khong ton tai");
    }

    @GetMapping("/{orderCode}")
    @Transactional
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @AuthenticationPrincipal UserContext ctx,
            @PathVariable String orderCode
    ) {
        verifyAccess(orderCode, ctx.id());
        chatRepository.markAdminMessagesAsRead(orderCode);
        List<ChatMessageResponse> messages = chatRepository
                .findByOrderCodeOrderByCreatedAtAsc(orderCode)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
        return ApiResponse.ok(messages);
    }

    @PostMapping("/{orderCode}")
    @Transactional
    public ApiResponse<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserContext ctx,
            @PathVariable String orderCode,
            @RequestBody Map<String, String> body
    ) {
        verifyAccess(orderCode, ctx.id());

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Noi dung tin nhan khong duoc de trong");
        }

        UserEntity user = userRepository.findById(ctx.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Nguoi dung khong ton tai"));

        String senderName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : (user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "Khach hang");

        ChatMessageEntity msg = ChatMessageEntity.builder()
                .orderCode(orderCode)
                .userId(ctx.id())
                .senderRole("USER")
                .senderName(senderName)
                .content(content.trim())
                .build();

        ChatMessageEntity saved = chatRepository.save(msg);
        realtimeSocketHub.afterCommit(() -> {
            realtimeSocketHub.pushChatMessageToUser(ctx.id(), saved);
            realtimeSocketHub.pushChatMessageToAdmins(saved);
        });
        return ApiResponse.ok("Gui tin nhan thanh cong", ChatMessageResponse.from(saved));
    }
}
