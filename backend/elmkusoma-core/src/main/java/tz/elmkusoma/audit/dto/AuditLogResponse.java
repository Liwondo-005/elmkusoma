package tz.elmkusoma.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AuditLogResponse {

    private UUID id;
    private UUID institutionId;
    private UUID userId;
    private String userEmail;
    private String userRole;
    private String entityType;
    private UUID entityId;
    private String entityName;
    private String action;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestUrl;
    private Integer responseStatus;
    private Long durationMs;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, Object> getOldValues() { return oldValues; }
    public void setOldValues(Map<String, Object> oldValues) { this.oldValues = oldValues; }
    public Map<String, Object> getNewValues() { return newValues; }
    public void setNewValues(Map<String, Object> newValues) { this.newValues = newValues; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static AuditLogResponse of(UUID id, UUID institutionId, UUID userId,
                                      String userEmail, String userRole, String entityType,
                                      UUID entityId, String entityName, String action,
                                      Map<String, Object> oldValues, Map<String, Object> newValues,
                                      String ipAddress, String userAgent, String requestMethod,
                                      String requestUrl, Integer responseStatus, Long durationMs,
                                      LocalDateTime createdAt) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.id = id;
        resp.institutionId = institutionId;
        resp.userId = userId;
        resp.userEmail = userEmail;
        resp.userRole = userRole;
        resp.entityType = entityType;
        resp.entityId = entityId;
        resp.entityName = entityName;
        resp.action = action;
        resp.oldValues = oldValues;
        resp.newValues = newValues;
        resp.ipAddress = ipAddress;
        resp.userAgent = userAgent;
        resp.requestMethod = requestMethod;
        resp.requestUrl = requestUrl;
        resp.responseStatus = responseStatus;
        resp.durationMs = durationMs;
        resp.createdAt = createdAt;
        return resp;
    }
}