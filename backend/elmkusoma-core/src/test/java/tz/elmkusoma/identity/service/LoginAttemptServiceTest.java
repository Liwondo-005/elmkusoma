package tz.elmkusoma.identity.service;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Focused policy tests for {@link LoginAttemptService} with mocked Redis
 * (no Spring context). Backs the mock with an in-memory map.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginAttemptServiceTest {

    private final Map<String, Long> store = new HashMap<>();

    @Mock
    private TenantAwareRedisTemplate redis;
    @Mock
    private RedisTemplate<String, Object> raw;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        store.clear();
        service = new LoginAttemptService(redis);
        when(redis.getRawTemplate()).thenReturn(raw);
        when(raw.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
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
    void accountBlockedAfterFiveFailures() {
        String email = "victim@example.com";
        String ip = "10.0.0.1";
        for (int i = 0; i < 4; i++) {
            assertFalse(service.isBlocked(email, ip));
            assertFalse(service.recordFailure(email, ip));
        }
        assertTrue(service.recordFailure(email, ip));
        assertTrue(service.isBlocked(email, ip));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkBlocked(email, ip));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void ipBlockedAfterTwentyFailuresAcrossAccounts() {
        String ip = "10.0.0.9";
        for (int i = 0; i < 19; i++) {
            assertFalse(service.isBlocked("user" + i + "@example.com", ip));
            service.recordFailure("user" + i + "@example.com", ip);
        }
        assertFalse(service.isBlocked("user19@example.com", ip));
        assertTrue(service.recordFailure("user19@example.com", ip));
        assertTrue(service.isBlocked("anyone-else@example.com", ip));
    }

    @Test
    void successClearsAccountAndIpCounters() {
        String email = "victim@example.com";
        String ip = "10.0.0.2";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(email, ip);
        }
        assertTrue(service.isBlocked(email, ip));
        service.recordSuccess(email, ip);
        assertFalse(service.isBlocked(email, ip));
        assertDoesNotThrow(() -> service.checkBlocked(email, ip));
    }

    @Test
    void emailKeyNormalizedTrimAndLowercase() {
        assertEquals(LoginAttemptService.accountKey("user@example.com"),
                LoginAttemptService.accountKey("  USER@Example.COM  "));
        String freshIp = "10.0.0.3";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(i % 2 == 0 ? "  User@Example.com " : "USER@example.com", freshIp);
        }
        // Same normalized account key, fresh IP isolates the account counter.
        assertTrue(service.isBlocked("user@example.com", "192.168.99.99"));
    }

    @Test
    void redisFailureFailsOpen() {
        when(redis.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(redis.increment(anyString(), anyLong())).thenThrow(new RuntimeException("redis down"));
        assertFalse(service.isBlocked("user@example.com", "10.0.0.4"));
        assertDoesNotThrow(() -> service.checkBlocked("user@example.com", "10.0.0.4"));
        assertFalse(service.recordFailure("user@example.com", "10.0.0.4"));
        assertDoesNotThrow(() -> service.recordSuccess("user@example.com", "10.0.0.4"));
    }
}
