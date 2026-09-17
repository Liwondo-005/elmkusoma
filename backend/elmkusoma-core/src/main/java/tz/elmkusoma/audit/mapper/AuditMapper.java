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
        return AuditLogResponse.of(
                log.getId(),
                log.getInstitutionId(),
                log.getUserId(),
                log.getUserEmail(),
                log.getUserRole(),
                log.getEntityType(),
                log.getEntityId(),
                log.getEntityName(),
                log.getAction().name(),
                log.getOldValues(),
                log.getNewValues(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getRequestMethod(),
                log.getRequestUrl(),
                log.getResponseStatus(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }

    public ActivityFeedResponse toActivityFeedResponse(ActivityFeed feed) {
        return ActivityFeedResponse.of(
                feed.getId(),
                feed.getInstitutionId(),
                feed.getUserId(),
                feed.getActorName(),
                feed.getAction(),
                feed.getDescription(),
                feed.getEntityType(),
                feed.getEntityId(),
                feed.getEntityName(),
                feed.getMetadata(),
                feed.getVisibility(),
                feed.getCreatedAt()
        );
    }

    public SecurityEventResponse toSecurityEventResponse(SecurityEvent event) {
        return SecurityEventResponse.of(
                event.getId(),
                event.getInstitutionId(),
                event.getUserId(),
                event.getUserEmail(),
                event.getEventType().name(),
                event.getDescription(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getLocation(),
                event.getSeverity().name(),
                event.getMetadata(),
                event.getResolved(),
                event.getResolvedAt(),
                event.getResolvedBy(),
                event.getCreatedAt()
        );
    }
}