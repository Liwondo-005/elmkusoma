package tz.elmkusoma.config.security;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for authentication endpoints.
 *
 * <p>Layered keys (IP, account, endpoint) keep brute-force protection from
 * becoming a global denial-of-service: a shared NAT IP hitting one bucket
 * does not block unrelated users. Single-instance scope: with several app
 * replicas each enforces its own window (fail-closed per instance), which is
 * documented for horizontal-scale deployments that should front this with a
 * shared (e.g. Redis) limiter.
 */
@Service
public class RateLimitService {

    private static final int MAX_BUCKETS = 50_000;

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    /**
     * Records one hit and reports whether the caller is still within budget.
     *
     * @param key bucket identifier (endpoint + dimension, e.g. IP or account)
     * @param limit maximum hits per window
     * @param windowMillis window length in milliseconds
     * @return {@code true} when allowed, {@code false} when over budget
     */
    public synchronized boolean allow(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        if (buckets.size() > MAX_BUCKETS) {
            buckets.entrySet().removeIf(entry -> {
                Deque<Long> times = entry.getValue();
                while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
                    times.pollFirst();
                }
                return times.isEmpty();
            });
        }
        Deque<Long> times = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
            times.pollFirst();
        }
        if (times.size() >= limit) {
            return false;
        }
        times.addLast(now);
        return true;
    }

    /**
     * Seconds until the oldest hit in the bucket expires (for Retry-After).
     */
    public synchronized long retryAfterSeconds(String key, long windowMillis) {
        Deque<Long> times = buckets.get(key);
        if (times == null || times.isEmpty()) {
            return 1L;
        }
        long elapsed = System.currentTimeMillis() - times.peekFirst();
        return Math.max(1L, (windowMillis - elapsed + 999) / 1000);
    }
}
