package tz.elmkusoma.certificate.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateSignatorySnapshotRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * The export gate is what keeps an unreleased certificate from becoming a file.
 *
 * <p>Everything else about authorisation is shared with the read path and covered by
 * {@code CertificateOwnershipTest}. What is unique here is the status rule: once a PDF is saved
 * somewhere, the system can no longer refuse to serve it, so "has this been issued?" has to be
 * answered before the bytes are produced, not after.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificatePdfServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private CertificateTemplateRepository templateRepository;
    @Mock private CertificateSignatorySnapshotRepository signatorySnapshotRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private CertificatePdfRenderer renderer;

    @InjectMocks private CertificatePdfService service;

    private UUID certificateId;
    private UUID institutionId;

    @BeforeEach
    void setUp() throws Exception {
        certificateId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        when(signatorySnapshotRepository
                .findByCertificateIdAndIsDeletedFalseOrderByDisplayOrderAsc(any()))
                .thenReturn(java.util.List.of());
        when(templateRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.empty());
        when(institutionRepository.findById(any())).thenReturn(Optional.empty());
        when(renderer.render(any(), anyList(), any(), any()))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});
    }

    private Certificate certificate(Certificate.CertificateStatus status) {
        Certificate certificate = Certificate.builder()
                .templateId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .serialNumber("CERT-CMP-2026-000042")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .completionDate(LocalDate.now())
                .issueDate(java.time.LocalDateTime.now())
                .status(status)
                .verificationCode("VERIFY-ME")
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);
        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));
        return certificate;
    }

    @Test
    @DisplayName("an issued certificate exports")
    void issuedCertificateExports() {
        certificate(Certificate.CertificateStatus.ISSUED);

        byte[] pdf = service.exportCertificate(certificateId, institutionId);

        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F'}, pdf);
    }

    @Test
    @DisplayName("a revoked certificate still exports, so a holder can keep the record of what was withdrawn")
    void revokedCertificateExports() {
        certificate(Certificate.CertificateStatus.REVOKED);

        byte[] pdf = service.exportCertificate(certificateId, institutionId);

        assertEquals(4, pdf.length);
    }

    @Test
    @DisplayName("a draft refuses to export: it is not a document yet")
    void draftRefusesToExport() {
        certificate(Certificate.CertificateStatus.DRAFT);

        // IllegalStateException maps to 409 CONFLICT in GlobalExceptionHandler, so a caller
        // asking for an unreleased certificate gets a status that says "wrong state", not 200.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.exportCertificate(certificateId, institutionId));

        assertTrue(ex.getMessage().contains("DRAFT"),
                "the operator needs to know what is blocking them");
        assertTrue(ex.getMessage().contains("Issue it first"),
                "and what to do about it");
    }

    @Test
    @DisplayName("a certificate in review refuses to export for the same reason")
    void pendingApprovalRefusesToExport() {
        certificate(Certificate.CertificateStatus.PENDING_APPROVAL);

        assertThrows(IllegalStateException.class,
                () -> service.exportCertificate(certificateId, institutionId));
    }

    @Test
    @DisplayName("another institution's certificate cannot be exported even with its id")
    void foreignCertificateIsRefused() {
        Certificate certificate = certificate(Certificate.CertificateStatus.ISSUED);
        certificate.setInstitutionId(UUID.randomUUID());

        assertThrows(ForbiddenException.class,
                () -> service.exportCertificate(certificateId, institutionId));
    }

    @Test
    @DisplayName("a certificate that does not exist is a 404, not a 500")
    void missingCertificateIsNotFound() {
        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.empty());

        assertThrows(tz.elmkusoma.exception.ResourceNotFoundException.class,
                () -> service.exportCertificate(certificateId, institutionId));
    }

    @Test
    @DisplayName("the renderer receives the certificate, its snapshots and the institution name")
    void institutionContextIsPassedThrough() throws Exception {
        certificate(Certificate.CertificateStatus.ISSUED);

        service.exportCertificate(certificateId, institutionId);

        org.mockito.ArgumentCaptor<Certificate> rendered =
                org.mockito.ArgumentCaptor.forClass(Certificate.class);
        org.mockito.ArgumentCaptor<String> name = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(renderer).render(rendered.capture(), anyList(), name.capture(), any());

        assertEquals(certificateId, rendered.getValue().getId(),
                "the certificate that was loaded is the one that must be drawn");
        assertNull(name.getValue(), "no institution row means no invented name");
    }

    @Test
    @DisplayName("the renderer failing becomes a conflict, not a raw IOException to the client")
    void rendererFailureIsWrapped() throws Exception {
        certificate(Certificate.CertificateStatus.ISSUED);
        when(renderer.render(any(), anyList(), any(), any()))
                .thenThrow(new java.io.IOException("disk full"));

        assertThrows(IllegalStateException.class,
                () -> service.exportCertificate(certificateId, institutionId));
    }
}

