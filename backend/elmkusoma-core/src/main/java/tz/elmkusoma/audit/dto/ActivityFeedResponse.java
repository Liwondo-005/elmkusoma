package tz.elmkusoma.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static ActivityFeedResponse of(UUID id, UUID institutionId, UUID userId,
                                          String actorName, String action, String description,
                                          String entityType, UUID entityId, String entityName,
                                          Map<String, Object> metadata, String visibility,
                                          LocalDateTime createdAt) {
        ActivityFeedResponse resp = new ActivityFeedResponse();
        resp.id = id;
        resp.institutionId = institutionId;
        resp.userId = userId;
        resp.actorName = actorName;
        resp.action = action;
        resp.description = description;
        resp.entityType = entityType;
        resp.entityId = entityId;
        resp.entityName = entityName;
        resp.metadata = metadata;
        resp.visibility = visibility;
        resp.createdAt = createdAt;
        return resp;
    }
}