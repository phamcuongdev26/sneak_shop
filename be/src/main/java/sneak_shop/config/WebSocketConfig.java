package sneak_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import sneak_shop.websocket.RealtimeWebSocketAuthInterceptor;
import sneak_shop.websocket.RealtimeWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final RealtimeWebSocketAuthInterceptor realtimeWebSocketAuthInterceptor;

    public WebSocketConfig(RealtimeWebSocketHandler realtimeWebSocketHandler,
                           RealtimeWebSocketAuthInterceptor realtimeWebSocketAuthInterceptor) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.realtimeWebSocketAuthInterceptor = realtimeWebSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .addInterceptors(realtimeWebSocketAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
