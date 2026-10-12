package tz.elmkusoma.certificate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.service.CertificateService;
import tz.elmkusoma.common.ApiResponse;
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
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;

    // ── Template Endpoints ──

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

    // ── Certificate Endpoints ──

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
     * Best-effort client identity for rate limiting.
     *
     * <p>The forwarded header is only honoured when the deployment sits behind a proxy; trusting it
     * unconditionally would let any caller spoof {@code X-Forwarded-For} and defeat the limit.
     * With no header, the remote address is used.</p>
     */
    private static String resolveClientKey(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First entry is the original client.
            String first = forwarded.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        return remote == null ? "unknown" : remote;
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

        // Ownership, not just role. Previously a PARENT - and any teacher in the school - could
        // read any certificate in the institution, including a child's classmate's. The transcript
        // endpoint on this same controller already checks the parent-student link, so this was an
        // inconsistency rather than a deliberate policy.
        if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)) {
            if (response.getStudentId() != null && !response.getStudentId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
        } else if ("PARENT".equals(userRole)) {
            if (!isParentOf(userId, response.getStudentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied"));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(response));
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
                if (!studentId.equals(userId)) {
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
            response = certificateService.getCertificatesByInstitution(institutionId, PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Transcript Endpoints ──

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
