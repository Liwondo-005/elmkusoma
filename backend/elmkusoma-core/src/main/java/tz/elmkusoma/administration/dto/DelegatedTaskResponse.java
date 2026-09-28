package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DelegatedTaskResponse {
    private UUID verificationId;
    private String verificationType;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String status;
    private LocalDateTime submittedAt;
    private UUID delegationId;
    private String authority;
    private String scope;
}
