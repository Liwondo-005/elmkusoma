package tz.elmkusoma.administration.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IncidentSummaryResponse {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String status;
    private String affectedService;
    private String assignedTo;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
