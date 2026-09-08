package tz.elmkusoma.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.audit.dto.*;
import tz.elmkusoma.audit.mapper.AuditMapper;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit & Compliance", description = "Audit logs, activity feeds, security events, and compliance reporting")
public class AuditController {

    private final AuditService auditService;
    private final AuditMapper auditMapper;

    // ── Audit Logs ──

    @GetMapping("/logs")
    @Operation(summary = "Get audit logs by institution or date range")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @RequestAttribute UUID institutionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (from != null && to != null) {
            List<AuditLogResponse> response = auditService.getAuditLogsByDateRange(institutionId, from, to);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        Page<AuditLogResponse> response = auditService.getAuditLogs(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogResponse> response = auditService.getAuditLogsByUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/logs/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByEntity(
            @RequestAttribute UUID institutionId,
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<AuditLogResponse> response = auditService.getAuditLogsByEntity(institutionId, entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Activity Feed ──

    @GetMapping("/activity")
    @Operation(summary = "Get activity feed for institution")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getActivityFeed(
            @RequestAttribute UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityFeedResponse> response = auditService.getActivityFeed(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/activity/user/{userId}")
    @Operation(summary = "Get activity feed for a specific user")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getActivityFeedByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityFeedResponse> response = auditService.getActivityFeedByUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    // ── Security Events ──

    @GetMapping("/security")
    @Operation(summary = "Get security events for institution")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getSecurityEvents(
            @RequestAttribute UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SecurityEventResponse> response = auditService.getSecurityEvents(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response.getContent()));
    }

    @GetMapping("/security/unresolved")
    @Operation(summary = "Get unresolved security events")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getUnresolvedSecurityEvents(
            @RequestAttribute UUID institutionId) {
        List<SecurityEventResponse> response = auditService.getUnresolvedSecurityEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/security/{eventId}/resolve")
    @Operation(summary = "Resolve a security event")
    public ResponseEntity<ApiResponse<SecurityEventResponse>> resolveSecurityEvent(
            @PathVariable UUID eventId,
            @RequestAttribute("userId") UUID userId) {
        var event = auditService.resolveSecurityEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success("Security event resolved", auditMapper.toSecurityEventResponse(event)));
    }

    // ── Compliance Reporting ──

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance report for institution")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> getComplianceReport(
            @RequestAttribute UUID institutionId) {
        ComplianceReportResponse response = auditService.getComplianceReport(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
