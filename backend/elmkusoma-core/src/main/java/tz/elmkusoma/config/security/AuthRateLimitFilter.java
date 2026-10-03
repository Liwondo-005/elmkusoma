package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * IP-level throttling for authentication and recovery endpoints.
 *
 * <p>Deliberately body-blind: the filter never reads request bodies (so it can
 * never disturb downstream message conversion). Per-account buckets live in
 * {@code AuthServiceImpl}, which sees the parsed DTOs. Denials are uniform
 * HTTP 429 and carry no account-existence signal.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;

    private final RateLimitService rateLimitService;

    public AuthRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod())) {
            String path = request.getRequestURI();
            String bucket = ipBucketFor(path);
            if (bucket != null) {
                String ip = clientIp(request);
                String key = bucket + ":ip:" + ip;
                long[] budget = budgetFor(bucket);
                if (!rateLimitService.allow(key, (int) budget[0], budget[1])) {
                    long retryAfter = rateLimitService.retryAfterSeconds(key, budget[1]);
                    log.warn("Rate limit exceeded: rule={} ip={}", bucket, ip);
                    response.setStatus(429);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setHeader("Retry-After", String.valueOf(retryAfter));
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String ipBucketFor(String path) {
        if (path.equals("/v1/auth/login")) {
            return "login";
        }
        if (path.equals("/v1/auth/forgot-password")) {
            return "forgot";
        }
        if (path.equals("/v1/auth/reset-password")) {
            return "reset";
        }
        if (path.equals("/v1/auth/send-code")) {
            return "send-code";
        }
        if (path.equals("/v1/auth/verify-code")) {
            return "verify-code";
        }
        if (path.matches("/v1/platform-admin/users/[^/]+/reset-password")
                || path.matches("/v1/platform-admin/users/[^/]+/send-reset-link")) {
            return "admin-recovery";
        }
        return null;
    }

    /** Returns {limit, windowMillis} for the IP bucket of a rule. */
    private long[] budgetFor(String bucket) {
        return switch (bucket) {
            case "login" -> new long[]{30, MINUTE};
            case "forgot" -> new long[]{30, HOUR};
            case "reset" -> new long[]{30, MINUTE};
            case "send-code" -> new long[]{30, HOUR};
            case "verify-code" -> new long[]{60, HOUR};
            case "admin-recovery" -> new long[]{60, MINUTE};
            default -> new long[]{30, MINUTE};
        };
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
