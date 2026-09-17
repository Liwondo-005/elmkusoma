package tz.elmkusoma.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.elmkusoma.audit.domain.ActivityFeed;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.dto.*;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.repository.ActivityFeedRepository;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ActivityFeedRepository activityFeedRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AuditMapper auditMapper;

    // ── Audit Log Operations ──

    public void recordAuditLog(UUID institutionId, UUID userId, String userEmail, String userRole,
                                String entityType, UUID entityId, String entityName,
                                AuditLog.AuditAction action,
                                Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(institutionId);
        auditLog.setUserId(userId);
        auditLog.setUserEmail(userEmail);
        auditLog.setUserRole(userRole);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setEntityName(entityName);
        auditLog.setAction(action);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);

        auditLogRepository.save(auditLog);
    }

    public void recordAuditLogWithContext(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLogResponse> getAuditLogs(UUID institutionId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByInstitutionId(institutionId, org.springframework.data.domain.PageRequest.of(page, size))
                .map(auditMapper::toAuditLogResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByDateRange(UUID institutionId, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByInstitutionIdAndDateRange(institutionId, from, to).stream()
                .map(auditMapper::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLogResponse> getAuditLogsByUser(UUID userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
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
        ActivityFeed feed = new ActivityFeed();
        feed.setInstitutionId(institutionId);
        feed.setUserId(userId);
        feed.setActorName(actorName);
        feed.setAction(action);
        feed.setDescription(description);
        feed.setEntityType(entityType);
        feed.setEntityId(entityId);
        feed.setEntityName(entityName);
        feed.setVisibility("PRIVATE");

        activityFeedRepository.save(feed);
        return feed;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ActivityFeedResponse> getActivityFeed(UUID institutionId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        return activityFeedRepository.findByInstitutionId(institutionId, pageable)
                .map(auditMapper::toActivityFeedResponse);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ActivityFeedResponse> getActivityFeedByUser(UUID userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
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
        SecurityEvent event = new SecurityEvent();
        event.setInstitutionId(institutionId);
        event.setUserId(userId);
        event.setUserEmail(userEmail);
        event.setEventType(eventType);
        event.setDescription(description);
        event.setSeverity(severity);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);

        securityEventRepository.save(event);
        return event;
    }

    public SecurityEvent resolveSecurityEvent(UUID eventId, UUID resolvedBy) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("SecurityEvent", "id", eventId));

        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedBy(resolvedBy);

        securityEventRepository.save(event);
        return event;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SecurityEventResponse> getSecurityEvents(UUID institutionId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
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
                .map(row -> ComplianceReportResponse.EventTypeCount.of(
                        ((Enum<?>) row[0]).name(),
                        (Long) row[1]))
                .collect(Collectors.toList());

        // Severity breakdown
        List<Object[]> severityCounts = securityEventRepository.countBySeverityForInstitution(institutionId);
        List<ComplianceReportResponse.SeverityCount> severityBreakdown = severityCounts.stream()
                .map(row -> ComplianceReportResponse.SeverityCount.of(
                        ((Enum<?>) row[0]).name(),
                        (Long) row[1]))
                .collect(Collectors.toList());

        return ComplianceReportResponse.of(
                totalAuditLogs,
                totalSecurityEvents,
                unresolvedSecurityEvents,
                criticalEvents,
                failedLoginAttempts,
                topEventTypes,
                severityBreakdown
        );
    }
}