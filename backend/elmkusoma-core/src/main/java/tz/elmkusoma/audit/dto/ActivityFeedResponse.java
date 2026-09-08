package tz.elmkusoma.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedResponse {

    private UUID id;
    private UUID institutionId;
    private UUID userId;
    private String actorName;
    private String action;
    private String description;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private Map<String, Object> metadata;
    private String visibility;
    private LocalDateTime createdAt;
}
