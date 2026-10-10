package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.dto.InvitationResponse;
import tz.elmkusoma.administration.dto.PlatformInviteRequest;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.UUID;

/**
 * Platform-issued account invitations.
 *
 * <p>This is the only supported way to create a PROVIDER_ADMIN account. The alternative,
 * {@code PlatformAdminService.createUser}, takes an admin-chosen password and produces a
 * permanent credential; it stays in place for the existing platform-admin UI, but it is not the
 * path for granting provider authority, because a provider administrator should be created by
 * an invitation that expires, is delivered once, and lets the recipient set their own secret.</p>
 *
 * <p>It reuses {@link InstitutionPeopleService#inviteUser} so the role policy, expiry and
 * token generation stay in exactly one place.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAccountInvitationService {

    private final InstitutionPeopleService peopleService;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;

    /**
     * @throws org.springframework.security.access.AccessDeniedException if the target already
     *         has an account, so an invitation cannot be used as a password-reset side channel
     */
    @Transactional
    public InvitationResponse invite(PlatformInviteRequest request, UUID invitedBy) {
        String role = InvitationRolePolicy.assertInvitable(request.getRole(), true);
        if (!institutionRepository.existsByIdAndIsDeletedFalse(request.getInstitutionId())) {
            throw new IllegalArgumentException(
                    "Institution " + request.getInstitutionId() + " does not exist");
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            // Redeeming an invitation for an existing address would silently attach a second
            // organization to somebody's account. Surface it as a client error instead.
            //
            // Deliberately IllegalArgumentException, not DuplicateKeyException: a
            // DuplicateKeyException is a DataIntegrityViolationException, and that handler
            // replaces the message with a generic "Data conflict - constraint violation", so the
            // admin would never learn which address already exists or why it matters.
            throw new IllegalArgumentException(
                    "An account already exists for " + request.getEmail()
                            + ". Invitations activate a new account; to add an organization to an"
                            + " existing member, invite them from that institution instead.");
        }

        InvitationResponse invitation = peopleService.inviteUser(
                request.getInstitutionId(),
                tz.elmkusoma.administration.dto.InviteUserRequest.builder()
                        .email(request.getEmail())
                        .role(role)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .build(),
                invitedBy,
                true);

        log.info("Platform invitation issued by {} to {} for role {} in institution {}",
                invitedBy, request.getEmail(), role, request.getInstitutionId());
        return invitation;
    }
}
