package tz.elmkusoma.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.finance.domain.ReconciliationException;
import tz.elmkusoma.finance.service.FinancialReportingService;
import tz.elmkusoma.finance.service.ReconciliationService;
import tz.elmkusoma.finance.service.TransactionSearchService;

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
    private final TransactionSearchService transactionSearchService;

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
    // Transactions
    // ------------------------------------------------------------------

    @GetMapping("/transactions")
    @Operation(summary = "Search transactions. Every filter is optional; the result is paged.")
    public ResponseEntity<ApiResponse<Page<TransactionSearchService.TransactionRow>>> transactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {

        Pageable pageable = resolvePageable(sortBy, sortDir, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                transactionSearchService.search(criteria(query, status, serviceType, provider,
                        institutionId, parentId, from, to), pageable)));
    }

    @GetMapping("/transactions/export")
    @Operation(summary = "Export the same transaction search as CSV. Bounded and audited.")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        byte[] csv = transactionSearchService.exportCsv(
                criteria(query, status, serviceType, provider, institutionId, parentId, from, to));
        String stamp = java.time.LocalDate.now().toString();
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition",
                        "attachment; filename=transactions_" + stamp + ".csv")
                .body(csv);
    }

    private TransactionSearchService.SearchCriteria criteria(
            String query, String status, String serviceType, String provider,
            UUID institutionId, UUID parentId, LocalDateTime from, LocalDateTime to) {
        return new TransactionSearchService.SearchCriteria(
                blankToNull(query), blankToNull(status), blankToNull(serviceType), blankToNull(provider),
                institutionId, parentId, from, to);
    }

    /**
     * Sorting is restricted to a known column and the page size is capped, because both arrive
     * from the caller: an arbitrary sort property would let a request name a column Hibernate does
     * not expose, and an unbounded page size would be a denial-of-service lever on a table that
     * only grows.
     */
    private Pageable resolvePageable(String sortBy, String sortDir, int page, int size) {
        List<String> allowed = List.of("paidAt", "createdAt", "amount", "status", "serviceType");
        String property = (sortBy == null || !allowed.contains(sortBy)) ? "paidAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Sort sort = Sort.by(direction, property);
        if ("paidAt".equals(property)) {
            // No NULLS LAST here: Hibernate's Criteria API cannot render it, and a payment that
            // has never been paid has no paidAt to sort on. createdAt is always present.
            sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }
        return PageRequest.of(safePage, safeSize, sort);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
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
