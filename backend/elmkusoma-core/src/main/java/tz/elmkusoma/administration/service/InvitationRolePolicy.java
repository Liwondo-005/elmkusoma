package tz.elmkusoma.administration.service;

import java.util.Locale;
import java.util.Set;

/**
 * Who may hand out which roles, and through which door.
 *
 * <p>Invitations create accounts, so the invitee's {@code role} field is the most
 * security-sensitive string in the product: whoever may write it decides who can
 * administer whom. It used to be an unvalidated {@code @NotBlank String} on an endpoint that
 * {@code INSTITUTION_ADMIN} and {@code PROVIDER_ADMIN} could both call, which meant an
 * institution administrator could mint a PROVIDER_ADMIN (or ADMIN, or NATIONAL_ADMIN)
 * invitation - and accepting it wrote a <em>global</em> {@link tz.elmkusoma.shared.domain.User}
 * role. Platform Admin is the only authority that may create those, so the rule lives here,
 * in one place, instead of being re-derived per endpoint.</p>
 *
 * <p>Institution administrators may still delegate inside their own institution (including
 * deputy INSTITUTION_ADMINs) because that authority is bounded by the tenant that already
 * scopes the caller.</p>
 */
public final class InvitationRolePolicy {

    /**
     * Roles a tenant administrator may grant. Everything here resolves to authority that the
     * tenant boundary already constrains: the acceptor gains a membership, not a new tenant.
     */
    private static final Set<String> INSTITUTION_INVITABLE = Set.of(
            "STUDENT",
            "LEARNER",
            "OTHER_LEARNER",
            "TEACHER",
            "INSTRUCTOR",
            "PARENT",
            "INSTITUTION_ADMIN"
    );

    /**
     * Roles only the platform may create. PROVIDER_ADMIN/STAFF because a provider account is
     * platform-granted authority over a provider organisation; the platform administration
     * roles because they are explicitly not delegable.
     */
    private static final Set<String> PLATFORM_ONLY = Set.of(
            "PROVIDER_ADMIN",
            "PROVIDER_STAFF",
            "ADMIN",
            "NATIONAL_ADMIN",
            "REGIONAL_ADMIN",
            "DISTRICT_ADMIN"
    );

    /** Every role the platform may place on an invitation. */
    public static final Set<String> ALL_INVITABLE =
            java.util.stream.Stream.concat(INSTITUTION_INVITABLE.stream(), PLATFORM_ONLY.stream())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private InvitationRolePolicy() {
    }

    /**
     * Roles that a {@code PROVIDER_ADMIN}-grade actor may never be granted by invitation,
     * regardless of the institution named in the request.
     */
    public static boolean isPlatformOnly(String role) {
        return role != null && PLATFORM_ONLY.contains(normalize(role));
    }

    public static boolean isInvitable(String role) {
        return role != null && ALL_INVITABLE.contains(normalize(role));
    }

    /**
     * Validates an invitation's requested role.
     *
     * @param requestedRole the role as supplied by the caller; matched case-insensitively
     * @param platformActor whether the inviter is a platform administrator
     * @return the normalized role, safe to persist
     * @throws IllegalArgumentException if the role is unknown, or is platform-only and the
     *                                  inviter is not a platform administrator
     */
    public static String assertInvitable(String requestedRole, boolean platformActor) {
        if (requestedRole == null || requestedRole.isBlank()) {
            throw new IllegalArgumentException("An invitation must name a role");
        }
        String role = normalize(requestedRole);
        if (PLATFORM_ONLY.contains(role) && !platformActor) {
            // Deliberately does not echo back which roles exist as platform-only: an
            // institution administrator should not be able to enumerate the privilege
            // vocabulary by probing this message.
            throw new IllegalArgumentException(
                    "You are not permitted to invite users with this role. Ask a platform administrator.");
        }
        if (!ALL_INVITABLE.contains(role)) {
            throw new IllegalArgumentException("Unknown role for invitation: " + requestedRole);
        }
        return role;
    }

    /** A request attribute role such as {@code User.Role} as stored in {@code users.role}. */
    public static boolean isPlatformActor(String userRoleAttribute) {
        if (userRoleAttribute == null) {
            return false;
        }
        String role = normalize(userRoleAttribute);
        return "ADMIN".equals(role) || "NATIONAL_ADMIN".equals(role);
    }

    private static String normalize(String role) {
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
