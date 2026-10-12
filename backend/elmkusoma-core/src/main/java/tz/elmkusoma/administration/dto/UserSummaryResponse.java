package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean isActive;
    private UUID institutionId;
    private java.time.LocalDateTime createdAt;

    /**
     * Whether the address has been proven. Platform Admin needs this to tell a pending signup from
     * a usable account; without it a directory of unverified rows looked identical to a healthy one.
     */
    private Boolean isEmailVerified;

    /** Contact number, or null. Present for directory search - never a credential. */
    private String phone;

    /**
     * Every provider this person is scoped to, across all their memberships.
     *
     * <p>This is what makes the directory able to answer "who works for this provider". Without it
     * a provider employee looked identical to any other {@code PROVIDER_STAFF} account, because
     * {@code users} has no provider column - provider scope lives on the membership row.</p>
     *
     * <p>It is a list on purpose: a person may legitimately staff more than one provider, and
     * collapsing that to the first membership would both misreport their access and hide a
     * duplicate-identity bug.</p>
     */
    private java.util.List<UUID> providerIds;

    /** How many live memberships this person holds. Multi-role, multi-tenant, multi-provider. */
    private Integer membershipCount;
}
