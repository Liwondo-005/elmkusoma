package tz.elmkusoma.realtime.config;

import io.jsonwebtoken.Claims;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor, ApplicationListener<SessionDisconnectEvent> {

    public static final String QUEUE_NOTIFICATIONS_PREFIX = "/queue/notifications/";
    public static final String TOPIC_INSTITUTION_PREFIX = "/topic/institution/";
    public static final String TOKEN_HEADER = "token";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser jwtTokenParser;
    private final Map<String, StompPrincipal> sessions = new ConcurrentHashMap<>();

    public StompAuthChannelInterceptor(JwtTokenParser jwtTokenParser) {
        this.jwtTokenParser = jwtTokenParser;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            case DISCONNECT -> handleDisconnect(accessor);
            default -> { }
        }
        return message;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public void forgetSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Claims claims = jwtTokenParser.parse(extractToken(accessor));
        String userId = claims != null ? claims.get("userId", String.class) : null;
        if (!StringUtils.hasText(userId)) {
            throw new AccessDeniedException("STOMP CONNECT rejected: missing or invalid token");
        }
        StompPrincipal principal = new StompPrincipal(userId, claims.get("institutionId", String.class));
        accessor.setUser(principal);
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessions.put(sessionId, principal);
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        StompPrincipal principal = resolvePrincipal(accessor);
        if (principal == null) {
            throw new AccessDeniedException("STOMP SUBSCRIBE rejected: not authenticated");
        }
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            return;
        }
        if (destination.startsWith(QUEUE_NOTIFICATIONS_PREFIX)) {
            String targetUserId = destination.substring(QUEUE_NOTIFICATIONS_PREFIX.length());
            if (!principal.userId().equals(targetUserId)) {
                throw new AccessDeniedException("STOMP SUBSCRIBE rejected: destination does not match authenticated user");
            }
            return;
        }
        if (destination.startsWith(TOPIC_INSTITUTION_PREFIX)) {
            String remainder = destination.substring(TOPIC_INSTITUTION_PREFIX.length());
            int slash = remainder.indexOf('/');
            String targetInstitutionId = slash >= 0 ? remainder.substring(0, slash) : remainder;
            if (!StringUtils.hasText(principal.institutionId())
                    || !principal.institutionId().equals(targetInstitutionId)) {
                throw new AccessDeniedException("STOMP SUBSCRIBE rejected: destination does not match authenticated institution");
            }
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        forgetSession(accessor.getSessionId());
    }

    private StompPrincipal resolvePrincipal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof StompPrincipal principal) {
            return principal;
        }
        String sessionId = accessor.getSessionId();
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader(TOKEN_HEADER);
        if (!StringUtils.hasText(token)) {
            String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
            if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
                token = authorization.substring(BEARER_PREFIX.length());
            }
        }
        return token;
    }

    public record StompPrincipal(String userId, String institutionId) implements Principal {
        @Override
        public String getName() {
            return userId;
        }
    }
}
