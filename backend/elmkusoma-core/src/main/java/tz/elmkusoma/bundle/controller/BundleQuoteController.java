package tz.elmkusoma.bundle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.bundle.service.BundleTopUpService;
import tz.elmkusoma.common.ApiResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What this learner would actually pay for a bundle they partly already hold (BR-03).
 *
 * <p>This exists as its own endpoint rather than being folded into the catalog detail for one
 * reason: the honest price depends on who is asking and what they already hold. A catalog listing
 * that quoted a single figure would either overstate what an existing learner owes or understate
 * what a new one does, and the figure that reaches checkout must be the one the server computes
 * for this learner at this moment.
 *
 * <p>The quote is advisory for display. The charge is still settled at purchase time against the
 * authoritative pricing policy, so a policy change between quote and purchase is caught by the
 * backend rather than silently accepted.
 */
@RestController
@RequestMapping("/v1/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundles")
@PreAuthorize("isAuthenticated()")
public class BundleQuoteController {

    private final BundleTopUpService bundleTopUpService;

    @GetMapping("/{bundleId}/quote")
    @Operation(summary = "Price a bundle for one learner, charging only for what they do not hold.")
    public ResponseEntity<ApiResponse<BundleTopUpService.TopUpQuote>> quote(
            @PathVariable UUID bundleId,
            @RequestParam UUID studentId,
            @RequestParam(required = false) String at) {
        // An explicit timestamp is accepted only for tests; production always prices against now.
        LocalDateTime when = (at == null || at.isBlank()) ? LocalDateTime.now() : LocalDateTime.parse(at);
        return ResponseEntity.ok(ApiResponse.success(
                bundleTopUpService.quote(bundleId, studentId, when)));
    }
}