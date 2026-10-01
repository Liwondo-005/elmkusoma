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
public class VerificationDetailResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String verificationType;
    private String status;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private List<String> documents;
    private String reviewerNotes;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
}
