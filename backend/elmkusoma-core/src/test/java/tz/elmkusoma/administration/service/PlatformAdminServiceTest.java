package tz.elmkusoma.administration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.administration.domain.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.repository.*;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private LiveClassRepository liveClassRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private SecurityEventRepository securityEventRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private PlatformServiceRepository platformServiceRepository;
    @Mock private PlatformIncidentRepository incidentRepository;
    @Mock private PlatformConfigRepository configRepository;
    @Mock private PlatformNotificationRepository notificationRepository;
    @Mock private AdminDelegationRepository delegationRepository;
    @Mock private VerificationRecordRepository verificationRepository;
    @Mock private tz.elmkusoma.course.repository.CourseRepository courseRepository;
    @Mock private tz.elmkusoma.event.repository.EventRepository eventRepository;
    @Mock private tz.elmkusoma.liveclass.repository.MediaAssetRepository mediaAssetRepository;
    @Mock private tz.elmkusoma.learning.repository.ResourceRepository resourceRepository;
    @Mock private tz.elmkusoma.administration.repository.ProviderServiceEntitlementRepository providerEntitlementRepository;
    @Mock private tz.elmkusoma.administration.repository.ContentReportRepository contentReportRepository;
    @Mock private tz.elmkusoma.parent.repository.SupportTicketRepository supportTicketRepository;
    @Mock private PlatformFeatureRepository featureRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private DashboardSnapshotRepository snapshotRepository;
    @Mock private tz.elmkusoma.learner.repository.LearnerNotificationRepository learnerNotificationRepository;
    @Mock private PlatformIntegrationService integrationService;
    @Mock private BackupStatusService backupStatusService;

    @Mock private tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    @InjectMocks
    private PlatformAdminService service;

    private static final UUID TEST_ID = UUID.randomUUID();

    @Test
    void getPlatformDashboard_returnsCounts() {
        when(userRepository.countByIsDeletedFalse()).thenReturn(100L);
        when(userRepository.countByRoleAndIsDeletedFalse(User.Role.STUDENT)).thenReturn(60L);
        when(userRepository.countByRoleAndIsDeletedFalse(User.Role.TEACHER)).thenReturn(20L);
        when(userRepository.countByRoleAndIsDeletedFalse(User.Role.PARENT)).thenReturn(15L);
        when(institutionRepository.countByIsDeletedFalse()).thenReturn(5L);
        when(liveClassRepository.countByIsDeletedFalse()).thenReturn(10L);
        when(liveClassRepository.countByStatusAndIsDeletedFalse("LIVE")).thenReturn(3L);
        when(certificateRepository.countByIsDeletedFalse()).thenReturn(200L);
        when(securityEventRepository.countByResolvedFalse()).thenReturn(2L);

        var result = service.getPlatformDashboard();

        assertEquals(100L, result.getTotalUsers());
        assertEquals(60L, result.getTotalStudents());
        assertEquals(20L, result.getTotalTeachers());
        assertEquals(5L, result.getTotalInstitutions());
        assertEquals(3L, result.getActiveLiveClasses());
        assertEquals(200L, result.getTotalCertificates());
        assertEquals(2L, result.getUnresolvedSecurityEvents());
    }

    @Test
    void createService_persistsAndReturns() {
        ServiceCreateRequest req = ServiceCreateRequest.builder()
                .name("Test Service")
                .code("TEST_SVC")
                .description("Test")
                .category("EDUCATION")
                .isActive(true)
                .build();
        when(platformServiceRepository.save(any(PlatformService.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.createService(req);

        assertEquals("Test Service", result.getName());
        assertEquals("TEST_SVC", result.getCode());
        assertEquals("EDUCATION", result.getCategory());
        verify(platformServiceRepository).save(any(PlatformService.class));
    }

    @Test
    void updateService_updatesFields() {
        PlatformService existing = PlatformService.builder()
                .id(TEST_ID).name("Old").code("OLD").category("OTHER").isActive(false).build();
        when(platformServiceRepository.findById(TEST_ID)).thenReturn(Optional.of(existing));
        when(platformServiceRepository.save(any(PlatformService.class))).thenAnswer(i -> i.getArgument(0));

        ServiceCreateRequest req = ServiceCreateRequest.builder()
                .name("New Name").category("NEW_CAT").isActive(true).build();

        var result = service.updateService(TEST_ID, req);

        assertEquals("New Name", result.getName());
        assertEquals("NEW_CAT", result.getCategory());
        assertTrue(result.getIsActive());
    }

    @Test
    void updateService_throwsWhenNotFound() {
        when(platformServiceRepository.findById(TEST_ID)).thenReturn(Optional.empty());
        ServiceCreateRequest req = ServiceCreateRequest.builder().name("X").build();

        assertThrows(ResourceNotFoundException.class, () -> service.updateService(TEST_ID, req));
    }

    @Test
    void createDelegation_persists() {
        UUID delegator = UUID.randomUUID();
        UUID delegate = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(delegator))
                .thenReturn(Optional.of(adminUser(delegator, User.Role.ADMIN)));
        when(userRepository.findByIdAndIsDeletedFalse(delegate))
                .thenReturn(Optional.of(adminUser(delegate, User.Role.REGIONAL_ADMIN)));
        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(delegator)
                .delegateId(delegate)
                .permissions("VIEW,REVIEW")
                .authority("GENERAL_ADMIN")
                .reason("Legacy contract cover")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(delegationRepository.save(any(AdminDelegation.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.createDelegation(req);

        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getStartsAt());
        verify(delegationRepository).save(any(AdminDelegation.class));
    }

    @Test
    void revokeDelegation_setsRevoked() {
        AdminDelegation del = AdminDelegation.builder()
                .id(TEST_ID).status("ACTIVE").build();
        when(delegationRepository.findById(TEST_ID)).thenReturn(Optional.of(del));
        when(delegationRepository.save(any(AdminDelegation.class))).thenAnswer(i -> i.getArgument(0));

        service.revokeDelegation(TEST_ID, UUID.randomUUID(), "Test revoke");

        assertEquals("REVOKED", del.getStatus());
        assertNotNull(del.getRevokedAt());
        assertEquals("Test revoke", del.getRevocationReason());
    }

    @Test
    void listPendingVerifications_filtersByStatus() {
        VerificationRecord rec = VerificationRecord.builder()
                .id(TEST_ID).entityType("INSTITUTION").status("PENDING")
                .verificationType("QUALIFICATION").submittedAt(LocalDateTime.now())
                .build();
        when(verificationRepository.findByStatusAndIsDeletedFalse("PENDING"))
                .thenReturn(List.of(rec));

        var result = service.listPendingVerifications();

        assertEquals(1, result.size());
        assertEquals("INSTITUTION", result.get(0).getEntityType());
    }

    @Test
    void reviewVerification_updatesStatus() {
        VerificationRecord rec = VerificationRecord.builder()
                .id(TEST_ID).entityType("INSTITUTION").status("PENDING")
                .build();
        when(verificationRepository.findById(TEST_ID)).thenReturn(Optional.of(rec));
        when(verificationRepository.save(any(VerificationRecord.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.reviewVerification(TEST_ID, UUID.randomUUID(), "APPROVED", "Looks good");

        assertEquals("APPROVED", result.getStatus());
        assertEquals("Looks good", rec.getNotes());
        assertNotNull(rec.getReviewedAt());
    }

    @Test
    void reviewVerification_throwsWhenNotFound() {
        when(verificationRepository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.reviewVerification(TEST_ID, UUID.randomUUID(), "APPROVED", null));
    }

    @Test
    void listDelegations_filtersDeleted() {
        AdminDelegation del = AdminDelegation.builder()
                .id(TEST_ID).delegatorId(UUID.randomUUID()).delegateId(UUID.randomUUID())
                .status("ACTIVE").scope("PLATFORM")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(delegationRepository.findAll()).thenReturn(List.of(del));

        var result = service.listDelegations();

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    void listDelegations_filtersExpired() {
        AdminDelegation expired = AdminDelegation.builder()
                .id(TEST_ID).delegatorId(UUID.randomUUID()).delegateId(UUID.randomUUID())
                .status("EXPIRED").scope("PLATFORM")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(delegationRepository.findAll()).thenReturn(List.of(expired));

        var result = service.listDelegations();

        assertEquals(0, result.size());
    }

    @Test
    void getAttention_returnsSecurityItems() {
        when(securityEventRepository.countByResolvedFalse()).thenReturn(5L);
        when(incidentRepository.countByStatusAndIsDeletedFalse("DETECTED")).thenReturn(0L);
        when(incidentRepository.countByStatusAndIsDeletedFalse("INVESTIGATING")).thenReturn(0L);
        when(verificationRepository.countByStatusAndIsDeletedFalse("PENDING")).thenReturn(0L);
        when(liveClassRepository.countByStatusAndIsDeletedFalse("LIVE")).thenReturn(0L);

        var result = service.getAttentionItems();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(i -> i.getCategory().equals("SECURITY")));
    }

    @Test
    void createIncident_startsDetectedWithAudit() {
        IncidentCreateRequest req = IncidentCreateRequest.builder()
                .title("DB latency spike").category("DATABASE").severity("HIGH")
                .affectedService("postgres").build();
        when(incidentRepository.save(any(PlatformIncident.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(tz.elmkusoma.audit.domain.AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.createIncident(req);

        assertEquals("DETECTED", result.getStatus());
        verify(incidentRepository).save(any(PlatformIncident.class));
        verify(auditLogRepository).save(any(tz.elmkusoma.audit.domain.AuditLog.class));
    }

    @Test
    void updateIncidentStatus_followsLifecycle() {
        PlatformIncident inc = PlatformIncident.builder()
                .id(TEST_ID).title("Outage").status("DETECTED").build();
        when(incidentRepository.findById(TEST_ID)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(any(PlatformIncident.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(tz.elmkusoma.audit.domain.AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.updateIncidentStatus(TEST_ID, "INVESTIGATING", null);
        assertEquals("INVESTIGATING", inc.getStatus());
        assertNotNull(inc.getAcknowledgedAt());

        service.updateIncidentStatus(TEST_ID, "CONTAINED", null);
        assertEquals("CONTAINED", inc.getStatus());

        service.updateIncidentStatus(TEST_ID, "RESOLVED", "mitigated");
        assertEquals("RESOLVED", inc.getStatus());
        assertEquals("mitigated", inc.getResolutionNotes());

        service.updateIncidentStatus(TEST_ID, "REVIEWED", null);
        assertEquals("REVIEWED", inc.getStatus());
    }

    @Test
    void updateIncidentStatus_rejectsIllegalJump() {
        PlatformIncident inc = PlatformIncident.builder()
                .id(TEST_ID).title("Outage").status("DETECTED").build();
        when(incidentRepository.findById(TEST_ID)).thenReturn(Optional.of(inc));

        assertThrows(IllegalStateException.class,
                () -> service.updateIncidentStatus(TEST_ID, "RESOLVED", null));
    }

    @Test
    void updateIncidentStatus_acknowledgedMapsToInvestigating() {
        PlatformIncident inc = PlatformIncident.builder()
                .id(TEST_ID).title("Outage").status("DETECTED").build();
        when(incidentRepository.findById(TEST_ID)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(any(PlatformIncident.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(tz.elmkusoma.audit.domain.AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.updateIncidentStatus(TEST_ID, "ACKNOWLEDGED", null);

        assertEquals("INVESTIGATING", inc.getStatus());
    }

    @Test
    void listFeatures_mapsRows() {
        PlatformFeature f = PlatformFeature.builder()
                .id(TEST_ID).featureKey("payments").name("Payments").status("ACTIVE").build();
        when(featureRepository.findByIsDeletedFalse()).thenReturn(List.of(f));

        var result = service.listFeatures();

        assertEquals(1, result.size());
        assertEquals("payments", result.get(0).getKey());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    void updateFeatureStatus_enforcesTransitions() {
        PlatformFeature f = PlatformFeature.builder()
                .id(TEST_ID).featureKey("integration_registry").name("Registry").status("ROLLOUT").build();
        when(featureRepository.findByFeatureKeyAndIsDeletedFalse("integration_registry"))
                .thenReturn(Optional.of(f));
        when(featureRepository.save(any(PlatformFeature.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(tz.elmkusoma.audit.domain.AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.updateFeatureStatus("integration_registry", "ACTIVE");

        assertEquals("ACTIVE", result.getStatus());

        assertThrows(IllegalStateException.class,
                () -> service.updateFeatureStatus("integration_registry", "PLANNED"));
    }

    @Test
    void updateSupportTicketStatus_actionRequiredLifecycle() {
        tz.elmkusoma.parent.domain.SupportTicket t = tz.elmkusoma.parent.domain.SupportTicket.builder()
                .id(TEST_ID).subject("Payment issue").description("desc").category("PAYMENT").status("INVESTIGATING")
                .institutionId(TEST_ID).build();
        when(supportTicketRepository.findById(TEST_ID)).thenReturn(Optional.of(t));
        when(supportTicketRepository.save(any(tz.elmkusoma.parent.domain.SupportTicket.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(tz.elmkusoma.audit.domain.AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        service.updateSupportTicketStatus(TEST_ID, "ACTION_REQUIRED");
        assertEquals("ACTION_REQUIRED", t.getStatus());

        service.updateSupportTicketStatus(TEST_ID, "RESOLVED");
        assertEquals("RESOLVED", t.getStatus());

        service.updateSupportTicketStatus(TEST_ID, "CLOSED");
        assertEquals("CLOSED", t.getStatus());
    }

    @Test
    void updateSupportTicketStatus_rejectsIllegalJump() {
        tz.elmkusoma.parent.domain.SupportTicket t = tz.elmkusoma.parent.domain.SupportTicket.builder()
                .id(TEST_ID).subject("Access issue").description("desc").category("ACCOUNT").status("CLOSED")
                .institutionId(TEST_ID).build();
        when(supportTicketRepository.findById(TEST_ID)).thenReturn(Optional.of(t));

        assertThrows(IllegalStateException.class,
                () -> service.updateSupportTicketStatus(TEST_ID, "RESOLVED"));
    }

    @Test
    void listCertificates_withFilters_mapsEnrichedSummary() {
        tz.elmkusoma.certificate.domain.Certificate cert = tz.elmkusoma.certificate.domain.Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateNumber("CERT-ABC123")
                .certificateType(tz.elmkusoma.certificate.domain.Certificate.CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .courseOrProgramme("Mathematics")
                .status(tz.elmkusoma.certificate.domain.Certificate.CertificateStatus.ISSUED)
                .issueDate(java.time.LocalDateTime.now())
                .completionDate(java.time.LocalDate.of(2026, 1, 1))
                .verificationCode("ABC123")
                .build();
        cert.setId(TEST_ID);
        cert.setInstitutionId(TEST_ID);

        when(certificateRepository.searchCertificates(eq("john"),
                eq(tz.elmkusoma.certificate.domain.Certificate.CertificateStatus.ISSUED),
                isNull(), isNull(),
                eq(tz.elmkusoma.administration.service.PlatformAdminService.FILTER_FROM_SENTINEL),
                eq(tz.elmkusoma.administration.service.PlatformAdminService.FILTER_TO_SENTINEL),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cert)));
        when(institutionRepository.findById(TEST_ID))
                .thenReturn(Optional.of(tz.elmkusoma.shared.domain.Institution.builder().name("ELMKUSOMA HQ").build()));

        tz.elmkusoma.common.PageResponse<tz.elmkusoma.administration.dto.CertificateSummaryResponse> out =
                service.listCertificates(0, 20, "john", "ISSUED", null, null, null, null);

        assertEquals(1, out.getContent().size());
        tz.elmkusoma.administration.dto.CertificateSummaryResponse summary = out.getContent().get(0);
        assertEquals("John Doe", summary.getStudentName());
        assertEquals("ELMKUSOMA HQ", summary.getInstitutionName());
        assertEquals("COMPLETION", summary.getCertificateType());
        assertEquals("Mathematics", summary.getCourseOrProgramme());
        assertEquals("ISSUED", summary.getStatus());
    }

    // ── Delegation governance ──

    private User adminUser(UUID id, User.Role role) {
        User u = User.builder().email("u-" + id + "@test.go.tz").firstName("Test").lastName("User")
                .passwordHash("x").role(role).isActive(true).build();
        u.setId(id);
        return u;
    }

    private AdminDelegation activeDelegation(UUID delegator, UUID delegate) {
        AdminDelegation d = AdminDelegation.builder()
                .delegatorId(delegator).delegateId(delegate)
                .permissions("[\"VERIFY\",\"REVIEW\"]").scope("PLATFORM")
                .authority("PROVIDER_VERIFICATION").status("ACTIVE")
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30)).build();
        d.setId(UUID.randomUUID());
        return d;
    }

    @Test
    void createDelegation_validCreatesActive() {
        UUID delegator = UUID.randomUUID();
        UUID delegate = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(delegator)).thenReturn(Optional.of(adminUser(delegator, User.Role.ADMIN)));
        when(userRepository.findByIdAndIsDeletedFalse(delegate)).thenReturn(Optional.of(adminUser(delegate, User.Role.REGIONAL_ADMIN)));
        when(delegationRepository.save(any(AdminDelegation.class))).thenAnswer(i -> i.getArgument(0));

        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(delegator).delegateId(delegate)
                .permissions("[\"VIEW\",\"REVIEW\",\"VERIFY\"]").scope("PLATFORM")
                .authority("PROVIDER_VERIFICATION").reason("Workload cover").build();

        var out = service.createDelegation(req);

        assertEquals("ACTIVE", out.getStatus());
        assertEquals("PROVIDER_VERIFICATION", out.getAuthority());
        verify(delegationRepository).save(any(AdminDelegation.class));
        verify(auditLogRepository).save(any(tz.elmkusoma.audit.domain.AuditLog.class));
    }

    @Test
    void createDelegation_requiresApprovalCreatesPending() {
        UUID delegator = UUID.randomUUID();
        UUID delegate = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(delegator)).thenReturn(Optional.of(adminUser(delegator, User.Role.ADMIN)));
        when(userRepository.findByIdAndIsDeletedFalse(delegate)).thenReturn(Optional.of(adminUser(delegate, User.Role.DISTRICT_ADMIN)));
        when(delegationRepository.save(any(AdminDelegation.class))).thenAnswer(i -> i.getArgument(0));

        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(delegator).delegateId(delegate)
                .permissions("REVIEW").authority("INSTITUTION_REVIEW")
                .reason("Needs sign-off").requiresApproval(true).build();

        var out = service.createDelegation(req);

        assertEquals("PENDING_APPROVAL", out.getStatus());
    }

    @Test
    void createDelegation_invalidAuthorityRejected() {
        UUID delegator = UUID.randomUUID();
        UUID delegate = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(delegator)).thenReturn(Optional.of(adminUser(delegator, User.Role.ADMIN)));
        when(userRepository.findByIdAndIsDeletedFalse(delegate)).thenReturn(Optional.of(adminUser(delegate, User.Role.ADMIN)));

        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(delegator).delegateId(delegate)
                .permissions("VIEW").authority("DELETE_EVERYTHING").reason("x").build();

        assertThrows(IllegalArgumentException.class, () -> service.createDelegation(req));
    }

    @Test
    void createDelegation_sameUserRejected() {
        UUID u = UUID.randomUUID();
        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(u).delegateId(u).permissions("VIEW").reason("x").build();
        assertThrows(IllegalArgumentException.class, () -> service.createDelegation(req));
    }

    @Test
    void createDelegation_unknownPermissionRejected() {
        UUID delegator = UUID.randomUUID();
        UUID delegate = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(delegator)).thenReturn(Optional.of(adminUser(delegator, User.Role.ADMIN)));
        when(userRepository.findByIdAndIsDeletedFalse(delegate)).thenReturn(Optional.of(adminUser(delegate, User.Role.ADMIN)));
        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(delegator).delegateId(delegate)
                .permissions("[\"FLY\"]").reason("x").build();
        assertThrows(IllegalArgumentException.class, () -> service.createDelegation(req));
    }

    @Test
    void approveDelegation_pendingBecomesActive() {
        AdminDelegation d = activeDelegation(UUID.randomUUID(), UUID.randomUUID());
        d.setStatus("PENDING_APPROVAL");
        when(delegationRepository.findById(d.getId())).thenReturn(Optional.of(d));
        when(delegationRepository.save(any(AdminDelegation.class))).thenAnswer(i -> i.getArgument(0));

        var out = service.approveDelegation(d.getId(), UUID.randomUUID(), "ok");

        assertEquals("ACTIVE", out.getStatus());
        assertNotNull(d.getApprovedAt());
    }

    @Test
    void approveDelegation_nonPendingThrows() {
        AdminDelegation d = activeDelegation(UUID.randomUUID(), UUID.randomUUID());
        when(delegationRepository.findById(d.getId())).thenReturn(Optional.of(d));
        assertThrows(IllegalStateException.class, () -> service.approveDelegation(d.getId(), UUID.randomUUID(), "ok"));
    }

    @Test
    void rejectDelegation_requiresReason() {
        AdminDelegation d = activeDelegation(UUID.randomUUID(), UUID.randomUUID());
        d.setStatus("PENDING_APPROVAL");
        when(delegationRepository.findById(d.getId())).thenReturn(Optional.of(d));
        assertThrows(IllegalArgumentException.class, () -> service.rejectDelegation(d.getId(), UUID.randomUUID(), "  "));
    }

    @Test
    void revokeDelegation_terminalCannotRevokeAgain() {
        AdminDelegation d = activeDelegation(UUID.randomUUID(), UUID.randomUUID());
        when(delegationRepository.findById(d.getId())).thenReturn(Optional.of(d));
        service.revokeDelegation(d.getId(), UUID.randomUUID(), "done");
        assertEquals("REVOKED", d.getStatus());
        assertThrows(IllegalStateException.class, () -> service.revokeDelegation(d.getId(), UUID.randomUUID(), "again"));
    }

    @Test
    void extendDelegation_revokedThrows() {
        AdminDelegation d = activeDelegation(UUID.randomUUID(), UUID.randomUUID());
        d.setStatus("REVOKED");
        when(delegationRepository.findById(d.getId())).thenReturn(Optional.of(d));
        assertThrows(IllegalStateException.class,
                () -> service.extendDelegation(d.getId(), LocalDateTime.now().plusDays(60), "renew"));
    }

    @Test
    void hasDelegatedAuthority_enforcesTimeAndScope() {
        UUID delegate = UUID.randomUUID();
        AdminDelegation ok = activeDelegation(UUID.randomUUID(), delegate);
        AdminDelegation expired = activeDelegation(UUID.randomUUID(), delegate);
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        AdminDelegation otherAuthority = activeDelegation(UUID.randomUUID(), delegate);
        otherAuthority.setAuthority("PLATFORM_SUPPORT");
        when(delegationRepository.findByDelegateIdAndIsDeletedFalse(delegate))
                .thenReturn(List.of(ok, expired, otherAuthority));

        assertTrue(service.hasDelegatedAuthority(delegate, "PROVIDER_VERIFICATION", "VERIFY", UUID.randomUUID(), null));
        assertFalse(service.hasDelegatedAuthority(delegate, "CONTENT_GOVERNANCE", "VERIFY", UUID.randomUUID(), null));
        assertFalse(service.hasDelegatedAuthority(delegate, "PROVIDER_VERIFICATION", "SUSPEND", UUID.randomUUID(), null));

        AdminDelegation scoped = activeDelegation(UUID.randomUUID(), delegate);
        UUID instId = UUID.randomUUID();
        scoped.setScope("INSTITUTION:" + instId);
        when(delegationRepository.findByDelegateIdAndIsDeletedFalse(delegate)).thenReturn(List.of(scoped));
        assertTrue(service.hasDelegatedAuthority(delegate, "PROVIDER_VERIFICATION", "VERIFY", instId, null));
        assertFalse(service.hasDelegatedAuthority(delegate, "PROVIDER_VERIFICATION", "VERIFY", UUID.randomUUID(), null));
    }

    @Test
    void reviewProviderVerification_delegatedOfficerAllowed() {
        UUID actor = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        VerificationRecord rec = VerificationRecord.builder()
                .entityType("INSTITUTION").entityId(instId)
                .verificationType("PROVIDER_LICENSE").status("PENDING")
                .submittedAt(LocalDateTime.now()).build();
        rec.setId(UUID.randomUUID());
        User officer = adminUser(actor, User.Role.TEACHER);
        Institution inst = Institution.builder().name("Test NGO").code("TNGO")
                .type(Institution.InstitutionType.NGO).country("Tanzania").isActive(true).status("ACTIVE").build();
        inst.setId(instId);

        when(verificationRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(userRepository.findById(actor)).thenReturn(Optional.of(officer));
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(inst));
        when(delegationRepository.findByDelegateIdAndIsDeletedFalse(actor))
                .thenReturn(List.of(activeDelegation(UUID.randomUUID(), actor)));
        when(verificationRepository.save(any(VerificationRecord.class))).thenAnswer(i -> i.getArgument(0));

        var out = service.reviewProviderVerification(rec.getId(), actor, "APPROVED", "looks good");

        assertEquals("APPROVED", out.getStatus());
    }

    @Test
    void reviewProviderVerification_unauthorizedDenied() {
        UUID actor = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        VerificationRecord rec = VerificationRecord.builder()
                .entityType("INSTITUTION").entityId(instId)
                .verificationType("PROVIDER_LICENSE").status("PENDING")
                .submittedAt(LocalDateTime.now()).build();
        rec.setId(UUID.randomUUID());
        User officer = adminUser(actor, User.Role.TEACHER);
        Institution inst = Institution.builder().name("Test NGO").code("TNGO2")
                .type(Institution.InstitutionType.NGO).country("Tanzania").isActive(true).status("ACTIVE").build();
        inst.setId(instId);

        when(verificationRepository.findById(rec.getId())).thenReturn(Optional.of(rec));
        when(userRepository.findById(actor)).thenReturn(Optional.of(officer));
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(inst));
        when(delegationRepository.findByDelegateIdAndIsDeletedFalse(actor)).thenReturn(List.of());

        assertThrows(SecurityException.class,
                () -> service.reviewProviderVerification(rec.getId(), actor, "APPROVED", "x"));
    }

    @Test
    void reviewProviderVerification_alreadyDecidedThrows() {
        UUID actor = UUID.randomUUID();
        VerificationRecord rec = VerificationRecord.builder()
                .entityType("INSTITUTION").entityId(UUID.randomUUID())
                .verificationType("PROVIDER_LICENSE").status("APPROVED")
                .submittedAt(LocalDateTime.now()).build();
        rec.setId(UUID.randomUUID());
        when(verificationRepository.findById(rec.getId())).thenReturn(Optional.of(rec));

        assertThrows(IllegalStateException.class,
                () -> service.reviewProviderVerification(rec.getId(), actor, "REJECTED", "late"));
    }

    // ── Provider governance ──

    @Test
    void listProviders_onlyProviderTypes() {
        Institution school = Institution.builder().name("A School").code("SCH1")
                .type(Institution.InstitutionType.PRIMARY).country("Tanzania").isActive(true).status("ACTIVE").build();
        school.setId(UUID.randomUUID());
        Institution ngo = Institution.builder().name("B NGO").code("NGO1")
                .type(Institution.InstitutionType.NGO).country("Tanzania").isActive(true).status("ACTIVE").build();
        ngo.setId(UUID.randomUUID());
        when(institutionRepository.findByIsDeletedFalse()).thenReturn(List.of(school, ngo));
        when(verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(eq("INSTITUTION"), any(UUID.class)))
                .thenReturn(List.of());
        when(membershipRepository.findByInstitutionIdAndIsActiveTrue(any(UUID.class))).thenReturn(List.of());

        var out = service.listProviders(null, null, null, null, 0, 20);

        assertEquals(1, out.getContent().size());
        assertEquals("B NGO", out.getContent().get(0).getName());
        assertEquals("NONE", out.getContent().get(0).getVerificationStatus());
    }

    @Test
    void getProviderDetail_nonProviderRejected() {
        UUID id = UUID.randomUUID();
        Institution school = Institution.builder().name("A School").code("SCH9")
                .type(Institution.InstitutionType.SECONDARY).country("Tanzania").build();
        school.setId(id);
        when(institutionRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(school));
        assertThrows(IllegalArgumentException.class, () -> service.getProviderDetail(id));
    }

    @Test
    void getProviderAttention_flagsSuspendedAndPending() {
        Institution suspended = Institution.builder().name("S Co").code("SC1")
                .type(Institution.InstitutionType.COMPANY).country("Tanzania")
                .email("s@co.tz").phone("+255700000001")
                .isActive(false).status("SUSPENDED").build();
        suspended.setId(UUID.randomUUID());
        Institution pending = Institution.builder().name("P NGO").code("PN1")
                .type(Institution.InstitutionType.NGO).country("Tanzania")
                .email("p@ngo.tz").phone("+255700000002")
                .isActive(true).status("ACTIVE").build();
        pending.setId(UUID.randomUUID());
        VerificationRecord v = VerificationRecord.builder()
                .entityType("INSTITUTION").entityId(pending.getId())
                .verificationType("PROVIDER_LICENSE").status("PENDING")
                .submittedAt(LocalDateTime.now()).build();
        when(institutionRepository.findByIsDeletedFalse()).thenReturn(List.of(suspended, pending));
        when(verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(eq("INSTITUTION"), eq(suspended.getId())))
                .thenReturn(List.of());
        when(verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(eq("INSTITUTION"), eq(pending.getId())))
                .thenReturn(List.of(v));

        var out = service.getProviderAttention();

        assertTrue(out.stream().anyMatch(a -> a.getCategory().equals("SUSPENDED") && a.getProviderId().equals(suspended.getId())));
        assertTrue(out.stream().anyMatch(a -> a.getCategory().equals("PENDING_VERIFICATION") && a.getProviderId().equals(pending.getId())));
    }
}
