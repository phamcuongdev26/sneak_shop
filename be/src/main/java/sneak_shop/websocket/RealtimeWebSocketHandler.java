package sneak_shop.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeSocketHub socketHub;

    public RealtimeWebSocketHandler(RealtimeSocketHub socketHub) {
        this.socketHub = socketHub;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        socketHub.register(session, userId, role);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Client does not need to send messages; keep the channel for pushes only.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        socketHub.unregister(session);
    }
}
