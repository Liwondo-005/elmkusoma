package tz.elmkusoma.certificate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.Certificate.CertificateStatus;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.*;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateTemplateRepository templateRepository;
    @Mock
    private CertificateSignatoryRepository signatoryRepository;
    @Mock
    private CertificateTemplateSignatoryRepository templateSignatoryRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private TranscriptRepository transcriptRepository;
    @Mock
    private TranscriptEntryRepository transcriptEntryRepository;
    @Mock
    private CertificateMapper certificateMapper;
    @Mock
    private tz.elmkusoma.certificate.repository.CertificateSignatorySnapshotRepository signatorySnapshotRepository;
    // The approval gate is mocked so this class keeps testing the pre-existing lifecycle behaviour.
    // The gate itself is covered end to end by CertificateApprovalServiceTest.
    @Mock
    private CertificateApprovalService approvalService;
    @Mock
    private AuditService auditService;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private tz.elmkusoma.shared.repository.UserRepository userRepository;
    @Mock
    private LearnerNotificationRepository learnerNotificationRepository;
    @Mock
    private tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;

    @InjectMocks
    private CertificateService certificateService;

    private UUID institutionId;
    private UUID templateId;
    private UUID certificateId;
    private UUID issuedByUserId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        certificateId = UUID.randomUUID();
        issuedByUserId = UUID.randomUUID();

        // Issuance re-resolves and revalidates the template. Tests that care about a specific
        // template override this; the default keeps the pre-existing lifecycle tests working.
        // lenient() because this class runs Mockito's strict stubs: many tests here never reach the
        // issuance path at all, and an unused shared stub would otherwise fail them.
        CertificateTemplate issuable = CertificateTemplate.builder()
                .name("Default Template")
                .templateType(CertificateTemplate.TemplateType.COMPLETION)
                .isActive(true)
                .build();
        issuable.setId(templateId);
        issuable.setInstitutionId(institutionId);
        lenient().when(templateRepository.findByIdAndIsDeletedFalse(templateId))
                .thenReturn(Optional.of(issuable));
        lenient().when(certificateRepository.findLiveAward(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void generateCertificate_shouldCreateCertificateWithVerificationCode() {
        GenerateCertificateRequest request = GenerateCertificateRequest.builder()
                .templateId(templateId)
                .studentId(UUID.randomUUID())
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .completionDate(LocalDate.of(2025, 6, 15))
                .build();

        CertificateTemplate template = CertificateTemplate.builder()
                .name("Default Template")
                .isActive(true)
                .build();
        template.setId(templateId);
        template.setInstitutionId(institutionId);

        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(certificateRepository.countByInstitutionId(institutionId)).thenReturn(0L);

        // The recipient is now resolved from the student record rather than taken from the
        // request, so the fixture has to provide one. Previously a missing student was silently
        // tolerated and the caller's raw id was stored.
        UUID studentUserId = UUID.randomUUID();
        Student studentRecord = Student.builder().status(StudentStatus.ACTIVE).build();
        studentRecord.setId(request.getStudentId());
        studentRecord.setUserId(studentUserId);
        studentRecord.setInstitutionId(institutionId);
        when(studentRepository.findByIdAndIsDeletedFalse(request.getStudentId()))
                .thenReturn(Optional.of(studentRecord));
        when(userRepository.findByIdAndIsDeletedFalse(studentUserId)).thenReturn(Optional.of(
                User.builder().firstName("John").lastName("Doe").build()));

        Certificate savedCertificate = Certificate.builder()
                .templateId(templateId)
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .status(CertificateStatus.DRAFT)
                .verificationCode("ABC123DEF456GHIJ")
                .verificationUrl("/api/v1/certificates/ABC123DEF456GHIJ/verify")
                .completionDate(LocalDate.of(2025, 6, 15))
                .issueDate(LocalDateTime.now())
                .build();
        savedCertificate.setId(certificateId);
        savedCertificate.setInstitutionId(institutionId);

        when(certificateRepository.save(any(Certificate.class))).thenReturn(savedCertificate);

        CertificateResponse expectedResponse = CertificateResponse.builder()
                .id(certificateId)
                .serialNumber("CERT-CMP-2026-000001")
                .status("DRAFT")
                .title("Course Completion")
                .build();

        when(certificateMapper.toCertificateResponse(any(Certificate.class))).thenReturn(expectedResponse);

        CertificateResponse response = certificateService.generateCertificate(
                request, institutionId, issuedByUserId, "admin@test.com", "ADMIN");

        assertNotNull(response);
        assertEquals("CERT-CMP-2026-000001", response.getSerialNumber());
        assertEquals("DRAFT", response.getStatus());
        verify(certificateRepository).save(any(Certificate.class));
        verify(auditService).recordAuditLog(eq(institutionId), eq(issuedByUserId), anyString(), anyString(),
                eq("Certificate"), any(), anyString(), any(), isNull(), anyMap());
    }

    @Test
    void generateCertificate_whenTemplateNotFound_shouldThrow() {
        GenerateCertificateRequest request = GenerateCertificateRequest.builder()
                .templateId(templateId)
                .studentId(UUID.randomUUID())
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .completionDate(LocalDate.of(2025, 6, 15))
                .build();

        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> certificateService.generateCertificate(
                        request, institutionId, issuedByUserId, "admin@test.com", "ADMIN"));
    }

    @Test
    void generateCertificate_whenTemplateWrongInstitution_shouldThrowForbidden() {
        UUID otherInstitutionId = UUID.randomUUID();
        GenerateCertificateRequest request = GenerateCertificateRequest.builder()
                .templateId(templateId)
                .studentId(UUID.randomUUID())
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .completionDate(LocalDate.of(2025, 6, 15))
                .build();

        CertificateTemplate template = CertificateTemplate.builder()
                .name("Default Template")
                .build();
        template.setId(templateId);
        template.setInstitutionId(otherInstitutionId);

        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));

        assertThrows(ForbiddenException.class,
                () -> certificateService.generateCertificate(
                        request, institutionId, issuedByUserId, "admin@test.com", "ADMIN"));
    }

    @Test
    void issueCertificate_shouldTransitionFromDraftToIssued() {
        Certificate certificate = Certificate.builder()
                .templateId(templateId)
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .status(CertificateStatus.DRAFT)
                .verificationCode("ABC123")
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any(Certificate.class))).thenReturn(certificate);

        CertificateResponse expectedResponse = CertificateResponse.builder()
                .id(certificateId)
                .status("ISSUED")
                .serialNumber("CERT-CMP-2026-000001")
                .build();
        when(certificateMapper.toCertificateResponse(any(Certificate.class))).thenReturn(expectedResponse);

        CertificateResponse response = certificateService.issueCertificate(
                certificateId, institutionId, issuedByUserId, "admin@test.com", "ADMIN");

        assertNotNull(response);
        assertEquals("ISSUED", response.getStatus());
        assertEquals(CertificateStatus.ISSUED, certificate.getStatus());
        verify(certificateRepository).save(certificate);
        verify(auditService).recordAuditLog(eq(institutionId), any(), anyString(), anyString(),
                eq("Certificate"), any(), anyString(), any(), anyMap(), anyMap());
    }

    @Test
    void issueCertificate_whenAlreadyIssued_shouldThrow() {
        Certificate certificate = Certificate.builder()
                .status(CertificateStatus.ISSUED)
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> certificateService.issueCertificate(
                        certificateId, institutionId, issuedByUserId, "admin@test.com", "ADMIN"));
        // The guard no longer says "only DRAFT", because a certificate in review is issuable once
        // its approvals are in. An already-issued or revoked one is not.
        assertTrue(exception.getMessage().contains("ISSUED"), exception.getMessage());
    }

    @Test
    void revokeCertificate_shouldSetRevokedStatus() {
        Certificate certificate = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .title("Course Completion")
                .status(CertificateStatus.ISSUED)
                .verificationCode("ABC123")
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any(Certificate.class))).thenReturn(certificate);

        RevokeCertificateRequest request = new RevokeCertificateRequest();
        request.setReason("Academic dishonesty");

        CertificateResponse expectedResponse = CertificateResponse.builder()
                .id(certificateId)
                .status("REVOKED")
                .revokedReason("Academic dishonesty")
                .build();
        when(certificateMapper.toCertificateResponse(any(Certificate.class))).thenReturn(expectedResponse);

        CertificateResponse response = certificateService.revokeCertificate(
                certificateId, institutionId, request, issuedByUserId, "admin@test.com", "ADMIN");

        assertNotNull(response);
        assertEquals("REVOKED", response.getStatus());
        assertEquals("Academic dishonesty", response.getRevokedReason());
        assertNotNull(certificate.getRevokedAt());
        verify(certificateRepository).save(certificate);
        verify(auditService).recordSecurityEvent(eq(institutionId), any(), anyString(), any(), anyString(), any(), any(), any());
    }

    @Test
    void revokeCertificate_whenAlreadyRevoked_shouldThrow() {
        Certificate certificate = Certificate.builder()
                .status(CertificateStatus.REVOKED)
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));

        RevokeCertificateRequest request = new RevokeCertificateRequest();
        request.setReason("Duplicate revocation");

        assertThrows(IllegalStateException.class,
                () -> certificateService.revokeCertificate(
                        certificateId, institutionId, request, issuedByUserId, "admin@test.com", "ADMIN"));
    }

    @Test
    void verifyCertificate_whenValidIssued_shouldReturnValid() {
        Certificate certificate = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .status(CertificateStatus.ISSUED)
                .verificationCode("ABC123")
                .issueDate(LocalDateTime.now())
                .completionDate(LocalDate.of(2025, 6, 15))
                .build();
        certificate.setId(certificateId);

        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("ABC123"))
                .thenReturn(Optional.of(certificate));

        CertificateVerificationResponse response = certificateService.verifyCertificate("ABC123", "test-client");

        assertTrue(response.isValid());
        assertEquals("John Doe", response.getStudentName());
        assertEquals("ISSUED", response.getStatus());
        assertEquals("Certificate is valid and verified", response.getMessage());
    }

    @Test
    void verifyCertificate_whenRevoked_shouldReturnInvalid() {
        Certificate certificate = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .studentName("John Doe")
                .status(CertificateStatus.REVOKED)
                .revokedReason("Academic dishonesty")
                .verificationCode("ABC123")
                .completionDate(LocalDate.of(2025, 6, 15))
                .build();
        certificate.setId(certificateId);

        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("ABC123"))
                .thenReturn(Optional.of(certificate));

        CertificateVerificationResponse response = certificateService.verifyCertificate("ABC123", "test-client");

        assertFalse(response.isValid());
        assertEquals("REVOKED", response.getStatus());
        assertTrue(response.getMessage().contains("revoked"));
    }

    @Test
    void verifyCertificate_whenNotFound_shouldReturnInvalid() {
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("INVALID"))
                .thenReturn(Optional.empty());

        CertificateVerificationResponse response = certificateService.verifyCertificate("INVALID", "test-client");

        assertFalse(response.isValid());
        assertEquals("Certificate not found", response.getMessage());
    }

    @Test
    void getCertificatesByInstitution_shouldReturnList() {
        Certificate cert = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(CertificateType.COMPLETION)
                .title("Course Completion")
                .status(CertificateStatus.ISSUED)
                .build();
        cert.setId(certificateId);
        cert.setInstitutionId(institutionId);

        when(certificateRepository.findAllByInstitutionId(institutionId)).thenReturn(List.of(cert));

        CertificateResponse expectedResponse = CertificateResponse.builder()
                .id(certificateId)
                .serialNumber("CERT-CMP-2026-000001")
                .status("ISSUED")
                .build();
        when(certificateMapper.toCertificateResponse(cert)).thenReturn(expectedResponse);

        List<CertificateResponse> responses = certificateService.getCertificatesByInstitution(institutionId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CERT-CMP-2026-000001", responses.get(0).getSerialNumber());
    }

    @Test
    void getCertificateById_whenWrongInstitution_shouldThrowForbidden() {
        UUID otherInstitutionId = UUID.randomUUID();
        Certificate certificate = Certificate.builder()
                .status(CertificateStatus.ISSUED)
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(otherInstitutionId);

        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));

        assertThrows(ForbiddenException.class,
                () -> certificateService.getCertificateById(certificateId, institutionId));
    }
}
