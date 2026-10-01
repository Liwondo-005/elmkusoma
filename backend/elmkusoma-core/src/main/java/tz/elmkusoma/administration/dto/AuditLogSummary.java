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
public class AuditLogSummary {
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String action;
    private String actorName;
    private String actorRole;
    private String institutionName;
    private LocalDateTime timestamp;
}
