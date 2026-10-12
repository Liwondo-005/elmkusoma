package tz.elmkusoma.certificate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateApproval.ApprovalStage;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.CertificateTemplate.TemplateFamily;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * How many approval stages a certificate actually needs.
 *
 * <p>The audit found no approval concept at all. The temptation is to add four approvals to every
 * certificate so the system looks rigorous. That is worse than useless: it makes issuance slow for
 * a 20-minute attendance certificate, and people learn to rubber-stamp. So the requirement is
 * expressed as policy and defaults conservatively for the credential that carries the most weight -
 * a formal academic award - while staying proportionate for a training certificate.</p>
 *
 * <p>Both families use the same approval engine; only the required stage set differs. There is no
 * second lifecycle state machine to contradict this one.</p>
 */
@Component
@Slf4j
public class CertificateApprovalPolicy {

    /**
     * Overridable so a deployment can tighten a family without a code change. Parsed defensively:
     * a malformed value falls back to the safe default rather than silently weakening control.
     */
    private final Set<ApprovalStage> trainingStages;

    private final Set<ApprovalStage> academicStages;

    private final boolean allowSelfApproval;

    public CertificateApprovalPolicy(
            @Value("${certificate.approval.training-stages:PREPARATION,APPROVAL}") String trainingStages,
            @Value("${certificate.approval.academic-stages:PREPARATION,VALIDATION,APPROVAL}") String academicStages,
            @Value("${certificate.approval.allow-self-approval:false}") boolean allowSelfApproval) {

        this.trainingStages = parse(trainingStages, EnumSet.of(ApprovalStage.PREPARATION, ApprovalStage.APPROVAL));
        this.academicStages = parse(academicStages,
                EnumSet.of(ApprovalStage.PREPARATION, ApprovalStage.VALIDATION, ApprovalStage.APPROVAL));
        this.allowSelfApproval = allowSelfApproval;

        log.info("Certificate approval policy: training={}, academic={}, selfApprovalAllowed={}",
                this.trainingStages, this.academicStages, this.allowSelfApproval);
    }

    private static Set<ApprovalStage> parse(String raw, Set<ApprovalStage> fallback) {
        try {
            EnumSet<ApprovalStage> parsed = EnumSet.noneOf(ApprovalStage.class);
            for (String part : raw.split(",")) {
                String name = part.trim().toUpperCase(java.util.Locale.ROOT);
                if (!name.isEmpty()) {
                    parsed.add(ApprovalStage.valueOf(name));
                }
            }
            return parsed.isEmpty() ? fallback : parsed;
        } catch (IllegalArgumentException e) {
            log.warn("Unparseable certificate approval stages '{}'; using {}", raw, fallback);
            return fallback;
        }
    }

    /** The stages this certificate must satisfy before it may be issued. */
    public Set<ApprovalStage> requiredStages(CertificateTemplate.TemplateFamily family) {
        return TemplateFamily.FAMILY_ACADEMIC.equals(family) ? academicStages : trainingStages;
    }

    /**
     * Stages required for a certificate, given its template.
     *
     * <p>A certificate with no template - the automatic participation paths create them that way -
     * falls back to the training family, which is the lighter of the two. It is the conservative
     * choice here only because those paths carry their own eligibility evidence; it is not a claim
     * that an unreviewed document is academic.</p>
     */
    public Set<ApprovalStage> requiredStages(CertificateTemplate template) {
        TemplateFamily family = template == null ? TemplateFamily.FAMILY_TRAINING : template.getTemplateFamily();
        return requiredStages(family);
    }

    public Set<ApprovalStage> requiredStages(Certificate certificate, CertificateTemplate template) {
        return requiredStages(template);
    }

    /**
     * Whether the person who prepared the certificate may also approve it.
     *
     * <p>Off by default for academic certificates, which is the case where the segregation of
     * duties is the point rather than a formality.</p>
     */
    public boolean allowsSelfApproval(TemplateFamily family) {
        if (TemplateFamily.FAMILY_ACADEMIC.equals(family)) {
            return false;
        }
        return allowSelfApproval;
    }

    /** The lifecycle state a certificate should hold once its required stages are all approved. */
    public Certificate.CertificateStatus statusWhenApproved() {
        return Certificate.CertificateStatus.APPROVED;
    }

    /** Stages a user with the given role is permitted to decide. */
    public boolean canDecide(ApprovalStage stage, String role) {
        boolean admin = "ADMIN".equals(role) || "INSTITUTION_ADMIN".equals(role);
        boolean teacher = "TEACHER".equals(role);
        return switch (stage) {
            case PREPARATION -> admin || teacher;
            // Evidence checking is an academic function; a teacher cannot self-validate an award.
            case VALIDATION -> admin;
            case APPROVAL -> admin;
        };
    }

    public List<ApprovalStage> orderedStages() {
        return List.of(ApprovalStage.PREPARATION, ApprovalStage.VALIDATION, ApprovalStage.APPROVAL);
    }
}
