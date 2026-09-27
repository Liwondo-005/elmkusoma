package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderDetailResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String description;
    private String address;
    private String city;
    private String region;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    private Boolean isActive;
    private String status;
    private String enabledServices;
    private String verificationStatus;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrgMemberResponse> admins;
    private List<ProviderVerificationItem> verificationHistory;
    private List<ProviderQuotaResponse> serviceEntitlements;
    private List<String> complianceFlags;
    private List<AuditLogResponse> recentAudit;
}
