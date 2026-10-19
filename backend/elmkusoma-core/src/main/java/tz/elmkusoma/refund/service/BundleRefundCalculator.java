package tz.elmkusoma.refund.service;

import tz.elmkusoma.parent.domain.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BR-02: the deterministic calculation behind a partial bundle refund.
 *
 * <p>Deliberately not a component valuation. Pricing what an individual item inside a bundle is
 * "worth" is a commercial decision nobody has approved, so this does not attempt one. What it
 * does instead is bound and apportion using figures the approved policy already produced and that
 * were snapshotted onto the payment at confirmation:
 *
 * <ul>
 *   <li>the <b>eligible amount</b> is the discount-adjusted amount actually collected, minus what
 *       has already been refunded. A refund can never exceed what was paid and never exceed what
 *       remains.</li>
 *   <li>the <b>platform commission</b> reversed is a proportion of the <em>recorded</em>
 *       commission, using the approved policy snapshot on the payment rather than a rate
 *       recomputed today. Recomputing from the current rate would silently rewrite history: a
 *       policy change after the sale must not alter what the original split was.</li>
 *   <li>the <b>provider share</b> reversed is the remainder, so the two always sum back to exactly
 *       the refunded amount. No rounding residue is left unaccounted for.</li>
 * </ul>
 *
 * <p>The same inputs always produce the same output. There is no clock, no randomness and no
 * policy lookup, which is what makes the result auditable after the fact.
 */
public final class BundleRefundCalculator {

    private BundleRefundCalculator() {
    }

    /**
     * What a refund request may draw against, and how it splits.
     *
     * @param requested          the amount being refunded
     * @param eligibleRemaining  net collected minus everything already refunded
     * @param refundedTotal      amount of the refund itself
     * @param commissionReversed platform commission given back with the refund
     * @param providerReversed   provider share given back, the remainder
     */
    public record RefundBreakdown(BigDecimal requested,
                                  BigDecimal eligibleRemaining,
                                  BigDecimal refundedTotal,
                                  BigDecimal commissionReversed,
                                  BigDecimal providerReversed) {
    }

    /**
     * What may still be refunded on this payment.
     *
     * <p>Uses the net amount actually collected rather than the headline amount, because a refund
     * must never hand back more than the payer actually paid after any discount.
     */
    public static BigDecimal eligibleRemaining(Payment payment) {
        BigDecimal net = nz(payment.getNetAmount());
        if (net.signum() == 0) {
            net = nz(payment.getGrossAmount());
        }
        if (net.signum() == 0) {
            net = nz(payment.getAmount());
        }
        return net.subtract(nz(payment.getRefundedAmount())).max(BigDecimal.ZERO);
    }

    /**
     * Splits a refund between the platform and the beneficiary.
     *
     * @throws IllegalArgumentException when the amount is not positive or exceeds what remains,
     *         which is how an over-refund is stopped before any money moves
     */
    public static RefundBreakdown breakdown(Payment payment, BigDecimal requested) {
        if (requested == null || requested.signum() <= 0) {
            throw new IllegalArgumentException("A refund must be for a positive amount");
        }
        BigDecimal remaining = eligibleRemaining(payment);
        if (requested.compareTo(remaining) > 0) {
            // Refusing here is the guard against refunding more than was ever collected.
            throw new IllegalArgumentException(
                    "Refund of " + requested + " exceeds the remaining eligible amount of "
                            + remaining);
        }

        // Commission reversed is a proportion of the commission recorded on this payment. The
        // recorded figures are used deliberately: the policy that produced them was approved for
        // this sale, and today's policy must not rewrite it.
        BigDecimal recordedCommission = nz(payment.getCommissionAmount());
        BigDecimal net = nz(payment.getNetAmount());
        if (net.signum() == 0) {
            net = nz(payment.getGrossAmount());
        }
        if (net.signum() == 0) {
            net = requested;
        }

        BigDecimal commissionReversed = recordedCommission
                .multiply(requested)
                .divide(net, 2, RoundingMode.HALF_UP);

        // The provider share absorbs the rounding residue so the two parts always sum to exactly
        // the refunded amount. Leaving a few cents unassigned would make the ledger disagree with
        // itself.
        BigDecimal providerReversed = requested.subtract(commissionReversed);

        return new RefundBreakdown(requested, remaining, requested,
                commissionReversed, providerReversed);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}