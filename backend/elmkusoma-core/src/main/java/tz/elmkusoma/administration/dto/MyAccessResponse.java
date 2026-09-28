package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Effective access of the calling administrator inside the resolved
 * organization context. Everything is derived server-side from
 * OrganizationContext / OrganizationContextResolver — no client input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAccessResponse {

    private UUID userId;
    private String userEmail;
    private String systemRole;
    private String membershipRole;
    private String membershipStatus;
    private Boolean membershipActive;

    private UUID institutionId;
    private String institutionName;
    private String institutionType;
    private Boolean institutionActive;

    private Scope scope;
    private List<String> permissions;
    private List<OrganizationSummary> organizations;
    private List<DelegationSummary> delegations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scope {
        private String type;   // INSTITUTION | DEPARTMENT | CAMPUS
        private UUID id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationSummary {
        private UUID id;
        private String name;
        private String type;
        private String logoUrl;
        private Boolean active;
        private Boolean current;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DelegationSummary {
        private UUID id;
        private String scope;
        private String status;
        private List<String> permissions;
        private LocalDateTime startsAt;
        private LocalDateTime expiresAt;
    }
}
