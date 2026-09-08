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
}
