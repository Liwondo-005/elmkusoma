package tz.elmkusoma.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tz.elmkusoma.config.TenantAwareRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Additional {@link LoginAttemptService} policy cases with mocked Redis (no
 * Spring context). Complements {@link LoginAttemptServiceTest} — every case
 * here is verified absent there before writing:
 *
 * <ul>
 *   <li>6th-attempt view of the account block ({@code MAX_ACCOUNT_FAILURES=5}:
 *       five failures trip the threshold, the 6th login is rejected with the
 *       exact 429 contract message);</li>
 *   <li>TTL-expiry release (block keys vanish -&gt; logins allowed again);</li>
 *   <li>threshold-edge return values of {@code recordFailure} (true exactly at
 *       the 5th account / 20th IP failure);</li>
 *   <li>{@code resolveClientIp} X-Forwarded-For handling used by the login path;</li>
 *   <li>{@code getRetryAfterSeconds} fail-open behaviour.</li>
 * </ul>
 *
 * <p>HTTP-level throttling tests are intentionally absent: the test profile
 * points at {@code localhost:6379} with no guaranteed test Redis (no
 * testcontainers per repo conventions), and the service fails open without
 * Redis, so an HTTP 429 assertion would be vacuous.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginAttemptServiceRegressionTest {

    private final Map<String, Long> store = new HashMap<>();
    private final Map<String, Long> expireStore = new HashMap<>();

    @Mock
    private TenantAwareRedisTemplate redis;
    @Mock
    private RedisTemplate<String, Object> raw;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        store.clear();
        expireStore.clear();
        service = new LoginAttemptService(redis);
        when(redis.expire(anyString(), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            expireStore.put(inv.getArgument(0), inv.getArgument(1));
            return true;
        });
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenAnswer(inv -> expireStore.get(inv.getArgument(0)));
        when(redis.increment(anyString(), anyLong())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            long next = store.getOrDefault(key, 0L) + 1L;
            store.put(key, next);
            return next;
        });
        when(redis.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return true;
        });
    }

    @Test
    void sixthAttemptAfterFiveFailures_throws429WithContractMessage() {
        String email = "target@example.com";
        String ip = "10.1.0.1";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(email, ip);
        }
        // The 6th login attempt is the one rejected.
        assertTrue(service.isBlocked(email, ip));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkBlocked(email, ip));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertEquals("Too many login attempts. Please try again later.", ex.getReason());
    }

    @Test
    void blockReleasedWhenTtlKeysExpire() {
        String email = "target@example.com";
        String ip = "10.1.0.2";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(email, ip);
        }
        assertTrue(service.isBlocked(email, ip));

        // Redis TTL expiry deletes the counter keys; the block must lift with them.
        store.clear();
        assertFalse(service.isBlocked(email, ip));
        assertDoesNotThrow(() -> service.checkBlocked(email, ip));
    }

    @Test
    void recordFailure_returnsTrueExactlyAtThreshold() {
        String ip = "10.1.0.3";
        // Account threshold: true exactly on the 5th failure.
        for (int i = 0; i < 4; i++) {
            assertFalse(service.recordFailure("edge@example.com", ip));
        }
        assertTrue(service.recordFailure("edge@example.com", ip));
        assertFalse(service.recordFailure("edge@example.com", ip));

        // IP threshold: true exactly on the 20th failure.
        String ip2 = "10.1.0.4";
        for (int i = 0; i < 19; i++) {
            assertFalse(service.recordFailure("u" + i + "@example.com", ip2));
        }
        assertTrue(service.recordFailure("u19@example.com", ip2));
    }

    @Test
    void resolveClientIp_prefersFirstForwardedEntry() {
        HttpServletRequest proxied = mock(HttpServletRequest.class);
        when(proxied.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        when(proxied.getRemoteAddr()).thenReturn("9.9.9.9");
        assertEquals("1.2.3.4", LoginAttemptService.resolveClientIp(proxied));

        HttpServletRequest direct = mock(HttpServletRequest.class);
        when(direct.getHeader("X-Forwarded-For")).thenReturn("  ");
        when(direct.getRemoteAddr()).thenReturn("9.9.9.9");
        assertEquals("9.9.9.9", LoginAttemptService.resolveClientIp(direct));

        assertEquals("unknown", LoginAttemptService.resolveClientIp(null));
    }

    @Test
    void retryAfter_failsOpenAndFallsBackToWindowWhenBlocked() {
        // Redis down entirely: best-effort 0, never throws.
        when(redis.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(redis.increment(anyString(), anyLong())).thenThrow(new RuntimeException("redis down"));
        assertEquals(0, service.getRetryAfterSeconds("u@example.com", "10.1.0.5"));

        // Blocked but no TTL info (e.g. key without expiry): falls back to the window.
        // NOTE: doAnswer (not when/thenAnswer) — re-stubbing a throwing stub
        // with when() would re-fire the old stub during evaluation.
        store.clear();
        store.put(LoginAttemptService.accountKey("blocked@example.com"), 5L);
        lenient().doAnswer(inv -> store.get(inv.getArgument(0))).when(redis).get(anyString());
        assertEquals(LoginAttemptService.WINDOW_MINUTES * 60,
                service.getRetryAfterSeconds("blocked@example.com", "10.9.9.9"));
    }
}
