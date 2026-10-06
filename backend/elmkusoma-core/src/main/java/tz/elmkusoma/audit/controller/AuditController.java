package tz.elmkusoma.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.audit.dto.*;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
// §Nationaladmin.md §22 — NATIONAL_ADMIN gets read-only compliance oversight.
// Write endpoints re-narrow below with a method-level @PreAuthorize (method wins
// over class level), so mutations stay ADMIN/INSTITUTION_ADMIN only.
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'NATIONAL_ADMIN')")
@Tag(name = "Audit & Compliance", description = "Audit logs, activity feeds, security events, and compliance reporting")
public class AuditController {

    private static final String NATIONAL_ADMIN = "NATIONAL_ADMIN";

    private final AuditService auditService;
    private final AuditMapper auditMapper;

    /**
     * NATIONAL_ADMIN oversees every institution, so its reads are platform-wide
     * (null institution scope). Every other caller stays pinned to the institution
     * resolved from its own membership — a missing institution is refused rather
     * than silently widening the scope.
     */
    private UUID resolveScopeInstitution(UUID institutionId, String userRole) {
        if (NATIONAL_ADMIN.equals(userRole)) {
            return null;
        }
        if (institutionId == null) {
            throw new tz.elmkusoma.exception.ForbiddenException("audit", "access");
        }
        return institutionId;
    }

    // ── Audit Logs ──

    @GetMapping("/logs")
    @Operation(summary = "Get audit logs by institution or date range")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID scopeId = resolveScopeInstitution(institutionId, userRole);

        if (from != null && to != null) {
            List<AuditLogResponse> response = auditService.getAuditLogsByDateRange(scopeId, from, to);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        Page<AuditLogResponse> response = auditService.getAuditLogs(scopeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByUser(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        Page<AuditLogResponse> response = auditService.getAuditLogsByUser(scopeId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/logs/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByEntity(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        List<AuditLogResponse> response = auditService.getAuditLogsByEntity(scopeId, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Activity Feed ──

    @GetMapping("/activity")
    @Operation(summary = "Get activity feed for institution")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getActivityFeed(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        Page<ActivityFeedResponse> response = auditService.getActivityFeed(scopeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/activity/user/{userId}")
    @Operation(summary = "Get activity feed for a specific user")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getActivityFeedByUser(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        Page<ActivityFeedResponse> response = auditService.getActivityFeedByUser(scopeId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    // ── Security Events ──

    @GetMapping("/security")
    @Operation(summary = "Get security events for institution")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getSecurityEvents(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        Page<SecurityEventResponse> response = auditService.getSecurityEvents(scopeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/security/unresolved")
    @Operation(summary = "Get unresolved security events")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getUnresolvedSecurityEvents(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        List<SecurityEventResponse> response = auditService.getUnresolvedSecurityEvents(scopeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/security/{eventId}/resolve")
    // Write stays ADMIN/INSTITUTION_ADMIN — NATIONAL_ADMIN has read-only oversight.
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    @Operation(summary = "Resolve a security event")
    public ResponseEntity<ApiResponse<SecurityEventResponse>> resolveSecurityEvent(
            @PathVariable UUID eventId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        var event = auditService.resolveSecurityEvent(eventId, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Security event resolved", auditMapper.toSecurityEventResponse(event)));
    }

    // ── Compliance Reporting ──

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance report for institution")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> getComplianceReport(
            @RequestAttribute(required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        UUID scopeId = resolveScopeInstitution(institutionId, userRole);
        ComplianceReportResponse response = auditService.getComplianceReport(scopeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
