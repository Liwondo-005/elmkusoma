package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionScopeService {

    private final InstitutionMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public boolean isUserMemberOfInstitution(UUID userId, UUID institutionId) {
        return membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId);
    }

    public boolean isUserAdminOfInstitution(UUID userId, UUID institutionId) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(userId);
        return memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .anyMatch(m -> m.getRole() == InstitutionMembership.Role.OWNER
                        || m.getRole() == InstitutionMembership.Role.ADMIN
                        || m.getRole() == InstitutionMembership.Role.INSTITUTION_ADMIN);
    }

    public boolean isUserOwnerOfInstitution(UUID userId, UUID institutionId) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(userId);
        return memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .anyMatch(m -> m.getRole() == InstitutionMembership.Role.OWNER);
    }

    public InstitutionMembership.Role getMembershipRole(UUID userId, UUID institutionId) {
        List<InstitutionMembership> memberships = membershipRepository.findByUserIdAndIsActiveTrue(userId);
        return memberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .map(InstitutionMembership::getRole)
                .findFirst()
                .orElse(null);
    }

    public InstitutionMembership getMembership(UUID userId, UUID institutionId) {
        return membershipRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .findFirst()
                .orElse(null);
    }

    public List<UUID> getInstitutionIdsForUser(UUID userId) {
        return membershipRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(InstitutionMembership::getInstitutionId)
                .toList();
    }

    public List<User> getUsersInInstitution(UUID institutionId) {
        return userRepository.findAllByInstitutionId(institutionId);
    }

    public long countUsersInInstitution(UUID institutionId, InstitutionMembership.Role role) {
        return membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                List.of(institutionId), role);
    }

    public boolean hasScopeAccess(UUID userId, UUID institutionId, UUID scopeId, String scopeType) {
        InstitutionMembership membership = getMembership(userId, institutionId);
        if (membership == null) return false;

        if ("department".equals(scopeType)) {
            return membership.getDepartmentId() != null && membership.getDepartmentId().equals(scopeId);
        } else if ("campus".equals(scopeType)) {
            return membership.getCampusId() != null && membership.getCampusId().equals(scopeId);
        }
        return false;
    }

    public void validateAccess(UUID userId, UUID institutionId, String operation) {
        if (!isUserMemberOfInstitution(userId, institutionId)) {
            log.warn("IDOR prevention: User {} attempted {} on institution {} without membership",
                    userId, operation, institutionId);
            throw new SecurityException("Access denied: you are not a member of this institution");
        }
    }

    public void validateAdminAccess(UUID userId, UUID institutionId, String operation) {
        if (!isUserAdminOfInstitution(userId, institutionId)) {
            log.warn("IDOR prevention: User {} attempted admin operation '{}' on institution {} without admin role",
                    userId, operation, institutionId);
            throw new SecurityException("Access denied: admin privileges required for this operation");
        }
    }

    public void validateScopeAccess(UUID userId, UUID institutionId, UUID scopeId, String scopeType, String operation) {
        if (!hasScopeAccess(userId, institutionId, scopeId, scopeType)) {
            log.warn("Scope violation: User {} attempted {} on {} {} in institution {} without scope access",
                    userId, operation, scopeType, scopeId, institutionId);
            throw new SecurityException("Access denied: insufficient scope permissions for this operation");
        }
    }
}
