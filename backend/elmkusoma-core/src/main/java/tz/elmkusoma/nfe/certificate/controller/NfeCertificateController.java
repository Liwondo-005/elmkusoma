package tz.elmkusoma.nfe.certificate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateRequest;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateResponse;
import tz.elmkusoma.nfe.certificate.service.NfeCertificateService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/certificates")
@RequiredArgsConstructor
@Tag(name = "NFE Certificate Management", description = "CRUD operations for NFE certificates")
public class NfeCertificateController {

    private final NfeCertificateService certificateService;

    @PostMapping("/providers/{providerId}")
    @Operation(summary = "Create a new certificate for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<NfeCertificateResponse>> createCertificate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId,
            @Valid @RequestBody NfeCertificateRequest request) {
        NfeCertificateResponse certificate = certificateService.createCertificate(institutionId, providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Certificate created successfully", certificate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a certificate by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NfeCertificateResponse>> getCertificate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        NfeCertificateResponse certificate = certificateService.getCertificate(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(certificate));
    }

    @GetMapping("/providers/{providerId}")
    @Operation(summary = "List all certificates for a provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<NfeCertificateResponse>>> listCertificates(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<NfeCertificateResponse> certificates = certificateService.listCertificates(institutionId, providerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(certificates));
    }

    @GetMapping("/learners/{learnerId}")
    @Operation(summary = "Get certificates for a learner")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<NfeCertificateResponse>>> getCertificatesByLearner(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID learnerId) {
        List<NfeCertificateResponse> certificates = certificateService.getCertificatesByLearner(institutionId, learnerId);
        return ResponseEntity.ok(ApiResponse.success(certificates));
    }

    @GetMapping("/verify/{verificationCode}")
    @Operation(summary = "Verify a certificate by verification code")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<NfeCertificateResponse>> verifyCertificate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable String verificationCode) {
        NfeCertificateResponse certificate = certificateService.verifyCertificate(institutionId, verificationCode);
        return ResponseEntity.ok(ApiResponse.success(certificate));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<NfeCertificateResponse>> updateCertificate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody NfeCertificateRequest request) {
        NfeCertificateResponse certificate = certificateService.updateCertificate(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Certificate updated successfully", certificate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a certificate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        certificateService.deleteCertificate(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Certificate deleted successfully", null));
    }
}
