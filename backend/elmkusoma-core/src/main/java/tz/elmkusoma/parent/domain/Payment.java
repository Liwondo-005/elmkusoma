package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "TZS";

    private String description;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "service_id")
    private UUID serviceId;

    private String provider;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    // -------------------------------------------------------------------------
    // Terms this transaction was sold under. Snapshotted at initiation so a later
    // price or policy change can never rewrite what was actually charged - a receipt
    // read back a year later has to show the price the payer agreed to, not today's.
    // -------------------------------------------------------------------------

    @Column(name = "policy_key", length = 100)
    private String policyKey;

    @Column(name = "policy_version")
    private Integer policyVersion;

    /** Catalogue price before any discount. */
    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount;

    /** Amount deducted by an approved discount. Never negative, never invented. */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** What was actually charged - gross minus discount. */
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    /** Access length this purchase buys, in days, as resolved at initiation. */
    @Column(name = "access_days")
    private Integer accessDays;

    /** Cumulative refunded against this payment. Partial refunds accumulate here. */
    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "receipt_number", length = 64)
    private String receiptNumber;

    /**
     * Client-supplied idempotency key. Two concurrent initiation attempts for the same purchase
     * must resolve to one payment, enforced by a partial unique index rather than by hope.
     */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    // -------------------------------------------------------------------------
    // Revenue allocation, computed once at confirmation and stamped here.
    //
    // Reading the policy that is live today and applying it to last month's sale would silently
    // recalculate history. A NULL commission means "no approved rate existed at the time", which
    // is a genuinely different fact from a commission of zero.
    // -------------------------------------------------------------------------

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "provider_share_amount", precision = 12, scale = 2)
    private BigDecimal providerShareAmount;

    /** Allocation status at confirmation: RESOLVED, NO_ACTIVE_POLICY, RATE_NOT_APPROVED, ... */
    @Column(name = "commission_status", length = 40)
    private String commissionStatus;

    @Column(name = "commission_policy_key", length = 100)
    private String commissionPolicyKey;

    @Column(name = "commission_policy_version")
    private Integer commissionPolicyVersion;

    /** True only when an approved, rate-bearing policy produced a real allocation. */
    public boolean hasResolvedAllocation() {
        return commissionAmount != null && commissionPolicyKey != null;
    }

    public boolean isFullyRefunded() {
        return refundedAmount != null && amount != null
                && refundedAmount.compareTo(amount) >= 0;
    }

    /** Amount still refundable, never negative. */
    public BigDecimal refundableAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal refunded = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
        BigDecimal remaining = amount.subtract(refunded);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }
}
