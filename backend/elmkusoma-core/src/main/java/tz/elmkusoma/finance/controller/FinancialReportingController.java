package tz.elmkusoma.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.service.FinancialReportingService;
import tz.elmkusoma.finance.service.ReconciliationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform Admin financial command centre and reconciliation.
 *
 * <p>Restricted to the Platform Admin. Every figure served here is derived from stored
 * transactions; where a figure cannot be computed honestly it is returned as
 * {@code availability: UNAVAILABLE} with the reason attached, rather than as a plausible-looking
 * number. Profit is permanently in that state until an approved accounting methodology exists.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/financial")
@RequiredArgsConstructor
@Tag(name = "Platform Admin - Financial Command Center")
@PreAuthorize("hasRole('ADMIN')")
public class FinancialReportingController {

    private final FinancialReportingService financialReportingService;
    private final ReconciliationService reconciliationService;

    @GetMapping("/summary")
    @Operation(summary = "Financial summary over a period. Unavailable metrics explain why.")
    public ResponseEntity<ApiResponse<FinancialReportingService.FinancialSummary>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(required = false) Integer lastNDays) {

        FinancialReportingService.UUIDRange range;
        if (from != null || to != null) {
            range = FinancialReportingService.UUIDRange.of(from, to);
        } else if (lastNDays != null && lastNDays > 0) {
            range = FinancialReportingService.UUIDRange.lastNDays(lastNDays);
        } else {
            range = FinancialReportingService.UUIDRange.lastNDays(30);
        }
        return ResponseEntity.ok(ApiResponse.success(financialReportingService.summaryFor(range)));
    }

    // ------------------------------------------------------------------
    // Reconciliation
    // ------------------------------------------------------------------

    @PostMapping("/reconciliation/scan")
    @Operation(summary = "Detect inconsistencies. Read-and-record only; nothing is rewritten.")
    public ResponseEntity<ApiResponse<ReconciliationService.ScanResult>> scan() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.scan()));
    }

    @GetMapping("/reconciliation/exceptions")
    @Operation(summary = "Open reconciliation exceptions")
    public ResponseEntity<ApiResponse<List<ReconciliationException>>> openExceptions() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.openExceptions()));
    }

    @PostMapping("/reconciliation/exceptions/{exceptionId}/assign")
    public ResponseEntity<ApiResponse<ReconciliationException>> assign(
            @PathVariable UUID exceptionId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reconciliationService.assign(exceptionId, currentUserId(request))));
    }

    @PostMapping("/reconciliation/exceptions/{exceptionId}/resolve")
    @Operation(summary = "Close an exception with a resolution note. Never edits the financial record.")
    public ResponseEntity<ApiResponse<ReconciliationException>> resolve(
            @PathVariable UUID exceptionId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reconciliationService.resolve(exceptionId, currentUserId(request),
                        body == null ? null : body.get("note"))));
    }

    private UUID currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof UUID uuid) {
            return uuid;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "An authorized Platform Admin action requires an authenticated user");
    }
}
