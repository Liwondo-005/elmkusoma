package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tz.elmkusoma.administration.domain.InstitutionInvitation;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionPeopleService {

    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final InstitutionInvitationRepository invitationRepository;
    private final InstitutionScopeService scopeService;
    private final PlatformPolicyService platformPolicyService;

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

        User user = userRepository.findById(memberUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
    }

    public InvitationResponse inviteUser(UUID institutionId, InviteUserRequest request, UUID invitedBy) {
        if (platformPolicyService != null && !platformPolicyService.inviteEnabled()) {
            throw new IllegalStateException("Platform policy forbids inviting participants");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        InstitutionInvitation invitation = InstitutionInvitation.builder()
                .institutionId(institutionId)
                .email(request.getEmail())
                .role(request.getRole())
                .invitedBy(invitedBy)
                .token(token)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        invitation = invitationRepository.save(invitation);

        log.info("Invitation sent to {} for institution {} with role {}", request.getEmail(), institutionId, request.getRole());

        return InvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
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
                        .build())
                .toList();
    }

    public void cancelInvitation(UUID institutionId, UUID invitationId) {
        InstitutionInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        if (!invitation.getInstitutionId().equals(institutionId)) {
            throw new SecurityException("Invitation does not belong to this institution");
        }
        invitation.setStatus("CANCELLED");
        invitation.setIsDeleted(true);
        invitationRepository.save(invitation);
    }
}
