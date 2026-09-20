package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegationCreateRequest {
    private UUID delegatorId;
    private UUID delegateId;
    private String permissions;
    private String scope;
    private LocalDateTime expiresAt;
}
