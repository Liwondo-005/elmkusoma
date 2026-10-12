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
    private final ClientIpResolver clientIpResolver;

    public AuthRateLimitFilter(RateLimitService rateLimitService, ClientIpResolver clientIpResolver) {
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
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
        if (path.equals("/v1/auth/register")) {
            return "register";
        }
        if (path.equals("/v1/auth/refresh")) {
            return "refresh";
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
        // Covers mfa/verify plus the authenticated setup routes (enroll,
        // confirm, codes/regenerate) so none of them is a free work oracle.
        if (path.startsWith("/v1/auth/mfa/")) {
            return "mfa";
        }
        if (path.equals("/v1/auth/emergency-recover")) {
            return "emergency";
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
            case "register" -> new long[]{30, HOUR};
            case "refresh" -> new long[]{120, MINUTE};
            case "forgot" -> new long[]{30, HOUR};
            case "reset" -> new long[]{30, MINUTE};
            case "send-code" -> new long[]{30, HOUR};
            case "verify-code" -> new long[]{60, HOUR};
            case "mfa" -> new long[]{60, HOUR};
            case "emergency" -> new long[]{10, HOUR};
            case "admin-recovery" -> new long[]{60, MINUTE};
            default -> new long[]{30, MINUTE};
        };
    }

    /**
     * The per-IP bucket key.
     *
     * <p>Delegated to {@link ClientIpResolver}. This previously read {@code X-Forwarded-For} and
     * {@code X-Real-IP} unconditionally, both of which the caller chooses. Varying the header per
     * request meant an unlimited number of buckets on the login and password-reset paths, which are
     * precisely the endpoints these limits exist to constrain. The forwarded header is now honoured
     * only behind a configured trusted proxy; otherwise the socket address is used.</p>
     */
    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
