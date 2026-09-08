package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtRequestAttributeFilter extends OncePerRequestFilter implements Ordered {

    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    private static final Logger log = LoggerFactory.getLogger(JwtRequestAttributeFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;

    public JwtRequestAttributeFilter(JwtTokenProvider jwtTokenProvider,
                                     UserRepository userRepository,
                                     InstitutionMembershipRepository membershipRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String token = extractToken(request);
                if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmailFromToken(token);

                    User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
                    if (user != null) {
                        request.setAttribute("userId", user.getId());

                        List<InstitutionMembership> memberships =
                                membershipRepository.findByUserIdAndIsActiveTrue(user.getId());
                        if (!memberships.isEmpty()) {
                            request.setAttribute("institutionId", memberships.get(0).getInstitutionId());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set request attributes from JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
