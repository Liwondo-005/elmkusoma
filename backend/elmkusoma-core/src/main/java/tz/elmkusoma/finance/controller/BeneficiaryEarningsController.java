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
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.finance.service.EarningsService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Earnings for the person asking - a teacher, trainer, provider or event organiser.
 *
 * <p>Available only because V179 records a beneficiary on each payment. Before that no payment
 * named the person who earned the provider share, so the only honest statement anyone could make
 * was an institution total. Deriving a per-persona figure from a current profile would have
 * fabricated earnings for money that was never attributed to anyone, so nothing was exposed until
 * the attribution actually existed.
 *
 * <p><b>There is deliberately no beneficiary id parameter.</b> A caller can only ever read their own
 * earnings: the beneficiary is taken from the authenticated principal. Accepting a beneficiary id
 * from the request would be a straightforward IDOR - changing one id in a URL would expose another
 * person's revenue, which is the exact failure this controller exists to avoid.
 *
 * <p>Scope is not a UI concern. Hiding the menu entry is not access control; the server refuses to
 * answer for anybody but the caller.
 */
@RestController
@RequestMapping("/v1/earnings")
@RequiredArgsConstructor
@Tag(name = "Personal Earnings")
public class BeneficiaryEarningsController {

    private final EarningsService earningsService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', "
            + "'PROVIDER_ADMIN', 'STUDENT')")
    @Operation(summary = "Earnings attributable to the authenticated user. Profit is never reported.")
    public ResponseEntity<ApiResponse<EarningsService.EarningsSummary>> myEarnings(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        // The beneficiary is the caller, never the request. Anyone who can authenticate sees only
        // money recorded as theirs.
        return ResponseEntity.ok(ApiResponse.success(
                earningsService.summaryForBeneficiary(userId, from, to)));
    }
}