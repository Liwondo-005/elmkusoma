package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionAuditLogResponse {
    private UUID id;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
