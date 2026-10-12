package tz.elmkusoma.liveclass.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tz.elmkusoma.config.security.JwtTokenProvider;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Token transport for the live-classroom WebSocket handshake.
 * <p>
 * Browsers cannot set an {@code Authorization} header on a WebSocket, so the token is
 * negotiated as a subprotocol ({@code elmkusoma.jwt.v1, bearer.&lt;jwt&gt;}) which keeps the
 * JWT out of request URLs, proxy logs and browser history. The legacy {@code ?token=}
 * query form must keep working, and a request carrying no token must be rejected.
 */
class JwtHandshakeInterceptorTransportTest {

    // ==================== subprotocol extraction ====================

    @Test
    void tokenIsReadFromCommaSeparatedSubprotocolList() {
        assertEquals("abc.def.ghi",
                JwtHandshakeInterceptor.extractBearerSubprotocol(
                        List.of("elmkusoma.jwt.v1, bearer.abc.def.ghi")));
    }

    @Test
    void tokenIsReadFromSingleSubprotocolHeader() {
        assertEquals("abc.def.ghi",
                JwtHandshakeInterceptor.extractBearerSubprotocol(List.of("bearer.abc.def.ghi")));
    }

    @Test
    void whitespaceAroundSubprotocolsIsTolerated() {
        assertEquals("abc.def.ghi",
                JwtHandshakeInterceptor.extractBearerSubprotocol(
                        List.of("  elmkusoma.jwt.v1 ,   bearer.abc.def.ghi  ")));
    }

    @Test
    void multipleHeaderValuesAreAllInspected() {
        assertEquals("second.value",
                JwtHandshakeInterceptor.extractBearerSubprotocol(
                        List.of("elmkusoma.jwt.v1", "bearer.second.value")));
    }

    @Test
    void bareBearerPrefixIsNotAToken() {
        assertNull(JwtHandshakeInterceptor.extractBearerSubprotocol(List.of("elmkusoma.jwt.v1, bearer.")));
    }

    @Test
    void unrelatedSubprotocolsYieldNoToken() {
        assertNull(JwtHandshakeInterceptor.extractBearerSubprotocol(List.of("chat", "superchat")));
        assertNull(JwtHandshakeInterceptor.extractBearerSubprotocol(null));
    }

    // ==================== handshake behaviour ====================

    @Test
    void handshakeWithoutAnyTokenIsRejected() {
        JwtTokenProviderSpy spy = new JwtTokenProviderSpy();
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(spy);

        Map2 attrs = new Map2();
        assertFalse(interceptor.beforeHandshake(request(null, null), response(), handler(), attrs.map));
        assertTrue(attrs.map.isEmpty(), "no session attributes may exist for a rejected handshake");
    }

    @Test
    void subprotocolTokenWinsOverQueryToken() {
        JwtTokenProviderSpy spy = new JwtTokenProviderSpy();
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(spy);

        Map2 attrs = new Map2();
        assertTrue(interceptor.beforeHandshake(
                request("elmkusoma.jwt.v1, bearer.sub.protocol", "query.protocol"),
                response(), handler(), attrs.map));
        assertEquals("sub.protocol", spy.lastToken);
    }

    @Test
    void legacyQueryTokenStillAuthenticates() {
        JwtTokenProviderSpy spy = new JwtTokenProviderSpy();
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(spy);

        Map2 attrs = new Map2();
        assertTrue(interceptor.beforeHandshake(request(null, "legacy.protocol"),
                response(), handler(), attrs.map));
        assertEquals("legacy.protocol", spy.lastToken);
    }

    @Test
    void serverEchoesTheNegotiatedSubprotocolMarker() {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(new JwtTokenProviderSpy());
        ServerHttpResponse response = response();

        assertTrue(interceptor.beforeHandshake(
                request("elmkusoma.jwt.v1, bearer.abc", null), response, handler(), new Map2().map));

        assertEquals(JwtHandshakeInterceptor.SUBPROTOCOL_MARKER,
                response.getHeaders().getFirst("Sec-WebSocket-Protocol"),
                "the server must select the marker subprotocol it offered support for");
    }

    @Test
    void noSubprotocolHeaderMeansNoEcho() {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(new JwtTokenProviderSpy());
        ServerHttpResponse response = response();

        assertTrue(interceptor.beforeHandshake(request(null, "legacy.protocol"),
                response, handler(), new Map2().map));

        assertNull(response.getHeaders().getFirst("Sec-WebSocket-Protocol"));
    }

    // ==================== helpers ====================

    private static final class Map2 {
        final java.util.Map<String, Object> map = new java.util.HashMap<>();
    }

    private ServerHttpRequest request(String subprotocolHeader, String queryToken) {
        HttpHeaders headers = new HttpHeaders();
        if (subprotocolHeader != null) {
            headers.put("Sec-WebSocket-Protocol", List.of(subprotocolHeader));
        }
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        when(req.getHeaders()).thenReturn(headers);
        when(req.getURI()).thenReturn(queryToken == null
                ? URI.create("/ws/live-class/abc")
                : URI.create("/ws/live-class/abc?token=" + queryToken));
        when(req.getMethod()).thenReturn(HttpMethod.GET);
        return req;
    }

    private ServerHttpResponse response() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        HttpHeaders headers = new HttpHeaders();
        when(response.getHeaders()).thenReturn(headers);
        return response;
    }

    private WebSocketHandler handler() {
        return new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };
    }

    /** Records which token the interceptor passed for validation, and accepts any non-blank. */
    static class JwtTokenProviderSpy extends JwtTokenProvider {
        String lastToken;

        @Override
        public boolean validateToken(String token) {
            this.lastToken = token;
            return token != null && !token.isBlank();
        }

        @Override
        public String getUserIdFromToken(String token) {
            return "00000000-0000-0000-0000-00000000cafe";
        }

        @Override
        public String getInstitutionIdFromToken(String token) {
            return "00000000-0000-0000-0000-000000000001";
        }

        @Override
        public String getEmailFromToken(String token) {
            return token;
        }

        @Override
        public String getRoleFromToken(String token) {
            return "TEACHER";
        }
    }
}