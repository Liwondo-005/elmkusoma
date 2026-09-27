package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderVerificationItem {
    private UUID id;
    private String verificationType;
    private String status;
    private UUID submittedBy;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private UUID reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String notes;
    private String documents;
}
