package tz.elmkusoma.pricing.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One versioned row of revenue-sharing / commission policy.
 *
 * <p>Commission is configuration, not code, and crucially it is <b>not invented</b>. Payment.md
 * forbids a universal commission percentage, so a policy is born {@code DRAFT} with a
 * {@code NULL} rate and an authorized business decision is what fills it in. Until an
 * {@code ACTIVE} policy with a rate exists, allocations are reported as <b>unresolved</b> rather
 * than silently treated as zero - a zero commission and an unknown commission look identical on
 * a dashboard but mean completely different things.</p>
 *
 * <p>{@code feeBearer} records who bears the gateway processing fee. It is also {@code NULL} by
 * default: silently assuming either the platform or the provider bears it would invent a
 * business term that has not been agreed.</p>
 */
@Entity
@Table(name = "commission_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommissionPolicy extends BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RETIRED = "RETIRED";

    /** Who bears the gateway processing fee, when that decision has actually been made. */
    public static final String FEE_BEARER_PLATFORM = "PLATFORM";
    public static final String FEE_BEARER_PROVIDER = "PROVIDER";
    public static final String FEE_BEARER_CUSTOMER = "CUSTOMER";

    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    /** DRAFT, ACTIVE or RETIRED. Only ACTIVE rows are used to compute an allocation. */
    @Column(nullable = false, length = 20)
    private String status;

    /** Scope of this policy; null on a provider-specific row. */
    @Column(name = "service_type", length = 50)
    private String serviceType;

    /** Scope of this policy; null on a service-type-wide row. */
    @Column(name = "provider_id")
    private UUID providerId;

    /**
     * Fraction of the net amount taken as platform commission, e.g. {@code 0.150000} for 15%.
     * NULL means "no authorized rate yet" - never zero.
     */
    @Column(name = "commission_rate", precision = 7, scale = 6)
    private BigDecimal commissionRate;

    /** Alternative to a rate: a flat commission per transaction. */
    @Column(name = "fixed_amount", precision = 12, scale = 2)
    private BigDecimal fixedAmount;

    /** PLATFORM, PROVIDER or CUSTOMER. NULL means the decision is still open. */
    @Column(name = "fee_bearer", length = 30)
    private String feeBearer;

    @Column(length = 10)
    @Builder.Default
    private String currency = "TZS";

    @Column(length = 500)
    private String description;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** True when this policy actually states a rate, i.e. is usable for an allocation. */
    public boolean hasRate() {
        return commissionRate != null || fixedAmount != null;
    }
}
