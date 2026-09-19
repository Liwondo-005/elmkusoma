package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tz.elmkusoma.administration.domain.InstitutionActivity;
import tz.elmkusoma.administration.domain.InstitutionAuditLog;
import tz.elmkusoma.administration.dto.InstitutionAuditLogResponse;
import tz.elmkusoma.administration.repository.InstitutionActivityRepository;
import tz.elmkusoma.administration.repository.InstitutionAuditLogRepository;
import tz.elmkusoma.administration.repository.InstitutionInvitationRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionAuditService {

    private final InstitutionAuditLogRepository auditLogRepository;
    private final InstitutionActivityRepository activityRepository;
    private final InstitutionInvitationRepository invitationRepository;

    public void log(UUID institutionId, UUID actorId, String actorEmail, String action,
                    String targetType, String targetId, String details, String ipAddress) {
        InstitutionAuditLog auditLog = InstitutionAuditLog.builder()
                .institutionId(institutionId)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actorRole("ADMIN")
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log: institution={} action={} target={}/{}", institutionId, action, targetType, targetId);
    }

    public void recordActivity(UUID institutionId, UUID actorId, String actorName,
                               String activityType, String title, String description) {
        InstitutionActivity activity = InstitutionActivity.builder()
                .institutionId(institutionId)
                .actorId(actorId)
                .actorName(actorName)
                .activityType(activityType)
                .title(title)
                .description(description)
                .build();
        activityRepository.save(activity);
    }

    public List<InstitutionAuditLogResponse> getAuditLogs(UUID institutionId, Pageable pageable) {
        return auditLogRepository.findRecentByInstitutionId(institutionId, pageable).stream()
                .map(log -> InstitutionAuditLogResponse.builder()
                        .id(log.getId())
                        .actorEmail(log.getActorEmail())
                        .actorRole(log.getActorRole())
                        .action(log.getAction())
                        .targetType(log.getTargetType())
                        .targetId(log.getTargetId())
                        .details(log.getDetails())
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    public List<InstitutionActivity> getRecentActivity(UUID institutionId, int limit) {
        return activityRepository.findRecentByInstitutionId(institutionId, PageRequest.of(0, limit));
    }

    public long countUnreadNotifications(UUID institutionId) {
        return activityRepository.countByInstitutionIdAndIsReadFalse(institutionId);
    }

    public long countPendingInvitations(UUID institutionId) {
        return invitationRepository.findPendingByInstitutionId(institutionId).size();
    }
}
