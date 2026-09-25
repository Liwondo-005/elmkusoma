package tz.elmkusoma.config.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tz.elmkusoma.shared.domain.InstitutionMembership;

import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class OrganizationContext {

    private final UUID userId;
    private final String userEmail;
    private final String userRole;
    private final UUID institutionId;
    private final InstitutionMembership.Role membershipRole;
    private final List<InstitutionMembership> allMemberships;
    private final List<UUID> accessibleInstitutionIds;
    private final List<String> userPermissions;

    public boolean isMemberOf(UUID institutionId) {
        return accessibleInstitutionIds.contains(institutionId);
    }

    public boolean isAdminOf(UUID institutionId) {
        if (!isMemberOf(institutionId)) return false;
        return membershipRole == InstitutionMembership.Role.OWNER
                || membershipRole == InstitutionMembership.Role.ADMIN
                || membershipRole == InstitutionMembership.Role.INSTITUTION_ADMIN;
    }

    public boolean isOwnerOf(UUID institutionId) {
        if (!isMemberOf(institutionId)) return false;
        return membershipRole == InstitutionMembership.Role.OWNER;
    }

    public UUID getEffectiveInstitutionId() {
        return institutionId;
    }

    public boolean hasScope(UUID scopeId, String scopeType) {
        return allMemberships.stream()
                .filter(m -> m.getInstitutionId().equals(institutionId))
                .anyMatch(m -> {
                    if (scopeType.equals("department")) {
                        return m.getDepartmentId() != null && m.getDepartmentId().equals(scopeId);
                    } else if (scopeType.equals("campus")) {
                        return m.getCampusId() != null && m.getCampusId().equals(scopeId);
                    }
                    return false;
                });
    }

    public boolean hasPermission(String permission) {
        return userPermissions != null && userPermissions.contains(permission);
    }
}