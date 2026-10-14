package tz.elmkusoma.nfe.certificate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.nfe.certificate.domain.NfeCertificate;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateRequest;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateResponse;
import tz.elmkusoma.nfe.certificate.repository.NfeCertificateRepository;
import tz.elmkusoma.nfe.certificate.service.impl.NfeCertificateServiceImpl;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.service.NfeScopeValidator;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>The NFE certificate is a second, parallel credential stack. The shared lifecycle gates
 * issuance on eligibility, approvals and signatories; this one is a plain row, so the only thing
 * standing between a provider admin and a printable credential is what this service will accept.</p>
 *
 * <p>These tests are that line.</p>
 */
@ExtendWith(MockitoExtension.class)
class NfeCertificateServiceTest {

    @Mock private NfeCertificateRepository certificateRepository;
    @Mock private NfeScopeValidator scopeValidator;

    @InjectMocks private NfeCertificateServiceImpl service;

    private UUID institutionId;
    private UUID providerId;
    private UUID learnerId;
    private UUID programId;
    private UUID certificateId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        programId = UUID.randomUUID();
        certificateId = UUID.randomUUID();
    }

    private void stubScope() {
        EducationProvider provider = EducationProvider.builder().build();
        provider.setId(providerId);
        when(scopeValidator.requireOwnedProvider(institutionId, providerId)).thenReturn(provider);
        when(scopeValidator.requireOwnedLearner(institutionId, learnerId)).thenReturn(learnerId);
        when(scopeValidator.requireOwnedProgram(institutionId, programId)).thenReturn(programId);
    }

    private NfeCertificateRequest request(String status) {
        return NfeCertificateRequest.builder()
                .learnerId(learnerId)
                .programId(programId)
                .certificateType("COMPLETION")
                .title("Advanced Welding")
                .studentName("Asha Kimaro")
                .status(status)
                .build();
    }

    private NfeCertificate existing(NfeCertificate.CertificateStatus status) {
        NfeCertificate c = NfeCertificate.builder()
                .providerId(providerId)
                .learnerId(learnerId)
                .programId(programId)
                .certificateType(NfeCertificate.CertificateType.COMPLETION)
                .title("Advanced Welding")
                .studentName("Asha Kimaro")
                .serialNumber("NFE/2026/ABCD1234")
                .verificationCode("NFE-ABCDEFGH1234")
                .status(status)
                .build();
        c.setId(certificateId);
        c.setInstitutionId(institutionId);
        return c;
    }

    private void stubExisting(NfeCertificate certificate) {
        when(certificateRepository.findByIdAndInstitutionId(certificateId, institutionId))
                .thenReturn(Optional.of(certificate));
    }

    // ---- Creation ----

    @Test
    @DisplayName("a certificate cannot be created already revoked - that records an event that never happened")
    void createCertificate_cannotStartAsRevoked() {
        stubScope();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.createCertificate(institutionId, providerId, request("REVOKED")));

        assertTrue(ex.getMessage().contains("REVOKED"), ex.getMessage());
        verify(certificateRepository, never()).save(any(NfeCertificate.class));
    }

    @Test
    @DisplayName("a draft is still the default, and asking for one still works")
    void createCertificate_defaultsToDraft() {
        stubScope();
        when(certificateRepository.save(any(NfeCertificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NfeCertificateResponse response =
                service.createCertificate(institutionId, providerId, request(null));

        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    @DisplayName("a provider registering an already-issued credential is still allowed")
    void createCertificate_mayStartIssued() {
        stubScope();
        when(certificateRepository.save(any(NfeCertificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NfeCertificateResponse response =
                service.createCertificate(institutionId, providerId, request("ISSUED"));

        assertEquals("ISSUED", response.getStatus());
    }

    // ---- Updates ----

    @Test
    @DisplayName("an issued certificate cannot be quietly returned to draft")
    void updateCertificate_cannotReturnIssuedToDraft() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        stubExisting(certificate);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateCertificate(institutionId, certificateId, request("DRAFT")));

        assertTrue(ex.getMessage().contains("DRAFT"), ex.getMessage());
        assertEquals(NfeCertificate.CertificateStatus.ISSUED, certificate.getStatus(),
                "the row must be left exactly as it was");
        verify(certificateRepository, never()).save(any(NfeCertificate.class));
    }

    @Test
    @DisplayName("a revoked certificate cannot be un-revoked by a second request")
    void updateCertificate_cannotUnRevoke() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.REVOKED);
        stubExisting(certificate);

        assertThrows(IllegalStateException.class,
                () -> service.updateCertificate(institutionId, certificateId, request("ISSUED")));

        assertEquals(NfeCertificate.CertificateStatus.REVOKED, certificate.getStatus());
        verify(certificateRepository, never()).save(any(NfeCertificate.class));
    }

    @Test
    @DisplayName("an issued certificate can still be revoked - that is the whole point of revoking")
    void updateCertificate_canRevokeIssued() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        stubExisting(certificate);
        when(certificateRepository.save(any(NfeCertificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NfeCertificateResponse response =
                service.updateCertificate(institutionId, certificateId, request("REVOKED"));

        assertEquals("REVOKED", response.getStatus());
    }

    @Test
    @DisplayName("re-stating the status it already has is a no-op rather than a rejection")
    void updateCertificate_mayRepeatItsOwnStatus() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.DRAFT);
        stubExisting(certificate);
        when(certificateRepository.save(any(NfeCertificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NfeCertificateResponse response =
                service.updateCertificate(institutionId, certificateId, request("DRAFT"));

        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    @DisplayName("the verification code printed on a learner's document cannot be re-pointed")
    void updateCertificate_cannotChangeVerificationCode() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        stubExisting(certificate);

        NfeCertificateRequest request = request("ISSUED");
        request.setVerificationCode("NFE-ZZZZZZZZZZZZ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateCertificate(institutionId, certificateId, request));

        assertTrue(ex.getMessage().contains("verification code"), ex.getMessage());
        assertEquals("NFE-ABCDEFGH1234", certificate.getVerificationCode());
        verify(certificateRepository, never()).save(any(NfeCertificate.class));
    }

    @Test
    @DisplayName("the serial number is fixed for the same reason")
    void updateCertificate_cannotChangeSerialNumber() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.DRAFT);
        stubExisting(certificate);

        NfeCertificateRequest request = request("DRAFT");
        request.setSerialNumber("NFE/2026/FAKE0001");

        assertThrows(IllegalStateException.class,
                () -> service.updateCertificate(institutionId, certificateId, request));
        assertEquals("NFE/2026/ABCD1234", certificate.getSerialNumber());
    }

    @Test
    @DisplayName("an ordinary edit still goes through - the guard is not a general freeze")
    void updateCertificate_canStillEditContent() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.DRAFT);
        stubExisting(certificate);
        when(certificateRepository.save(any(NfeCertificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NfeCertificateRequest request = request(null);
        request.setTitle("Advanced Welding (Revised)");

        NfeCertificateResponse response =
                service.updateCertificate(institutionId, certificateId, request);

        assertEquals("Advanced Welding (Revised)", response.getTitle());
        assertEquals("DRAFT", response.getStatus());
    }

    // ---- Verification ----

    @Test
    @DisplayName("verification tells the truth about a certificate whose expiry has passed")
    void verifyCertificate_reportsExpiredOncePastItsExpiryDate() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        certificate.setExpiryDate(LocalDate.now().minusDays(1));
        when(certificateRepository.findByVerificationCode("NFE-ABCDEFGH1234", institutionId))
                .thenReturn(Optional.of(certificate));

        NfeCertificateResponse response =
                service.verifyCertificate(institutionId, "NFE-ABCDEFGH1234");

        assertEquals("EXPIRED", response.getStatus(),
                "a lapsed certificate must not verify as ISSUED");
    }

    @Test
    @DisplayName("verification of a current certificate reports it as issued")
    void verifyCertificate_reportsIssuedWhileStillCurrent() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        certificate.setExpiryDate(LocalDate.now().plusYears(1));
        when(certificateRepository.findByVerificationCode("NFE-ABCDEFGH1234", institutionId))
                .thenReturn(Optional.of(certificate));

        assertEquals("ISSUED",
                service.verifyCertificate(institutionId, "NFE-ABCDEFGH1234").getStatus());
    }

    @Test
    @DisplayName("revocation outranks an expiry date that has not yet arrived")
    void verifyCertificate_reportsRevokedAheadOfEverythingElse() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.REVOKED);
        certificate.setExpiryDate(LocalDate.now().plusYears(1));
        when(certificateRepository.findByVerificationCode("NFE-ABCDEFGH1234", institutionId))
                .thenReturn(Optional.of(certificate));

        assertEquals("REVOKED",
                service.verifyCertificate(institutionId, "NFE-ABCDEFGH1234").getStatus());
    }

    @Test
    @DisplayName("a certificate that was never issued does not read as issued")
    void verifyCertificate_reportsDraftHonestly() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.DRAFT);
        when(certificateRepository.findByVerificationCode("NFE-ABCDEFGH1234", institutionId))
                .thenReturn(Optional.of(certificate));

        assertEquals("DRAFT",
                service.verifyCertificate(institutionId, "NFE-ABCDEFGH1234").getStatus());
    }

    @Test
    @DisplayName("a certificate with no expiry date never reads as expired")
    void verifyCertificate_withoutAnExpiryDateStaysAsRecorded() {
        NfeCertificate certificate = existing(NfeCertificate.CertificateStatus.ISSUED);
        when(certificateRepository.findByVerificationCode("NFE-ABCDEFGH1234", institutionId))
                .thenReturn(Optional.of(certificate));

        assertEquals("ISSUED",
                service.verifyCertificate(institutionId, "NFE-ABCDEFGH1234").getStatus());
    }
}
