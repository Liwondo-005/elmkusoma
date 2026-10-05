package tz.elmkusoma.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class OrganizationContextResolver extends OncePerRequestFilter implements Ordered {

    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 3;
    private static final String HEADER_INSTITUTION_ID = "X-Institution-Id";
    private static final String HEADER_INSTITUTION_ID_ALT = "X-Organization-Id";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final OrganizationContextHolder contextHolder;
    private final jakarta.persistence.EntityManager entityManager;
    private final PermissionService permissionService;

    public OrganizationContextResolver(JwtTokenProvider jwtTokenProvider,
                                        UserRepository userRepository,
                                        InstitutionMembershipRepository membershipRepository,
                                        OrganizationContextHolder contextHolder,
                                        jakarta.persistence.EntityManager entityManager,
                                        PermissionService permissionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.contextHolder = contextHolder;
        this.entityManager = entityManager;
        this.permissionService = permissionService;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        // This filter runs at HIGHEST_PRECEDENCE + 3, before DispatcherServlet
        // activates the request scope, but OrganizationContextHolder is a
        // request-scoped bean. Temporarily bind the scope so setContext() works;
        // the scoped instance is cached as a request attribute and stays visible
        // to everything running inside DispatcherServlet afterwards.
        boolean scopeActivated = false;
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() == null) {
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                    new org.springframework.web.context.request.ServletRequestAttributes(request));
            scopeActivated = true;
        }
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String token = extractToken(request);
                if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
                    if (user != null) {
                        List<InstitutionMembership> memberships =
                                membershipRepository.findByUserIdAndIsActiveTrue(user.getId());

                        UUID targetInstitutionId = null;
                        InstitutionMembership activeMembership = null;

                        if (!memberships.isEmpty()) {
                            UUID resolvedId = resolveTargetInstitutionId(request, token, user, memberships);
                            if (resolvedId != null) {
                                targetInstitutionId = resolvedId;
                                activeMembership = memberships.stream()
                                        .filter(m -> m.getInstitutionId().equals(resolvedId))
                                        .findFirst()
                                        .orElse(null);
                            }
                        } else {
                            targetInstitutionId = resolveTargetInstitutionId(request, token, user, memberships);
                        }

                        // Final fallback: query institutionId directly from database
                        if (targetInstitutionId == null) {
                            try {
                                List<?> rows = entityManager.createNativeQuery(
                                        "SELECT institution_id FROM users WHERE id = ? AND institution_id IS NOT NULL")
                                        .setParameter(1, user.getId())
                                        .getResultList();
                                if (!rows.isEmpty() && rows.get(0) != null) {
                                    targetInstitutionId = (UUID) rows.get(0);
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        // Cross-institution header denial: an authenticated request that
                        // explicitly names an institution the caller has no membership in
                        // must be rejected outright instead of silently falling back to the
                        // caller's own institution (which would let IDOR probing succeed).
                        String requestedHeader = request.getHeader(HEADER_INSTITUTION_ID);
                        if (requestedHeader == null) {
                            requestedHeader = request.getHeader(HEADER_INSTITUTION_ID_ALT);
                        }
                        if (StringUtils.hasText(requestedHeader)
                                && (targetInstitutionId == null
                                    || !requestedHeader.equalsIgnoreCase(targetInstitutionId.toString()))) {
                            log.warn("Cross-institution header denied for user {}: header={} resolved={}",
                                    user.getId(), requestedHeader, targetInstitutionId);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Access to the requested institution is not permitted\"}");
                            return;
                        }

                        if (targetInstitutionId != null) {
                            // Ensure we have an activeMembership for the context
                            if (activeMembership == null) {
                                activeMembership = InstitutionMembership.builder()
                                        .userId(user.getId())
                                        .institutionId(targetInstitutionId)
                                        .role(mapUserRoleToMembershipRole(user.getRole()))
                                        .isActive(true)
                                        .build();
                            }
                            List<InstitutionMembership> effectiveMemberships = memberships.isEmpty()
                                    ? List.of(activeMembership)
                                    : memberships;

                            List<UUID> accessibleIds = effectiveMemberships.stream()
                                    .map(InstitutionMembership::getInstitutionId)
                                    .toList();

                            // Compute permissions from memberships
                            List<String> userPermissions = permissionService.getCombinedPermissions(effectiveMemberships);

                            OrganizationContext context = new OrganizationContext(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getRole().name(),
                                    targetInstitutionId,
                                    activeMembership.getRole(),
                                    effectiveMemberships,
                                    accessibleIds,
                                    userPermissions
                            );

                            contextHolder.setContext(context);

                            request.setAttribute("organizationContext", context);
                            request.setAttribute("userId", user.getId());
                            request.setAttribute("userEmail", user.getEmail());
                            request.setAttribute("userRole", user.getRole().name());
                            request.setAttribute("institutionId", targetInstitutionId);
                            request.setAttribute("membershipRole", activeMembership.getRole().name());
                            request.setAttribute("userPermissions", userPermissions);
                        } else {
                            log.warn("Could not resolve institution for user {}", user.getId());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error resolving organization context: {}", ex.getMessage(), ex);
        } finally {
            if (scopeActivated) {
                org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * User.Role → organization membership role for synthesized contexts. PUBLIC so provisioning
     * paths (PlatformAdminService.createUser) bind identical org roles. §12: PROVIDER_ADMIN
     * operates their provider organization (ADMIN-equivalent inside it); PROVIDER_STAFF gets
     * delivery capabilities without platform authority.
     */
    public static InstitutionMembership.Role mapUserRoleToMembershipRole(User.Role userRole) {
        return switch (userRole) {
            case TEACHER -> InstitutionMembership.Role.TEACHER;
            case PARENT -> InstitutionMembership.Role.PARENT;
            case STUDENT, OTHER_LEARNER -> InstitutionMembership.Role.STUDENT;
            case INSTITUTION_ADMIN, ADMIN -> InstitutionMembership.Role.ADMIN;
            case NATIONAL_ADMIN -> InstitutionMembership.Role.NATIONAL_ADMIN;
            case INSTRUCTOR -> InstitutionMembership.Role.INSTRUCTOR;
            case PROVIDER_ADMIN -> InstitutionMembership.Role.ADMIN;
            case PROVIDER_STAFF -> InstitutionMembership.Role.TEACHER;
            default -> InstitutionMembership.Role.STUDENT;
        };
    }

    private UUID resolveHeaderInstitutionId(HttpServletRequest request) {
        String headerInstitutionId = request.getHeader(HEADER_INSTITUTION_ID);
        if (!StringUtils.hasText(headerInstitutionId)) {
            headerInstitutionId = request.getHeader(HEADER_INSTITUTION_ID_ALT);
        }
        if (StringUtils.hasText(headerInstitutionId)) {
            try {
                return UUID.fromString(headerInstitutionId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid institution ID in header: {}", headerInstitutionId);
            }
        }
        return null;
    }

    private UUID resolveTargetInstitutionId(HttpServletRequest request, String token,
                                            User user, List<InstitutionMembership> memberships) {
        UUID headerId = resolveHeaderInstitutionId(request);

        // 1. Explicit header that matches an active membership
        if (headerId != null && memberships.stream().anyMatch(m -> m.getInstitutionId().equals(headerId))) {
            return headerId;
        }
        if (headerId != null) {
            log.warn("User {} attempted to access institution {} without membership; falling back",
                    user.getEmail(), headerId);
        }

        // 2. Institution embedded in the JWT
        String tokenInstitutionId = jwtTokenProvider.getInstitutionIdFromToken(token);
        if (StringUtils.hasText(tokenInstitutionId)) {
            try {
                UUID tokenOrgId = UUID.fromString(tokenInstitutionId);
                if (memberships.stream().anyMatch(m -> m.getInstitutionId().equals(tokenOrgId))) {
                    return tokenOrgId;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid institution ID in token: {}", tokenInstitutionId);
            }
        }

        // 3. Institution embedded in the request path
        UUID pathInstitutionId = extractInstitutionIdFromPath(request.getRequestURI());
        if (pathInstitutionId != null
                && memberships.stream().anyMatch(m -> m.getInstitutionId().equals(pathInstitutionId))) {
            return pathInstitutionId;
        }

        // 4. First active membership
        if (!memberships.isEmpty()) {
            return memberships.get(0).getInstitutionId();
        }

        return null;
    }

    private UUID extractInstitutionIdFromPath(String path) {
        if (path == null) return null;

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (("institutions".equals(parts[i]) || "admin".equals(parts[i]) || "organizations".equals(parts[i]))
                    && i + 1 < parts.length) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}