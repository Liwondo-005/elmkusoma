package tz.elmkusoma.subscription.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The commercial record of a recurring purchase: which payment paid for which
 * period, when that period runs, and where in its lifecycle the subscription is.
 *
 * <p>This is deliberately NOT a second entitlement. {@link
 * tz.elmkusoma.parent.domain.Entitlement} remains the single authority on
 * whether a learner may use a service, and it already owns
 * {@code startsAt}/{@code expiresAt}. A Subscription exists because those two
 * questions are not the same one:</p>
 *
 * <ul>
 *   <li>"may this learner use the service now" — access, owned by Entitlement;</li>
 *   <li>"what did this learner pay for, for how long, and is it still
 *       renewing" — a commercial fact, owned here.</li>
 * </ul>
 *
 * <p>Keeping them apart is what stops a renewal from silently re-granting
 * access, and what lets a cancellation or an expiry be reported without
 * rewriting the entitlement history.</p>
 *
 * <p><b>What this class deliberately does not model.</b> Billing period
 * length, grace periods, cancellation effective dates, renewal-failure
 * handling and automatic-renewal consent are all <em>business policy</em> and
 * none of them is decided here. The period is taken from the
 * {@code accessDays} value snapshotted on the payment at initiation — the same
 * approved duration the entitlement already grants — rather than from a second
 * invented rule. {@link #requestCancellation} refuses outright because the
 * effective date is a policy this code is not entitled to choose.</p>
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Subscription extends BaseEntity {

    /** Paying for the first time. The only state a subscription may be created in. */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** The payer has ended the subscription. Terminal for billing purposes. */
    public static final String STATUS_CANCELLED = "CANCELLED";
    /** The paid period ran out and nothing renewed it. */
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** The paying account, matching {@code Payment.parentId}'s convention for the caller. */
    @Column(name = "payer_id", nullable = false)
    private UUID payerId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "service_id")
    private UUID serviceId;

    /**
     * The payment that activated this subscription. One subscription per paid
     * period, so this is unique — a duplicated callback cannot open a second
     * one. Renewal creates the next record rather than mutating this one.
     */
    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    /** The entitlement granted for this same payment, for traceability. */
    @Column(name = "entitlement_id")
    private UUID entitlementId;

    @Column(nullable = false)
    @Builder.Default
    private String status = STATUS_ACTIVE;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Why the subscription ended, as recorded by the acting party. */
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    /** True while the paid period has not run out, whatever the lifecycle status. */
    public boolean isWithinPeriod(LocalDateTime now) {
        return periodStart != null && periodEnd != null
                && !now.isBefore(periodStart) && now.isBefore(periodEnd);
    }

    /**
     * Whether the paid period has elapsed. Evaluated on read as well as written,
     * so a delayed scheduled job can never report an expired period as current.
     */
    public boolean isPeriodElapsed(LocalDateTime now) {
        return periodEnd != null && !now.isBefore(periodEnd);
    }

    /**
     * A state change is refused when it would revive a settled subscription.
     *
     * <p>Only forward movement is allowed, and EXPIRED is absorbing: nothing
     * reopens a period that has run out, which is what stops a stale webhook
     * from resurrecting lapsed access.</p>
     */
    public boolean canTransitionTo(String next) {
        if (next == null || next.equals(this.status)) {
            return false;
        }
        return switch (this.status) {
            case STATUS_ACTIVE -> STATUS_CANCELLED.equals(next) || STATUS_EXPIRED.equals(next);
            case STATUS_CANCELLED -> STATUS_EXPIRED.equals(next);
            case STATUS_EXPIRED -> false;
            default -> false;
        };
    }

    /**
     * Records a cancellation.
     *
     * <p>Refuses when the effective date is not supplied by an authorised
     * caller. Whether cancellation takes effect immediately or at the end of
     * the paid period is a commercial decision this service is not entitled to
     * make on its own, and guessing it would either cut off access the payer
     * already paid for or keep billing someone who asked to stop.</p>
     *
     * @throws IllegalStateException when no policy-supplied effective date is given
     */
    public void requestCancellation(LocalDateTime effectiveAt, String reason) {
        if (effectiveAt == null) {
            throw new IllegalStateException(
                    "A cancellation effective date must be supplied by the approved policy; "
                            + "this service does not choose one");
        }
        // Idempotent. A learner who taps cancel twice, or a retried request, must get the same
        // outcome rather than an error - refusing the second attempt would tell them the
        // cancellation had failed when it had already succeeded. The original effective date and
        // reason are kept, because moving the date on a repeated call would silently extend or
        // shorten a period somebody already paid for.
        if (STATUS_CANCELLED.equals(this.status)) {
            return;
        }
        if (!canTransitionTo(STATUS_CANCELLED)) {
            throw new IllegalStateException(
                    "A subscription in status " + status + " cannot be cancelled");
        }
        this.status = STATUS_CANCELLED;
        this.cancelledAt = effectiveAt;
        this.cancellationReason = reason;
    }

    /**
     * Whether this subscription may still renew after {@code now}.
     *
     * <p>BR-01: cancelling prevents the NEXT renewal. Access itself is unaffected, because it is
     * decided from the period boundary rather than from this status - a cancelled subscription
     * whose paid period has not ended still grants access until it does, which is exactly what the
     * payer paid for.
     *
     * <p>Only the final period is cancelled. When several periods are chained, cancelling must
     * suppress the next renewal without touching any later one.
     */
    public boolean preventsRenewalAfter(LocalDateTime now) {
        return STATUS_CANCELLED.equals(this.status);
    }
}