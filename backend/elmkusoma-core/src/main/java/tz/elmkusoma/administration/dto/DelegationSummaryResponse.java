package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationSummaryResponse {
    private UUID id;
    private UUID delegatorId;
    private String delegatorName;
    private UUID delegateId;
    private String delegateName;
    private String permissions;
    private String scope;
    private String authority;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
