package tz.elmkusoma.nfe.provider.controller;

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
import tz.elmkusoma.nfe.provider.dto.ProviderRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderResponse;
import tz.elmkusoma.nfe.provider.dto.ProviderStatsResponse;
import tz.elmkusoma.nfe.provider.service.ProviderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/providers")
@RequiredArgsConstructor
@Tag(name = "NFE Provider Management", description = "CRUD operations for NFE education providers")
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    @Operation(summary = "Create a new education provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> createProvider(
            @RequestAttribute UUID institutionId,
            @Valid @RequestBody ProviderRequest request) {
        ProviderResponse provider = providerService.createProvider(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Provider created successfully", provider));
    }

    @GetMapping("/me")
    @Operation(summary = "Get (and provision on first use) the provider for the caller's institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ApiResponse<ProviderResponse>> getMyProvider(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") String actorId) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.getOrCreateProviderForInstitution(institutionId, actorId)));
    }

/**
 * Platform approval: verify or revoke a provider.
 *
 * <p>Platform ADMIN only. This previously also accepted INSTITUTION_ADMIN and PROVIDER_ADMIN,
 * which let a provider flip {@code is_verified} on its own provider record - self-approval of
 * the trust decision verification represents, and a second way around the real review workflow
 * in {@code /v1/verifications}. The operation summary always described it as platform approval;
 * the role list was the part that was wrong.</p>
 *
 * <p>Previously-asserted provider self-verification is now a 403 by decision, not oversight:
 * see {@code ProviderAdminProvisioningSecurityTest}.</p>
     */
    @PutMapping("/{id}/verification")
    @Operation(summary = "Platform approval: verify or revoke a provider")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> setVerification(
            @RequestAttribute UUID institutionId,
            @PathVariable UUID id,
            @RequestParam boolean verified) {
        return ResponseEntity.ok(ApiResponse.success("Provider verification updated",
                providerService.setProviderVerified(institutionId, id, verified)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an education provider by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ApiResponse<ProviderResponse>> getProvider(
            @RequestAttribute UUID institutionId,
            @PathVariable UUID id) {
        ProviderResponse provider = providerService.getProvider(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(provider));
    }

    @GetMapping
    @Operation(summary = "List all education providers in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<ProviderResponse>>> listProviders(
            @RequestAttribute UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ProviderResponse> providers = providerService.listProviders(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active education providers")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> getActiveProviders(
            @RequestAttribute UUID institutionId) {
        List<ProviderResponse> providers = providerService.getActiveProviders(institutionId);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an education provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> updateProvider(
            @RequestAttribute UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ProviderRequest request) {
        ProviderResponse provider = providerService.updateProvider(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Provider updated successfully", provider));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an education provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @RequestAttribute UUID institutionId,
            @PathVariable UUID id) {
        providerService.deleteProvider(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Provider deleted successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get provider dashboard statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
    public ResponseEntity<ApiResponse<ProviderStatsResponse>> getProviderStats(
            @RequestAttribute UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProviderStats(institutionId)));
    }
}
