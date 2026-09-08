package tz.elmkusoma.audit.mapper;

import org.springframework.stereotype.Component;
import tz.elmkusoma.audit.domain.ActivityFeed;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.dto.ActivityFeedResponse;
import tz.elmkusoma.audit.dto.AuditLogResponse;
import tz.elmkusoma.audit.dto.SecurityEventResponse;

@Component
public class AuditMapper {

    public AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .institutionId(log.getInstitutionId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .userRole(log.getUserRole())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .action(log.getAction().name())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestMethod(log.getRequestMethod())
                .requestUrl(log.getRequestUrl())
                .responseStatus(log.getResponseStatus())
                .durationMs(log.getDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    public ActivityFeedResponse toActivityFeedResponse(ActivityFeed feed) {
        return ActivityFeedResponse.builder()
                .id(feed.getId())
                .institutionId(feed.getInstitutionId())
                .userId(feed.getUserId())
                .actorName(feed.getActorName())
                .action(feed.getAction())
                .description(feed.getDescription())
                .entityType(feed.getEntityType())
                .entityId(feed.getEntityId())
                .entityName(feed.getEntityName())
                .metadata(feed.getMetadata())
                .visibility(feed.getVisibility())
                .createdAt(feed.getCreatedAt())
                .build();
    }

    public SecurityEventResponse toSecurityEventResponse(SecurityEvent event) {
        return SecurityEventResponse.builder()
                .id(event.getId())
                .institutionId(event.getInstitutionId())
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())
                .eventType(event.getEventType().name())
                .description(event.getDescription())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .location(event.getLocation())
                .severity(event.getSeverity().name())
                .metadata(event.getMetadata())
                .resolved(event.getResolved())
                .resolvedAt(event.getResolvedAt())
                .resolvedBy(event.getResolvedBy())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
