package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationSummaryResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String verificationType;
    private String status;
    private UUID submittedBy;
    private UUID reviewedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
