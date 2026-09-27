package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationDetailResponse {
    private UUID id;
    private UUID delegatorId;
    private String delegatorName;
    private String delegatorEmail;
    private UUID delegateId;
    private String delegateName;
    private String delegateEmail;
    private String authority;
    private String permissions;
    private String scope;
    private List<UUID> resourceIds;
    private List<String> resourceNames;
    private String status;
    private String reason;
    private String notes;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime revokedAt;
    private UUID revokedBy;
    private String revokedByName;
    private String revocationReason;
    private LocalDateTime createdAt;
    private Boolean currentlyEffective;
}
