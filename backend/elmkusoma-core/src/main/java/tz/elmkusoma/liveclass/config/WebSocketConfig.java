package tz.elmkusoma.liveclass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import tz.elmkusoma.liveclass.handler.LiveClassWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveClassWebSocketHandler liveClassWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(LiveClassWebSocketHandler liveClassWebSocketHandler,
                           JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.liveClassWebSocketHandler = liveClassWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveClassWebSocketHandler, "/ws/live-class/{classId}")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://elmkusoma.com",
                        "https://www.elmkusoma.com"
                );
    }
}
