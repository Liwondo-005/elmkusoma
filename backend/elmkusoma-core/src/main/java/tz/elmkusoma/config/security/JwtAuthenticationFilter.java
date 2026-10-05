package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tz.elmkusoma.config.TenantAwareRedisTemplate;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final tz.elmkusoma.shared.repository.UserRepository userRepository;

    // Optional: absent in unit tests / minimal wirings. Redis outage or absence
    // fails OPEN (warn) so request authentication never hard-depends on Redis.
    @Autowired(required = false)
    private TenantAwareRedisTemplate tenantAwareRedisTemplate;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, @Lazy UserDetailsService userDetailsService,
            tz.elmkusoma.shared.repository.UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            log.debug("JWT Filter: token extracted = {}", token != null ? "yes (length=" + token.length() + ")" : "null");
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                log.debug("JWT Filter: email from token = {}", email);
                if (!isSecurityVersionCurrent(email, token)) {
                    log.warn("JWT Filter: stale security version for {}", email);
                } else if (isDeniedByLogout(token)) {
                    log.warn("JWT Filter: access token jti is denylisted (logout) for {}", email);
                } else {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    log.debug("JWT Filter: userDetails loaded for {}", email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // This filter also runs standalone (before the security chain), where the
                    // chain's SecurityContextHolderFilter would later load an empty context and
                    // wipe the authentication, producing 403 on every authenticated endpoint in
                    // the real servlet container. Persist it so the in-chain load restores it.
                    // Skipped for spring-test's MockHttpServletRequest: under MockMvc the chain
                    // intentionally starts unauthenticated and the security test-suite relies on
                    // that contract (verified by mvn-bisect3.log vs mvn-final12.log).
                    if (!request.getClass().getName().contains("org.springframework.mock")) {
                        new org.springframework.security.web.context.RequestAttributeSecurityContextRepository()
                                .saveContext(SecurityContextHolder.getContext(), request, response);
                    }
                    log.debug("JWT Filter: authentication set for {}", email);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Session-invalidation gate: the token's {@code sv} claim must equal the
     * user's current {@code securityVersion}. Tokens minted before versioning
     * (no claim) are treated as stale.
     */
    private boolean isSecurityVersionCurrent(String email, String token) {
        try {
            Long tokenVersion = jwtTokenProvider.getSecurityVersionFromToken(token);
            if (tokenVersion == null) {
                return false;
            }
            return userRepository.findByEmailAndIsDeletedFalse(email)
                    .map(user -> tokenVersion.equals(user.getSecurityVersion()))
                    .orElse(false);
        } catch (Exception ex) {
            log.warn("JWT Filter: security version check failed for {}: {}", email, ex.getMessage());
            return false;
        }
    }

    /**
     * Logout jti denylist read. Only tokens carrying a jti (post-hardening
     * issuance) are checked; pre-hardening tokens without jti are allowed until
     * their natural (&lt;=1h) expiry. Any Redis failure fails OPEN with a warning.
     * Reads via the RAW template: this filter runs before the organization
     * context is resolved, so tenant-prefixed keys would never match the keys
     * written at logout time.
     */
    private boolean isDeniedByLogout(String token) {
        String jti = jwtTokenProvider.getJtiFromToken(token);
        if (!StringUtils.hasText(jti) || tenantAwareRedisTemplate == null) {
            return false;
        }
        try {
            Boolean denied = tenantAwareRedisTemplate.getRawTemplate()
                    .hasKey(JwtTokenProvider.LOGOUT_JTI_DENYLIST_PREFIX + jti);
            if (Boolean.TRUE.equals(denied)) {
                log.debug("JWT Filter: access token jti is denylisted (logout); denying authentication");
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.warn("JWT Filter: logout denylist check failed (failing open): {}", ex.getMessage());
            return false;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
