package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationCreateRequest {
    private UUID delegatorId;
    private UUID delegateId;
    private String permissions;
    private String scope;
    private String authority;
    private String reason;
    private String notes;
    private List<UUID> resourceIds;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean requiresApproval;
}
