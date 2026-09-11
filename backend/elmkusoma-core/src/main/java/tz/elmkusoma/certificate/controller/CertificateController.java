package tz.elmkusoma.certificate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.service.CertificateService;
import tz.elmkusoma.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate Management", description = "Certificate templates, generation, and verification")
public class CertificateController {

    private final CertificateService certificateService;

    // ── Template Endpoints ──

    @PostMapping("/templates")
    @Operation(summary = "Create a certificate template")
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
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates(
            @RequestAttribute("institutionId") UUID institutionId) {
        List<TemplateResponse> response = certificateService.getTemplates(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get a specific certificate template")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateById(
            @PathVariable UUID templateId,
            @RequestAttribute("institutionId") UUID institutionId) {
        TemplateResponse response = certificateService.getTemplateById(templateId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Certificate Endpoints ──

    @PostMapping("/generate")
    @Operation(summary = "Generate a new certificate")
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
    public ResponseEntity<ApiResponse<CertificateResponse>> issueCertificate(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.issueCertificate(certificateId, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Certificate issued successfully", response));
    }

    @PostMapping("/{certificateId}/revoke")
    @Operation(summary = "Revoke an issued certificate")
    public ResponseEntity<ApiResponse<CertificateResponse>> revokeCertificate(
            @PathVariable UUID certificateId,
            @Valid @RequestBody RevokeCertificateRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        CertificateResponse response = certificateService.revokeCertificate(certificateId, institutionId, request, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked successfully", response));
    }

    @GetMapping("/verify/{verificationCode}")
    @Operation(summary = "Verify a certificate (public endpoint)")
    public ResponseEntity<ApiResponse<CertificateVerificationResponse>> verifyCertificate(
            @PathVariable String verificationCode) {
        CertificateVerificationResponse response = certificateService.verifyCertificate(verificationCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{certificateId}")
    @Operation(summary = "Get a specific certificate")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificateById(
            @PathVariable UUID certificateId,
            @RequestAttribute("institutionId") UUID institutionId) {
        CertificateResponse response = certificateService.getCertificateById(certificateId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List certificates by student or institution")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getCertificates(
            @RequestParam(required = false) UUID studentId,
            @RequestAttribute("institutionId") UUID institutionId) {
        List<CertificateResponse> response;
        if (studentId != null) {
            response = certificateService.getCertificatesByStudent(studentId);
        } else {
            response = certificateService.getCertificatesByInstitution(institutionId);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Transcript Endpoints ──

    @PostMapping("/transcripts/generate")
    @Operation(summary = "Generate a transcript")
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
    public ResponseEntity<ApiResponse<List<TranscriptResponse>>> getTranscripts(
            @RequestParam UUID studentId) {
        List<TranscriptResponse> response = certificateService.getTranscriptsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
