package tz.elmkusoma.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class SecurityEventResponse {

    private UUID id;
    private UUID institutionId;
    private UUID userId;
    private String userEmail;
    private String eventType;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String location;
    private String severity;
    private Map<String, Object> metadata;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static SecurityEventResponse of(UUID id, UUID institutionId, UUID userId,
                                           String userEmail, String eventType, String description,
                                           String ipAddress, String userAgent, String location,
                                           String severity, Map<String, Object> metadata,
                                           Boolean resolved, LocalDateTime resolvedAt,
                                           UUID resolvedBy, LocalDateTime createdAt) {
        SecurityEventResponse resp = new SecurityEventResponse();
        resp.id = id;
        resp.institutionId = institutionId;
        resp.userId = userId;
        resp.userEmail = userEmail;
        resp.eventType = eventType;
        resp.description = description;
        resp.ipAddress = ipAddress;
        resp.userAgent = userAgent;
        resp.location = location;
        resp.severity = severity;
        resp.metadata = metadata;
        resp.resolved = resolved;
        resp.resolvedAt = resolvedAt;
        resp.resolvedBy = resolvedBy;
        resp.createdAt = createdAt;
        return resp;
    }
}