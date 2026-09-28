package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.DelegatedTaskResponse;
import tz.elmkusoma.administration.dto.VerificationSummaryResponse;
import tz.elmkusoma.administration.service.PlatformAdminService;
import tz.elmkusoma.common.ApiResponse;

import java.util.UUID;

/**
 * Delegation-aware verification review.
 *
 * <p>This controller intentionally lives OUTSIDE {@code /v1/platform-admin/**},
 * which is URL-locked to {@code ROLE_ADMIN} in {@code SecurityConfig}. A delegated
 * officer (e.g. a TEACHER holding an ACTIVE PROVIDER_VERIFICATION delegation) must
 * be able to reach this endpoint: any authenticated user may call it, and
 * {@link PlatformAdminService#reviewProviderVerification} enforces
 * platform-admin-role OR active scoped delegation on every call.
 * Frontend button hiding is NOT security — the check runs server-side.</p>
 */
@RestController
@RequestMapping("/v1/verifications")
@RequiredArgsConstructor
@Tag(name = "Verification Review")
public class VerificationReviewController {

    private final PlatformAdminService platformAdminService;

    @PutMapping("/{verificationId}/provider-review")
    @Operation(summary = "Review a provider/institution verification with delegation enforcement (APPROVED|REJECTED|CHANGES_REQUIRED)")
    public ResponseEntity<ApiResponse<VerificationSummaryResponse>> reviewProviderVerification(
            @PathVariable UUID verificationId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        UUID actorId = request.getAttribute("userId") != null ? UUID.fromString(request.getAttribute("userId").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.reviewProviderVerification(verificationId, actorId, status, notes)));
    }

    @GetMapping("/delegated-tasks")
    @Operation(summary = "Pending verification tasks covered by the caller's active delegations")
    public ResponseEntity<ApiResponse<java.util.List<DelegatedTaskResponse>>> delegatedTasks(
            HttpServletRequest request) {
        UUID actorId = request.getAttribute("userId") != null ? UUID.fromString(request.getAttribute("userId").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listDelegatedTasks(actorId)));
    }
}
