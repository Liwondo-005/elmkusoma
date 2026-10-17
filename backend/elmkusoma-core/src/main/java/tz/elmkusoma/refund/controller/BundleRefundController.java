package tz.elmkusoma.refund.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.refund.service.BundleRefundService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BR-02: requesting a partial bundle refund.
 *
 * <p>Authorisation is {@code @PreAuthorize}, the same mechanism the existing Platform Admin refund
 * endpoints use, so this cannot be reached without an authenticated administrator role. The
 * service re-checks independently using the real granted authorities rather than a client-supplied
 * value - a hidden control is not access control, and a service that trusted a string from the
 * caller would be theatre.
 *
 * <p>The amount is bounded before anything is written, so an over-refund cannot be recorded even as
 * a pending request that a later approval might wave through unchecked.
 */
@RestController
@RequestMapping("/v1/platform-admin/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundle Refunds")
@PreAuthorize("hasRole('ADMIN')")
public class BundleRefundController {

    private final BundleRefundService bundleRefundService;

    @Getter
    @Setter
    public static class PartialRefundRequest {
        @NotNull private UUID paymentId;
        @NotNull private BigDecimal amount;
        private String reason;
    }

    @GetMapping("/payments/{paymentId}/refundable")
    @Operation(summary = "How much of this payment may still be refunded.")
    public ResponseEntity<ApiResponse<RefundableAmount>> refundable(@PathVariable UUID paymentId) {
        BigDecimal remaining = bundleRefundService.eligibleRemaining(paymentId);
        return ResponseEntity.ok(ApiResponse.success(new RefundableAmount(remaining)));
    }

    @PostMapping("/refunds")
    @Operation(summary = "Request a partial bundle refund. Enters the normal refund lifecycle.")
    public ResponseEntity<ApiResponse<RefundRequest>> request(
            @RequestAttribute("userId") UUID userId,
            Authentication authentication,
            @RequestBody PartialRefundRequest request) {

        // The real granted authorities, not anything from the request body.
        String roles = authentication == null ? "" : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        RefundRequest created = bundleRefundService.requestPartialRefund(
                request.getPaymentId(), request.getAmount(), userId, request.getReason(), roles);

        // Created as REQUESTED, not completed: approval, execution and failure stay distinct
        // states in the established lifecycle.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    /** The eligible balance. Null currency means the payment carried none. */
    public record RefundableAmount(BigDecimal amount) {
    }
}