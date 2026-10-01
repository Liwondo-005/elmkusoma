package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSummary {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String verificationType;
    private String status;
    private UUID submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
