package tz.elmkusoma.certificate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateSignatory;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.CertificateTemplateSignatory;
import tz.elmkusoma.certificate.dto.GenerateCertificateRequest;
import tz.elmkusoma.certificate.dto.RevokeCertificateRequest;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateSignatoryRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateSignatoryRepository;
import tz.elmkusoma.certificate.repository.TranscriptEntryRepository;
import tz.elmkusoma.certificate.repository.TranscriptRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security hardening of the certificate lifecycle.
 *
 * <p>Each test here corresponds to a defect found by the audit, and each failed before the fix.
 * They are grouped in this file rather than scattered because they share one theme: the issuance
 * and verification paths trusted things they had no business trusting.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificateIssuanceSecurityTest {

    @Mock private CertificateTemplateRepository templateRepository;
    @Mock private CertificateSignatoryRepository signatoryRepository;
    @Mock private CertificateTemplateSignatoryRepository templateSignatoryRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private TranscriptRepository transcriptRepository;
    @Mock private TranscriptEntryRepository transcriptEntryRepository;
    @Mock private CertificateMapper certificateMapper;
    @Mock private AuditService auditService;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private LearnerNotificationRepository learnerNotificationRepository;
    @Mock private InstitutionRepository institutionRepository;

    @InjectMocks private CertificateService certificateService;

    private UUID institutionId;
    private UUID studentId;
    private UUID userId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        ReflectionTestUtils.setField(certificateService, "publicFrontendUrl", "https://elmkusoma.co.tz");
        // Strictness is LENIENT for this class, so plain when(...) is safe even where a test does
        // not reach these stubs. Note lenient().when(mock).call(...) does NOT compile: when()
        // returns the stubber, so the call has to sit inside it.
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(certificateMapper.toCertificateResponse(any(Certificate.class)))
                .thenReturn(tz.elmkusoma.certificate.dto.CertificateResponse.builder().build());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ cross-tenant issuance Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void generate_refusesAStudentFromAnotherInstitution() {
        Student foreign = student();
        foreign.setInstitutionId(UUID.randomUUID());
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.of(foreign));
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(activeTemplate()));

        // Issuing an institutional record about another school's student is a cross-tenant write,
        // and it was previously possible: the lookup tolerated a miss and used the raw value.
        assertThrows(ForbiddenException.class, () -> certificateService.generateCertificate(
                request(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    @Test
    void generate_refusesAnUnknownStudent() {
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.empty());
        when(studentRepository.findByUserIdAndIsDeletedFalse(studentId)).thenReturn(Optional.empty());
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(activeTemplate()));

        // Previously a miss fell through and the caller's raw id was stored anyway.
        assertThrows(RuntimeException.class, () -> certificateService.generateCertificate(
                request(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ recipient name must come from the record Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void generate_takesTheRecipientNameFromTheStudentRecordNotTheRequest() {
        Student student = student();
        student.setInstitutionId(institutionId);
        when(studentRepository.findByIdAndIsDeletedFalse(studentId)).thenReturn(Optional.of(student));
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(activeTemplate()));
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(
                User.builder().firstName("Amini").lastName("Mwakalinga").build()));

        GenerateCertificateRequest request = request();
        // A name the operator typed. It must never reach the certificate.
        request.setStudentName("Definitely Not The Student");

        certificateService.generateCertificate(request, institutionId, UUID.randomUUID(),
                "admin@x.tz", "ADMIN");

        var captor = org.mockito.ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(captor.capture());
        assertEquals("Amini Mwakalinga", captor.getValue().getStudentName());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ template activation Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void generate_refusesADeactivatedTemplate() {
        CertificateTemplate template = activeTemplate();
        template.setIsActive(false);
        when(templateRepository.findByIdAndIsDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(studentRepository.findByIdAndIsDeletedFalse(studentId))
                .thenReturn(Optional.of(student()));

        // Platform Admin deactivates a template to withdraw it. That is meaningless if issuance
        // ignores the flag.
        assertThrows(IllegalStateException.class, () -> certificateService.generateCertificate(
                request(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ signatory authorization, at issuance Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void issue_refusesWhenALinkedSignatoryHasBeenDeactivated() {
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(link()));
        when(signatoryRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(
                signatory(CertificateSignatory.SignatoryStatus.INACTIVE, null, null, null)));

        // The check existed, but only when a signatory was LINKED. Deactivating a signatory
        // afterwards changed nothing about what could still be issued.
        assertThrows(IllegalStateException.class, () -> certificateService.issueCertificate(
                certificate.getId(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    @Test
    void issue_refusesWhenASignatoryHasExpired() {
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(link()));
        when(signatoryRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(
                signatory(CertificateSignatory.SignatoryStatus.ACTIVE, null,
                        LocalDate.now().minusDays(30), LocalDate.now().minusDays(1))));

        assertThrows(IllegalStateException.class, () -> certificateService.issueCertificate(
                certificate.getId(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    @Test
    void issue_refusesASignatoryScopedToOtherInstitutions() {
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(link()));
        when(signatoryRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(
                signatory(CertificateSignatory.SignatoryStatus.ACTIVE,
                        UUID.randomUUID(), null, null)));

        assertThrows(ForbiddenException.class, () -> certificateService.issueCertificate(
                certificate.getId(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    @Test
    void issue_refusesASignatoryNotScopedToThisCertificateType() {
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(link()));
        when(signatoryRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(
                signatory(CertificateSignatory.SignatoryStatus.ACTIVE, null, null, null, "ACHIEVEMENT")));

        assertThrows(ForbiddenException.class, () -> certificateService.issueCertificate(
                certificate.getId(), institutionId, UUID.randomUUID(), "admin@x.tz", "ADMIN"));
    }

    @Test
    void issue_succeedsWhenTheSignatoryIsAuthorized() {
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId))
                .thenReturn(List.of(link()));
        when(signatoryRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(
                signatory(CertificateSignatory.SignatoryStatus.ACTIVE, institutionId,
                        LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))));
        when(learnerNotificationRepository.save(any(tz.elmkusoma.learner.domain.LearnerNotification.class))).thenReturn(null);

        certificateService.issueCertificate(certificate.getId(), institutionId,
                UUID.randomUUID(), "admin@x.tz", "ADMIN");

        assertEquals(Certificate.CertificateStatus.ISSUED, certificate.getStatus());
    }

    @Test
    void issue_isAllowedForATemplateWithNoSignatories() {
        // A template with no signature slots is a legitimate configuration, not a defect.
        Certificate certificate = draftCertificate();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId)).thenReturn(List.of());
        when(learnerNotificationRepository.save(any(tz.elmkusoma.learner.domain.LearnerNotification.class))).thenReturn(null);

        certificateService.issueCertificate(certificate.getId(), institutionId,
                UUID.randomUUID(), "admin@x.tz", "ADMIN");

        assertEquals(Certificate.CertificateStatus.ISSUED, certificate.getStatus());
    }

    @Test
    void issue_recordsTheActorInTheAuditTrail() {
        Certificate certificate = draftCertificate();
        UUID actor = UUID.randomUUID();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));
        when(templateSignatoryRepository.findByTemplateId(templateId)).thenReturn(List.of());
        when(learnerNotificationRepository.save(any(tz.elmkusoma.learner.domain.LearnerNotification.class))).thenReturn(null);

        certificateService.issueCertificate(certificate.getId(), institutionId, actor,
                "admin@x.tz", "ADMIN");

        // The audit used to record userId = null, so the trail showed that a certificate had
        // been issued but not who issued it.
        verify(auditService).recordAuditLog(eq(institutionId), eq(actor), eq("admin@x.tz"),
                eq("ADMIN"), eq("Certificate"), eq(certificate.getId()), any(),
                any(), any(), any());
    }

    @Test
    void revoke_recordsTheActorInTheAuditTrail() {
        Certificate certificate = draftCertificate();
        certificate.setStatus(Certificate.CertificateStatus.ISSUED);
        UUID actor = UUID.randomUUID();
        when(certificateRepository.findByIdAndIsDeletedFalse(certificate.getId()))
                .thenReturn(Optional.of(certificate));

        RevokeCertificateRequest request = new RevokeCertificateRequest();
        request.setReason("Issued in error");
        certificateService.revokeCertificate(certificate.getId(), institutionId, request, actor,
                "admin@x.tz", "ADMIN");

        verify(auditService).recordAuditLog(eq(institutionId), eq(actor), eq("admin@x.tz"),
                eq("ADMIN"), eq("Certificate"), eq(certificate.getId()), any(),
                any(), any(), any());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ public verification privacy Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void verification_doesNotLeakTheInternalRowId() {
        Certificate certificate = issuedCertificate();
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("CODE123"))
                .thenReturn(Optional.of(certificate));

        var response = certificateService.verifyCertificate("CODE123", "1.2.3.4");

        // The public endpoint is unauthenticated; handing out primary keys tells an attacker how
        // many rows exist and lets them probe neighbouring ids.
        assertNull(response.getId(), "the public response must not carry the internal row id");
        assertEquals("CERT-CMP-2026-000001", response.getSerialNumber());
    }

    @Test
    void verification_doesNotLeakGradeSkillsOrInstructor() {
        Certificate certificate = issuedCertificate();
        certificate.setGrade("A-");
        certificate.setSkills(List.of("Advanced Mathematics"));
        certificate.setInstructorName("Dr Confidential");
        certificate.setStudentIdNumber("S-2026-0001");
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("CODE123"))
                .thenReturn(Optional.of(certificate));

        var response = certificateService.verifyCertificate("CODE123", "1.2.3.4");

        // A grade on an anonymous endpoint is an academic record disclosure.
        assertNull(response.getGrade());
        assertNull(response.getSkills());
        assertNull(response.getInstructorName());
        // The holder's name stays: it is what proves the document is theirs.
        assertEquals("Amini Mwakalinga", response.getStudentName());
    }

    @Test
    void verification_doesNotEchoTheRevocationReason() {
        Certificate certificate = issuedCertificate();
        certificate.setStatus(Certificate.CertificateStatus.REVOKED);
        certificate.setRevokedReason("Academic misconduct - disciplinary panel referral");
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("CODE123"))
                .thenReturn(Optional.of(certificate));

        var response = certificateService.verifyCertificate("CODE123", "1.2.3.4");

        assertFalse(response.isValid());
        assertFalse(response.getMessage().contains("misconduct"),
                "the free-text revocation reason is not the public's business: " + response.getMessage());
    }

    @Test
    void verification_ofADraftIsNeverReportedAsValid() {
        Certificate certificate = draftCertificate();
        certificate.setVerificationCode("CODE123");
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("CODE123"))
                .thenReturn(Optional.of(certificate));

        var response = certificateService.verifyCertificate("CODE123", "1.2.3.4");

        assertFalse(response.isValid());
        // Previously valid=false was returned alongside the message "Certificate is valid and
        // verified", which is worse than either answer alone.
        assertFalse(response.getMessage().toLowerCase().contains("valid and verified"),
                "status and message must not contradict each other: " + response.getMessage());
    }

    @Test
    void verification_auditsAnUnknownCode() {
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse("NOPE"))
                .thenReturn(Optional.empty());

        certificateService.verifyCertificate("NOPE", "1.2.3.4");

        // Failed lookups returned before the audit, so brute-force guessing left no trace.
        verify(auditService).recordAuditLog(any(), eq(null), eq(null), eq("PUBLIC"),
                eq("Certificate"), eq(null), eq(null), any(), eq(null), any());
    }

    @Test
    void verification_rateLimitIsScopedPerCallerNotGlobal() {
        when(certificateRepository.findByVerificationCodeAndIsDeletedFalse(any()))
                .thenReturn(Optional.empty());

        // One caller exhausting their own budget must not lock everyone else out.
        for (int i = 0; i < 15; i++) {
            try {
                certificateService.verifyCertificate("CODE", "10.0.0.1");
            } catch (RuntimeException ignored) {
                // this caller is now rate limited, as intended
            }
        }
        assertThrows(RuntimeException.class,
                () -> certificateService.verifyCertificate("CODE", "10.0.0.1"));

        // A different caller is unaffected.
        assertDoesNotThrowVerify("10.0.0.2");
    }

    private void assertDoesNotThrowVerify(String clientKey) {
        try {
            certificateService.verifyCertificate("CODE", clientKey);
        } catch (Exception e) {
            // A 404-shaped result is fine; being throttled is not.
            assertFalse(e.getMessage() != null && e.getMessage().contains("Too many"),
                    "a second caller must not inherit the first caller's rate limit");
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ helpers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private GenerateCertificateRequest request() {
        GenerateCertificateRequest request = new GenerateCertificateRequest();
        request.setTemplateId(templateId);
        request.setStudentId(studentId);
        request.setStudentName("Ignored Name");
        request.setCertificateType(Certificate.CertificateType.COMPLETION);
        request.setTitle("Certificate of Completion");
        request.setCompletionDate(LocalDate.now());
        return request;
    }

    private CertificateTemplate activeTemplate() {
        CertificateTemplate template = CertificateTemplate.builder()
                .name("Standard completion")
                .templateType(CertificateTemplate.TemplateType.COMPLETION)
                .build();
        template.setInstitutionId(institutionId);
        template.setIsActive(true);
        return template;
    }

    private Student student() {
        Student student = Student.builder().status(tz.elmkusoma.student.domain.StudentStatus.ACTIVE).build();
        student.setId(studentId);
        student.setUserId(userId);
        student.setInstitutionId(institutionId);
        return student;
    }

    private Certificate draftCertificate() {
        Certificate certificate = Certificate.builder()
                .templateId(templateId)
                .studentId(studentId)
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .completionDate(LocalDate.now())
                .status(Certificate.CertificateStatus.DRAFT)
                .verificationCode("CODE123")
                .build();
        certificate.setId(UUID.randomUUID());
        certificate.setInstitutionId(institutionId);
        return certificate;
    }

    private Certificate issuedCertificate() {
        Certificate certificate = draftCertificate();
        certificate.setStatus(Certificate.CertificateStatus.ISSUED);
        return certificate;
    }

    private CertificateTemplateSignatory link() {
        CertificateTemplateSignatory link = CertificateTemplateSignatory.builder()
                .templateId(templateId)
                .signatoryId(UUID.randomUUID())
                .displayOrder(1)
                .build();
        return link;
    }

    private CertificateSignatory signatory(CertificateSignatory.SignatoryStatus status,
                                           UUID institution,
                                           LocalDate validFrom,
                                           LocalDate validUntil) {
        return signatory(status, institution, validFrom, validUntil, "COMPLETION");
    }

    private CertificateSignatory signatory(CertificateSignatory.SignatoryStatus status,
                                           UUID institution,
                                           LocalDate validFrom,
                                           LocalDate validUntil,
                                           String certificateTypes) {
        CertificateSignatory signatory = CertificateSignatory.builder()
                .fullName("Principal")
                .positionTitle("Principal")
                .status(status)
                .certificateTypes(certificateTypes)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .build();
        signatory.setInstitutionId(institution);
        return signatory;
    }
}
