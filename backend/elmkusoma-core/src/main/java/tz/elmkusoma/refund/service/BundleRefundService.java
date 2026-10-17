package tz.elmkusoma.refund.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Payment;
import tz.elmkusoma.parent.domain.RefundRequest;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.service.RefundService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BR-02 entry point: a partial bundle refund, requested by Platform Admin and handled by the
 * existing refund lifecycle.
 *
 * <p>No new refund machinery is introduced. A request goes through
 * {@code RefundService} as REQUESTED and then travels the established
 * REQUESTED -> UNDER_REVIEW -> APPROVED -> PROCESSING -> COMPLETED path, so approval, execution
 * and failure stay distinct states rather than collapsing into one "refund" action.
 *
 * <p>Authorisation is enforced here, on the server. A hidden control is not access control: the
 * check is against the caller's actual role, and the amount bound is checked before any state
 * changes, so an over-refund cannot even be recorded as pending.
 */
@Service
@RequiredArgsConstructor
public class BundleRefundService {

    /** Roles permitted to request a partial bundle refund under BR-02. */
    private static final String AUTHORISED_ROLES = "ADMIN,NATIONAL_ADMIN";

    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    /**
     * Requests a partial refund of a bundle payment.
     *
     * @param callerRoles comma-separated role names of the authenticated caller
     * @throws ForbiddenException     when the caller is not a Platform Admin
     * @throws IllegalArgumentException when the amount is not positive or exceeds what remains
     */
    @Transactional
    public RefundRequest requestPartialRefund(UUID paymentId, BigDecimal amount,
                                               UUID requestedBy, String reason,
                                               String callerRoles) {
        requirePlatformAdmin(callerRoles);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        // The calculation is performed, and the bound enforced, before anything is written. An
        // over-refund must not reach the ledger even as a pending request somebody could later
        // approve without re-checking.
        BundleRefundCalculator.RefundBreakdown breakdown =
                BundleRefundCalculator.breakdown(payment, amount);

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "Only a completed payment can be refunded; this one is " + payment.getStatus());
        }

        return refundService.requestRefund(paymentId, requestedBy, breakdown.refundedTotal(),
                reason == null || reason.isBlank()
                        ? "Partial bundle refund (BR-02)"
                        : reason,
                null);
    }

    /** What remains refundable, for a UI to show before a request is made. */
    @Transactional(readOnly = true)
    public BigDecimal eligibleRemaining(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return BundleRefundCalculator.eligibleRemaining(payment);
    }

    private void requirePlatformAdmin(String callerRoles) {
        if (callerRoles == null || callerRoles.isBlank()) {
            throw new ForbiddenException("A refund requires an authenticated Platform Admin");
        }
        for (String role : callerRoles.split(",")) {
            String trimmed = role.trim().toUpperCase(java.util.Locale.ROOT);
            if (AUTHORISED_ROLES.contains(trimmed)) {
                return;
            }
        }
        throw new ForbiddenException(
                "A partial bundle refund may only be requested by a Platform Admin");
    }
}