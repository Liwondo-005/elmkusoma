package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.PlatformAdminService;
import tz.elmkusoma.audit.dto.ActivityFeedResponse;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/platform-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Administration", description = "Platform-wide governance, ecosystem overview, and operational management")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    // ── Command Center ──

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform-wide dashboard overview with KPIs")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> getPlatformDashboard() {
        PlatformDashboardResponse response = platformAdminService.getPlatformDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/attention")
    @Operation(summary = "Get attention center items requiring admin action")
    public ResponseEntity<ApiResponse<List<AttentionItemResponse>>> getAttentionItems() {
        List<AttentionItemResponse> response = platformAdminService.getAttentionItems();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/activity")
    @Operation(summary = "Get recent platform-wide activity feed")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getRecentActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ActivityFeedResponse> response = platformAdminService.getRecentActivity(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    @Operation(summary = "Get platform service health status")
    public ResponseEntity<ApiResponse<PlatformHealthResponse>> getPlatformHealth() {
        PlatformHealthResponse response = platformAdminService.getPlatformHealth();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Users ──

    @GetMapping("/users")
    @Operation(summary = "List all users across the platform")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId) {
        PageResponse<UserSummaryResponse> response = platformAdminService.listUsers(page, size, role, search, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUser(@PathVariable UUID userId) {
        UserSummaryResponse response = platformAdminService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Activate or suspend a user account")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean active) {
        UserSummaryResponse response = platformAdminService.updateUserStatus(userId, active);
        return ResponseEntity.ok(ApiResponse.success("User status updated", response));
    }

    // ── Institutions ──

    @GetMapping("/institutions")
    @Operation(summary = "List all institutions across the platform")
    public ResponseEntity<ApiResponse<PageResponse<InstitutionSummaryResponse>>> listInstitutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        PageResponse<InstitutionSummaryResponse> response = platformAdminService.listInstitutions(page, size, search, type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/institutions/{institutionId}")
    @Operation(summary = "Get institution details with stats")
    public ResponseEntity<ApiResponse<InstitutionDetailResponse>> getInstitutionDetail(@PathVariable UUID institutionId) {
        InstitutionDetailResponse response = platformAdminService.getInstitutionDetail(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/institutions/{institutionId}/status")
    @Operation(summary = "Activate or suspend an institution")
    public ResponseEntity<ApiResponse<InstitutionSummaryResponse>> updateInstitutionStatus(
            @PathVariable UUID institutionId,
            @RequestParam boolean active) {
        InstitutionSummaryResponse response = platformAdminService.updateInstitutionStatus(institutionId, active);
        return ResponseEntity.ok(ApiResponse.success("Institution status updated", response));
    }

    // ── Live Classes ──

    @GetMapping("/live-classes")
    @Operation(summary = "List live classes across the platform")
    public ResponseEntity<ApiResponse<PageResponse<LiveClassSummaryResponse>>> listLiveClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID institutionId) {
        PageResponse<LiveClassSummaryResponse> response = platformAdminService.listLiveClasses(page, size, status, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Payments & Entitlements ──

    @GetMapping("/payments")
    @Operation(summary = "List payments across the platform")
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryResponse>>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID institutionId) {
        PageResponse<PaymentSummaryResponse> response = platformAdminService.listPayments(page, size, status, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Certificates ──

    @GetMapping("/certificates")
    @Operation(summary = "List certificates issued across the platform")
    public ResponseEntity<ApiResponse<PageResponse<CertificateSummaryResponse>>> listCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID institutionId) {
        PageResponse<CertificateSummaryResponse> response = platformAdminService.listCertificates(page, size, institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Security ──

    @GetMapping("/security/events")
    @Operation(summary = "List unresolved security events")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getUnresolvedSecurityEvents() {
        List<SecurityEventResponse> response = platformAdminService.getUnresolvedSecurityEvents();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/security/events/{eventId}/resolve")
    @Operation(summary = "Resolve a security event")
    public ResponseEntity<ApiResponse<SecurityEventResponse>> resolveSecurityEvent(@PathVariable UUID eventId) {
        SecurityEventResponse response = platformAdminService.resolveSecurityEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("Security event resolved", response));
    }

    // ── Audit ──

    @GetMapping("/audit/logs")
    @Operation(summary = "Get platform-wide audit logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType) {
        List<AuditLogResponse> response = platformAdminService.getAuditLogs(page, size, action, entityType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Global Search ──

    @GetMapping("/search")
    @Operation(summary = "Global platform search across authorized resources")
    public ResponseEntity<ApiResponse<List<GlobalSearchResult>>> globalSearch(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        List<GlobalSearchResult> response = platformAdminService.globalSearch(q, type, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
