package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.InstitutionInvitation;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.config.security.PermissionCacheService;
import tz.elmkusoma.identity.domain.VerificationCode;
import tz.elmkusoma.identity.repository.VerificationCodeRepository;
import tz.elmkusoma.identity.service.OtpMailService;
import tz.elmkusoma.identity.service.PasswordPolicy;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
// Audit B-30: this service performs multi-repository writes (10 save() calls across user,
// membership and invitation repositories). Without a transaction boundary a partial failure
// could persist a user without a membership -- which the authorization resolver reads as
// "no organization". Class-level so every existing entry point becomes atomic.
@Transactional
public class InstitutionPeopleService {

    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final InstitutionInvitationRepository invitationRepository;
    private final InstitutionScopeService scopeService;
    private final PlatformPolicyService platformPolicyService;
    private final PermissionCacheService permissionCacheService;
    private final PasswordEncoder passwordEncoder;
    private final EducationProviderRepository educationProviderRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final OtpMailService otpMailService;
    private final PasswordPolicy passwordPolicy;

    /** Mirrors the OTP lifetime used by the public registration verification flow. */
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final String PENDING = "PENDING";
    private static final String AWAITING_VERIFICATION = "AWAITING_VERIFICATION";
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    public List<PeopleMemberResponse> listPeople(UUID institutionId, int page, int size) {
        List<User> users = scopeService.getUsersInInstitution(institutionId);
        int start = page * size;
        int end = Math.min(start + size, users.size());

        if (start >= users.size()) return List.of();

        return users.subList(start, end).stream()
                .map(user -> {
                    InstitutionMembership.Role membershipRole = scopeService.getMembershipRole(user.getId(), institutionId);
                    return PeopleMemberResponse.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .middleName(user.getMiddleName())
                            .lastName(user.getLastName())
                            .fullName(user.getFullName())
                            .phone(user.getPhone())
                            .membershipRole(membershipRole != null ? membershipRole.name() : user.getRole().name())
                            .isActive(user.getIsActive())
                            .isEmailVerified(user.getIsEmailVerified())
                            .profileImageUrl(user.getProfileImageUrl())
                            .build();
                })
                .toList();
    }

    public PeopleMemberResponse getMember(UUID institutionId, UUID memberUserId) {
        User user = userRepository.findById(memberUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!scopeService.isUserMemberOfInstitution(memberUserId, institutionId)) {
            throw new SecurityException("User is not a member of this institution");
        }

        InstitutionMembership.Role membershipRole = scopeService.getMembershipRole(memberUserId, institutionId);
        return PeopleMemberResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .membershipRole(membershipRole != null ? membershipRole.name() : user.getRole().name())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public PeopleMemberResponse updateMemberRole(UUID institutionId, UUID memberUserId, String newRole, String updatedBy) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(memberUserId);
        InstitutionMembership membership = memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Membership not found"));

        InstitutionMembership.Role role = InstitutionMembership.Role.valueOf(newRole.toUpperCase());
        membership.setRole(role);
        membershipRepository.save(membership);

        permissionCacheService.invalidateUserPermissions(memberUserId, institutionId);
        permissionCacheService.invalidateUserRole(memberUserId, institutionId);
        permissionCacheService.invalidateMembership(memberUserId, institutionId);

        User user = userRepository.findById(memberUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Keep users.role (JWT role claim + @PreAuthorize checks) consistent with
        // the membership role; otherwise role changes silently do not take effect.
        User.Role mappedRole = mapMembershipToUserRole(role);
        boolean preservePlatformAdmin = user.getRole() == User.Role.NATIONAL_ADMIN
                && mappedRole != User.Role.NATIONAL_ADMIN;
        if (!preservePlatformAdmin && user.getRole() != mappedRole) {
            user.setRole(mappedRole);
            userRepository.save(user);
        }

        InstitutionMembership.Role membershipRole = scopeService.getMembershipRole(memberUserId, institutionId);
        return PeopleMemberResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .membershipRole(membershipRole != null ? membershipRole.name() : user.getRole().name())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public void deactivateMember(UUID institutionId, UUID memberUserId) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(memberUserId);
        memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .findFirst()
                .ifPresent(m -> {
                    m.setIsActive(false);
                    membershipRepository.save(m);
                });

        permissionCacheService.invalidateUserPermissions(memberUserId, institutionId);
        permissionCacheService.invalidateUserRole(memberUserId, institutionId);
        permissionCacheService.invalidateMembership(memberUserId, institutionId);
    }

    public void activateMember(UUID institutionId, UUID memberUserId) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(memberUserId);
        memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .findFirst()
                .ifPresent(m -> {
                    m.setIsActive(true);
                    membershipRepository.save(m);
                });

        permissionCacheService.invalidateUserPermissions(memberUserId, institutionId);
        permissionCacheService.invalidateUserRole(memberUserId, institutionId);
        permissionCacheService.invalidateMembership(memberUserId, institutionId);
    }

