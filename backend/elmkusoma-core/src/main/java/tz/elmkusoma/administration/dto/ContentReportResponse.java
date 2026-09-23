package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ContentReportResponse {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String entityTitle;
    private UUID reporterId;
    private String reason;
    private String description;
    private String status;
    private String resolutionNotes;
    private UUID resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
