package tz.elmkusoma.liveclass.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tz.elmkusoma.config.security.JwtTokenProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authenticates the live-classroom WebSocket handshake.
 * <p>
 * Token sources, in order of preference:
 * <ol>
 *   <li>{@code Sec-WebSocket-Protocol: elmkusoma.jwt.v1, bearer.&lt;jwt&gt;} — browsers cannot
 *       set custom headers on a WebSocket, so the subprotocol list is used instead of the URL.
 *       This keeps the JWT out of request URLs, proxy logs and browser history.</li>
 *   <li>{@code ?token=} query parameter — legacy clients, still accepted.</li>
 *   <li>{@code Authorization: Bearer &lt;jwt&gt;} — non-browser clients.</li>
 * </ol>
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    /** Marker protocol the client offers first and the server echoes back. */
    /** Standard header carrying the negotiated WebSocket subprotocol. */
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    static final String SUBPROTOCOL_MARKER = "elmkusoma.jwt.v1";
    /** Prefix of the subprotocol entry carrying the JWT. */
    static final String BEARER_SUBPROTOCOL_PREFIX = "bearer.";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_INSTITUTION_ID = "institutionId";
    private static final String ATTR_USER_NAME = "userName";
    private static final String ATTR_USER_ROLE = "userRole";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Preferred: token negotiated as a WebSocket subprotocol ("bearer.<jwt>"), which keeps
        // the JWT out of the request URL (and therefore out of proxy/access logs and browser
        // history). The subprotocol list is echoed back as {@link #SUBPROTOCOL_MARKER}.
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid or missing token");
            return false;
        }
        if (request.getHeaders().getFirst(SEC_WEBSOCKET_PROTOCOL) != null) {
            response.getHeaders().set(SEC_WEBSOCKET_PROTOCOL, SUBPROTOCOL_MARKER);
        }

        try {
            String userIdStr = jwtTokenProvider.getUserIdFromToken(token);
            String institutionIdStr = jwtTokenProvider.getInstitutionIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            if (userIdStr == null) {
                log.warn("WebSocket handshake rejected: no userId in token");
                return false;
            }

            UUID userId = UUID.fromString(userIdStr);
            attributes.put(ATTR_USER_ID, userId);
            attributes.put("email", email);
            if (role != null) {
                attributes.put(ATTR_USER_ROLE, role);
            }

            if (institutionIdStr != null) {
                attributes.put(ATTR_INSTITUTION_ID, UUID.fromString(institutionIdStr));
            }

            log.info("WebSocket handshake accepted: userId={}, institutionId={}, role={}", userId, institutionIdStr, role);
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
        // Preferred: Sec-WebSocket-Protocol: elmkusoma.jwt.v1, bearer.<jwt>
        String subprotocolToken = extractBearerSubprotocol(request);
        if (subprotocolToken != null) {
            return subprotocolToken;
        }

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryToken = servletRequest.getServletRequest().getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }

        // Fallback for adapters that are not servlet-backed: read ?token= from the URI.
        if (request.getURI() != null && request.getURI().getRawQuery() != null) {
            for (String pair : request.getURI().getRawQuery().split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                    String value = java.net.URLDecoder.decode(
                            pair.substring(eq + 1), StandardCharsets.UTF_8);
                    if (!value.isBlank()) {
                        return value;
                    }
                }
            }
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /** Reads the "bearer.&lt;jwt&gt;" entry from the offered subprotocol list, if present. */
    private String extractBearerSubprotocol(ServerHttpRequest request) {
        return extractBearerSubprotocol(request.getHeaders().get(SEC_WEBSOCKET_PROTOCOL));
    }

    /**
     * Extracts the JWT from a {@code Sec-WebSocket-Protocol} header value list.
     * Package-private and static so the token transport can be unit-tested without a
     * servlet container.
     *
     * @param offeredSubprotocols raw header values, each possibly a comma-separated list
     * @return the token, or null when no {@code bearer.<jwt>} entry is present
     */
    static String extractBearerSubprotocol(List<String> offeredSubprotocols) {
        if (offeredSubprotocols == null) {
            return null;
        }
        for (String headerValue : offeredSubprotocols) {
            if (headerValue == null) {
                continue;
            }
            for (String candidate : headerValue.split(",")) {
                String value = candidate.trim();
                if (value.startsWith(BEARER_SUBPROTOCOL_PREFIX) && value.length() > BEARER_SUBPROTOCOL_PREFIX.length()) {
                    return value.substring(BEARER_SUBPROTOCOL_PREFIX.length());
                }
            }
        }
        return null;
    }
}
