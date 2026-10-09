package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.AdministrationService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.config.security.OrganizationContext;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN')")
@Tag(name = "Administration", description = "System settings, roles, dashboard, and user management")
public class AdministrationController {

    private final AdministrationService administrationService;

    // â”€â”€ Dashboard â”€â”€

    @GetMapping("/dashboard")
    @Operation(summary = "Get institution dashboard aggregations")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestAttribute UUID institutionId) {
        DashboardResponse response = administrationService.getDashboard(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard/enhanced")
    @Operation(summary = "Get enhanced role-aware dashboard with attention items, activity feed, quick actions, and health")
    public ResponseEntity<ApiResponse<EnhancedDashboardResponse>> getEnhancedDashboard(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("userPermissions") List<String> userPermissions) {
        EnhancedDashboardResponse response = administrationService.getEnhancedDashboard(institutionId, userRole, userPermissions);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // â”€â”€ Access & Permission Center â”€â”€

    @GetMapping("/my-access")
    @Operation(summary = "Effective access of the calling administrator: role, organization, membership, scope, and permissions")
    public ResponseEntity<ApiResponse<MyAccessResponse>> getMyAccess(
            @RequestAttribute("organizationContext") OrganizationContext organizationContext) {
        MyAccessResponse response = administrationService.getMyAccess(organizationContext);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // â”€â”€ Settings â”€â”€

    @GetMapping("/settings")
    @Operation(summary = "List all system settings for institution")
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getSettings(
            @RequestAttribute UUID institutionId) {
        List<SettingResponse> response = administrationService.getSettings(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/settings/{key}")
    @Operation(summary = "Get a specific setting by key")
    public ResponseEntity<ApiResponse<SettingResponse>> getSettingByKey(
            @RequestAttribute UUID institutionId,
            @PathVariable String key) {
        SettingResponse response = administrationService.getSettingByKey(institutionId, key);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/settings")
    @Operation(summary = "Create or update a system setting")
    public ResponseEntity<ApiResponse<SettingResponse>> upsertSetting(
            @Valid @RequestBody SettingRequest request,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        SettingResponse response = administrationService.createOrUpdateSetting(request, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", response));
    }

    // â”€â”€ Role Management â”€â”€

    @PostMapping("/roles")
    @Operation(summary = "Create a custom role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        RoleResponse response = administrationService.createRole(request, institutionId, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", response));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all custom roles for institution")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @RequestAttribute UUID institutionId) {
        List<RoleResponse> response = administrationService.getRoles(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/roles/{roleId}")
    @Operation(summary = "Get a specific role by ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(
            @PathVariable UUID roleId,
            @RequestAttribute UUID institutionId) {
        RoleResponse response = administrationService.getRoleById(roleId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "Delete a custom role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID roleId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        administrationService.deleteRole(roleId, institutionId, userEmail, userRole);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }

    // â”€â”€ User Import â”€â”€

    @PostMapping("/users/import")
    @Operation(summary = "Import users from a CSV file")
    public ResponseEntity<ApiResponse<ImportJobResponse>> createImportJob(
            @RequestParam String importType,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail,
            @RequestAttribute("userRole") String userRole) {
        // Audit B-28: this used to accept only a fileName, so no rows were ever imported.
        ImportJobResponse response = administrationService.createImportJob(importType, file,
                institutionId, userId, userEmail, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Import job processed successfully", response));
    }

    @GetMapping("/users/import")
    @Operation(summary = "List all import jobs for institution")
    public ResponseEntity<ApiResponse<List<ImportJobResponse>>> getImportJobs(
            @RequestAttribute UUID institutionId) {
        List<ImportJobResponse> response = administrationService.getImportJobs(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/import/{jobId}")
    @Operation(summary = "Get import job by ID")
    public ResponseEntity<ApiResponse<ImportJobResponse>> getImportJobById(
            @PathVariable UUID jobId,
            @RequestAttribute UUID institutionId) {
        ImportJobResponse response = administrationService.getImportJobById(jobId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users and entities within this institution")
    public ResponseEntity<ApiResponse<List<GlobalSearchResult>>> orgSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestAttribute UUID institutionId) {
        List<GlobalSearchResult> results = administrationService.orgSearch(institutionId, q, type, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/export")
    @Operation(summary = "Export institution data as CSV")
    public ResponseEntity<byte[]> exportData(
            @RequestParam String entityType,
            @RequestAttribute UUID institutionId) {
        byte[] csv = administrationService.exportData(institutionId, entityType);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=" + entityType + "_export.csv")
                .body(csv);
    }
}
