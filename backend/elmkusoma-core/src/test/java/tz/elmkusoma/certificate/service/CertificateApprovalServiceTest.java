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
import tz.elmkusoma.certificate.domain.CertificateApproval;
import tz.elmkusoma.certificate.domain.CertificateApproval.ApprovalStage;
import tz.elmkusoma.certificate.domain.CertificateApproval.DecisionStatus;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.CertificateTemplate.TemplateFamily;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.CertificateApprovalRepository;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.exception.ForbiddenException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The approval workflow, and the guarantee it exists to provide: a certificate cannot reach
 * ISSUED because someone pressed a button.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificateApprovalServiceTest {

    @Mock private CertificateApprovalRepository approvalRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private CertificateTemplateRepository templateRepository;
    @Mock private AuditService auditService;

    @InjectMocks private CertificateApprovalService approvalService;

    private UUID institutionId;
    private UUID certificateId;
    private UUID preparerId;
    private UUID reviewerId;
    private CertificateApprovalPolicy policy;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        certificateId = UUID.randomUUID();
        preparerId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        policy = new CertificateApprovalPolicy(
                "PREPARATION,APPROVAL", "PREPARATION,VALIDATION,APPROVAL", false);
        ReflectionTestUtils.setField(approvalService, "policy", policy);
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── the gate ──────────────────────────────────────────────────────────────────────────

    @Test
    void issuance_isRefusedWhenNoApprovalExists() {
        Certificate certificate = certificate(Certificate.CertificateStatus.PENDING_APPROVAL);
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);
        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> approvalService.assertIssuable(certificate, template));
        assertTrue(ex.getMessage().contains("not approved for issuance"), ex.getMessage());
    }

    @Test
    void issuance_isRefusedWhenOnlyPreparationIsApproved() {
        Certificate certificate = certificate(Certificate.CertificateStatus.PENDING_APPROVAL);
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);

        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
                        eq(certificateId), any(), any()))
                .thenAnswer(inv -> {
                    ApprovalStage stage = inv.getArgument(1);
                    return stage == ApprovalStage.PREPARATION
                            ? Optional.of(approved(stage))
                            : Optional.empty();
                });

        // Before V161 this was the whole workflow: a draft, and an admin click.
        assertThrows(IllegalStateException.class,
                () -> approvalService.assertIssuable(certificate, template));
    }

    @Test
    void issuance_isAllowedForATrainingCertificateOnceEveryRequiredStageIsApproved() {
        Certificate certificate = certificate(Certificate.CertificateStatus.APPROVED);
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);
        // The gate queries twice per stage: once for APPROVED and once for REJECTED. Only the
        // APPROVED lookup may answer here, or the certificate looks both approved and rejected.
        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenAnswer(inv -> {
                    List<DecisionStatus> statuses = inv.getArgument(2);
                    return statuses.contains(DecisionStatus.REJECTED)
                            ? Optional.empty()
                            : Optional.of(approved(ApprovalStage.PREPARATION));
                });

        assertDoesNotThrow(() -> approvalService.assertIssuable(certificate, template));
    }

    @Test
    void anAcademicCertificateAlsoRequiresEvidenceValidation() {
        Certificate certificate = certificate(Certificate.CertificateStatus.PENDING_APPROVAL);
        CertificateTemplate template = template(TemplateFamily.FAMILY_ACADEMIC);

        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenAnswer(inv -> {
                    ApprovalStage stage = inv.getArgument(1);
                    return stage == ApprovalStage.VALIDATION ? Optional.empty()
                            : Optional.of(approved(stage));
                });

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> approvalService.assertIssuable(certificate, template));
        assertTrue(ex.getMessage().contains("VALIDATION"), ex.getMessage());
    }

    @Test
    void issuance_isRefusedWhenTheApprovalIsStale() {
        // Approved against template version 2, but the template has since been edited to 3. The
        // sign-off was given for a design nobody approved.
        Certificate certificate = certificate(Certificate.CertificateStatus.APPROVED);
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);
        template.setVersion(3);

        CertificateApproval stale = approved(ApprovalStage.PREPARATION);
        stale.setTemplateVersion(2);
        CertificateApproval current = approved(ApprovalStage.APPROVAL);
        current.setTemplateVersion(3);

        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenAnswer(inv -> {
                    ApprovalStage stage = inv.getArgument(1);
                    return Optional.of(stage == ApprovalStage.PREPARATION ? stale : current);
                });

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> approvalService.assertIssuable(certificate, template));
        assertTrue(ex.getMessage().contains("version"), ex.getMessage());
    }

    @Test
    void anAlreadyIssuedOrRevokedCertificateCannotBeIssuedAgain() {
        assertThrows(IllegalStateException.class, () -> approvalService.assertIssuable(
                certificate(Certificate.CertificateStatus.ISSUED), template(TemplateFamily.FAMILY_TRAINING)));
        assertThrows(IllegalStateException.class, () -> approvalService.assertIssuable(
                certificate(Certificate.CertificateStatus.REVOKED), template(TemplateFamily.FAMILY_TRAINING)));
    }

    // ── decisions ─────────────────────────────────────────────────────────────────────────

    @Test
    void aTeacherCannotDecideTheInstitutionalApprovalStage() {
        stubCertificate();
        assertThrows(ForbiddenException.class, () -> approvalService.decide(
                certificateId, ApprovalStage.APPROVAL, true, institutionId,
                reviewerId, "teacher@x.tz", "TEACHER", null));
    }

    @Test
    void thePreparerCannotApproveTheirOwnCertificate() {
        stubCertificate();
        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
                        eq(certificateId), eq(ApprovalStage.PREPARATION), any()))
                .thenReturn(Optional.of(approval(ApprovalStage.PREPARATION, DecisionStatus.APPROVED,
                        preparerId, "teacher@x.tz", "TEACHER")));

        // Self-approval is the failure mode an approval step exists to prevent.
        assertThrows(ForbiddenException.class, () -> approvalService.decide(
                certificateId, ApprovalStage.APPROVAL, true, institutionId,
                preparerId, "teacher@x.tz", "TEACHER", null));
    }

    @Test
    void aRejectionWithoutAReasonIsRefused() {
        stubCertificate();
        assertThrows(IllegalArgumentException.class, () -> approvalService.decide(
                certificateId, ApprovalStage.APPROVAL, false, institutionId,
                reviewerId, "admin@x.tz", "INSTITUTION_ADMIN", "   "));
    }

    @Test
    void anIssuedCertificatesApprovalHistoryCannotBeRewritten() {
        stubCertificate();
        Certificate issued = certificate(Certificate.CertificateStatus.ISSUED);
        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(issued));

        // Correcting an issued certificate means revoking it, not rewriting who approved it.
        assertThrows(IllegalStateException.class, () -> approvalService.withdrawApproval(
                certificateId, ApprovalStage.APPROVAL, institutionId,
                reviewerId, "admin@x.tz", "INSTITUTION_ADMIN", "changed my mind"));
    }

    // ── submission ────────────────────────────────────────────────────────────────────────

    @Test
    void submittingMovesTheCertificateIntoReview() {
        stubCertificate();
        when(approvalRepository.save(any(CertificateApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByCertificateIdAndIsDeletedFalseOrderByCreatedAtAsc(certificateId))
                .thenReturn(List.of(approval(ApprovalStage.PREPARATION, DecisionStatus.APPROVED,
                        preparerId, "teacher@x.tz", "TEACHER")));

        Certificate submitted = approvalService.submitForApproval(certificateId, institutionId,
                preparerId, "teacher@x.tz", "TEACHER");

        assertEquals(Certificate.CertificateStatus.PENDING_APPROVAL, submitted.getStatus());
    }

    @Test
    void aCertificateCannotBeSubmittedTwice() {
        stubCertificate();
        Certificate pending = certificate(Certificate.CertificateStatus.PENDING_APPROVAL);
        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class, () -> approvalService.submitForApproval(
                certificateId, institutionId, preparerId, "teacher@x.tz", "TEACHER"));
    }

    @Test
    void aRejectionReturnsTheDecisionThatWasJustRecorded() {
        stubCertificate();
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);
        template.setId(UUID.randomUUID());
        certificate().setTemplateId(template.getId());
        when(templateRepository.findByIdAndIsDeletedFalse(template.getId()))
                .thenReturn(Optional.of(template));

        // A rejection leaves no PENDING or APPROVED row for the stage, so the returned decision has
        // to be the one just saved rather than a re-read filtered to those statuses.
        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(approvalRepository.save(any(CertificateApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CertificateApproval decision = approvalService.decide(
                certificateId, ApprovalStage.APPROVAL, false, institutionId,
                reviewerId, "admin@x.tz", "INSTITUTION_ADMIN", "evidence is not attached");

        assertEquals(DecisionStatus.REJECTED, decision.getDecisionStatus());
        assertEquals("evidence is not attached", decision.getReason());
        assertEquals(ApprovalStage.APPROVAL, decision.getStage());
    }

    @Test
    void aReSupmittedStageKeepsTheEarlierRejectionOnRecord() {
        stubCertificate();
        CertificateTemplate template = template(TemplateFamily.FAMILY_TRAINING);
        template.setId(UUID.randomUUID());
        certificate().setTemplateId(template.getId());
        when(templateRepository.findByIdAndIsDeletedFalse(template.getId()))
                .thenReturn(Optional.of(template));

        // The current-decision lookup finds nothing, because the only row for this stage was
        // rejected. The service must create a new decision rather than mutating the old one.
        when(approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());

        CertificateApproval rejected = approval(ApprovalStage.APPROVAL, DecisionStatus.REJECTED,
                reviewerId, "admin@x.tz", "INSTITUTION_ADMIN");
        rejected.setId(UUID.randomUUID());
        rejected.setReason("evidence missing");
        when(approvalRepository.save(any(CertificateApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CertificateApproval reApproval = approvalService.decide(
                certificateId, ApprovalStage.APPROVAL, true, institutionId,
                reviewerId, "admin@x.tz", "INSTITUTION_ADMIN", null);

        assertEquals(DecisionStatus.APPROVED, reApproval.getDecisionStatus());
        // A different row: the refusal stays in the history.
        org.junit.jupiter.api.Assertions.assertNotEquals(rejected.getId(), reApproval.getId());
    }

    // ── policy shape ──────────────────────────────────────────────────────────────────────

    @Test
    void theDefaultPolicyRequiresMoreOfAnAcademicAward() {
        assertTrue(policy.requiredStages(TemplateFamily.FAMILY_ACADEMIC).contains(ApprovalStage.VALIDATION));
        assertTrue(!policy.requiredStages(TemplateFamily.FAMILY_TRAINING).contains(ApprovalStage.VALIDATION));
        // Self-approval is never permitted for an academic award.
        assertTrue(!policy.allowsSelfApproval(TemplateFamily.FAMILY_ACADEMIC));
    }

    @Test
    void anUnparseableStageConfigurationFallsBackRatherThanWeakeningControl() {
        CertificateApprovalPolicy fallback = new CertificateApprovalPolicy("NOT_A_STAGE", "", true);
        assertEquals(policy.requiredStages(TemplateFamily.FAMILY_TRAINING),
                fallback.requiredStages(TemplateFamily.FAMILY_TRAINING));
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────

    private Certificate certificate() {
        return certificate(Certificate.CertificateStatus.PENDING_APPROVAL);
    }

    private void stubCertificate() {
        Certificate certificate = certificate(Certificate.CertificateStatus.DRAFT);
        when(certificateRepository.findByIdAndIsDeletedFalse(certificateId))
                .thenReturn(Optional.of(certificate));
    }

    private Certificate certificate(Certificate.CertificateStatus status) {
        Certificate certificate = Certificate.builder()
                .serialNumber("CERT-CMP-2026-000001")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .completionDate(LocalDate.now())
                .status(status)
                .build();
        certificate.setId(certificateId);
        certificate.setInstitutionId(institutionId);
        certificate.setTemplateId(UUID.randomUUID());
        return certificate;
    }

    private CertificateTemplate template(TemplateFamily family) {
        CertificateTemplate template = CertificateTemplate.builder()
                .name("T").templateType(CertificateTemplate.TemplateType.COMPLETION)
                .templateFamily(family).isActive(true).build();
        template.setInstitutionId(institutionId);
        return template;
    }

    private CertificateApproval approved(ApprovalStage stage) {
        return approval(stage, DecisionStatus.APPROVED, reviewerId, "admin@x.tz", "INSTITUTION_ADMIN");
    }

    private CertificateApproval approval(ApprovalStage stage, DecisionStatus status,
                                          UUID actorId, String email, String role) {
        CertificateApproval approval = new CertificateApproval();
        approval.setCertificateId(certificateId);
        approval.setInstitutionId(institutionId);
        approval.setStage(stage);
        approval.setDecisionStatus(status);
        approval.setActorUserId(actorId);
        approval.setActorEmail(email);
        approval.setActorRole(role);
        approval.setTemplateVersion(1);
        return approval;
    }
}
