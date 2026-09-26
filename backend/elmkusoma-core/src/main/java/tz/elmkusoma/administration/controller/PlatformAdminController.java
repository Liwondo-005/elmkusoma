package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.PlatformAdminService;
import tz.elmkusoma.audit.dto.ActivityFeedResponse;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.institution.dto.request.CreateInstitutionRequest;
import tz.elmkusoma.institution.dto.request.UpdateInstitutionRequest;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/platform-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Administration", description = "Platform-wide governance, ecosystem overview, and operational management")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;
    private final tz.elmkusoma.administration.service.PlatformCommerceService platformCommerceService;
    private final tz.elmkusoma.certificate.service.CertificateGovernanceService certificateGovernanceService;
    private final tz.elmkusoma.institution.service.InstitutionService institutionService;

    private static UUID actorId(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        return id instanceof UUID uuid ? uuid : null;
    }

    private static String actorEmail(HttpServletRequest request) {
        Object email = request.getAttribute("userEmail");
        return email != null ? email.toString() : "admin";
    }

    private static String actorRole(HttpServletRequest request) {
        Object role = request.getAttribute("userRole");
        return role != null ? role.toString() : "ADMIN";
    }

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
            @RequestParam(required = false) String search) {
        PageResponse<UserSummaryResponse> response = platformAdminService.listUsers(page, size, role, search);
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

    @PostMapping("/institutions")
    @Operation(summary = "Create a new institution")
    public ResponseEntity<ApiResponse<InstitutionResponse>> createInstitution(
            HttpServletRequest request,
            @Valid @RequestBody CreateInstitutionRequest req) {
        UUID ownerUserId = actorId(request);
        if (ownerUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authenticated user not found", "UNAUTHORIZED"));
        }
        InstitutionResponse response = institutionService.createInstitution(req, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Institution created successfully", response));
    }

    @PutMapping("/institutions/{institutionId}")
    @Operation(summary = "Update institution details")
    public ResponseEntity<ApiResponse<InstitutionResponse>> updateInstitution(
            @PathVariable UUID institutionId,
            @Valid @RequestBody UpdateInstitutionRequest req) {
        InstitutionResponse response = institutionService.updateInstitution(institutionId, req);
        return ResponseEntity.ok(ApiResponse.success("Institution updated successfully", response));
    }

    @DeleteMapping("/institutions/{institutionId}")
    @Operation(summary = "Soft-delete an institution")
    public ResponseEntity<ApiResponse<Void>> deleteInstitution(@PathVariable UUID institutionId) {
        institutionService.deleteInstitution(institutionId);
        return ResponseEntity.ok(ApiResponse.success("Institution deleted successfully", null));
    }

    @GetMapping("/institutions/{institutionId}/members")
    @Operation(summary = "List organization members and their roles")
    public ResponseEntity<ApiResponse<List<OrgMemberResponse>>> listInstitutionMembers(
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listInstitutionMembers(institutionId)));
    }

    @PutMapping("/institutions/{institutionId}/members/{userId}/role")
    @Operation(summary = "Change a member's role within the organization")
    public ResponseEntity<ApiResponse<OrgMemberResponse>> updateInstitutionMemberRole(
            @PathVariable UUID institutionId,
            @PathVariable UUID userId,
            @RequestParam String role) {
        OrgMemberResponse response = platformAdminService.updateInstitutionMemberRole(institutionId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Member role updated", response));
    }

    // ── Live Classes ──

    @GetMapping("/live-classes")
    @Operation(summary = "List live classes across the platform")
    public ResponseEntity<ApiResponse<PageResponse<LiveClassSummaryResponse>>> listLiveClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        PageResponse<LiveClassSummaryResponse> response = platformAdminService.listLiveClasses(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Payments & Entitlements ──

    @GetMapping("/payments")
    @Operation(summary = "List payments across the platform")
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryResponse>>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        PageResponse<PaymentSummaryResponse> response = platformAdminService.listPayments(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Certificates ──

    @GetMapping("/certificates")
    @Operation(summary = "List certificates issued across the platform (search + filters)")
    public ResponseEntity<ApiResponse<PageResponse<CertificateSummaryResponse>>> listCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        java.time.LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        java.time.LocalDateTime toDate = to != null ? to.plusDays(1).atStartOfDay().minusNanos(1) : null;
        PageResponse<CertificateSummaryResponse> response = platformAdminService.listCertificates(
                page, size, search, status, type, institutionId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/certificates/overview")
    @Operation(summary = "Real certificate counts by status/type + verification activity (last 30 days)")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.CertificateOverviewResponse>> certificateOverview() {
        return ResponseEntity.ok(ApiResponse.success(certificateGovernanceService.overview()));
    }

    @GetMapping("/certificates/{certificateId}")
    @Operation(summary = "Full certificate detail: recipient, issuer, template and signatories")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.CertificateDetailResponse>> certificateDetail(
            @PathVariable UUID certificateId) {
        return ResponseEntity.ok(ApiResponse.success(certificateGovernanceService.getCertificateDetail(certificateId)));
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
    @Operation(summary = "Get platform-wide audit logs (optionally filtered by a specific entity id)")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId) {
        List<AuditLogResponse> response = platformAdminService.getAuditLogs(page, size, action, entityType, entityId);
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

    // ── Enhanced Dashboard ──

    @GetMapping("/dashboard/enhanced")
    @Operation(summary = "Get enhanced platform dashboard with all KPIs")
    public ResponseEntity<ApiResponse<EnhancedPlatformDashboardResponse>> getEnhancedDashboard() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.getEnhancedDashboard()));
    }

    // ── Services ──

    @GetMapping("/services")
    @Operation(summary = "List platform services")
    public ResponseEntity<ApiResponse<PageResponse<ServiceSummaryResponse>>> listServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listServices(page, size, category)));
    }

    @PostMapping("/services")
    @Operation(summary = "Create a platform service")
    public ResponseEntity<ApiResponse<ServiceSummaryResponse>> createService(@Valid @RequestBody ServiceCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Service created", platformAdminService.createService(req)));
    }

    @PutMapping("/services/{serviceId}")
    @Operation(summary = "Update a platform service")
    public ResponseEntity<ApiResponse<ServiceSummaryResponse>> updateService(
            @PathVariable UUID serviceId, @Valid @RequestBody ServiceCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Service updated", platformAdminService.updateService(serviceId, req)));
    }

    // ── Incidents ──

    @GetMapping("/incidents")
    @Operation(summary = "List platform incidents")
    public ResponseEntity<ApiResponse<PageResponse<IncidentSummaryResponse>>> listIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listIncidents(page, size, status, severity)));
    }

    @PostMapping("/incidents")
    @Operation(summary = "Create a platform incident")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> createIncident(@RequestBody IncidentCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Incident created", platformAdminService.createIncident(req)));
    }

    @PutMapping("/incidents/{incidentId}/status")
    @Operation(summary = "Update incident status")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> updateIncidentStatus(
            @PathVariable UUID incidentId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.updateIncidentStatus(incidentId, status, notes)));
    }

    // ── Platform Config ──

    @GetMapping("/config")
    @Operation(summary = "List platform configuration")
    public ResponseEntity<ApiResponse<List<PlatformConfigResponse>>> listConfig(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listConfig(category)));
    }

    @PutMapping("/config/{key}")
    @Operation(summary = "Update platform configuration")
    public ResponseEntity<ApiResponse<PlatformConfigResponse>> updateConfig(
            @PathVariable String key,
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request) {
        String actor = request.getAttribute("userEmail") != null ? request.getAttribute("userEmail").toString() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Config updated", platformAdminService.updateConfig(key, body.get("value"), actor)));
    }

    // ── Notifications ──

    @GetMapping("/notifications")
    @Operation(summary = "List platform notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationSummaryResponse>>> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listNotifications(page, size)));
    }

    @PostMapping("/notifications")
    @Operation(summary = "Send a platform notification")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> sendNotification(
            @RequestBody NotificationCreateRequest req, HttpServletRequest request) {
        String actor = request.getAttribute("userEmail") != null ? request.getAttribute("userEmail").toString() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Notification sent", platformAdminService.sendNotification(req, actor)));
    }

    // ── Delegations ──

    @GetMapping("/delegations")
    @Operation(summary = "List admin delegations")
    public ResponseEntity<ApiResponse<List<DelegationSummaryResponse>>> listDelegations() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listDelegations()));
    }

    @PostMapping("/delegations")
    @Operation(summary = "Create an admin delegation")
    public ResponseEntity<ApiResponse<DelegationSummaryResponse>> createDelegation(@RequestBody DelegationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Delegation created", platformAdminService.createDelegation(req)));
    }

    @PutMapping("/delegations/{delegationId}/revoke")
    @Operation(summary = "Revoke an admin delegation")
    public ResponseEntity<ApiResponse<String>> revokeDelegation(
            @PathVariable UUID delegationId,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        UUID actorId = request.getAttribute("userId") != null ? UUID.fromString(request.getAttribute("userId").toString()) : null;
        platformAdminService.revokeDelegation(delegationId, actorId, reason);
        return ResponseEntity.ok(ApiResponse.success("Delegation revoked", null));
    }

    // ── Verifications ──

    @GetMapping("/verifications/pending")
    @Operation(summary = "List pending verification records")
    public ResponseEntity<ApiResponse<List<VerificationSummaryResponse>>> listPendingVerifications() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listPendingVerifications()));
    }

    @PutMapping("/verifications/{verificationId}/review")
    @Operation(summary = "Review a verification record")
    public ResponseEntity<ApiResponse<VerificationSummaryResponse>> reviewVerification(
            @PathVariable UUID verificationId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        UUID actorId = request.getAttribute("userId") != null ? UUID.fromString(request.getAttribute("userId").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.reviewVerification(verificationId, actorId, status, notes)));
    }

    // ── Entitlements ──

    @GetMapping("/entitlements")
    @Operation(summary = "List platform entitlements")
    public ResponseEntity<ApiResponse<PageResponse<EntitlementSummaryResponse>>> listEntitlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listEntitlements(page, size, status)));
    }

    // ── Platform Learning/Content/Event/Media/Resources ──

    @GetMapping("/courses")
    @Operation(summary = "List courses across platform (governance)")
    public ResponseEntity<ApiResponse<PageResponse<PlatformCourseResponse>>> listPlatformCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listPlatformCourses(page, size, search)));
    }

    @GetMapping("/events")
    @Operation(summary = "List events across platform (governance)")
    public ResponseEntity<ApiResponse<PageResponse<PlatformEventResponse>>> listPlatformEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listPlatformEvents(page, size, search)));
    }

    @GetMapping("/media")
    @Operation(summary = "List media assets across platform (governance)")
    public ResponseEntity<ApiResponse<PageResponse<PlatformMediaResponse>>> listPlatformMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listPlatformMedia(page, size)));
    }

    @GetMapping("/resources")
    @Operation(summary = "List resources across platform (governance)")
    public ResponseEntity<ApiResponse<PageResponse<PlatformResourceResponse>>> listPlatformResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listPlatformResources(page, size)));
    }

    // ── Provider Quotas / Entitlements ──

    @GetMapping("/providers/{providerId}/quotas")
    @Operation(summary = "List provider service entitlements and seat quotas")
    public ResponseEntity<ApiResponse<List<ProviderQuotaResponse>>> listProviderQuotas(@PathVariable UUID providerId) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listProviderQuotas(providerId)));
    }

    @PutMapping("/entitlements/{entitlementId}")
    @Operation(summary = "Update provider entitlement quota (max seats / status)")
    public ResponseEntity<ApiResponse<ProviderQuotaResponse>> updateProviderEntitlement(
            @PathVariable UUID entitlementId,
            @RequestBody EntitlementUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Entitlement updated", platformAdminService.updateProviderEntitlement(entitlementId, request)));
    }

    // ── Institution Lifecycle (offboarding) ──

    @PutMapping("/institutions/{institutionId}/lifecycle")
    @Operation(summary = "Update institution lifecycle status (ACTIVE/SUSPENDED/DEACTIVATED/ARCHIVED)")
    public ResponseEntity<ApiResponse<InstitutionSummaryResponse>> updateInstitutionLifecycle(
            @PathVariable UUID institutionId,
            @RequestBody LifecycleStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lifecycle updated", platformAdminService.updateInstitutionLifecycle(institutionId, request.getStatus())));
    }

    // ── Commerce: Sponsored Seats (Payment Model B/C) ──

    @PostMapping("/commerce/sponsor-seats")
    @Operation(summary = "Grant sponsored seats from a provider package (spec §36)")
    public ResponseEntity<ApiResponse<SponsorGrantResponse>> grantSponsoredSeats(
            @Valid @RequestBody SponsorSeatGrantRequest request) {
        UUID grantedBy = null;
        return ResponseEntity.ok(ApiResponse.success("Seats granted",
                platformCommerceService.grantSponsoredSeats(request, grantedBy)));
    }

    @PutMapping("/commerce/entitlements/{entitlementId}/revoke")
    @Operation(summary = "Revoke a sponsored entitlement and free its seat")
    public ResponseEntity<ApiResponse<String>> revokeSponsoredEntitlement(@PathVariable UUID entitlementId) {
        platformCommerceService.revokeSponsoredEntitlement(entitlementId, null);
        return ResponseEntity.ok(ApiResponse.success("Entitlement revoked", "REVOKED"));
    }

    // ── Support Cases (M26) ──

    @GetMapping("/support/tickets")
    @Operation(summary = "List platform support tickets")
    public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> listSupportTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listSupportTickets(page, size, status)));
    }

    @PutMapping("/support/tickets/{ticketId}/status")
    @Operation(summary = "Advance support ticket status (OPEN → ASSIGNED → INVESTIGATING → RESOLVED → CLOSED)")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateSupportTicketStatus(
            @PathVariable UUID ticketId,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated",
                platformAdminService.updateSupportTicketStatus(ticketId, status)));
    }

    // ── Content Moderation (M10/M11) ──

    @GetMapping("/moderation/reports")
    @Operation(summary = "List content reports (moderation queue)")
    public ResponseEntity<ApiResponse<PageResponse<ContentReportResponse>>> listContentReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listContentReports(page, size, status)));
    }

    @PostMapping("/moderation/reports")
    @Operation(summary = "Create a content report")
    public ResponseEntity<ApiResponse<ContentReportResponse>> createContentReport(
            @Valid @RequestBody ContentReportCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Report created", platformAdminService.createContentReport(request)));
    }

    @PutMapping("/moderation/reports/{reportId}/action")
    @Operation(summary = "Act on a content report (REVIEWING/RESOLVED/DISMISSED)")
    public ResponseEntity<ApiResponse<ContentReportResponse>> actOnContentReport(
            @PathVariable UUID reportId,
            @RequestBody ContentReportActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Report updated",
                platformAdminService.actOnContentReport(reportId, request)));
    }

    // ── Data Governance Export (M24) ──

    @GetMapping("/export")
    @Operation(summary = "Export platform data as CSV (users|institutions|audit) — audit-logged")
    public ResponseEntity<byte[]> exportPlatformData(@RequestParam(defaultValue = "users") String type) {
        String csv = platformAdminService.exportPlatformData(type);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=platform_" + type.toLowerCase() + "_export.csv")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ── BATCH 13: Integrations, Webhooks, Backup, Policy, Governance, Admins, Features ──

    private final tz.elmkusoma.administration.service.PlatformIntegrationService integrationService;
    private final tz.elmkusoma.administration.service.BackupStatusService backupStatusService;
    private final tz.elmkusoma.administration.service.DataGovernanceService dataGovernanceService;
    private final tz.elmkusoma.administration.service.PlatformPolicyService policyService;

    @GetMapping("/integrations")
    @Operation(summary = "List integration registry status (no secrets)")
    public ResponseEntity<ApiResponse<List<IntegrationStatusResponse>>> listIntegrations() {
        return ResponseEntity.ok(ApiResponse.success(integrationService.listIntegrations()));
    }

    @PostMapping("/integrations/{key}/probe")
    @Operation(summary = "Run a live probe against an integration")
    public ResponseEntity<ApiResponse<IntegrationStatusResponse>> probeIntegration(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success("Probe complete", integrationService.probe(key)));
    }

    @GetMapping("/webhook-events")
    @Operation(summary = "List recorded webhook events (payment / livekit)")
    public ResponseEntity<ApiResponse<PageResponse<WebhookEventResponse>>> listWebhookEvents(
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "false") boolean failedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                integrationService.listWebhookEvents(source, failedOnly, page, size)));
    }

    @GetMapping("/backup/status")
    @Operation(summary = "Real backup status from status file and dump directory (never fabricated)")
    public ResponseEntity<ApiResponse<BackupStatusResponse>> backupStatus() {
        return ResponseEntity.ok(ApiResponse.success(backupStatusService.getBackupStatus()));
    }

    @GetMapping("/policies")
    @Operation(summary = "List central platform policy flags")
    public ResponseEntity<ApiResponse<List<PolicyFlagResponse>>> listPolicies() {
        return ResponseEntity.ok(ApiResponse.success(policyService.listPolicies()));
    }

    @PutMapping("/policies/{key}")
    @Operation(summary = "Update a platform policy flag (true/false)")
    public ResponseEntity<ApiResponse<PolicyFlagResponse>> updatePolicy(
            @PathVariable String key,
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request) {
        String actor = request.getAttribute("userEmail") != null ? request.getAttribute("userEmail").toString() : "admin";
        String value = body.get("value");
        if (value == null || !(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("value must be true or false"));
        }
        var result = platformAdminService.updateConfig(key, value, actor);
        return ResponseEntity.ok(ApiResponse.success("Policy updated", PolicyFlagResponse.builder()
                .key(result.getConfigKey()).value(result.getConfigValue())
                .description(result.getDescription()).category(result.getCategory()).build()));
    }

    @GetMapping("/data-governance/retention")
    @Operation(summary = "Data retention status (audit archive, config, last sweep)")
    public ResponseEntity<ApiResponse<RetentionStatusResponse>> retentionStatus() {
        return ResponseEntity.ok(ApiResponse.success(dataGovernanceService.retentionStatus()));
    }

    @PostMapping("/data-governance/retention/run")
    @Operation(summary = "Run retention sweep (archive old audit logs, purge soft-deleted reports)")
    public ResponseEntity<ApiResponse<RetentionStatusResponse>> runRetentionSweep(HttpServletRequest request) {
        String actor = request.getAttribute("userEmail") != null ? request.getAttribute("userEmail").toString() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Sweep complete", dataGovernanceService.runSweep(actor)));
    }

    @GetMapping("/data-governance/quality")
    @Operation(summary = "Data quality checks (live queries)")
    public ResponseEntity<ApiResponse<List<DataQualityCheckResponse>>> dataQuality() {
        return ResponseEntity.ok(ApiResponse.success(dataGovernanceService.dataQuality()));
    }

    @PutMapping("/support/tickets/{ticketId}/assign")
    @Operation(summary = "Assign a support ticket to an admin user")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assignSupportTicket(
            @PathVariable UUID ticketId,
            @RequestBody java.util.Map<String, UUID> body) {
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned",
                platformAdminService.assignSupportTicket(ticketId, body.get("assigneeId"))));
    }

    @GetMapping("/admins")
    @Operation(summary = "List admin accounts with role permissions")
    public ResponseEntity<ApiResponse<List<AdminAccountResponse>>> listAdmins() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listAdmins()));
    }

    @GetMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Get permissions for a role / admin user id")
    public ResponseEntity<ApiResponse<List<String>>> getRolePermissions(@PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.getRolePermissions(roleId)));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Replace permissions for a role / admin user id")
    public ResponseEntity<ApiResponse<List<String>>> updateRolePermissions(
            @PathVariable UUID roleId,
            @RequestBody RolePermissionUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Permissions updated",
                platformAdminService.updateRolePermissions(roleId, req)));
    }

    @GetMapping("/institutions/{institutionId}/offboarding")
    @Operation(summary = "Provider/institution offboarding checklist")
    public ResponseEntity<ApiResponse<OffboardingChecklistResponse>> offboardingChecklist(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.offboardingChecklist(institutionId)));
    }

    @PostMapping("/content/bulk-action")
    @Operation(summary = "Bulk content action (COURSE|EVENT|RESOURCE × PUBLISH|UNPUBLISH|ARCHIVE|RESTORE)")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> bulkContentAction(
            @RequestBody BulkContentActionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Bulk action applied", platformAdminService.bulkContentAction(req)));
    }

    @GetMapping("/features")
    @Operation(summary = "List platform features with lifecycle status")
    public ResponseEntity<ApiResponse<List<FeatureStatusResponse>>> listFeatures() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.listFeatures()));
    }

    @PutMapping("/features/{key}/status")
    @Operation(summary = "Transition feature lifecycle status")
    public ResponseEntity<ApiResponse<FeatureStatusResponse>> updateFeatureStatus(
            @PathVariable String key,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Feature updated",
                platformAdminService.updateFeatureStatus(key, body.get("status"))));
    }

    @GetMapping("/communications/delivery")
    @Operation(summary = "Notification delivery statistics")
    public ResponseEntity<ApiResponse<CommunicationDeliveryResponse>> communicationDelivery() {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.communicationDelivery()));
    }

    @GetMapping("/analytics/snapshots")
    @Operation(summary = "List stored analytics snapshots")
    public ResponseEntity<ApiResponse<List<AnalyticsSnapshotResponse>>> analyticsSnapshots(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(platformAdminService.analyticsSnapshots(limit)));
    }

    @PostMapping("/analytics/snapshots")
    @Operation(summary = "Generate a new platform analytics snapshot")
    public ResponseEntity<ApiResponse<AnalyticsSnapshotResponse>> createAnalyticsSnapshot() {
        return ResponseEntity.ok(ApiResponse.success("Snapshot created", platformAdminService.createAnalyticsSnapshot()));
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    @Operation(summary = "Platform-level certificate revocation (cross-institution)")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> revokeCertificatePlatform(
            @PathVariable UUID certificateId,
            @RequestBody(required = false) java.util.Map<String, String> body,
            HttpServletRequest request) {
        String actor = request.getAttribute("userEmail") != null ? request.getAttribute("userEmail").toString() : "admin";
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked",
                platformAdminService.revokeCertificatePlatform(certificateId, reason, actor)));
    }

    // ── Certificate Template Governance ──

    @GetMapping("/certificate-templates")
    @Operation(summary = "List certificate templates across the platform (with usage and signatories)")
    public ResponseEntity<ApiResponse<PageResponse<tz.elmkusoma.certificate.dto.PlatformTemplateResponse>>> listCertificateTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                certificateGovernanceService.listTemplates(search, type, institutionId, page, size)));
    }

    @PostMapping("/certificate-templates")
    @Operation(summary = "Create a certificate template for an institution")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.PlatformTemplateResponse>> createCertificateTemplate(
            @RequestBody tz.elmkusoma.certificate.dto.CertificateTemplateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template created",
                certificateGovernanceService.createTemplate(body, actorId(request), actorEmail(request), actorRole(request))));
    }

    @PutMapping("/certificate-templates/{templateId}")
    @Operation(summary = "Update a template (previous content archived as a version)")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.PlatformTemplateResponse>> updateCertificateTemplate(
            @PathVariable UUID templateId,
            @RequestBody tz.elmkusoma.certificate.dto.CertificateTemplateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template updated",
                certificateGovernanceService.updateTemplate(templateId, body, actorId(request), actorEmail(request), actorRole(request))));
    }

    @PutMapping("/certificate-templates/{templateId}/status")
    @Operation(summary = "Activate/deactivate a certificate template")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.PlatformTemplateResponse>> setCertificateTemplateStatus(
            @PathVariable UUID templateId,
            @RequestBody tz.elmkusoma.certificate.dto.CertificateTemplateStatusRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template status updated",
                certificateGovernanceService.setTemplateStatus(templateId, body.getIsActive(),
                        actorId(request), actorEmail(request), actorRole(request))));
    }

    @GetMapping("/certificate-templates/{templateId}/signatories")
    @Operation(summary = "Authorised signatories linked to a template (display order)")
    public ResponseEntity<ApiResponse<List<tz.elmkusoma.certificate.dto.SignatoryResponse>>> getTemplateSignatories(
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(ApiResponse.success(certificateGovernanceService.getTemplateSignatories(templateId)));
    }

    @PutMapping("/certificate-templates/{templateId}/signatories")
    @Operation(summary = "Replace a template's authorised signatories (validates authorisation rules)")
    public ResponseEntity<ApiResponse<List<tz.elmkusoma.certificate.dto.SignatoryResponse>>> replaceTemplateSignatories(
            @PathVariable UUID templateId,
            @RequestBody tz.elmkusoma.certificate.dto.TemplateSignatoriesRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Template signatories updated",
                certificateGovernanceService.replaceTemplateSignatories(templateId, body,
                        actorId(request), actorEmail(request), actorRole(request))));
    }

    @GetMapping("/certificate-templates/{templateId}/versions")
    @Operation(summary = "Archived versions of a template (content coexistence)")
    public ResponseEntity<ApiResponse<List<tz.elmkusoma.certificate.dto.TemplateVersionResponse>>> getTemplateVersions(
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(ApiResponse.success(certificateGovernanceService.getTemplateVersions(templateId)));
    }

    // ── Certificate Signatories ──

    @GetMapping("/certificate-signatories")
    @Operation(summary = "List signatory profiles with authorisation scope")
    public ResponseEntity<ApiResponse<PageResponse<tz.elmkusoma.certificate.dto.SignatoryResponse>>> listSignatories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                certificateGovernanceService.listSignatories(search, status, institutionId, page, size)));
    }

    @PostMapping("/certificate-signatories")
    @Operation(summary = "Create a signatory profile with authorisation scope")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.SignatoryResponse>> createSignatory(
            @RequestBody tz.elmkusoma.certificate.dto.SignatoryRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Signatory created",
                certificateGovernanceService.createSignatory(body, actorId(request), actorEmail(request), actorRole(request))));
    }

    @PutMapping("/certificate-signatories/{signatoryId}")
    @Operation(summary = "Update a signatory profile / authorisation")
    public ResponseEntity<ApiResponse<tz.elmkusoma.certificate.dto.SignatoryResponse>> updateSignatory(
            @PathVariable UUID signatoryId,
            @RequestBody tz.elmkusoma.certificate.dto.SignatoryRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Signatory updated",
                certificateGovernanceService.updateSignatory(signatoryId, body,
                        actorId(request), actorEmail(request), actorRole(request))));
    }

    @DeleteMapping("/certificate-signatories/{signatoryId}")
    @Operation(summary = "Soft-delete a signatory (links removed, audit preserved)")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> deleteSignatory(
            @PathVariable UUID signatoryId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Signatory removed",
                certificateGovernanceService.deleteSignatory(signatoryId,
                        actorId(request), actorEmail(request), actorRole(request))));
    }
}
