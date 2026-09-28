package tz.elmkusoma.administration.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationSubmitRequest {
    private String entityType;
    private UUID entityId;
    private String verificationType;
    private UUID submittedBy;
    private String documents;
    private String notes;
}
