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
}