    /**
     * Issues an invitation scoped to one institution.
     *
     * <p>The requested role is validated against {@link InvitationRolePolicy} before anything
     * is persisted: an unvalidated role field here would let any tenant administrator mint a
     * PROVIDER_ADMIN or platform-role invitation, and accepting it would write a global
     * {@code users.role}. Platform-only roles require a platform actor.</p>
     *
     * @param platformActor true when the inviter is a platform administrator
     */
    public InvitationResponse inviteUser(UUID institutionId, InviteUserRequest request, UUID invitedBy,
                                        boolean platformActor) {
        if (platformPolicyService != null && !platformPolicyService.inviteEnabled()) {
            throw new IllegalStateException("Platform policy forbids inviting participants");
        }
        String role = InvitationRolePolicy.assertInvitable(request.getRole(), platformActor);
        String token = UUID.randomUUID().toString().replace("-", "");
        InstitutionInvitation invitation = InstitutionInvitation.builder()
                .institutionId(institutionId)
                .email(request.getEmail())
                .role(role)
                .invitedBy(invitedBy)
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        invitation = invitationRepository.save(invitation);

        log.info("Invitation sent to {} for institution {} with role {}", request.getEmail(), institutionId, role);

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

    public List<InvitationResponse> listInvitations(UUID institutionId) {
        return invitationRepository.findByInstitutionId(institutionId).stream()
                .map(inv -> InvitationResponse.builder()
                        .id(inv.getId())
                        .email(inv.getEmail())
                        .role(inv.getRole())
                        .status(inv.getStatus())
                        .expiresAt(inv.getExpiresAt())
                        .createdAt(inv.getCreatedAt())
                        // §11: tokens are never re-listed — only the create response exposes them once
                        .build())
                .toList();
    }

    public void cancelInvitation(UUID institutionId, UUID invitationId) {
        InstitutionInvitation invitation = invitationRepository.findById(invitationId)
                // Typed so a bad id is a 404. A bare RuntimeException is unmapped, so asking to
                // cancel an invitation that does not exist reported an HTTP 500 server fault -
                // which reads as "the system broke" rather than "that id is wrong".
                .orElseThrow(() -> new tz.elmkusoma.common.exception.ResourceNotFoundException(
                        "Invitation", "id", invitationId));
        // Reported as not-found rather than forbidden: telling a caller that an id exists but
        // belongs elsewhere is an existence oracle across tenants.
        if (!invitation.getInstitutionId().equals(institutionId)) {
            throw new tz.elmkusoma.common.exception.ResourceNotFoundException(
                    "Invitation", "id", invitationId);
        }
        invitation.setStatus("CANCELLED");
        invitation.setIsDeleted(true);
        invitationRepository.save(invitation);
    }

    /**
     * Step 1 of activation: proves the invitation is live and mails a one-time code to the
     * address it was issued for. It deliberately creates nothing.
 *
 * <p>The invitation token proves the <em>inviter's</em> intent, not the redeemer's identity:
     * the token is returned in the invite's API response, so the inviter always holds it. If the
     * token alone could activate an account, any administrator could register an account under
     * a third party's address with a password of their choosing - and that party could then
     * never claim it, because registration treats the address as taken. Ownership of the mailbox
     * is therefore a second, independent factor, checked in {@link #confirmInvitation}.</p>
     */
    @Transactional
    public void startInvitationActivation(String token) {
        InstitutionInvitation invitation = loadRedeemableInvitation(token);

        // Always the invited address. Never a client-supplied one, or the factor proves nothing.
        String email = invitation.getEmail().toLowerCase(Locale.ROOT);

        VerificationCode existing = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email).orElse(null);
        if (existing != null && existing.getCreatedAt() != null
                && existing.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting a new code");
        }

        // A resend supersedes every older unused code so a stale one can never activate.
        for (VerificationCode stale : verificationCodeRepository
                .findAllByEmailAndUsedFalseOrderByCreatedAtDesc(email)) {
            stale.setUsed(true);
            verificationCodeRepository.save(stale);
        }

        String code = String.format("%05d", SECURE_RANDOM.nextInt(100_000));
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code(sha256Hex(code))
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES))
                .used(false)
                .attempts(0)
                .build());

        otpMailService.sendVerificationCode(email, code);

        // The invitation stays redeemable while the code is outstanding; only acceptance closes it.
        invitation.setStatus("AWAITING_VERIFICATION");
        invitationRepository.save(invitation);

        log.info("Invitation activation code issued for {} (invitation {})", email, invitation.getId());
    }

    /**
     * Step 2 of activation: the code proves the caller controls the invited mailbox, and only
     * then is the account created and the membership bound.
     */
    @Transactional
    public void confirmInvitation(String token, String code, String password) {
        InstitutionInvitation invitation = loadRedeemableInvitation(token);

        // Order matters: reject a weak password before spending an attempt.
        passwordPolicy.rejectWeak(invitation.getEmail(), password);

        String email = invitation.getEmail().toLowerCase(Locale.ROOT);
        VerificationCode verification = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enter the verification code sent to " + email));

        if (!verification.isValid()) {
            // Count the miss so a 5-digit space cannot be walked, and reuse the same message
            // for "wrong code", "expired" and "locked" so none of them are distinguishable.
            verification.setAttempts(verification.getAttempts() + 1);
            verificationRepositorySave(verification);
            throw new IllegalArgumentException("That verification code is not valid");
        }
        if (!constantTimeEquals(verification.getCode(), sha256Hex(code))) {
            verification.setAttempts(verification.getAttempts() + 1);
            verificationRepositorySave(verification);
            throw new IllegalArgumentException("That verification code is not valid");
        }

        verification.setUsed(true);
        verificationRepositorySave(verification);

        // Create or find user. §11: a new account receives the invited organization role —
        // existing accounts keep their global identity (multi-role via membership, §13).
        InstitutionMembership.Role membershipRole = switch (invitation.getRole() != null ? invitation.getRole() : "") {
            case "TEACHER" -> InstitutionMembership.Role.TEACHER;
            case "ADMIN", "INSTITUTION_ADMIN" -> InstitutionMembership.Role.ADMIN;
            case "PARENT" -> InstitutionMembership.Role.PARENT;
            case "INSTRUCTOR" -> InstitutionMembership.Role.INSTRUCTOR;
            case "OTHER_LEARNER" -> InstitutionMembership.Role.OTHER_LEARNER;
            case "PROVIDER_ADMIN" -> InstitutionMembership.Role.ADMIN;
            case "PROVIDER_STAFF" -> InstitutionMembership.Role.TEACHER;
            default -> InstitutionMembership.Role.STUDENT;
        };

        // The invitation's role is re-checked here, not just at creation. Rows predate this
        // policy, and a hand-written or replayed invitation must not be able to grant a role
        // its issuer could not have granted. The issuer's authority is read from their current
        // user row rather than trusted from the invitation, so a demoted or deleted issuer
        // fails closed instead of leaving a redeemable platform-only token.
        if (InvitationRolePolicy.isPlatformOnly(invitation.getRole())) {
            boolean issuerMayStillGrant = mayIssuerGrantRole(invitation);
            if (!issuerMayStillGrant) {
                throw new SecurityException("This invitation cannot grant the requested role");
            }
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(invitation.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(invitation.getEmail())
                            .passwordHash(passwordEncoder.encode(password))
                            .firstName("New")
                            .lastName("User")
                            // The global role is derived from the invitation, never from the
                            // request body. Deriving it from the *membership* role would turn a
                            // PROVIDER_ADMIN invitation into a global INSTITUTION_ADMIN, because
                            // both map to membership ADMIN.
                            .role(globalRoleForInvitation(invitation.getRole()))
                            .isActive(true)
                            .isEmailVerified(true)
                            .build();
                    newUser.setInstitutionId(invitation.getInstitutionId());
                    return userRepository.save(newUser);
                });

        // Existing user: the mailbox is now proven, so the address may be marked verified, but
        // never re-point their account at another tenant and never resurrect a suspended one.
        // Both were reachable before: an institution administrator could invite an existing
        // user from a different institution, keep the token, and redeem it to move that
        // person's global institution_id into their own tenant.
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            user.setIsEmailVerified(true);
        }
        if (user.getInstitutionId() == null) {
            user.setInstitutionId(invitation.getInstitutionId());
        }
        userRepository.save(user);

        // Create membership (or refresh the existing one — never duplicate rows per org)
        InstitutionMembership membership = membershipRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .filter(m -> m.getInstitutionId().equals(invitation.getInstitutionId()))
                .findFirst()
                .orElseGet(() -> InstitutionMembership.builder()
                        .userId(user.getId())
                        .institutionId(invitation.getInstitutionId())
                        .isDeleted(false)
                        .build());
        membership.setRole(membershipRole); // §48: invited role wins over any client-supplied value
        membership.setIsActive(true);
        // A provider-role invitation must arrive pre-bound to the provider record, the same way
        // PlatformAdminService.createUser binds it, or the account has no provider scope. The
        // provider the invitation named wins, so an owner staffing one provider in a
        // multi-provider institution cannot accidentally grant the wrong one.
        UUID providerScope = resolveProviderScopeForInvitation(invitation.getRole(),
                invitation.getInstitutionId(), invitation.getProviderId());
        if (providerScope != null) {
            membership.setProviderId(providerScope);
        }
        membershipRepository.save(membership);

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        log.info("Invitation accepted for user {} in institution {}", user.getEmail(), invitation.getInstitutionId());
    }

    /**
     * Loads an invitation that may still be activated, or throws.
     *
     * <p>AWAITING_VERIFICATION is accepted here as well as PENDING: the code step moves the
     * invitation into that state, so confirmation has to be able to read it back.</p>
     */
    private InstitutionInvitation loadRedeemableInvitation(String token) {
        InstitutionInvitation invitation = invitationRepository.findByTokenAndIsDeletedFalse(token)
                // A typed not-found rather than a bare RuntimeException: this endpoint is
                // reachable without a session, and an unmapped exception would report a guessed
                // token as an HTTP 500 server fault instead of a client error.
                .orElseThrow(() -> new tz.elmkusoma.common.exception.ResourceNotFoundException(
                        "Invitation", "token", token));

        if (!PENDING.equals(invitation.getStatus()) && !AWAITING_VERIFICATION.equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is not pending");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Invitation has expired");
        }
        return invitation;
    }

    private void verificationRepositorySave(VerificationCode verification) {
        verificationCodeRepository.save(verification);
    }

    /** Length-independent comparison of two hex digests. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** Inverse of OrganizationContextResolver.mapUserRoleToMembershipRole. */
    private static User.Role mapMembershipToUserRole(InstitutionMembership.Role membershipRole) {
        return switch (membershipRole) {
            case ADMIN, OWNER, INSTITUTION_ADMIN -> User.Role.INSTITUTION_ADMIN;
            case TEACHER -> User.Role.TEACHER;
            case STUDENT -> User.Role.STUDENT;
            case PARENT -> User.Role.PARENT;
            case OTHER_LEARNER -> User.Role.OTHER_LEARNER;
            case INSTRUCTOR -> User.Role.INSTRUCTOR;
            case NATIONAL_ADMIN -> User.Role.NATIONAL_ADMIN;
        };
    }

    /**
     * The global {@code users.role} for an account created by accepting an invitation.
     *
     * <p>Keyed on the invitation's role, not the membership role, because membership collapses
     * distinctions the global role must keep: PROVIDER_ADMIN and INSTITUTION_ADMIN are both
     * membership ADMIN, and collapsing them would hand a provider operator global institution
     * administrator authority. An unrecognised role yields STUDENT, the least privilege.</p>
     */
    private static User.Role globalRoleForInvitation(String invitationRole) {
        if (invitationRole == null) {
            return User.Role.STUDENT;
        }
        try {
            return switch (invitationRole.trim().toUpperCase(java.util.Locale.ROOT)) {
                case "STUDENT" -> User.Role.STUDENT;
                case "LEARNER", "OTHER_LEARNER" -> User.Role.OTHER_LEARNER;
                case "TEACHER" -> User.Role.TEACHER;
                case "INSTRUCTOR" -> User.Role.INSTRUCTOR;
                case "PARENT" -> User.Role.PARENT;
                case "PROVIDER_ADMIN" -> User.Role.PROVIDER_ADMIN;
                case "PROVIDER_STAFF" -> User.Role.PROVIDER_STAFF;
                case "INSTITUTION_ADMIN" -> User.Role.INSTITUTION_ADMIN;
                default -> User.Role.STUDENT;
            };
        } catch (IllegalArgumentException e) {
            return User.Role.STUDENT;
        }
    }

    /**
     * Whether the invitation's issuer may still grant this invitation's role.
     *
     * <p>Two authorities can create a PROVIDER_STAFF invitation. Platform Admin can, as it always
     * could. A Provider Owner can too, but only for staff of a provider they actually administer -
     * the level C to D workflow. The distinction is carried by {@code provider_id}: an invitation
     * that names a provider is an owner-issued one and is re-checked against live membership, so an
     * owner demoted or removed after issuing still has a token that fails closed.</p>
     *
     * <p>An owner-issued invitation for a provider the issuer no longer owns is refused, and a
     * PROVIDER_STAFF invitation with no {@code provider_id} keeps requiring a platform issuer
     * exactly as before. PROVIDER_ADMIN and every platform role are unchanged: they remain
     * platform-only, and an owner cannot mint a peer owner.</p>
     */
    private boolean mayIssuerGrantRole(InstitutionInvitation invitation) {
        Optional<User> issuer = userRepository.findByIdAndIsDeletedFalse(invitation.getInvitedBy());
        if (issuer.isEmpty()) {
            // Deleted issuer: nothing left that could authorise this.
            return false;
        }
        String issuerRole = issuer.get().getRole() == null ? "" : issuer.get().getRole().name();
        if (InvitationRolePolicy.isPlatformActor(issuerRole)) {
            return true;
        }

        String invitedRole = invitation.getRole() == null
                ? "" : invitation.getRole().trim().toUpperCase(Locale.ROOT);
        if (!"PROVIDER_STAFF".equals(invitedRole) || invitation.getProviderId() == null) {
            return false;
        }
        return membershipRepository.findByUserIdAndIsActiveTrue(invitation.getInvitedBy()).stream()
                .anyMatch(m -> !Boolean.TRUE.equals(m.getIsDeleted())
                        && invitation.getProviderId().equals(m.getProviderId()));
    }

    /**
     * Binds a provider-role membership to a provider record, mirroring
     * {@code PlatformAdminService.resolveProviderScope}. Without it the account would have no
     * {@code provider_id} and its provider authority would be unscoped.
     *
     * <p>An invitation that names its provider (provider staff invited through a provider's own
     * workspace) is honoured exactly. Only when it does not - an older row, or a platform-level
     * invitation - does this fall back to the institution's first live provider. The {@code findFirst}
     * fallback is now a genuine last resort rather than the normal path: an institution can host
     * more than one provider, and granting the first one by accident bound staff to a provider
     * nobody chose.</p>
     */
    private UUID resolveProviderScopeForInvitation(String invitationRole, UUID institutionId,
                                                   UUID chosenProviderId) {
        String role = invitationRole == null ? "" : invitationRole.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"PROVIDER_ADMIN".equals(role) && !"PROVIDER_STAFF".equals(role)) {
            return null;
        }
        if (chosenProviderId != null) {
            boolean live = educationProviderRepository.findById(chosenProviderId)
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .isPresent();
            // Fall through to the legacy lookup if the recorded provider has since been deleted,
            // so an invitation issued before that still redeems rather than becoming unredeemable.
            if (live) {
                return chosenProviderId;
            }
        }
        return educationProviderRepository.findAllByInstitutionId(institutionId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(tz.elmkusoma.nfe.provider.domain.EducationProvider::getId)
                .findFirst()
                .orElse(null);
    }
}
