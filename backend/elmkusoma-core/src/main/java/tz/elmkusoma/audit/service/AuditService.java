package tz.elmkusoma.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.ActivityFeed;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.dto.*;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.repository.ActivityFeedRepository;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;
import tz.elmkusoma.shared.security.OwnershipGuard;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ActivityFeedRepository activityFeedRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AuditMapper auditMapper;

    // ── Audit Log Operations ──

    public void recordAuditLog(UUID institutionId, UUID userId, String userEmail, String userRole,
                                String entityType, UUID entityId, String entityName,
                                AuditLog.AuditAction action,
                                Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog auditLog = AuditLog.builder()
                .institutionId(institutionId)
                .userId(userId)
                .userEmail(userEmail)
                .userRole(userRole)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .action(action)
                .oldValues(oldValues)
                .newValues(newValues)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log recorded: {} {} {} by {}", action, entityType, entityId, userEmail);
    }

    public void recordAuditLogWithContext(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByInstitutionId(institutionId, pageable)
                .map(auditMapper::toAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByDateRange(UUID institutionId, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByInstitutionIdAndDateRange(institutionId, from, to).stream()
                .map(auditMapper::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(UUID userId, UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByUserId(userId, pageable)
                .map(auditMapper::toAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByEntity(UUID institutionId, String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(institutionId, entityType, entityId).stream()
                .map(auditMapper::toAuditLogResponse)
                .toList();
    }

    // ── Activity Feed Operations ──

    public ActivityFeed recordActivity(UUID institutionId, UUID userId, String actorName,
                                        String action, String description,
                                        String entityType, UUID entityId, String entityName) {
        ActivityFeed feed = ActivityFeed.builder()
                .institutionId(institutionId)
                .userId(userId)
                .actorName(actorName)
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .visibility("PRIVATE")
                .build();

        activityFeedRepository.save(feed);
        return feed;
    }

    @Transactional(readOnly = true)
    public Page<ActivityFeedResponse> getActivityFeed(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityFeedRepository.findByInstitutionId(institutionId, pageable)
                .map(auditMapper::toActivityFeedResponse);
    }

    @Transactional(readOnly = true)
    public Page<ActivityFeedResponse> getActivityFeedByUser(UUID userId, UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityFeedRepository.findByUserId(userId, pageable)
                .map(auditMapper::toActivityFeedResponse);
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedResponse> getActivityFeedByDateRange(UUID institutionId, LocalDateTime from, LocalDateTime to) {
        return activityFeedRepository.findByInstitutionIdAndDateRange(institutionId, from, to).stream()
                .map(auditMapper::toActivityFeedResponse)
                .toList();
    }

    // ── Security Event Operations ──

    public SecurityEvent recordSecurityEvent(UUID institutionId, UUID userId, String userEmail,
                                              SecurityEvent.SecurityEventType eventType,
                                              String description, SecurityEvent.Severity severity,
                                              String ipAddress, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .institutionId(institutionId)
                .userId(userId)
                .userEmail(userEmail)
                .eventType(eventType)
                .description(description)
                .severity(severity)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        securityEventRepository.save(event);
        log.info("Security event recorded: {} for user: {} in institution: {}", eventType, userEmail, institutionId);
        return event;
    }

    public SecurityEvent resolveSecurityEvent(UUID eventId, UUID institutionId, UUID resolvedBy) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("SecurityEvent", "id", eventId));
        OwnershipGuard.verifyInstitution(event.getInstitutionId(), institutionId, "security event");

        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedBy(resolvedBy);

        securityEventRepository.save(event);
        log.info("Security event {} resolved by {}", eventId, resolvedBy);
        return event;
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventResponse> getSecurityEvents(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return securityEventRepository.findByInstitutionId(institutionId, pageable)
                .map(auditMapper::toSecurityEventResponse);
    }

    @Transactional(readOnly = true)
    public List<SecurityEventResponse> getUnresolvedSecurityEvents(UUID institutionId) {
        return securityEventRepository.findUnresolvedByInstitutionId(institutionId).stream()
                .map(auditMapper::toSecurityEventResponse)
                .toList();
    }

    // ── Compliance Reporting ──

    @Transactional(readOnly = true)
    public ComplianceReportResponse getComplianceReport(UUID institutionId) {
        long totalAuditLogs = auditLogRepository.countByInstitutionId(institutionId);
        long totalSecurityEvents = securityEventRepository.countUnresolvedByInstitutionId(institutionId)
                + securityEventRepository.findByInstitutionId(institutionId, PageRequest.of(0, 1)).getTotalElements();
        long unresolvedSecurityEvents = securityEventRepository.countUnresolvedByInstitutionId(institutionId);
        long criticalEvents = securityEventRepository.countCriticalByInstitutionId(institutionId);
        long failedLoginAttempts = securityEventRepository.countFailedLoginsByInstitutionId(institutionId);

        // Top event types
        List<Object[]> eventTypeCounts = securityEventRepository.countByEventTypeForInstitution(institutionId);
        List<ComplianceReportResponse.EventTypeCount> topEventTypes = eventTypeCounts.stream()
                .map(row -> ComplianceReportResponse.EventTypeCount.builder()
                        .eventType(((Enum<?>) row[0]).name())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        // Severity breakdown
        List<Object[]> severityCounts = securityEventRepository.countBySeverityForInstitution(institutionId);
        List<ComplianceReportResponse.SeverityCount> severityBreakdown = severityCounts.stream()
                .map(row -> ComplianceReportResponse.SeverityCount.builder()
                        .severity(((Enum<?>) row[0]).name())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        return ComplianceReportResponse.builder()
                .totalAuditLogs(totalAuditLogs)
                .totalSecurityEvents(totalSecurityEvents)
                .unresolvedSecurityEvents(unresolvedSecurityEvents)
                .criticalEvents(criticalEvents)
                .failedLoginAttempts(failedLoginAttempts)
                .topEventTypes(topEventTypes)
                .severityBreakdown(severityBreakdown)
                .build();
    }
}
