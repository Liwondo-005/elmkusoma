package tz.elmkusoma.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.finance.service.EarningsService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Earnings and revenue-split reporting for an institution.
 *
 * <p>Distinct from the Platform Admin command centre, which sees every institution. This endpoint
 * sees one, and the caller must be a member of it: {@link InstitutionScopeService} rejects anyone
 * who is not, and the query is filtered by institution as well, so an administrator of one school
 * cannot read another's revenue by passing a different id.</p>
 *
 * <p>{@code PROVIDER_ADMIN} is included but is not given platform-wide reach: the scope check
 * applies to it exactly as it does to {@code INSTITUTION_ADMIN}, which is the point of the
 * requirement that a Provider Admin must not automatically see the whole platform's finances.</p>
 */
@RestController
@RequestMapping("/v1/institution/earnings")
@RequiredArgsConstructor
@Tag(name = "Institution Earnings")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'PROVIDER_ADMIN')")
public class InstitutionEarningsController {

    private final EarningsService earningsService;
    private final InstitutionScopeService scopeService;

    @GetMapping("/summary")
    @Operation(summary = "Earnings and revenue split for this institution. Profit is never reported.")
    public ResponseEntity<ApiResponse<EarningsService.EarningsSummary>> summary(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        // Scope first. Without this the institutionId header would be the only thing standing
        // between a provider administrator and every other school's revenue.
        scopeService.validateAccess(userId, institutionId, "view_institution_earnings");

        return ResponseEntity.ok(ApiResponse.success(
                earningsService.summaryFor(institutionId, from, to)));
    }
}