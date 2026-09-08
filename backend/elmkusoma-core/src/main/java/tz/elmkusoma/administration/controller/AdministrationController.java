package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.AdministrationService;
import tz.elmkusoma.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "System settings, roles, dashboard, and user management")
public class AdministrationController {

    private final AdministrationService administrationService;

    // ── Dashboard ──

    @GetMapping("/dashboard")
    @Operation(summary = "Get institution dashboard aggregations")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestAttribute UUID institutionId) {
        DashboardResponse response = administrationService.getDashboard(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Settings ──

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
            @RequestAttribute UUID institutionId) {
        SettingResponse response = administrationService.createOrUpdateSetting(request, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", response));
    }

    // ── Role Management ──

    @PostMapping("/roles")
    @Operation(summary = "Create a custom role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestAttribute UUID institutionId) {
        RoleResponse response = administrationService.createRole(request, institutionId);
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
            @RequestAttribute UUID institutionId) {
        administrationService.deleteRole(roleId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }

    // ── User Import ──

    @PostMapping("/users/import")
    @Operation(summary = "Create a CSV import job for bulk user creation")
    public ResponseEntity<ApiResponse<ImportJobResponse>> createImportJob(
            @RequestParam String importType,
            @RequestParam String fileName,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        ImportJobResponse response = administrationService.createImportJob(importType, fileName, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Import job created successfully", response));
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
}
