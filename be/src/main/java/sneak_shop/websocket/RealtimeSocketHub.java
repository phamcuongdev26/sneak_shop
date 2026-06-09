package sneak_shop.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import sneak_shop.dto.response.NotificationResponse;
import sneak_shop.entity.ChatMessageEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimeSocketHub {

    private final ObjectMapper objectMapper;
    private final Map<Integer, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, SessionTarget> sessionTargets = new ConcurrentHashMap<>();

    private record SessionTarget(Integer userId, String role) {}

    public RealtimeSocketHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session, Integer userId, String role) {
        if (session == null || userId == null || role == null) {
            return;
        }
        sessionTargets.put(session.getId(), new SessionTarget(userId, role));
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        if ("admin".equalsIgnoreCase(role)) {
            adminSessions.add(session);
        }
    }

    public void unregister(WebSocketSession session) {
        if (session == null) return;
        SessionTarget target = sessionTargets.remove(session.getId());
        if (target == null) return;

        Set<WebSocketSession> sessions = userSessions.get(target.userId());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(target.userId(), sessions);
            }
        }

        if ("admin".equalsIgnoreCase(target.role())) {
            adminSessions.remove(session);
        }
    }

    public void pushNotificationCreated(Integer userId, NotificationResponse notification, long unreadCount) {
        pushToUser(userId, Map.of(
                "channel", "notification",
                "type", "created",
                "notification", notification,
                "unreadCount", unreadCount
        ));
    }

    public void pushNotificationCount(Integer userId, long unreadCount) {
        pushToUser(userId, Map.of(
                "channel", "notification",
                "type", "count",
                "unreadCount", unreadCount
        ));
    }

    public void pushChatMessageToUser(Integer userId, ChatMessageEntity msg) {
        pushToUser(userId, buildChatPayload(msg));
    }

    public void pushChatMessageToAdmins(ChatMessageEntity msg) {
        pushToAdmins(buildChatPayload(msg));
    }

    public void pushAdminDashboardRefresh(String reason) {
        pushToAdmins(Map.of(
                "channel", "dashboard",
                "type", "refresh",
                "reason", reason == null ? "update" : reason
        ));
    }

    public void afterCommit(Runnable action) {
        if (action == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private Map<String, Object> buildChatPayload(ChatMessageEntity msg) {
        return Map.of(
                "channel", "chat",
                "type", "message",
                "orderCode", msg.getOrderCode(),
                "messageId", msg.getId(),
                "senderRole", msg.getSenderRole(),
                "senderName", msg.getSenderName(),
                "content", msg.getContent(),
                "createdAt", msg.getCreatedAt(),
                "isRead", Boolean.TRUE.equals(msg.getIsRead())
        );
    }

    private void pushToUser(Integer userId, Object payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        send(sessions, payload);
    }

    private void pushToAdmins(Object payload) {
        send(adminSessions, payload);
    }

    private void send(Collection<WebSocketSession> sessions, Object payload) {
        if (sessions == null || sessions.isEmpty()) return;
        String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }

        for (WebSocketSession session : new ArrayList<>(sessions)) {
            if (session == null || !session.isOpen()) {
                unregister(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(text));
                }
            } catch (Exception e) {
                unregister(session);
            }
        }
    }
}
