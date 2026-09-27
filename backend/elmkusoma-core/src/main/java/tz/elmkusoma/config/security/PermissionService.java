package tz.elmkusoma.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.shared.domain.InstitutionMembership;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.ofEntries(
            // Platform Admin - full access
            Map.entry("ADMIN", Set.of(
                    "*", "CREATE_COURSE", "INVITE_USER", "SCHEDULE_LIVE", "CREATE_EVENT",
                    "UPLOAD_RESOURCE", "REVIEW_CONTENT", "VERIFY_PAYMENT", "ISSUE_CERTIFICATE",
                    "MANAGE_USERS", "MANAGE_ROLES", "MANAGE_SETTINGS", "VIEW_AUDIT",
                    "EXPORT_DATA", "MANAGE_INSTITUTION"
            )),
            // Institution Admin - full institution access
            Map.entry("INSTITUTION_ADMIN", Set.of(
                    "CREATE_COURSE", "INVITE_USER", "SCHEDULE_LIVE", "CREATE_EVENT",
                    "UPLOAD_RESOURCE", "REVIEW_CONTENT", "VERIFY_PAYMENT", "ISSUE_CERTIFICATE",
                    "MANAGE_USERS", "MANAGE_ROLES", "MANAGE_SETTINGS", "VIEW_AUDIT",
                    "EXPORT_DATA", "MANAGE_INSTITUTION"
            )),
            // Owner - full access
            Map.entry("OWNER", Set.of(
                    "CREATE_COURSE", "INVITE_USER", "SCHEDULE_LIVE", "CREATE_EVENT",
                    "UPLOAD_RESOURCE", "REVIEW_CONTENT", "VERIFY_PAYMENT", "ISSUE_CERTIFICATE",
                    "MANAGE_USERS", "MANAGE_ROLES", "MANAGE_SETTINGS", "VIEW_AUDIT",
                    "EXPORT_DATA", "MANAGE_INSTITUTION"
            )),
            // Teacher - teaching-focused
            Map.entry("TEACHER", Set.of(
                    "CREATE_COURSE", "SCHEDULE_LIVE", "UPLOAD_RESOURCE", "REVIEW_CONTENT",
                    "VIEW_STUDENT_PROGRESS", "MANAGE_GRADES"
            )),
            // Instructor - teaching-focused
            Map.entry("INSTRUCTOR", Set.of(
                    "CREATE_COURSE", "SCHEDULE_LIVE", "UPLOAD_RESOURCE", "REVIEW_CONTENT",
                    "VIEW_STUDENT_PROGRESS", "MANAGE_GRADES"
            )),
            // Finance Admin - payment focused
            Map.entry("FINANCE_ADMIN", Set.of(
                    "VERIFY_PAYMENT", "VIEW_PAYMENT_REPORTS", "EXPORT_FINANCIAL_DATA",
                    "MANAGE_REFUNDS", "VIEW_TRANSACTIONS"
            )),
            // Content Admin - content review
            Map.entry("CONTENT_ADMIN", Set.of(
                    "REVIEW_CONTENT", "PUBLISH_CONTENT", "MANAGE_MEDIA",
                    "UPLOAD_RESOURCE", "VIEW_CONTENT_REPORTS"
            )),
            // Support Admin - support tickets
            Map.entry("SUPPORT_ADMIN", Set.of(
                    "VIEW_TICKETS", "ASSIGN_TICKETS", "RESPOND_TICKETS",
                    "VIEW_USER_PROFILES", "EXPORT_SUPPORT_DATA"
            ))
    );

    public List<String> getPermissionsForRole(String role) {
        String normalizedRole = role.toUpperCase();
        if ("INSTITUTION_ADMIN".equals(normalizedRole)) {
            return new ArrayList<>(ROLE_PERMISSIONS.getOrDefault("INSTITUTION_ADMIN", Set.of()));
        }
        return new ArrayList<>(ROLE_PERMISSIONS.getOrDefault(normalizedRole, Set.of()));
    }

    public List<String> getPermissionsForMembershipRole(InstitutionMembership.Role role) {
        // Map membership roles to permission roles
        String permissionRole = switch (role) {
            case OWNER -> "OWNER";
            case ADMIN, INSTITUTION_ADMIN -> "INSTITUTION_ADMIN";
            case NATIONAL_ADMIN -> "ADMIN";
            case TEACHER, INSTRUCTOR -> "TEACHER";
            default -> "STUDENT";
        };
        return getPermissionsForRole(permissionRole);
    }

    public List<String> getCombinedPermissions(List<InstitutionMembership> memberships) {
        Set<String> permissions = new HashSet<>();
        for (InstitutionMembership membership : memberships) {
            permissions.addAll(getPermissionsForMembershipRole(membership.getRole()));
        }
        return new ArrayList<>(permissions);
    }
}