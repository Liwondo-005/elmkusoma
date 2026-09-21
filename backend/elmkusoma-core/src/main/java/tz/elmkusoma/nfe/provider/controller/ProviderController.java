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
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> createProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ProviderRequest request) {
        ProviderResponse provider = providerService.createProvider(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Provider created successfully", provider));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an education provider by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ProviderResponse>> getProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        ProviderResponse provider = providerService.getProvider(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(provider));
    }

    @GetMapping
    @Operation(summary = "List all education providers in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<ProviderResponse>>> listProviders(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ProviderResponse> providers = providerService.listProviders(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active education providers")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ProviderResponse>>> getActiveProviders(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ProviderResponse> providers = providerService.getActiveProviders(institutionId);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an education provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProviderResponse>> updateProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ProviderRequest request) {
        ProviderResponse provider = providerService.updateProvider(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Provider updated successfully", provider));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an education provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        providerService.deleteProvider(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Provider deleted successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get provider dashboard statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ProviderStatsResponse>> getProviderStats(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProviderStats(institutionId)));
    }
}
