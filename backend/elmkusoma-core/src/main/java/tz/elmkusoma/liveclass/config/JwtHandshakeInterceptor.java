package tz.elmkusoma.liveclass.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tz.elmkusoma.config.security.JwtTokenProvider;

import java.util.Map;
import java.util.UUID;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_INSTITUTION_ID = "institutionId";
    private static final String ATTR_USER_NAME = "userName";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid or missing token");
            return false;
        }

        try {
            String userIdStr = jwtTokenProvider.getUserIdFromToken(token);
            String institutionIdStr = jwtTokenProvider.getInstitutionIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            if (userIdStr == null) {
                log.warn("WebSocket handshake rejected: no userId in token");
                return false;
            }

            UUID userId = UUID.fromString(userIdStr);
            attributes.put(ATTR_USER_ID, userId);
            attributes.put("email", email);

            if (institutionIdStr != null) {
                attributes.put(ATTR_INSTITUTION_ID, UUID.fromString(institutionIdStr));
            }

            log.info("WebSocket handshake accepted: userId={}, institutionId={}", userId, institutionIdStr);
            return true;
        } catch (Exception e) {
            log.error("WebSocket handshake rejected: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryToken = servletRequest.getServletRequest().getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
