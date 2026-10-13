package tz.elmkusoma.certificate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateApproval;
import tz.elmkusoma.certificate.service.CertificateService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.config.security.ClientIpResolver;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Certificate Management", description = "Certificate templates, generation, and verification")
public class CertificateController {

    private final CertificateService certificateService;
    private final tz.elmkusoma.certificate.service.CertificateApprovalService certificateApprovalService;
    private final ClientIpResolver clientIpResolver;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final tz.elmkusoma.certificate.pdf.CertificatePdfService certificatePdfService;

    // â”€â”€ Template Endpoints â”€â”€

    @PostMapping("/templates")
    @Operation(summary = "Create a certificate template")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        TemplateResponse response = certificateService.createTemplate(request, institutionId, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", response));
    }

    @GetMapping("/templates")
    @Operation(summary = "List all certificate templates for institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates(
            @RequestAttribute("institutionId") UUID institutionId) {
        List<TemplateResponse> response = certificateService.getTemplates(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get a specific certificate template")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateById(
            @PathVariable UUID templateId,
            @RequestAttribute("institutionId") UUID institutionId) {
        TemplateResponse response = certificateService.getTemplateById(templateId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // â”€â”€ Certificate Endpoints â”€â”€

    @PostMapping("/generate")
    @Operation(summary = "Generate a new certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponse>> generateCertificate(
            @Valid @RequestBody GenerateCertificateRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.generateCertificate(request, institutionId, userId, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Certificate generated successfully", response));
    }

    @PostMapping("/{certificateId}/issue")
    @Operation(summary = "Issue a draft certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponse>> issueCertificate(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID actorUserId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.issueCertificate(certificateId, institutionId, actorUserId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Certificate issued successfully", response));
    }

    @PostMapping("/{certificateId}/revoke")
    @Operation(summary = "Revoke an issued certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponse>> revokeCertificate(
            @PathVariable UUID certificateId,
            @Valid @RequestBody RevokeCertificateRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.revokeCertificate(certificateId, institutionId, request, userId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked successfully", response));
    }

    @GetMapping("/verify/{verificationCode}")
    @Operation(summary = "Verify a certificate (public endpoint)")
    @PreAuthorize("permitAll")
    public ResponseEntity<ApiResponse<CertificateVerificationResponse>> verifyCertificate(
            @PathVariable String verificationCode,
            HttpServletRequest request) {
        // Rate limiting is per caller. Keyed on the forwarded client address where a proxy sets
        // one, so a single caller cannot exhaust the budget for everybody else.
        String clientKey = resolveClientKey(request);
        CertificateVerificationResponse response = certificateService.verifyCertificate(verificationCode, clientKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Best-effort client identity for rate limiting, delegated to the shared resolver.
     *
     * <p>This previously read {@code X-Forwarded-For} unconditionally, while the surrounding
     * comment claimed the header was only honoured behind a proxy. The comment described the
     * behaviour that should exist; the code did the opposite. Because the header is chosen by the
     * caller, varying it per request produced a fresh rate-limit bucket per request, which is the
     * exact opposite of the control's purpose for an unauthenticated verification endpoint.</p>
     */
    private String resolveClientKey(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    @GetMapping("/{certificateId}")
    @Operation(summary = "Get a specific certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificateById(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.getCertificateById(certificateId, institutionId);

        String refusal = ownershipRefusal(userId, userRole, response.getStudentId());
        if (refusal != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(refusal));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Exports an issued certificate as a PDF.
     *
     * <p>Runs exactly the same tenant and ownership checks as reading the certificate above - a
     * learner who cannot open a certificate must not be able to download it either, and a file
     * that leaves the system is harder to recall than a page that can stop serving it.</p>
     *
     * <p>The remaining gate is status, enforced in the service: only ISSUED and REVOKED export,
     * because a certificate that has not been released is not yet a document.</p>
     */
    @GetMapping("/{certificateId}/pdf")
    @Operation(summary = "Download an issued certificate as a PDF")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'OTHER_LEARNER')")
    public ResponseEntity<byte[]> exportCertificatePdf(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.getCertificateById(certificateId, institutionId);

        String refusal = ownershipRefusal(userId, userRole, response.getStudentId());
        if (refusal != null) {
            throw new tz.elmkusoma.exception.ForbiddenException("certificate", "export");
        }

        byte[] pdf = certificatePdfService.exportCertificate(certificateId, institutionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + pdfFileName(response) + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * A filesystem-safe download name. The serial is operator-visible and may carry characters a
     * Content-Disposition header must not: a quote would terminate the filename early and let the
     * remainder be read as another header.
     */
    private String pdfFileName(CertificateResponse response) {
        String raw = response.getSerialNumber() != null && !response.getSerialNumber().isBlank()
                ? response.getSerialNumber()
                : String.valueOf(response.getId());
        String safe = raw.replaceAll("[^A-Za-z0-9._-]", "-");
        return "certificate-" + safe + ".pdf";
    }

    /**
     * The ownership rule shared by every certificate read on this controller.
     *
     * <p>Role is not enough. Previously a PARENT - and any teacher in the school - could read any
     * certificate in the institution, including a classmate's. The transcript endpoint on this
     * same controller already checks the parent-student link, so this was an inconsistency rather
     * than a deliberate policy.</p>
     *
     * @return an error message when the caller must be refused, or null when they may proceed
     */
    private String ownershipRefusal(UUID userId, String userRole, UUID certificateStudentId) {
        if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)) {
            return isOwnCertificate(userId, certificateStudentId) ? null : "Access denied";
        }
        if ("PARENT".equals(userRole)) {
            return isParentOf(userId, certificateStudentId) ? null : "Access denied";
        }
        return null;
    }

    /**
     * True when this authenticated user owns the certificate.
     *
     * <p>The certificate's studentId is a students.id, which is NOT the same identifier as the
     * authenticated users.id. Comparing the two directly denied every real student access to their
     * own certificate, because the two values could never be equal.</p>
     *
     * <p>So the caller's user id is resolved to their student row first, and ownership is then
     * compared on the same identifier space as the certificate. This mirrors what the transcript
     * endpoint on this controller already does.</p>
     */
    private boolean isOwnCertificate(UUID callerUserId, UUID certificateStudentId) {
        if (certificateStudentId == null) {
            return false;
        }
        var student = studentRepository.findByUserIdAndIsDeletedFalse(callerUserId).orElse(null);
        return student != null && certificateStudentId.equals(student.getId());
    }

    /**
     * True when this caller is linked as a parent of the given student.
     *
     * <p>Same parent-student check the transcript endpoint on this controller already performs,
     * extracted so the certificate paths use one rule rather than two.</p>
     */
    private boolean isParentOf(UUID parentUserId, UUID studentId) {
        if (studentId == null) {
            return false;
        }
        var parent = parentRepository.findByUserIdAndIsDeletedFalse(parentUserId).orElse(null);
        return parent != null
                && parentStudentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(
                        parent.getId(), studentId);
    }

    @GetMapping
    @Operation(summary = "List certificates by student or institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<Page<CertificateResponse>>> getCertificates(
            @RequestParam(required = false) UUID studentId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CertificateResponse> response;
        if (studentId != null) {
            if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)) {
                // Same identifier-space trap as the single-certificate path: studentId here is a
                // students.id, while userId is a users.id, so comparing them directly meant no
                // student could ever list their own certificates.
                if (!isOwnCertificate(userId, studentId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access denied: cannot view other students' certificates"));
                }
            } else if ("PARENT".equals(userRole) && !isParentOf(userId, studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: not linked to this student"));
            }
            // institutionId is passed through so the query itself is tenant-scoped, rather than
            // filtering rows out after they have already been read.
            response = certificateService.getCertificatesByStudent(studentId, institutionId,
                    PageRequest.of(page, size));
        } else {
            // Omitting studentId means "the whole institution", which is an institutional view.
            //
            // Previously any role could reach it, so a learner calling /v1/certificates with no
            // parameter received every certificate in their school: other students' names, serials,
            // grades and completion dates. The endpoint's role check admitted STUDENT/PARENT, but
            // the ownership checks below only ever ran on the studentId branch.
            //
            // Learner-facing callers are therefore required to name a student, and the student
            // branch above resolves their own record.
            if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)
                    || "PARENT".equals(userRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                "Access denied: name the student whose certificates you are requesting"));
            }
            response = certificateService.getCertificatesByInstitution(institutionId, PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // â”€â”€ Approval workflow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //
    // Exposed on the existing controller rather than a new one: this is the same certificate
    // lifecycle, not a parallel system, and a second controller is how two state machines end up
    // contradicting each other.

    @PostMapping("/{certificateId}/submit-for-approval")
    @Operation(summary = "Submit a draft certificate for review")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CertificateResponse>> submitForApproval(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        Certificate submitted = certificateApprovalService.submitForApproval(
                certificateId, institutionId, userId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success(
                "Certificate submitted for review",
                certificateService.getCertificateById(submitted.getId(), institutionId)));
    }

    @PostMapping("/{certificateId}/approvals/{stage}")
    @Operation(summary = "Approve or reject one stage of a certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CertificateResponse>> decideApproval(
            @PathVariable UUID certificateId,
            @PathVariable CertificateApproval.ApprovalStage stage,
            @Valid @RequestBody CertificateDecisionRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {

        certificateApprovalService.decide(certificateId, stage, request.isApproved(), institutionId,
                userId, userEmail, userRole, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                request.isApproved() ? "Stage approved" : "Stage rejected",
                certificateService.getCertificateById(certificateId, institutionId)));
    }

    @PostMapping("/{certificateId}/approvals/{stage}/withdraw")
    @Operation(summary = "Withdraw a previously granted approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CertificateResponse>> withdrawApproval(
            @PathVariable UUID certificateId,
            @PathVariable CertificateApproval.ApprovalStage stage,
            @Valid @RequestBody CertificateDecisionRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {

        certificateApprovalService.withdrawApproval(certificateId, stage, institutionId,
                userId, userEmail, userRole, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                "Approval withdrawn",
                certificateService.getCertificateById(certificateId, institutionId)));
    }

    @GetMapping("/{certificateId}/approvals")
    @Operation(summary = "Approval history for a certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<CertificateApprovalResponse>>> approvalHistory(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId) {
        // Reuse the detail endpoint so the tenant check happens before anything is disclosed.
        certificateService.getCertificateById(certificateId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                certificateApprovalService.historyWithContext(certificateId)));
    }

    @GetMapping("/approval-queue")
    @Operation(summary = "Outstanding approval work for this institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<CertificateApprovalResponse>>> approvalQueue(
            @RequestAttribute("institutionId") UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                certificateApprovalService.pendingQueue(institutionId)));
    }

    // â”€â”€ Transcript Endpoints â”€â”€

    @PostMapping("/transcripts/generate")
    @Operation(summary = "Generate a transcript")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TranscriptResponse>> generateTranscript(
            @Valid @RequestBody GenerateTranscriptRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        TranscriptResponse response = certificateService.generateTranscript(request, institutionId, userId, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transcript generated successfully", response));
    }

    @PostMapping("/transcripts/{transcriptId}/issue")
    @Operation(summary = "Issue a draft transcript")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<TranscriptResponse>> issueTranscript(
            @PathVariable UUID transcriptId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        TranscriptResponse response = certificateService.issueTranscript(transcriptId, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Transcript issued successfully", response));
    }

    @GetMapping("/transcripts")
    @Operation(summary = "List transcripts by student")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<ApiResponse<List<TranscriptResponse>>> getTranscripts(
            @RequestParam UUID studentId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)) {
            if (!studentId.equals(userId)) {
                var student = studentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
                if (student == null || !student.getId().equals(studentId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access denied: cannot view other students' transcripts"));
                }
            }
        } else if ("PARENT".equals(userRole)) {
            var parent = parentRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
            if (parent == null || !parentStudentLinkRepository.existsByParentIdAndStudentIdAndIsDeletedFalse(parent.getId(), studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: cannot view transcripts of students not linked to you"));
            }
        } else if ("TEACHER".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)) {
            var student = studentRepository.findById(studentId).orElse(null);
            if (student != null && student.getInstitutionId() != null && !student.getInstitutionId().equals(institutionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: student belongs to another institution"));
            }
        }
        List<TranscriptResponse> response = certificateService.getTranscriptsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
