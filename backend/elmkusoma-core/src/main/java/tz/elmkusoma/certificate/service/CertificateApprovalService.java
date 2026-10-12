package tz.elmkusoma.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateApproval;
import tz.elmkusoma.certificate.domain.CertificateApproval.ApprovalStage;
import tz.elmkusoma.certificate.domain.CertificateApproval.DecisionStatus;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.CertificateTemplate.TemplateFamily;
import tz.elmkusoma.certificate.repository.CertificateApprovalRepository;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The one approval workflow. Every certificate family uses it; only the required stage set differs.
 *
 * <p>Enforced on the server. A certificate cannot reach ISSUED because a frontend button was
 * pressed - {@link #assertIssuable} is called from the issuance service and refuses anything whose
 * required stages are missing, stale or rejected.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateApprovalService {

    private final CertificateApprovalRepository approvalRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository templateRepository;
    private final CertificateApprovalPolicy policy;
    private final AuditService auditService;

    // ── submission ────────────────────────────────────────────────────────────────────────

    /**
     * Submits a draft for review and records the PREPARATION decision in one step.
     *
     * <p>Preparation is a real decision with a real actor, so it is recorded rather than inferred
     * from the status alone.</p>
     */
    @Transactional
    public Certificate submitForApproval(UUID certificateId, UUID institutionId,
                                         UUID actorUserId, String actorEmail, String actorRole) {
        Certificate certificate = requireCertificate(certificateId, institutionId);

        if (certificate.getStatus() != Certificate.CertificateStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a DRAFT certificate can be submitted for review (this one is "
                            + certificate.getStatus() + ")");
        }
        CertificateTemplate template = certificate.getTemplateId() == null ? null
                : templateRepository.findByIdAndIsDeletedFalse(certificate.getTemplateId()).orElse(null);

        CertificateApproval preparation = new CertificateApproval();
        preparation.setCertificateId(certificateId);
        preparation.setInstitutionId(institutionId);
        preparation.setStage(ApprovalStage.PREPARATION);
        preparation.setDecisionStatus(DecisionStatus.APPROVED);
        preparation.setActorUserId(actorUserId);
        preparation.setActorEmail(actorEmail);
        preparation.setActorRole(actorRole);
        preparation.setTemplateVersion(template == null ? null : template.getVersion());
        preparation.setReason("Submitted for review");
        preparation.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(preparation);

        certificate.setStatus(Certificate.CertificateStatus.PENDING_APPROVAL);
        certificateRepository.save(certificate);

        auditService.recordAuditLog(institutionId, actorUserId, actorEmail, actorRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", "DRAFT"),
                Map.of("status", "PENDING_APPROVAL", "stage", "PREPARATION"));

        return certificate;
    }

    // ── decisions ─────────────────────────────────────────────────────────────────────────

    /** Records an approval or a rejection for one stage. */
    @Transactional
    public CertificateApproval decide(UUID certificateId, ApprovalStage stage, boolean approve,
                                      UUID institutionId, UUID actorUserId, String actorEmail,
                                      String actorRole, String reason) {

        Certificate certificate = requireCertificate(certificateId, institutionId);
        CertificateTemplate template = certificate.getTemplateId() == null ? null
                : templateRepository.findByIdAndIsDeletedFalse(certificate.getTemplateId()).orElse(null);
        TemplateFamily family = template == null ? TemplateFamily.FAMILY_TRAINING : template.getTemplateFamily();

        if (!policy.canDecide(stage, actorRole)) {
            throw new ForbiddenException("approval stage " + stage, "decide");
        }
        if (!policy.requiredStages(family).contains(stage)) {
            throw new IllegalArgumentException(
                    "This certificate's policy does not require a " + stage + " stage");
        }

        // Separation of duties. Self-approval is the failure mode an approval step exists to
        // prevent, and for an academic award it is never permitted.
        boolean sameActorAsPreparer = isPreparer(certificateId, actorUserId);
        if (approve && sameActorAsPreparer && !policy.allowsSelfApproval(family)) {
            throw new ForbiddenException("certificate prepared by this user", "self-approve");
        }

        if (!approve && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A rejection needs a reason explaining what must change");
        }

        // An approval that already exists for this stage is superseded rather than overwritten, so
        // the decision history stays truthful.
        Optional<CertificateApproval> current = approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(certificateId, stage,
                        List.of(DecisionStatus.PENDING, DecisionStatus.APPROVED));

        CertificateApproval decision;
        if (current.isPresent()) {
            CertificateApproval existing = current.get();
            existing.setDecisionStatus(approve ? DecisionStatus.APPROVED : DecisionStatus.REJECTED);
            existing.setActorUserId(actorUserId);
            existing.setActorEmail(actorEmail);
            existing.setActorRole(actorRole);
            existing.setReason(reason);
            existing.setDecidedAt(LocalDateTime.now());
            existing.setTemplateVersion(template == null ? null : template.getVersion());
            decision = approvalRepository.save(existing);
        } else {
            CertificateApproval approval = new CertificateApproval();
            approval.setCertificateId(certificateId);
            approval.setInstitutionId(institutionId);
            approval.setStage(stage);
            approval.setDecisionStatus(approve ? DecisionStatus.APPROVED : DecisionStatus.REJECTED);
            approval.setActorUserId(actorUserId);
            approval.setActorEmail(actorEmail);
            approval.setActorRole(actorRole);
            approval.setReason(reason);
            approval.setTemplateVersion(template == null ? null : template.getVersion());
            approval.setDecidedAt(LocalDateTime.now());
            decision = approvalRepository.save(approval);
        }

        refreshLifecycle(certificate, template);

        auditService.recordAuditLog(institutionId, actorUserId, actorEmail, actorRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("stage", stage.name()),
                Map.of("stage", stage.name(),
                        "decision", approve ? "APPROVED" : "REJECTED",
                        "reason", reason == null ? "" : reason));

        // The decision just recorded is returned rather than re-queried: after a rejection there is
        // by definition no PENDING or APPROVED row left for this stage, so a lookup filtered to
        // those statuses would fail to find the decision that was just made.
        return decision;
    }

    /**
     * Withdraws a previously granted approval, returning the certificate to review.
     *
     * <p>Needed because a material change after approval must not ride along on an old sign-off.</p>
     */
    @Transactional
    public Certificate withdrawApproval(UUID certificateId, ApprovalStage stage, UUID institutionId,
                                        UUID actorUserId, String actorEmail, String actorRole,
                                        String reason) {
        Certificate certificate = requireCertificate(certificateId, institutionId);
        if (certificate.getStatus() == Certificate.CertificateStatus.ISSUED
                || certificate.getStatus() == Certificate.CertificateStatus.REVOKED) {
            throw new IllegalStateException("An issued certificate's approval history is immutable; "
                    + "revoke it instead of withdrawing an approval");
        }

        CertificateApproval approval = approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(certificateId, stage,
                        List.of(DecisionStatus.PENDING, DecisionStatus.APPROVED))
                .orElseThrow(() -> new ResourceNotFoundException("CertificateApproval", "stage", stage.name()));

        approval.setDecisionStatus(DecisionStatus.WITHDRAWN);
        approval.setActorUserId(actorUserId);
        approval.setActorEmail(actorEmail);
        approval.setActorRole(actorRole);
        approval.setReason(reason);
        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        CertificateTemplate template = certificate.getTemplateId() == null ? null
                : templateRepository.findByIdAndIsDeletedFalse(certificate.getTemplateId()).orElse(null);
        refreshLifecycle(certificate, template);

        auditService.recordAuditLog(institutionId, actorUserId, actorEmail, actorRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("stage", stage.name(), "decision", "APPROVED"),
                Map.of("stage", stage.name(), "decision", "WITHDRAWN", "reason",
                        reason == null ? "" : reason));

        return certificate;
    }

    // ── issuance gate ─────────────────────────────────────────────────────────────────────

    /**
     * Refuses issuance unless every required stage is approved and current.
     *
     * <p>Called from the issuance service. Also checks that the approval was given against the
     * current template version, so a template edited after approval cannot be issued on the
     * strength of a sign-off nobody gave for that design.</p>
     */
    @Transactional(readOnly = true)
    public void assertIssuable(Certificate certificate, CertificateTemplate template) {
        if (certificate.getStatus() == Certificate.CertificateStatus.ISSUED) {
            throw new IllegalStateException("This certificate has already been issued");
        }
        if (certificate.getStatus() == Certificate.CertificateStatus.REVOKED) {
            throw new IllegalStateException("A revoked certificate cannot be issued");
        }

        Set<ApprovalStage> required = policy.requiredStages(template);
        if (required.isEmpty()) {
            return;
        }

        List<String> problems = new ArrayList<>();
        for (ApprovalStage stage : policy.orderedStages()) {
            if (!required.contains(stage)) {
                continue;
            }
            Optional<CertificateApproval> decision = approvalRepository
                    .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
                            certificate.getId(), stage, List.of(DecisionStatus.APPROVED));
            if (decision.isEmpty()) {
                problems.add(stage + " approval is missing");
                continue;
            }
            Integer approvedVersion = decision.get().getTemplateVersion();
            Integer currentVersion = template == null ? null : template.getVersion();
            if (approvedVersion != null && currentVersion != null
                    && !approvedVersion.equals(currentVersion)) {
                problems.add(stage + " approval was given against template version " + approvedVersion
                        + " but the template is now version " + currentVersion);
            }
        }

        boolean rejected = policy.orderedStages().stream()
                .filter(required::contains)
                .anyMatch(stage -> approvalRepository
                        .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
                                certificate.getId(), stage, List.of(DecisionStatus.REJECTED))
                        .isPresent());
        if (rejected) {
            problems.add("a required stage was rejected");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "This certificate is not approved for issuance: " + String.join("; ", problems));
        }
    }

    /** Stages still outstanding, for the UI and for the approval queue. */
    @Transactional(readOnly = true)
    public List<ApprovalStage> outstandingStages(Certificate certificate, CertificateTemplate template) {
        Set<ApprovalStage> required = policy.requiredStages(template);
        List<ApprovalStage> outstanding = new ArrayList<>();
        for (ApprovalStage stage : policy.orderedStages()) {
            if (!required.contains(stage)) {
                continue;
            }
            boolean approved = approvalRepository
                    .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
                            certificate.getId(), stage, List.of(DecisionStatus.APPROVED))
                    .isPresent();
            if (!approved) {
                outstanding.add(stage);
            }
        }
        return outstanding;
    }

    @Transactional(readOnly = true)
    public List<CertificateApproval> history(UUID certificateId) {
        return approvalRepository.findByCertificateIdAndIsDeletedFalseOrderByCreatedAtAsc(certificateId);
    }

    /**
     * Recomputes the lifecycle status from the recorded decisions.
     *
     * <p>Derived rather than set by each endpoint, so the status can never drift from the approval
     * rows that justify it.</p>
     */
    private void refreshLifecycle(Certificate certificate, CertificateTemplate template) {
        Set<ApprovalStage> required = policy.requiredStages(template);
        List<CertificateApproval> history =
                approvalRepository.findByCertificateIdAndIsDeletedFalseOrderByCreatedAtAsc(certificate.getId());

        boolean anyRejected = history.stream()
                .anyMatch(a -> a.getDecisionStatus() == DecisionStatus.REJECTED
                        && required.contains(a.getStage()));
        boolean allApproved = required.stream().allMatch(stage -> history.stream()
                .anyMatch(a -> a.getStage() == stage && a.getDecisionStatus() == DecisionStatus.APPROVED));

        if (certificate.getStatus() == Certificate.CertificateStatus.ISSUED
                || certificate.getStatus() == Certificate.CertificateStatus.REVOKED) {
            return; // terminal states are never recomputed
        }
        if (anyRejected) {
            certificate.setStatus(Certificate.CertificateStatus.DRAFT);
        } else if (allApproved) {
            certificate.setStatus(policy.statusWhenApproved());
        } else {
            certificate.setStatus(Certificate.CertificateStatus.PENDING_APPROVAL);
        }
        certificateRepository.save(certificate);
    }

    private boolean isPreparer(UUID certificateId, UUID actorUserId) {
        if (actorUserId == null) {
            return false;
        }
        return approvalRepository
                .findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(certificateId,
                        ApprovalStage.PREPARATION, List.of(DecisionStatus.APPROVED))
                .map(a -> actorUserId.equals(a.getActorUserId()))
                .orElse(false);
    }

    private Certificate requireCertificate(UUID certificateId, UUID institutionId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));
        if (certificate.getInstitutionId() == null
                || !certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "approve");
        }
        return certificate;
    }

    /** Exposed so the UI can render the policy it is being held to. */
    public Set<ApprovalStage> requiredStages(TemplateFamily family) {
        return EnumSet.copyOf(policy.requiredStages(family));
    }

    @Transactional(readOnly = true)
    public List<CertificateApproval> pendingForInstitution(UUID institutionId) {
        return approvalRepository.findByInstitutionIdAndIsDeletedFalseAndDecisionStatusOrderByCreatedAtAsc(
                institutionId, DecisionStatus.PENDING);
    }

    /** Dates are used by the queue view; kept here so the controller does not import java.time. */
    public LocalDate today() {
        return LocalDate.now();
    }
}
