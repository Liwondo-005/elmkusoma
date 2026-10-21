package tz.elmkusoma.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.finance.service.TransactionSearchService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Provider-facing read of this institution's own payment transactions.
 *
 * <p>RO-07: a Provider Admin (or Provider Staff) must see the sales recorded against their
 * institution without gaining any platform-wide reach. The institution scope is never taken
 * from a query parameter or a client-supplied header: the request attribute carries the id
 * the security filters resolved from the caller's own memberships, and
 * {@link InstitutionScopeService} re-checks membership before any row is read. A header
 * naming a foreign institution is rejected outright by the organization-context filter and
 * never reaches this controller; a caller with no institution at all is refused here.</p>
 *
 * <p>Distinct from {@code /v1/institution/earnings/summary}, which reports aggregates for one
 * institution, and from {@code /v1/platform-admin/financial/transactions}, which sees every
 * institution. This serves the underlying rows, read-only, with the same membership fence
 * applied to every role including ADMIN.</p>
 */
@RestController
@RequestMapping("/v1/provider/financial")
@RequiredArgsConstructor
@Tag(name = "Provider Financial")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
public class ProviderFinancialReportingController {

    private final TransactionSearchService transactionSearchService;
    private final InstitutionScopeService scopeService;

    @GetMapping("/transactions")
    @Operation(summary = "Sales transactions for the caller's own institution, paged. "
            + "Scope is derived from the authenticated principal, never from a client-supplied value.")
    public ResponseEntity<ApiResponse<Page<TransactionSearchService.TransactionRow>>> transactions(
            @RequestAttribute(name = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(name = "userId", required = false) UUID userId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Without a server-resolved institution there is nothing honest to return: an
        // unscoped query would leak every institution's sales, so this is a denial.
        if (institutionId == null || userId == null) {
            throw new AccessDeniedException("Access denied: no institution scope for this account");
        }

        // Scope first, exactly as the institution earnings endpoint does. Without this the
        // only thing standing between a provider administrator and every other school's
        // transactions would be the filter chain's attribute resolution.
        scopeService.validateAccess(userId, institutionId, "view_provider_transactions");

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        // institutionId is pinned to the server-resolved scope. No parameter binds to it, so
        // a caller cannot widen or redirect the query by adding one to the request.
        TransactionSearchService.SearchCriteria criteria =
                new TransactionSearchService.SearchCriteria(
                        blankToNull(query), blankToNull(status), blankToNull(serviceType), null,
                        institutionId, null, from, to);

        return ResponseEntity.ok(ApiResponse.success(
                transactionSearchService.search(criteria, pageable)));
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
