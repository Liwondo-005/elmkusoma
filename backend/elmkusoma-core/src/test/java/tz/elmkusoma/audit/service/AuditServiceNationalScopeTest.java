package tz.elmkusoma.audit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.repository.ActivityFeedRepository;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A null institution scope (NATIONAL_ADMIN oversight) reads platform-wide;
 * a non-null scope keeps reading institution-only queries exactly as before.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceNationalScopeTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ActivityFeedRepository activityFeedRepository;

    @Mock
    private SecurityEventRepository securityEventRepository;

    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    void getAuditLogs_nullInstitution_readsPlatformWide() {
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        auditService.getAuditLogs(null, 0, 20);

        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(any());
        verify(auditLogRepository, never()).findByInstitutionId(any(), any());
    }

    @Test
    void getAuditLogs_withInstitution_readsInstitutionOnly() {
        UUID institutionId = UUID.randomUUID();
        when(auditLogRepository.findByInstitutionId(eq(institutionId), any())).thenReturn(Page.empty());

        auditService.getAuditLogs(institutionId, 0, 20);

        verify(auditLogRepository).findByInstitutionId(eq(institutionId), any());
        verify(auditLogRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void getActivityFeed_nullInstitution_readsPlatformWide() {
        when(activityFeedRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        auditService.getActivityFeed(null, 0, 20);

        verify(activityFeedRepository).findAllByOrderByCreatedAtDesc(any());
        verify(activityFeedRepository, never()).findByInstitutionId(any(), any());
    }

    @Test
    void getSecurityEvents_nullInstitution_readsPlatformWide() {
        when(securityEventRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

        auditService.getSecurityEvents(null, 0, 20);

        verify(securityEventRepository).findAllByOrderByCreatedAtDesc(any());
        verify(securityEventRepository, never()).findByInstitutionId(any(), any());
    }

    @Test
    void getUnresolvedSecurityEvents_nullInstitution_readsPlatformWide() {
        when(securityEventRepository.findByResolvedFalse()).thenReturn(List.of());

        auditService.getUnresolvedSecurityEvents(null);

        verify(securityEventRepository).findByResolvedFalse();
        verify(securityEventRepository, never()).findUnresolvedByInstitutionId(any());
    }

    @Test
    void getComplianceReport_nullInstitution_usesPlatformCounts() {
        when(securityEventRepository.countByEventTypeForAll()).thenReturn(List.of());
        when(securityEventRepository.countBySeverityForAll()).thenReturn(List.of());

        auditService.getComplianceReport(null);

        verify(auditLogRepository).count();
        verify(securityEventRepository).countByResolvedFalse();
        verify(securityEventRepository).countBySeverity(SecurityEvent.Severity.CRITICAL);
        verify(securityEventRepository).countByEventType(SecurityEvent.SecurityEventType.LOGIN_FAILURE);
        verify(auditLogRepository, never()).countByInstitutionId(any());
    }

    @Test
    void getComplianceReport_withInstitution_usesInstitutionCounts() {
        UUID institutionId = UUID.randomUUID();
        when(securityEventRepository.findByInstitutionId(eq(institutionId), any())).thenReturn(Page.empty());
        when(securityEventRepository.countByEventTypeForInstitution(eq(institutionId))).thenReturn(List.of());
        when(securityEventRepository.countBySeverityForInstitution(eq(institutionId))).thenReturn(List.of());

        auditService.getComplianceReport(institutionId);

        verify(auditLogRepository).countByInstitutionId(institutionId);
        verify(auditLogRepository, never()).count();
        verify(securityEventRepository, never()).countByEventTypeForAll();
    }
}
