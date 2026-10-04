package tz.elmkusoma.identity.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tz.elmkusoma.config.TenantAwareRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Phase E login brute-force protection (minimal).
 *
 * <p>Policy: per-account {@code login:fail:account:<normalized-email>} — 5 failures
 * within 15 min blocks for 15 min; per-IP {@code login:fail:ip:<ip>} — 20 failures
 * within 15 min blocks for 15 min. No permanent lockout: blocks are Redis TTL keys
 * only, so they self-expire and can never become a standing DoS.</p>
 *
 * <p>Availability first: if Redis is unavailable every method fails OPEN (allows the
 * login attempt) and logs a warning once; a failure of this gate must never lock
 * users out or break login.</p>
 *
 * <p>Key note: login runs with no auth/tenant context, so {@code TenantAwareRedisTemplate}
 * uses unprefixed keys here — fine for these global login keys.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    public static final int MAX_ACCOUNT_FAILURES = 5;
    public static final int MAX_IP_FAILURES = 20;
    public static final long WINDOW_MINUTES = 15;

    private final TenantAwareRedisTemplate redis;

    private volatile boolean redisUnavailableLogged = false;

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static String accountKey(String email) {
        return "login:fail:account:" + normalizeEmail(email);
    }

    public static String ipKey(String ip) {
        String clean = ip == null ? "" : ip.trim();
        return "login:fail:ip:" + (clean.isEmpty() ? "unknown" : clean);
    }

    /** Client IP: first entry of X-Forwarded-For, else remoteAddr, else "unknown". */
    public static String resolveClientIp(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String first = (comma >= 0 ? xff.substring(0, comma) : xff).trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null && !remote.isBlank() ? remote : "unknown";
    }

    /** Throws 429 when the account or IP is currently blocked. Fail-open on Redis errors. */
    public void checkBlocked(String email, String ip) {
        if (isBlocked(email, ip)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.");
        }
    }

    public boolean isBlocked(String email, String ip) {
        try {
            return getCount(accountKey(email)) >= MAX_ACCOUNT_FAILURES
                    || getCount(ipKey(ip)) >= MAX_IP_FAILURES;
        } catch (Exception e) {
            logRedisUnavailable("isBlocked", e);
            return false;
        }
    }

    /**
     * Records one failed attempt on both keys (fixed 15-min window: TTL set on first
     * failure only, so the block self-expires). Returns true when this failure newly
     * reached a threshold (caller may emit ACCOUNT_LOCKED once). Fail-open on Redis errors.
     */
    public boolean recordFailure(String email, String ip) {
        boolean accountHit;
        boolean ipHit;
        try {
            accountHit = incrementWithWindow(accountKey(email)) == MAX_ACCOUNT_FAILURES;
        } catch (Exception e) {
            logRedisUnavailable("recordFailure(account)", e);
            accountHit = false;
        }
        try {
            ipHit = incrementWithWindow(ipKey(ip)) == MAX_IP_FAILURES;
        } catch (Exception e) {
            logRedisUnavailable("recordFailure(ip)", e);
            ipHit = false;
        }
        return accountHit || ipHit;
    }

    /** Clears both counters after a successful login. Fail-open on Redis errors. */
    public void recordSuccess(String email, String ip) {
        try {
            redis.delete(accountKey(email));
        } catch (Exception e) {
            logRedisUnavailable("recordSuccess(account)", e);
        }
        try {
            redis.delete(ipKey(ip));
        } catch (Exception e) {
            logRedisUnavailable("recordSuccess(ip)", e);
        }
    }

    /** Best-effort seconds until the current block lifts (for Retry-After wiring). */
    public long getRetryAfterSeconds(String email, String ip) {
        long retry = 0;
        try {
            Long accountTtl = redis.getExpire(accountKey(email), TimeUnit.SECONDS);
            Long ipTtl = redis.getExpire(ipKey(ip), TimeUnit.SECONDS);
            if (accountTtl != null && accountTtl > retry) {
                retry = accountTtl;
            }
            if (ipTtl != null && ipTtl > retry) {
                retry = ipTtl;
            }
        } catch (Exception e) {
            logRedisUnavailable("getRetryAfterSeconds", e);
        }
        try {
            if (retry <= 0 && isBlocked(email, ip)) {
                retry = WINDOW_MINUTES * 60;
            }
        } catch (Exception e) {
            logRedisUnavailable("getRetryAfterSeconds(fallback)", e);
        }
        return retry;
    }

    private long incrementWithWindow(String key) {
        Long count = redis.increment(key, 1L);
        long value = count == null ? 0L : count;
        if (value == 1L) {
            try {
                redis.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                logRedisUnavailable("expire", e);
            }
        }
        return value;
    }

    private long getCount(String key) {
        Object value = redis.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void logRedisUnavailable(String op, Exception e) {
        if (!redisUnavailableLogged) {
            redisUnavailableLogged = true;
            log.warn("LoginAttemptService: Redis unavailable during {} — failing open (allowing login attempt)", op, e);
        } else {
            log.debug("LoginAttemptService: Redis unavailable during {} — failing open", op);
        }
    }
}
