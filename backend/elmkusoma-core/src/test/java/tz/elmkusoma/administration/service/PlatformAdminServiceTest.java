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
        when(securityEventRepository.countByResolvedAndIsDeletedFalse(false)).thenReturn(2L);

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
        DelegationCreateRequest req = DelegationCreateRequest.builder()
                .delegatorId(UUID.randomUUID())
                .delegateId(UUID.randomUUID())
                .permissions("read,write")
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
        when(securityEventRepository.countByResolvedAndIsDeletedFalse(false)).thenReturn(5L);
        when(incidentRepository.countByStatusAndIsDeletedFalse("DETECTED")).thenReturn(0L);
        when(incidentRepository.countByStatusAndIsDeletedFalse("INVESTIGATING")).thenReturn(0L);
        when(verificationRepository.countByStatusAndIsDeletedFalse("PENDING")).thenReturn(0L);
        when(liveClassRepository.countByStatusAndIsDeletedFalse("LIVE")).thenReturn(0L);

        var result = service.getAttentionItems();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(i -> i.getCategory().equals("SECURITY")));
    }
}
