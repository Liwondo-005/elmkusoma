package tz.elmkusoma.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;

/**
 * Platform maintenance mode (spec §50 / feature maintenance_mode).
 * When platform.maintenance.enabled is true, only auth endpoints, actuator,
 * and ADMIN-role requests may proceed; everything else receives 503.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final PlatformConfigRepository configRepository;

    private static final String[] ALLOW_PREFIXES = {
            "/v1/auth/",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        boolean enabled;
        try {
            enabled = configRepository.findByConfigKeyAndIsDeletedFalse("platform.maintenance.enabled")
                    .map(e -> "true".equalsIgnoreCase(e.getConfigValue() != null ? e.getConfigValue().trim() : ""))
                    .orElse(false);
        } catch (Exception e) {
            return true; // fail open on config read errors — never brick the app
        }
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        for (String p : ALLOW_PREFIXES) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        var auth = request.getUserPrincipal();
        boolean isAdmin = false;
        if (auth != null && auth.getName() != null) {
            // role check via request attribute populated by JwtRequestAttributeFilter when present
            Object roleAttr = request.getAttribute("userRole");
            if (roleAttr != null && "ADMIN".equals(roleAttr.toString())) {
                isAdmin = true;
            }
        }
        if (isAdmin) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"Platform is in maintenance mode. Please try again later.\"}");
        return false;
    }
}
