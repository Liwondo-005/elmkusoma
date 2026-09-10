package tz.elmkusoma.shared.security;

import tz.elmkusoma.exception.ForbiddenException;

import java.util.UUID;

/**
 * Utility for enforcing institution ownership and role-based access control.
 * Use in service methods to verify that resources belong to the authenticated user's institution.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {}

    /**
     * Verifies that the resource belongs to the user's institution.
     * Throws ForbiddenException if the institution IDs don't match.
     *
     * @param resourceInstitutionId the institution ID on the resource entity
     * @param userInstitutionId     the authenticated user's institution ID from JWT
     * @param resourceType          human-readable resource name for the error message (e.g., "student", "course")
     */
    public static void verifyInstitution(UUID resourceInstitutionId, UUID userInstitutionId, String resourceType) {
        if (userInstitutionId == null) {
            throw new ForbiddenException("No institution context. Please authenticate with a valid institution.");
        }
        if (resourceInstitutionId == null) {
            throw new ForbiddenException(resourceType, "access — missing institution association");
        }
        if (!resourceInstitutionId.equals(userInstitutionId)) {
            throw new ForbiddenException(resourceType, "access — belongs to a different institution");
        }
    }

    /**
     * Verifies the user has one of the allowed roles.
     *
     * @param userRole    the user's role from the JWT/request attribute
     * @param allowedRoles the roles permitted to perform this action
     */
    public static void verifyRole(String userRole, String... allowedRoles) {
        if (userRole == null) {
            throw new ForbiddenException("No role assigned");
        }
        for (String allowed : allowedRoles) {
            if (userRole.equalsIgnoreCase(allowed)) {
                return;
            }
        }
        throw new ForbiddenException("perform this action", " — requires one of: " + String.join(", ", allowedRoles));
    }
}
