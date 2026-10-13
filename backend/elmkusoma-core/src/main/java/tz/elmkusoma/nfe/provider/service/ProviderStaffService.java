package tz.elmkusoma.nfe.provider.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.InstitutionInvitation;
import tz.elmkusoma.administration.dto.InvitationResponse;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.administration.service.InvitationRolePolicy;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.dto.ProviderStaffInviteRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderStaffResponse;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Provider staff provisioning and lifecycle (authority level C -&gt; D).
 *
 * <p>This is the workflow the product describes but did not have. Before it, a Provider Owner
 * could not staff their own provider: {@link InvitationRolePolicy} put {@code PROVIDER_STAFF} in
 * {@code PLATFORM_ONLY}, so the only way to create a staff account was for Platform Admin to invite
 * one, and there was no operation at all for listing a provider's staff, assigning a
 * responsibility, or removing someone. The database held one PROVIDER_ADMIN and no PROVIDER_STAFF,
 * so this path had never actually been exercised.</p>
 *
 * <p>Design constraints that shaped it:</p>
 * <ul>
 *   <li><b>The client never names a role.</b> {@link ProviderStaffInviteRequest} has no role
 *       field, so "grant me an admin" is not a request that can be expressed, rather than one that
 *       has to be validated. The single grantable outcome is PROVIDER_STAFF.</li>
 *   <li><b>No credential is ever created or returned.</b> The flow reuses
 *       {@code InstitutionInvitation} and the existing two-step redemption, so the account is only
 *       created once the invitee proves they control the invited mailbox and sets their own
 *       password. No temporary password is generated, stored or emailed.</li>
 *   <li><b>Removal is membership-scoped.</b> Deactivating a membership revokes this provider's
 *       access and the person's live sessions, and leaves their global account and every other
 *       membership intact.</li>
 *   <li><b>Every decision is audited</b> with the actor and the provider scope.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderStaffService {

    /** The one and only role this service can grant. */
    public static final String STAFF_ROLE = "PROVIDER_STAFF";

    /** Membership role a staff account receives. Existing convention, reused deliberately. */
    private static final InstitutionMembership.Role STAFF_MEMBERSHIP_ROLE = InstitutionMembership.Role.TEACHER;

    /** {@code [A-Z][A-Z0-9_]{1,59}} - a label, not prose, and never a payload for a renderer. */
    private static final Pattern RESPONSIBILITY = Pattern.compile("^[A-Z][A-Z0-9_]{1,59}$");

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int INVITATION_VALIDITY_DAYS = 7;

    private final EducationProviderRepository providerRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final InstitutionInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProviderAuthorityResolver authorityResolver;
    private final NfeScopeValidator scopeValidator;
    private final AuditService auditService;

    /**
     * The provider's staff roster, from real membership rows.
     *
     * <p>Returns inactive members too, so a removed person is visible as removed rather than
     * silently vanishing and looking like their access had been revoked by accident.</p>
     */
    @Transactional(readOnly = true)
    public List<ProviderStaffResponse> listStaff(UUID actorId, String actorRole, UUID providerId) {
        EducationProvider provider = requireReadableProvider(actorId, actorRole, providerId);
        return membershipRepository.findByProviderIdAndIsDeletedFalse(provider.getId()).stream()
                .map(this::toResponse)
                .sorted(Comparator
                        .comparing((ProviderStaffResponse s) -> Boolean.FALSE.equals(s.getIsActive()))
                        .thenComparing(ProviderStaffResponse::getFullName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Issues a staff invitation for one provider.
     *
     * <p>Authority is Platform Admin or an owner of <em>this</em> provider. A governance officer
     * (level B) is deliberately excluded: their delegated job is registering and steering
     * providers, not staffing them, and letting them do so would let a delegated administrator
     * seed staff inside providers Platform Admin never assigned them to.</p>
     *
     * @return the invitation, including its token - which is returned to the authorized inviter
     *         exactly once, for out-of-band delivery, and never re-listed afterwards
     */
    @Transactional
    public InvitationResponse inviteStaff(UUID actorId, String actorRole, UUID providerId,
                                          ProviderStaffInviteRequest request) {
        EducationProvider provider = requireOwnedWritableProvider(actorId, actorRole, providerId);

        // Refuses the whole request rather than downgrading it, so a client that sends a role
        // cannot quietly create a staff member with the owner's authority.
        authorityResolver.assertNotEscalating(actorRole);

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (invitationRepository.existsByEmailAndInstitutionIdAndStatusAndIsDeletedFalse(
                email, provider.getInstitutionId(), "PENDING")) {
            throw new IllegalArgumentException("An invitation is already pending for that address");
        }

        // Refuse rather than silently attach to an unrelated account. Linking an existing identity
        // is a deliberate, separate decision: an address that already belongs to a learner must
        // not quietly become provider staff because someone typed it in.
        userRepository.findByEmailAndIsDeletedFalse(email).ifPresent(existing -> {
            boolean alreadyStaff = membershipRepository
                    .findByUserIdAndIsActiveTrue(existing.getId()).stream()
                    .anyMatch(m -> provider.getId().equals(m.getProviderId()));
            if (!alreadyStaff) {
                throw new IllegalArgumentException(
                        "That address already belongs to an existing account. Ask a platform "
                                + "administrator to link it to this provider.");
            }
        });

        String token = randomToken();
        InstitutionInvitation invitation = invitationRepository.save(InstitutionInvitation.builder()
                .institutionId(provider.getInstitutionId())
                .email(email)
                .role(STAFF_ROLE)
                // Recorded now, not inferred at redemption - this is what fixes the old
                // findFirst() ambiguity when an institution hosts more than one provider.
                .providerId(provider.getId())
                .invitedBy(actorId)
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_VALIDITY_DAYS))
                .build());

        audit(actorId, actorRole, provider, AuditLog.AuditAction.CREATE, "PROVIDER_STAFF_INVITED",
                Map.of("invitationId", String.valueOf(invitation.getId()),
                       "role", STAFF_ROLE));

        log.info("Provider {} ({}) invited {} as staff by {}",
                provider.getName(), provider.getId(), email, actorId);

        return InvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .token(token)
                .build();
    }

    /**
     * Assigns or clears a staff member's responsibility.
     *
     * <p>Purely descriptive. It changes no permission: the specification treats responsibilities
     * as labels, and a label that silently granted access would be a second authorization model
     * that no audit could explain.</p>
     */
    @Transactional
    public ProviderStaffResponse setResponsibility(UUID actorId, String actorRole, UUID providerId,
                                                   UUID staffUserId, String requested) {
        EducationProvider provider = requireOwnedWritableProvider(actorId, actorRole, providerId);
        authorityResolver.assertNotEscalating(actorRole);

        String responsibility = normaliseResponsibility(requested);
        InstitutionMembership membership = requireStaffMembership(provider, staffUserId);
        membership.setResponsibility(responsibility);
        membershipRepository.save(membership);

        audit(actorId, actorRole, provider, AuditLog.AuditAction.UPDATE,
                "PROVIDER_STAFF_RESPONSIBILITY_CHANGED",
                Map.of("staffUserId", staffUserId.toString(),
                       "responsibility", String.valueOf(responsibility)));

        return toResponse(membership);
    }

    /**
     * Removes someone from this provider.
     *
     * <p>Deactivates the membership and revokes their live sessions. It does not delete the global
     * account and does not touch their other memberships: a person who is a staff member here and a
     * learner elsewhere, or an employee of a second provider, keeps all of it.</p>
     *
     * <p>Sessions are revoked through {@code securityVersion}, the platform's existing kill switch
     * - every JWT already in the wild carries the old version and stops authenticating on the next
     * request, without waiting for an expiry.</p>
     */
    @Transactional
    public void removeStaff(UUID actorId, String actorRole, UUID providerId, UUID staffUserId) {
        EducationProvider provider = requireOwnedWritableProvider(actorId, actorRole, providerId);
        authorityResolver.assertNotEscalating(actorRole);

        InstitutionMembership membership = requireStaffMembership(provider, staffUserId);

        // An owner cannot be removed through the staff endpoint, and cannot remove themselves.
        // Both would otherwise be a quiet self-lockout or an owner-demotion with no platform trail.
        if (isOwnerMembership(membership)) {
            throw new ForbiddenException(
                    "A provider owner cannot be removed here. Change their account through platform administration.");
        }
        if (staffUserId.equals(actorId)) {
            throw new ForbiddenException("You cannot remove your own provider membership");
        }

        membership.setIsActive(false);
        membershipRepository.save(membership);

        userRepository.findByIdAndIsDeletedFalse(staffUserId).ifPresent(user -> {
            user.setSecurityVersion(user.getSecurityVersion() == null ? 2L : user.getSecurityVersion() + 1);
            userRepository.save(user);
        });

        audit(actorId, actorRole, provider, AuditLog.AuditAction.DELETE, "PROVIDER_STAFF_REMOVED",
                Map.of("staffUserId", staffUserId.toString()));

        log.info("Provider {} ({}) removed staff member {} by {}",
                provider.getName(), provider.getId(), staffUserId, actorId);
    }

    // ── guards ──────────────────────────────────────────────────────────────

    /** Readable by Platform Admin, the owner of this provider, or a governance officer scoped to it. */
    private EducationProvider requireReadableProvider(UUID actorId, String actorRole, UUID providerId) {
        if (authorityResolver.isPlatform(actorRole)) {
            return loadProvider(providerId);
        }
        authorityResolver.requireOwnerOrGovernance(actorId, actorRole, providerId);
        return loadProvider(providerId);
    }

    /**
     * Writable only by Platform Admin or an owner of this provider, and only while the provider's
     * organisation is in a state that permits provider work.
     */
    private EducationProvider requireOwnedWritableProvider(UUID actorId, String actorRole, UUID providerId) {
        if (!authorityResolver.isPlatform(actorRole)) {
            authorityResolver.requireOwnedProvider(actorId, actorRole, providerId);
        }
        EducationProvider provider = loadProvider(providerId);
        if (Boolean.TRUE.equals(provider.getIsDeleted())) {
            throw new ResourceNotFoundException("Education Provider", "id", providerId);
        }
        // Reuses the existing lifecycle gate so a suspended organisation cannot staff itself.
        scopeValidator.requireWritableInstitution(provider.getInstitutionId());
        return provider;
    }

    private EducationProvider loadProvider(UUID providerId) {
        if (providerId == null) {
            throw new ResourceNotFoundException("Education Provider", "id", null);
        }
        return providerRepository.findById(providerId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));
    }

    /** The membership that binds this person to this provider, or not-found. */
    private InstitutionMembership requireStaffMembership(EducationProvider provider, UUID staffUserId) {
        return membershipRepository.findByProviderIdAndIsDeletedFalse(provider.getId()).stream()
                .filter(m -> staffUserId.equals(m.getUserId()))
                .findFirst()
                // Not-found, never forbidden: "that staff member exists in another provider" is an
                // existence oracle across provider boundaries.
                .orElseThrow(() -> new ResourceNotFoundException("Provider staff member", "id", staffUserId));
    }

    private boolean isOwnerMembership(InstitutionMembership membership) {
        return membership.getRole() == InstitutionMembership.Role.OWNER
                || membership.getRole() == InstitutionMembership.Role.ADMIN
                || membership.getRole() == InstitutionMembership.Role.INSTITUTION_ADMIN;
    }

    // ── mapping and helpers ──────────────────────────────────────────────────

    private ProviderStaffResponse toResponse(InstitutionMembership membership) {
        User user = userRepository.findByIdAndIsDeletedFalse(membership.getUserId()).orElse(null);
        return ProviderStaffResponse.builder()
                .userId(membership.getUserId())
                .email(user != null ? user.getEmail() : null)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .fullName(user != null ? user.getFullName() : null)
                .globalRole(user != null && user.getRole() != null ? user.getRole().name() : null)
                .membershipRole(membership.getRole() != null ? membership.getRole().name() : null)
                .responsibility(membership.getResponsibility())
                .isActive(membership.getIsActive())
                .isEmailVerified(user != null ? user.getIsEmailVerified() : null)
                .institutionId(membership.getInstitutionId())
                .providerId(membership.getProviderId())
                .membershipCreatedAt(membership.getCreatedAt())
                .updatedAt(membership.getUpdatedAt())
                .build();
    }

    private String normaliseResponsibility(String requested) {
        if (requested == null || requested.isBlank()) {
            return null;
        }
        String value = requested.trim().toUpperCase(Locale.ROOT);
        if (!RESPONSIBILITY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "A responsibility must be upper-case letters, digits and underscores, e.g. TRAINING_COORDINATOR");
        }
        return value;
    }

    /**
     * 32 hex characters from {@link SecureRandom} - 128 bits, so an invitation token cannot be
     * guessed or walked. Not a sequential id and not a predictable temporary password.
     */
    private static String randomToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Audit is best-effort by design: a logging failure must not roll back a legitimate
     * provisioning action, and must never surface credential or token material.
     *
     * <p>The {@link AuditLog.AuditAction} enum is a closed vocabulary of what kind of change was
     * made, so it records CREATE/UPDATE/DELETE and the specific event name travels in the context
     * map. Forcing a new enum constant per event would have widened a shared type for one feature,
     * and {@code AuditAction.valueOf} on an unlisted name throws - which, inside the catch below,
     * would have silently swallowed every audit write in this class.</p>
     */
    private void audit(UUID actorId, String actorRole, EducationProvider provider,
                       AuditLog.AuditAction action, String event, Map<String, Object> context) {
        try {
            Map<String, Object> detail = new java.util.LinkedHashMap<>(context);
            detail.put("event", event);
            auditService.recordAuditLog(
                    provider.getInstitutionId(), actorId, null, actorRole,
                    "PROVIDER_STAFF", provider.getId(), provider.getName(),
                    action, null, detail);
        } catch (RuntimeException e) {
            log.warn("Could not write provider staff audit entry {}: {}", event, e.getMessage());
        }
    }
}
