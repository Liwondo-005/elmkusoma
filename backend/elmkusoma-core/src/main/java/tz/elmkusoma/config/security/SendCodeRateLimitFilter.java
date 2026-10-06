package tz.elmkusoma.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global (per-client-IP) rate limit for {@code POST /v1/auth/send-code}.
 *
 * <p>The service layer already enforces the per-email 60s cooldown; this
 * filter closes the remaining gap — a single client spraying many distinct
 * emails — with a fixed hourly window per IP. Returns {@code 429} with the
 * standard {@code ApiResponse} error shape and a {@code Retry-After} header.</p>
 */
@Component
public class SendCodeRateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(SendCodeRateLimitFilter.class);

    private static final String SEND_CODE_PATH = "/v1/auth/send-code";
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_IPS = 10_000;
    public static final String RATE_LIMIT_MESSAGE =
            "Too many verification requests. Please try again later.";

    private final ObjectMapper objectMapper;

    /** Fixed window (start millis + hits) per client IP. */
    private record Window(long start, long hits) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${elmkusoma.security.send-code-per-minute:20}")
    private long limitPerMinute;

    public SendCodeRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        // After the auth filters (HIGHEST_PRECEDENCE +1/+2), before business ones.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getRequestURI (not getServletPath): MockMvc leaves servletPath empty.
        return !SEND_CODE_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        long now = System.currentTimeMillis();

        if (windows.size() >= MAX_TRACKED_IPS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= WINDOW_MS);
        }

        Window window = windows.compute(ip, (key, current) ->
                current == null || now - current.start() >= WINDOW_MS
                        ? new Window(now, 1)
                        : new Window(current.start(), current.hits() + 1));

        if (window.hits() > limitPerMinute) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - (now - window.start())) / 1000);
            log.warn("send-code rate limit exceeded for ip={} ({} hits in window)", ip, window.hits());
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            objectMapper.writeValue(response.getWriter(),
                    Map.of("success", false, "error", RATE_LIMIT_MESSAGE));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First hop is the original client; trim before any chain noise.
            int comma = forwarded.indexOf(',');
            String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty() && first.length() <= 45) {
                return first;
            }
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    // Visible for tests: reset the in-memory window state.
    void reset() {
        windows.clear();
    }
}
