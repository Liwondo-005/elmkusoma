package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.InstitutionServiceRequest;
import tz.elmkusoma.administration.dto.InstitutionServiceResponse;
import tz.elmkusoma.administration.service.InstitutionServiceManagementService;
import tz.elmkusoma.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
@Tag(name = "Institution Service Management", description = "Enable/disable platform services per institution")
public class InstitutionServiceController {

    private final InstitutionServiceManagementService serviceManagementService;

    @GetMapping
    @Operation(summary = "List all services for this institution")
    public ResponseEntity<ApiResponse<List<InstitutionServiceResponse>>> getServices(
            @RequestAttribute UUID institutionId) {
        List<InstitutionServiceResponse> response = serviceManagementService.getServices(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{featureKey}")
    @Operation(summary = "Get a specific service")
    public ResponseEntity<ApiResponse<InstitutionServiceResponse>> getService(
            @PathVariable String featureKey,
            @RequestAttribute UUID institutionId) {
        InstitutionServiceResponse response = serviceManagementService.getService(institutionId, featureKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{featureKey}/enable")
    @Operation(summary = "Enable a service for this institution")
    public ResponseEntity<ApiResponse<InstitutionServiceResponse>> enableService(
            @PathVariable String featureKey,
            @Valid @RequestBody InstitutionServiceRequest request,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        request.setFeatureKey(featureKey);
        InstitutionServiceResponse response = serviceManagementService.enableService(institutionId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service enabled", response));
    }

    @PostMapping("/{featureKey}/disable")
    @Operation(summary = "Disable a service for this institution")
    public ResponseEntity<ApiResponse<InstitutionServiceResponse>> disableService(
            @PathVariable String featureKey,
            @RequestAttribute UUID institutionId) {
        InstitutionServiceResponse response = serviceManagementService.disableService(institutionId, featureKey);
        return ResponseEntity.ok(ApiResponse.success("Service disabled", response));
    }

    @PutMapping("/{featureKey}")
    @Operation(summary = "Update service configuration")
    public ResponseEntity<ApiResponse<InstitutionServiceResponse>> updateService(
            @PathVariable String featureKey,
            @Valid @RequestBody InstitutionServiceRequest request,
            @RequestAttribute UUID institutionId) {
        request.setFeatureKey(featureKey);
        InstitutionServiceResponse response = serviceManagementService.updateService(institutionId, featureKey, request);
        return ResponseEntity.ok(ApiResponse.success("Service updated", response));
    }

    @DeleteMapping("/{featureKey}")
    @Operation(summary = "Remove service from institution")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable String featureKey,
            @RequestAttribute UUID institutionId) {
        serviceManagementService.deleteService(institutionId, featureKey);
        return ResponseEntity.ok(ApiResponse.success("Service removed", null));
    }
}