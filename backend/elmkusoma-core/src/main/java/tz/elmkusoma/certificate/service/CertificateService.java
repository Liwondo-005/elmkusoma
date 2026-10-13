package tz.elmkusoma.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.Certificate.CertificateStatus;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.CertificateSignatory;
import tz.elmkusoma.certificate.domain.CertificateSignatorySnapshot;
import tz.elmkusoma.certificate.domain.CertificateTemplateSignatory;
import tz.elmkusoma.certificate.domain.Transcript;
import tz.elmkusoma.certificate.domain.TranscriptEntry;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.*;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.audit.service.PlatformAuditScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CertificateService {

    private final CertificateTemplateRepository templateRepository;
    private final CertificateSignatoryRepository signatoryRepository;
    private final CertificateTemplateSignatoryRepository templateSignatoryRepository;
    private final CertificateSignatorySnapshotRepository signatorySnapshotRepository;
    private final CertificateApprovalService approvalService;
    private final CertificateRepository certificateRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptEntryRepository transcriptEntryRepository;
    private final CertificateMapper certificateMapper;
    private final AuditService auditService;
    private final StudentRepository studentRepository;
    private final tz.elmkusoma.shared.repository.UserRepository userRepository;
    private final LearnerNotificationRepository learnerNotificationRepository;
    private final tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tz.elmkusoma.administration.service.PlatformPolicyService platformPolicyService;

    /**
     * Public base URL of the web app, used to build the verification link printed on a
     * certificate. The same property already exists for auth emails; the certificate module simply
     * was not reading it, which is why every verification URL was a bare relative path.
     */
    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:3000}")
    private String publicFrontendUrl;

    private static final ConcurrentHashMap<String, VerifyWindow> verifyWindows = new ConcurrentHashMap<>();
    private static final int MAX_VERIFY_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute

    /**
     * Sliding one-minute window per rate-limit key. The window is recreated when it
     * expires, so verification is throttled per minute instead of once per process
     * lifetime (the previous implementation never reset its counter).
     */
    private static final class VerifyWindow {
        private final long startedAt = System.currentTimeMillis();
        private final AtomicInteger attempts = new AtomicInteger();
    }

    /**
     * Absolute verification URL, or the relative route when no public base URL is configured.
     *
     * <p>Null-safe on purpose: a missing configuration value must degrade to the old relative
     * behaviour rather than aborting the issuance of a certificate that is otherwise valid.</p>
     */
    private String buildVerificationUrl(String verificationCode) {
        String path = "/certificates/verify/" + verificationCode;
        if (publicFrontendUrl == null || publicFrontendUrl.isBlank()) {
            return path;
        }
        return publicFrontendUrl.replaceAll("/+$", "") + path;
    }

    /**
     * The student's own name, from the user account they sign in with.
     *
     * <p>Returns null rather than a placeholder. A certificate that says "Student" where a name
     * should be is worse than a refusal, so the caller treats null as a blocking condition.</p>
     */
    private String resolveStudentName(Student student) {
        if (student.getUserId() == null) {
            return null;
        }
        return userRepository.findByIdAndIsDeletedFalse(student.getUserId())
                .map(user -> {
                    String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
                    String last = user.getLastName() == null ? "" : user.getLastName().trim();
                    String combined = (first + " " + last).trim();
                    return combined.isEmpty() ? null : combined;
                })
                .orElse(null);
    }

    // ── Template Management ──
    public TemplateResponse createTemplate(CreateTemplateRequest request, UUID institutionId,
                                            String userEmail, String userRole) {
        if (templateRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(request.getName(), institutionId)) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
        }

        CertificateTemplate template = certificateMapper.toTemplateEntity(request, institutionId);
        templateRepository.save(template);

        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "CertificateTemplate", template.getId(), template.getName(),
                AuditLog.AuditAction.CREATE, null, Map.of("name", template.getName(), "type", template.getTemplateType().name()));

        log.info("Created certificate template: {} for institution: {}", template.getName(), institutionId);
        return certificateMapper.toTemplateResponse(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplates(UUID institutionId) {
        return templateRepository.findAllByInstitutionId(institutionId).stream()
                .map(certificateMapper::toTemplateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID templateId, UUID institutionId) {
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));

        if (!template.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("template", "access");
        }

        return certificateMapper.toTemplateResponse(template);
    }

    // ── Certificate Generation ──

    public CertificateResponse generateCertificate(GenerateCertificateRequest request, UUID institutionId,
                                                    UUID issuedBy, String userEmail, String userRole) {
        if (platformPolicyService != null && !platformPolicyService.certificateIssueEnabled()) {
            throw new IllegalStateException("Platform policy forbids issuing certificates");
        }
        // Validate template exists
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", request.getTemplateId()));

        if (!template.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("template", "use");
        }

        // A deactivated template must stop producing certificates. Platform Admins deactivate a
        // template to withdraw it - after a policy change, a brand dispute, or because it was
        // wrong - and that is meaningless while issuance ignores the flag.
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new IllegalStateException("This template has been deactivated and cannot be used");
        }

        // Generate serial number
        String serialNumber = generateSerialNumber(institutionId, request.getCertificateType());

        // Generate verification code
        String verificationCode = generateVerificationCode();

        // Absolute verification URL, built from the configured public frontend base URL. It was a
        // bare relative path, which cannot be printed onto a certificate, embedded in a QR code or
        // emailed - the three things it exists for.
        String verificationUrl = buildVerificationUrl(verificationCode);

        // Resolve the recipient from the authoritative student record.
        //
        // Previously the id was resolved but, on a miss, the raw client value was carried straight
        // through - so a caller could supply any id at all. The record must exist, must belong to
        // this institution, and its name is what gets printed.
        UUID requestedId = request.getStudentId();
        if (requestedId == null) {
            throw new IllegalArgumentException("A student must be selected for a certificate");
        }
        Student student = studentRepository.findByIdAndIsDeletedFalse(requestedId)
                .or(() -> studentRepository.findByUserIdAndIsDeletedFalse(requestedId))
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", requestedId));

        // Tenant check on the student. A certificate is an institutional record about one of its
        // own students; issuing one for a student from another school is a cross-tenant write.
        if (student.getInstitutionId() != null
                && !student.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("student", "certify");
        }
        UUID studentId = student.getId();

        // Name comes from the record, not the request. The recipient's name on a certificate is
        // the one field a visitor relies on to prove the document is theirs; it must not be
        // whatever the operator typed.
        String studentName = resolveStudentName(student);
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException(
                    "The selected student has no recorded name, so a certificate cannot be issued for them");
        }

        Certificate certificate = Certificate.builder()
                .templateId(request.getTemplateId())
                .studentId(studentId)
                .issuedBy(issuedBy)
                .serialNumber(serialNumber)
                .certificateType(request.getCertificateType())
                .title(request.getTitle())
                .description(request.getDescription())
                .studentName(studentName)
                // An id number is identifying data and is not read from the request. The
                // institution's own admission reference is what a certificate would print.
                .studentIdNumber(student.getAdmissionNumber())
                .courseOrProgramme(request.getCourseOrProgramme())
                .instructorName(request.getInstructorName())
                .grade(request.getGrade())
                .skills(request.getSkills())
                .completionDate(request.getCompletionDate())
                .issueDate(LocalDateTime.now())
                .expiryDate(request.getExpiryDate())
                .status(CertificateStatus.DRAFT)
                .verificationCode(verificationCode)
                .verificationUrl(verificationUrl)
                .build();
        certificate.setInstitutionId(institutionId);

        certificateRepository.save(certificate);

        auditService.recordAuditLog(institutionId, issuedBy, userEmail, userRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("serialNumber", serialNumber, "type", request.getCertificateType().name(),
                        "studentName", studentName, "title", request.getTitle()));

        log.info("Generated certificate: {} for student: {} in institution: {}",
                serialNumber, request.getStudentId(), institutionId);
        return certificateMapper.toCertificateResponse(certificate);
    }

    public CertificateResponse issueCertificate(UUID certificateId, UUID institutionId,
                                                  UUID actorUserId, String userEmail, String userRole) {
        if (platformPolicyService != null && !platformPolicyService.certificateIssueEnabled()) {
            throw new IllegalStateException("Platform policy forbids issuing certificates");
        }
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "issue");
        }

        if (certificate.getStatus() != CertificateStatus.DRAFT
                && certificate.getStatus() != CertificateStatus.PENDING_APPROVAL
                && certificate.getStatus() != CertificateStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only a certificate in review can be issued (this one is " + certificate.getStatus() + ")");
        }

        // Approval gate, enforced server-side. Before V161 a certificate went straight from DRAFT
        // to ISSUED on an admin click, so "approved" never meant anything.
        CertificateTemplate issueTemplate = requireIssuableTemplate(certificate);
        approvalService.assertIssuable(certificate, issueTemplate);

        // Re-check every linked signatory at the moment of issuance.
        //
        // Signatory authorization was only ever enforced when a signatory was LINKED to a
        // template. Nothing re-checked it when the certificate was actually issued, so a
        // signatory deactivated, expired, or moved to another institution afterwards still had
        // their name and signature printed. Authorization has to hold at the point of use, not
        // only at the point of setup.
        requireAuthorizedSignatories(certificate);

        // Refuse a second live certificate for an award the student already holds.
        //
        // Without this, an operator could issue the same certificate any number of times, and each
        // issuance produced a distinct serial number, a distinct verification code and its own QR
        // code. A holder with three certificates for one course cannot show which is genuine, and
        // a verifier checking one of them has no way to know it is not the withdrawn copy.
        //
        // Revoked and soft-deleted certificates are excluded deliberately: if an award was withdrawn,
        // re-issuing it is the remedy, and the record of the mistake should not block the fix.
        requireNotAlreadyAwarded(certificate);

        // Freeze who signed it. The certificate referenced the live signatory row, so editing a
        // signatory's name, title or signature image today would silently rewrite the appearance
        // of documents issued years ago.
        captureSignatorySnapshots(certificate, issueTemplate);

        // Point every certificate this issuance replaced at the new one, while both are still
        // inside the same transaction as the issuance itself. Recording it after the commit would
        // leave a window in which the replacement exists but nothing says so.
        markSupersededPredecessors(certificate, institutionId, actorUserId, userEmail, userRole);

        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setIssueDate(LocalDateTime.now());
        certificateRepository.save(certificate);

        try {
            LearnerNotification certNotification = LearnerNotification.builder()
                    .userId(certificate.getStudentId())
                    .title("Certificate Issued!")
                    .message("Your certificate \"" + certificate.getTitle() + "\" has been issued. Verification code: " + certificate.getVerificationCode())
                    .notificationType("CERTIFICATE")
                    .targetType("certificate")
                    .targetId(certificate.getId())
                    .build();
            learnerNotificationRepository.save(certNotification);
        } catch (Exception e) {
            log.warn("Failed to create certificate issuance notification for student {}: {}", certificate.getStudentId(), e.getMessage());
        }

        // The actor is recorded. It used to be null, which meant the audit trail showed that a
        // certificate had been issued or revoked but not by whom - the one fact anyone reading the
        // trail is looking for.
        auditService.recordAuditLog(institutionId, actorUserId, userEmail, userRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", "DRAFT"), Map.of("status", "ISSUED"));

        log.info("Issued certificate: {}", certificate.getSerialNumber());
        return certificateMapper.toCertificateResponse(certificate);
    }

    /**
     * Copies the approved signatories onto the certificate at issuance.
     *
     * <p>Historical documents must not change because a profile was edited later. If a signatory
     * row is deleted after issuance, the snapshot still carries the name, title and signature
     * asset the document was actually rendered with.</p>
     */
    private void captureSignatorySnapshots(Certificate certificate, CertificateTemplate template) {
        if (template == null) {
            return;
        }
        List<CertificateTemplateSignatory> links =
                templateSignatoryRepository.findByTemplateId(certificate.getTemplateId());
        if (links == null || links.isEmpty()) {
            return;
        }
        for (CertificateTemplateSignatory link : links) {
            if (Boolean.TRUE.equals(link.getIsDeleted())) {
                continue;
            }
            CertificateSignatory signatory = signatoryRepository
                    .findByIdAndIsDeletedFalse(link.getSignatoryId()).orElse(null);
            if (signatory == null) {
                continue;
            }
            CertificateSignatorySnapshot snapshot = new CertificateSignatorySnapshot();
            snapshot.setCertificateId(certificate.getId());
            snapshot.setInstitutionId(certificate.getInstitutionId());
            snapshot.setSignatoryId(signatory.getId());
            snapshot.setDisplayOrder(link.getDisplayOrder() == null ? 0 : link.getDisplayOrder());
            snapshot.setFullName(signatory.getFullName());
            snapshot.setPositionTitle(signatory.getPositionTitle());
            snapshot.setOrganization(signatory.getOrganization());
            snapshot.setSignatureImage(signatory.getSignatureImage());
            snapshot.setTemplateVersion(template.getVersion());
            signatorySnapshotRepository.save(snapshot);
        }
    }

    /**
     * Refuses to issue when a signatory linked to the template is no longer authorized.
     *
     * <p>Checks, at issuance: the link still exists and is not soft-deleted; the signatory is
     * ACTIVE; the signatory belongs to the issuing institution; the signatory's validity window
     * covers today; and the signatory's allowed certificate types include this one.</p>
     *
     * <p>A template with no signatories is allowed. Many institutions issue unbranded
     * certificates and the absence of a signature is a legitimate configuration, not a defect -
     * inventing a "missing signatory" failure would block certificates that are working today.</p>
     */
    /**
     * The template this certificate will be rendered from, validated at the moment of issuance.
     *
     * <p>Generation checks the template is active and compatible, but issuance did not. A
     * certificate drafted against a template could sit in review for weeks while that template was
     * deactivated, deleted, or had its type changed, and then be issued against it anyway. A
     * deactivated template is usually withdrawn precisely because the design turned out to be
     * wrong, so issuing from it produces documents the institution has already disowned.</p>
     *
     * <p>Returning null for a certificate with no template is deliberate and unchanged: the
     * automatic participation-certificate paths create certificates without a template, and a
     * missing template there is a legitimate configuration rather than a defect. Those paths are
     * not rendered from a template at all, so template validation does not apply to them.</p>
     */
    private CertificateTemplate requireIssuableTemplate(Certificate certificate) {
        if (certificate.getTemplateId() == null) {
            return null;
        }
        CertificateTemplate template = templateRepository
                .findByIdAndIsDeletedFalse(certificate.getTemplateId())
                .orElseThrow(() -> new IllegalStateException(
                        "The template this certificate was drafted against no longer exists, so it "
                                + "cannot be issued. Re-issue it against a current template."));

        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new IllegalStateException(
                    "The template for this certificate has been deactivated and cannot be issued from");
        }

        if (template.getInstitutionId() != null
                && !template.getInstitutionId().equals(certificate.getInstitutionId())) {
            throw new ForbiddenException("template", "issue");
        }

        // A template typed for one kind of award must not render another. Without this, an
        // achievement template could be used to produce what reads as a transcript certificate.
        // Compared by name because TemplateType and CertificateType are distinct enum types that
        // happen to share their vocabulary; a TRANSCRIPT-labelled template rendering an
        // ACHIEVEMENT certificate is exactly the substitution to prevent.
        if (template.getTemplateType() != null && certificate.getCertificateType() != null
                && !template.getTemplateType().name().equals(certificate.getCertificateType().name())) {
            throw new IllegalStateException(
                    "This template produces " + template.getTemplateType()
                            + " certificates but the certificate is " + certificate.getCertificateType());
        }

        return template;
    }

    /**
     * Refuses issuance when the student already holds a live certificate for this same award.
     *
     * <p>"Same award" means the same student, template, certificate type and title. Anything less
     * specific would block legitimate distinct certificates: a student may hold several
     * participation certificates for different sessions, or a completion certificate per course.</p>
     *
     * <p>A certificate with no template is skipped, because template identity is what distinguishes
     * one award from another here. Those paths are the automatic participation-certificate routes,
     * which already de-duplicate against the session title before creating anything.</p>
     */
    private void requireNotAlreadyAwarded(Certificate certificate) {
        if (certificate.getTemplateId() == null || certificate.getTitle() == null
                || certificate.getTitle().isBlank()) {
            return;
        }
        certificateRepository.findLiveAward(
                        certificate.getStudentId(),
                        certificate.getTemplateId(),
                        certificate.getCertificateType(),
                        certificate.getTitle())
                .filter(existing -> !existing.getId().equals(certificate.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "This student already holds a live certificate for this award "
                                    + "(serial " + existing.getSerialNumber()
                                    + "). Revoke it first if it must be replaced.");
                });
    }

    /**
     * Records that this issuance replaced earlier certificates for the same award.
     *
     * <p>{@link #requireNotAlreadyAwarded} is what makes a re-issue possible at all: it refuses a
     * second copy of an award while a live one exists, and tells the operator to revoke first.
     * That step - revoke, then re-issue - is the whole remedy, and until now nothing recorded
     * that the two rows were the same award at different moments.</p>
     *
     * <p>It matters from outside. A holder of the revoked copy scans its QR, which this codebase
     * now stamps on every certificate, and is told "revoked" - true, but it does not tell them a
     * valid replacement exists or which one. {@link #verifyCertificate} answers that using the
     * pointer written here.</p>
     *
     * <p>Runs only at the moment of issuance. Pointing at a certificate still in PENDING_APPROVAL
     * would mark the holder's existing document as replaced by something that may never be
     * issued.</p>
     *
     * <p>The award scope is deliberately the same key the duplicate guard uses. A predecessor is
     * only a predecessor of the same award.</p>
     */
    private void markSupersededPredecessors(Certificate certificate, UUID institutionId,
                                            UUID actorUserId, String userEmail, String userRole) {
        if (certificate.getTemplateId() == null || certificate.getTitle() == null
                || certificate.getTitle().isBlank()) {
            // The automatic participation routes create certificates with no template. They
            // de-duplicate against the session title instead, and there is no template identity
            // to match a predecessor on - the same reason requireNotAlreadyAwarded skips them.
            return;
        }

        List<Certificate> predecessors = certificateRepository.findPriorAwards(
                certificate.getStudentId(),
                certificate.getTemplateId(),
                certificate.getCertificateType(),
                certificate.getTitle(),
                certificate.getId());

        if (predecessors.isEmpty()) {
            return;
        }

        LocalDateTime supersededAt = LocalDateTime.now();
        for (Certificate prior : predecessors) {
            String previousStatus = prior.getStatus().name();
            prior.setSupersededByCertificateId(certificate.getId());
            prior.setSupersededAt(supersededAt);
            certificateRepository.save(prior);

            try {
                auditService.recordAuditLog(institutionId, actorUserId, userEmail, userRole,
                        "Certificate", prior.getId(), prior.getSerialNumber(),
                        AuditLog.AuditAction.UPDATE,
                        Map.of("status", previousStatus),
                        Map.of("status", previousStatus,
                                "supersededBy", certificate.getSerialNumber()));
            } catch (Exception e) {
                // The link is written; only the trail of who wrote it failed. Losing the audit
                // entry must not roll back an issuance that has otherwise succeeded.
                log.warn("Failed to audit supersession of certificate {}: {}",
                        prior.getSerialNumber(), e.getMessage());
            }
        }
    }

    private void requireAuthorizedSignatories(Certificate certificate) {
        List<CertificateTemplateSignatory> links =
                templateSignatoryRepository.findByTemplateId(certificate.getTemplateId());
        if (links == null || links.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (CertificateTemplateSignatory link : links) {
            if (Boolean.TRUE.equals(link.getIsDeleted())) {
                continue;
            }
            CertificateSignatory signatory = signatoryRepository
                    .findByIdAndIsDeletedFalse(link.getSignatoryId())
                    .orElse(null);
            if (signatory == null) {
                continue; // link to a deleted signatory: nothing can be printed, so nothing is
            }
            if (signatory.getStatus() != CertificateSignatory.SignatoryStatus.ACTIVE) {
                throw new IllegalStateException("A signatory on this template is no longer active: "
                        + signatory.getFullName());
            }
            if (signatory.getInstitutionId() != null && certificate.getInstitutionId() != null
                    && !signatory.getInstitutionId().equals(certificate.getInstitutionId())) {
                throw new ForbiddenException("signatory", "use");
            }
            if (signatory.getValidFrom() != null && signatory.getValidFrom().isAfter(today)) {
                throw new IllegalStateException("A signatory on this template is not yet valid: "
                        + signatory.getFullName());
            }
            if (signatory.getValidUntil() != null && signatory.getValidUntil().isBefore(today)) {
                throw new IllegalStateException("A signatory on this template has expired: "
                        + signatory.getFullName());
            }
            if (!isTypeAllowed(signatory, certificate.getCertificateType())) {
                throw new ForbiddenException("signatory", "use for this certificate type");
            }
        }
    }

    /** NULL or blank means "all types", which is how the governance screen expresses it. */
    private boolean isTypeAllowed(CertificateSignatory signatory, CertificateType type) {
        String allowed = signatory.getCertificateTypes();
        if (allowed == null || allowed.isBlank()) {
            return true;
        }
        for (String candidate : allowed.split(",")) {
            if (candidate.trim().equalsIgnoreCase(type.name())) {
                return true;
            }
        }
        return false;
    }

    public CertificateResponse revokeCertificate(UUID certificateId, UUID institutionId,
                                                    RevokeCertificateRequest request,
                                                    UUID actorUserId, String userEmail, String userRole) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "revoke");
        }

        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new IllegalStateException("Certificate is already revoked");
        }

        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokedReason(request.getReason());
        certificate.setRevokedAt(LocalDateTime.now());
        certificateRepository.save(certificate);

        auditService.recordAuditLog(institutionId, actorUserId, userEmail, userRole,
                "Certificate", certificate.getId(), certificate.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", "ISSUED"), Map.of("status", "REVOKED", "reason", request.getReason()));

        auditService.recordSecurityEvent(institutionId, actorUserId, userEmail,
                SecurityEvent.SecurityEventType.CERTIFICATE_REVOKED,
                "Certificate " + certificate.getSerialNumber() + " revoked: " + request.getReason(),
                SecurityEvent.Severity.MEDIUM, null, null);

        log.info("Revoked certificate: {} Reason: {}", certificate.getSerialNumber(), request.getReason());
        return certificateMapper.toCertificateResponse(certificate);
    }

    // ── Certificate Verification ──

    /**
     * Public verification. Reachable without authentication, so it is treated as the highest-risk
     * surface in the certificate module.
     *
     * <p>Three defects this replaces, all found by audit:</p>
     * <ul>
     *   <li>The rate-limit bucket was keyed on the constant {@code "verify"}, so all callers shared
     *       one 10-per-minute budget. A single third party could exhaust it and deny verification
     *       to every legitimate visitor. Now keyed per caller.</li>
     *   <li>The response carried the row's internal UUID, the student's grade, the skills list and
     *       the instructor's name, and echoed the free-text revocation reason to an anonymous
     *       caller. A revocation reason can contain disciplinary detail that is nobody's business.
     *       All of that is gone; see {@link #toPublicVerification}.</li>
     *   <li>A DRAFT certificate that had not expired fell through to the success branch and
     *       returned {@code valid=false} alongside the message "Certificate is valid and
     *       verified". The status and the message now always agree.</li>
     * </ul>
     */
    public CertificateVerificationResponse verifyCertificate(String verificationCode, String clientKey) {
        // Per-caller sliding window. The key is supplied by the controller from the request's
        // remote address; unknown callers share one bucket rather than getting a free pass.
        String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
        VerifyWindow window = verifyWindows.compute(key, (k, w) ->
                (w == null || System.currentTimeMillis() - w.startedAt >= WINDOW_MS) ? new VerifyWindow() : w);
        if (window.attempts.get() >= MAX_VERIFY_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts");
        }
        window.attempts.incrementAndGet();

        Certificate certificate = certificateRepository.findByVerificationCodeAndIsDeletedFalse(verificationCode)
                .orElse(null);

        if (certificate == null) {
            // Failed lookups are audited too. Returning early without a record left brute-force
            // guessing completely invisible, which is exactly when the trail matters.
            auditVerification(null, null, "NOT_FOUND", false);
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .status("NOT_FOUND")
                    .message("Certificate not found")
                    .build();
        }

        String institutionName = certificate.getInstitutionId() == null ? null
                : institutionRepository.findById(certificate.getInstitutionId())
                        .map(tz.elmkusoma.shared.domain.Institution::getName)
                        .orElse(null);

        // Resolve the states the public page must distinguish, in precedence order.
        boolean superseded = certificate.getSupersededByCertificateId() != null;
        boolean revoked = certificate.getStatus() == CertificateStatus.REVOKED;
        boolean expired = certificate.getExpiryDate() != null
                && certificate.getExpiryDate().isBefore(LocalDate.now());
        boolean issued = certificate.getStatus() == CertificateStatus.ISSUED;

        final boolean valid;
        final String publicStatus;
        final String message;
        if (superseded) {
            // Above REVOKED deliberately. In the normal re-issue flow the predecessor is both
            // revoked and superseded, and "revoked" alone sends a holder away empty-handed: it
            // says the document in their hand is worthless without saying a valid one now exists
            // and which it is. Supersession is the more actionable fact, so it is reported first.
            //
            // SUPERSEDED is a public presentation value, not a stored lifecycle value. The stored
            // column still says REVOKED. This distinction already exists here - EXPIRED, the next
            // branch below, appears in no enum either.
            valid = false;
            publicStatus = "SUPERSEDED";
            message = "This certificate has been replaced by a newer certificate issued for "
                    + "the same award.";
        } else if (revoked) {
            valid = false;
            publicStatus = "REVOKED";
            // Deliberately not the stored reason: it is free text an administrator typed and may
            // contain detail about the holder that an anonymous visitor has no right to.
            message = "This certificate has been revoked by the issuing institution.";
        } else if (expired) {
            valid = false;
            publicStatus = "EXPIRED";
            message = "This certificate has expired";
        } else if (issued) {
            valid = true;
            publicStatus = "ISSUED";
            message = "Certificate is valid and verified";
        } else {
            // A draft is never presented as valid, and the message no longer claims otherwise.
            valid = false;
            publicStatus = certificate.getStatus().name();
            message = "This certificate has not been issued";
        }

        CertificateVerificationResponse response = toPublicVerification(certificate, institutionName,
                valid, publicStatus, message);

        auditVerification(certificate.getInstitutionId(), certificate, publicStatus, valid);
        return response;
    }

    /**
     * Builds the anonymous response.
     *
     * <p>Includes: what a holder needs to confirm the document is theirs and genuine — serial,
     * holder name, certificate type, title, awarding institution, issue and completion dates.</p>
     *
     * <p>Excludes, deliberately: the internal row id, the grade, the skills list, the student's id
     * number and the instructor's name. None is needed to verify a certificate, and a grade or a
     * student number on an unauthenticated endpoint is an academic record disclosure.</p>
     */
    private CertificateVerificationResponse toPublicVerification(Certificate certificate,
                                                                 String institutionName,
                                                                 boolean valid,
                                                                 String publicStatus,
                                                                 String message) {
        return CertificateVerificationResponse.builder()
                .valid(valid)
                .serialNumber(certificate.getSerialNumber())
                .studentName(certificate.getStudentName())
                .certificateType(certificate.getCertificateType().name())
                .title(certificate.getTitle())
                .courseTitle(certificate.getCourseOrProgramme() != null
                        ? certificate.getCourseOrProgramme() : certificate.getTitle())
                .completionDate(certificate.getCompletionDate())
                .institutionName(institutionName)
                .status(publicStatus)
                .issueDate(certificate.getIssueDate())
                .message(message)
                .supersededBySerialNumber(resolveSupersededBySerial(certificate))
                .supersededAt(certificate.getSupersededAt())
                .build();
    }

    /**
     * Serial of the certificate that replaced this one, or null when nothing has.
     *
     * <p>Resolved through the foreign key rather than stored alongside it, so the serial can never
     * drift from the certificate it names if one of the two were edited. A failure to load leaves
     * the pointer reported as a bare supersession rather than raising: the holder is still owed the
     * fact that their document was replaced, even if the replacement cannot be named.</p>
     */
    private String resolveSupersededBySerial(Certificate certificate) {
        if (certificate.getSupersededByCertificateId() == null) {
            return null;
        }
        return certificateRepository.findById(certificate.getSupersededByCertificateId())
                .map(Certificate::getSerialNumber)
                .orElse(null);
    }

    /**
     * Records a public verification attempt.
     *
     * <p>The institution scope falls back to the platform scope rather than null. An unknown code
     * belongs to no school, but it is exactly the event an operator needs to see, and
     * {@code audit_logs.institution_id} is NOT NULL - so passing null failed the insert and the
     * catch below swallowed it. Failed lookups were therefore not recorded at all, which is the
     * one case the surrounding comment claimed to cover.</p>
     */
    private void auditVerification(UUID institutionId, Certificate certificate,
                                   String outcome, boolean valid) {
        try {
            auditService.recordAuditLog(
                    institutionId != null ? institutionId : PlatformAuditScope.PLATFORM_INSTITUTION_ID,
                    null, null, "PUBLIC",
                    "Certificate",
                    certificate == null ? null : certificate.getId(),
                    certificate == null ? null : certificate.getSerialNumber(),
                    AuditLog.AuditAction.VIEW, null,
                    java.util.Map.of("outcome", outcome, "valid", valid));
        } catch (Exception e) {
            // Audit must never turn a public verification into a 500.
            log.warn("Failed to audit certificate verification ({}): {}", outcome, e.getMessage(), e);
        }
    }

    // ── Certificate Queries ──

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByStudent(UUID studentId) {
        return certificateRepository.findAllByStudentId(studentId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CertificateResponse> getCertificatesByStudent(UUID studentId, UUID institutionId,
                                                              Pageable pageable) {
        // Scoped by institution as well as student. The previous query filtered on studentId only,
        // so any caller who could reach this endpoint could read another school's certificates by
        // guessing or obtaining a student id - a cross-tenant IDOR on an endpoint that carries role
        // checks but no ownership predicate.
        return certificateRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId, pageable)
                .map(certificateMapper::toCertificateResponse);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByInstitution(UUID institutionId) {
        return certificateRepository.findAllByInstitutionId(institutionId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CertificateResponse> getCertificatesByInstitution(UUID institutionId, Pageable pageable) {
        return certificateRepository.findByInstitutionIdAndIsDeletedFalse(institutionId, pageable)
                .map(certificateMapper::toCertificateResponse);
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(UUID certificateId, UUID institutionId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "access");
        }

        return certificateMapper.toCertificateResponse(certificate);
    }

    // ── Transcript Generation ──

    public TranscriptResponse generateTranscript(GenerateTranscriptRequest request, UUID institutionId,
                                                  UUID issuedBy, String userEmail, String userRole) {
        // Check for existing transcript for this period
        if (request.getAcademicYear() != null && request.getTerm() != null) {
            transcriptRepository.findByStudentAndPeriod(request.getStudentId(), request.getAcademicYear(), request.getTerm())
                    .ifPresent(t -> {
                        throw new IllegalArgumentException("Transcript already exists for this student and period");
                    });
        }

        String serialNumber = generateTranscriptSerialNumber(institutionId);

        Transcript transcript = Transcript.builder()
                .studentId(request.getStudentId())
                .issuedBy(issuedBy)
                .serialNumber(serialNumber)
                .academicYear(request.getAcademicYear())
                .term(request.getTerm())
                .status(Transcript.TranscriptStatus.DRAFT)
                .generatedAt(LocalDateTime.now())
                .remarks(request.getRemarks())
                .build();
        transcript.setInstitutionId(institutionId);

        transcriptRepository.save(transcript);

        // Save entries
        if (request.getEntries() != null && !request.getEntries().isEmpty()) {
            for (GenerateTranscriptRequest.TranscriptEntryRequest entryReq : request.getEntries()) {
                TranscriptEntry entry = TranscriptEntry.builder()
                        .transcriptId(transcript.getId())
                        .subjectName(entryReq.getSubjectName())
                        .subjectCode(entryReq.getSubjectCode())
                        .score(entryReq.getScore())
                        .grade(entryReq.getGrade())
                        .remarks(entryReq.getRemarks())
                        .build();
                entry.setInstitutionId(institutionId);
                transcriptEntryRepository.save(entry);
            }

            transcript.setTotalSubjects(request.getEntries().size());
            transcriptRepository.save(transcript);
        }

        log.info("Generated transcript: {} for student: {} in institution: {}",
                serialNumber, request.getStudentId(), institutionId);

        auditService.recordAuditLog(institutionId, issuedBy, userEmail, userRole,
                "Transcript", transcript.getId(), transcript.getSerialNumber(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("serialNumber", serialNumber, "studentId", request.getStudentId().toString()));

        List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(transcript.getId());
        return certificateMapper.toTranscriptResponse(transcript, entries);
    }

    public TranscriptResponse issueTranscript(UUID transcriptId, UUID institutionId,
                                                String userEmail, String userRole) {
        Transcript transcript = transcriptRepository.findByIdAndIsDeletedFalse(transcriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript", "id", transcriptId));

        if (!transcript.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("transcript", "issue");
        }

        if (transcript.getStatus() != Transcript.TranscriptStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT transcripts can be issued");
        }

        transcript.setStatus(Transcript.TranscriptStatus.ISSUED);
        transcript.setIssuedAt(LocalDateTime.now());
        transcriptRepository.save(transcript);

        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "Transcript", transcript.getId(), transcript.getSerialNumber(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", "DRAFT"), Map.of("status", "ISSUED"));

        List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(transcriptId);
        return certificateMapper.toTranscriptResponse(transcript, entries);
    }

    @Transactional(readOnly = true)
    public List<TranscriptResponse> getTranscriptsByStudent(UUID studentId) {
        List<Transcript> transcripts = transcriptRepository.findAllByStudentId(studentId);
        return transcripts.stream()
                .map(t -> {
                    List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(t.getId());
                    return certificateMapper.toTranscriptResponse(t, entries);
                })
                .toList();
    }

    // ── Helpers ──

    private String generateSerialNumber(UUID institutionId, CertificateType type) {
        String prefix = switch (type) {
            case COMPLETION -> "CERT-CMP";
            case ACHIEVEMENT -> "CERT-ACH";
            case PARTICIPATION -> "CERT-PAR";
            case TRANSCRIPT -> "CERT-TRN";
        };
        long count = certificateRepository.countByInstitutionId(institutionId) + 1;
        int year = LocalDate.now().getYear();
        return String.format("%s-%d-%06d", prefix, year, count);
    }

    private String generateTranscriptSerialNumber(UUID institutionId) {
        long count = transcriptRepository.findAllByInstitutionId(institutionId).size() + 1;
        int year = LocalDate.now().getYear();
        return String.format("TRAN-%d-%06d", year, count);
    }

    private String generateVerificationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
