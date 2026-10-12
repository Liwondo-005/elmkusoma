package tz.elmkusoma.nfe.provider.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.AdminDelegation;
import tz.elmkusoma.administration.repository.AdminDelegationRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves which level of the provider authority hierarchy a caller actually holds.
 *
 * <p><b>Why this exists.</b> {@code User.Role.PROVIDER_ADMIN} names two different things. In the
 * specification's hierarchy it is both <em>Level B</em>, a governance administrator Platform Admin
 * delegates to register and steer providers, and <em>Level C</em>, the owner who staffs one
 * provider. Those have opposite properties: a governance officer's authority comes from the
 * platform and is bounded by a delegation; an owner's authority comes from the provider and is
 * bounded by the providers they are a member of. Minting a second permanent role would have meant
 * a new enum constant, a database CHECK change, a frontend role string, a proxy entry, a workspace
 * branch and a sidebar branch - and it would still not have described either account correctly,
 * because a real person is frequently both an owner of their own provider and a governance officer
 * for others.</p>
 *
 * <p>So the distinction is drawn from data the platform already stores rather than from a new
 * label:</p>
 * <ul>
 *   <li><b>Level C (owner)</b> - a membership carrying an explicit {@code provider_id}. This is
 *       the same scope {@code OrganizationContextResolver} and {@code NfeScopeValidator} already
 *       enforce, so adding a concept here cannot diverge from what is actually enforced.</li>
 *   <li><b>Level B (governance officer)</b> - an ACTIVE, unexpired {@code AdminDelegation} with
 *       authority {@code PROVIDER_VERIFICATION}. Platform Admin already grants, reviews and
 *       revokes exactly these through {@code /v1/platform-admin/delegations}.</li>
 * </ul>
 *
 * <p>An account may hold either, both, or neither, and is evaluated independently for each. The
 * hierarchy is therefore enforced by scope and delegation rather than asserted by a role name,
 * which is what stops an owner from approving their own provider and an officer from reaching
 * outside the delegation they were granted.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProviderAuthorityResolver {

    /** The delegation authority that denotes Level B provider governance administration. */
    public static final String AUTHORITY_PROVIDER_VERIFICATION = "PROVIDER_VERIFICATION";

    /** Platform-wide roles. Authority above every level in the hierarchy. */
    private static final Set<String> PLATFORM_ROLES = Set.of("ADMIN", "NATIONAL_ADMIN");

    /** Global roles that may hold provider authority at all. */
    private static final Set<String> PROVIDER_ROLES = Set.of("PROVIDER_ADMIN", "PROVIDER_STAFF");

    /** Membership roles that denote owning authority inside a provider. */
    private static final Set<InstitutionMembership.Role> OWNER_MEMBERSHIP_ROLES = Set.of(
            InstitutionMembership.Role.OWNER,
            InstitutionMembership.Role.ADMIN,
            InstitutionMembership.Role.INSTITUTION_ADMIN);

    private final InstitutionMembershipRepository membershipRepository;
    private final AdminDelegationRepository delegationRepository;

    /**
     * True when the caller administers at least one specific provider, i.e. authority level C.
     *
     * <p>Platform admins are deliberately not owners: they administer providers through the
     * platform, not through membership, so that removing a membership cannot be mistaken for
     * removing platform authority.</p>
     */
    @Transactional(readOnly = true)
    public boolean isProviderOwner(UUID userId, String globalRole) {
        return !ownedProviderIds(userId, globalRole).isEmpty();
    }

    /**
     * The providers this caller owns (level C), read from live membership rows rather than from a
     * request attribute, so it holds outside a servlet request too - scheduler threads, service
     * calls, tests.
     *
     * @return provider ids, empty when the caller owns none
     */
    @Transactional(readOnly = true)
    public Set<UUID> ownedProviderIds(UUID userId, String globalRole) {
        if (userId == null || globalRole == null || !PROVIDER_ROLES.contains(globalRole)) {
            return Set.of();
        }
        Set<UUID> owned = new LinkedHashSet<>();
        for (InstitutionMembership membership : membershipRepository.findByUserIdAndIsActiveTrue(userId)) {
            if (Boolean.TRUE.equals(membership.getIsDeleted()) || membership.getProviderId() == null) {
                continue;
            }
            if (!OWNER_MEMBERSHIP_ROLES.contains(membership.getRole())) {
                continue;
            }
            owned.add(membership.getProviderId());
        }
        return owned;
    }

    /**
     * The providers this caller may act on as a provider governance administrator (level B).
     *
     * <p>A delegation with no {@code resource_ids} is read as platform-wide, matching how
     * {@code AdminDelegation} is written elsewhere. A delegation that has expired, been revoked,
     * or is not {@code ACTIVE} grants nothing - revocation must take effect immediately, not on
     * the next re-issue.</p>
     */
    @Transactional(readOnly = true)
    public Set<UUID> governanceProviderScope(UUID userId) {
        if (userId == null) {
            return Set.of();
        }
        Set<UUID> scope = new LinkedHashSet<>();
        for (AdminDelegation delegation : delegationRepository.findByDelegateIdAndIsDeletedFalse(userId)) {
            if (!AUTHORITY_PROVIDER_VERIFICATION.equals(delegation.getAuthority())) {
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(delegation.getStatus())) {
                continue;
            }
            if (delegation.getRevokedAt() != null) {
                continue;
            }
            if (delegation.getExpiresAt() != null && delegation.getExpiresAt().isBefore(LocalDateTime.now())) {
                continue;
            }
            scope.addAll(parseResourceIds(delegation.getResourceIds()));
        }
        return scope;
    }

    /**
     * Asserts the caller owns {@code providerId} and returns it.
     *
     * <p>Reported as not-found rather than forbidden for a provider the caller has never owned:
     * saying "that provider exists but is not yours" is an existence oracle across tenants, and
     * the platform already treats a foreign row as absent everywhere else. A caller who owns
     * <em>some</em> provider but is asking about a different one is told the same way, so the
     * message cannot be used to map the estate.</p>
     */
    @Transactional(readOnly = true)
    public void requireOwnedProvider(UUID userId, String globalRole, UUID providerId) {
        Set<UUID> owned = ownedProviderIds(userId, globalRole);
        if (owned.isEmpty()) {
            throw new ForbiddenException("You do not administer any provider");
        }
        if (providerId == null || !owned.contains(providerId)) {
            throw new tz.elmkusoma.exception.ResourceNotFoundException(
                    "Education Provider", "id", providerId);
        }
    }

    /**
     * Asserts the caller may act on {@code providerId} as a governance administrator (level B).
     *
     * <p>Both levels are accepted, because a governance officer legitimately administers providers
     * they do not belong to - that is the whole point of the delegation. Ownership alone is also
     * accepted, because an owner registering their own staff is the level C workflow.</p>
     *
     * <p>The two ways of failing are deliberately different. A caller with <em>some</em> provider
     * authority who asks about a provider that is not theirs is told the provider does not exist,
     * so the answer cannot be used to map which providers exist. A caller with none at all is told
     * they are not authorised, which reveals nothing beyond what they already know about
     * themselves.</p>
     */
    @Transactional(readOnly = true)
    public void requireOwnerOrGovernance(UUID userId, String globalRole, UUID providerId) {
        if (isPlatform(globalRole)) {
            return;
        }
        Set<UUID> owned = ownedProviderIds(userId, globalRole);
        if (owned.contains(providerId)) {
            return;
        }
        Set<UUID> governance = governanceProviderScope(userId);
        if (governance.contains(providerId)) {
            return;
        }
        if (!owned.isEmpty() || !governance.isEmpty()) {
            throw new tz.elmkusoma.exception.ResourceNotFoundException(
                    "Education Provider", "id", providerId);
        }
        throw new ForbiddenException("You are not authorised to administer this provider");
    }

    public boolean isPlatform(String globalRole) {
        return globalRole != null && PLATFORM_ROLES.contains(globalRole);
    }

    /**
     * A provider staff account may not mint peers with more authority than they hold.
     *
     * <p>Used to fail the whole request closed rather than downgrade the role, so a mis-configured
     * client cannot silently create a staff member with the owner's authority.</p>
     */
    public void assertNotEscalating(String globalRole) {
        if ("PROVIDER_STAFF".equals(globalRole)) {
            throw new ForbiddenException("Provider staff cannot provision other accounts");
        }
    }

    private Set<UUID> parseResourceIds(String json) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (json == null || json.isBlank()) {
            return ids;
        }
        List<String> raw = JsonIdParser.parseStringArray(json);
        for (String value : raw) {
            try {
                ids.add(UUID.fromString(value.trim()));
            } catch (IllegalArgumentException ignored) {
                // A malformed entry narrows the delegation rather than widening it: an id we
                // cannot read is one we will not act on.
                log.warn("Ignoring unparseable provider id in delegation scope: {}", value);
            }
        }
        return ids;
    }

    /** Minimal array-of-strings reader; the column is jsonb and this avoids a JSON dependency here. */
    static final class JsonIdParser {
        private JsonIdParser() {
        }

        static List<String> parseStringArray(String json) {
            List<String> values = new java.util.ArrayList<>();
            String trimmed = json.trim();
            if (trimmed.length() < 2 || trimmed.charAt(0) != '[') {
                return values;
            }
            String inner = trimmed.substring(1, trimmed.lastIndexOf(']'));
            for (String part : inner.split(",")) {
                String value = part.trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    values.add(value.substring(1, value.length() - 1).replace("\\\"", "\""));
                }
            }
            return values;
        }
    }
}
