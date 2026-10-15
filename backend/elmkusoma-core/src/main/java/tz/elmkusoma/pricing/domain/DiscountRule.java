package tz.elmkusoma.pricing.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One versioned discount rule.
 *
 * <p>A discount is an authorized exception to a price, not a free-for-all, so a rule is born
 * {@code DRAFT} and only an approved {@code ACTIVE} rule is ever applied. Exactly one mechanism
 * is defined per rule - a percentage <em>or</em> a fixed amount, never both and never neither -
 * enforced by a CHECK constraint so an ambiguous rule cannot exist at all.</p>
 *
 * <p>Usage is bounded: {@code maxUses} caps how many times a rule may be redeemed, and
 * {@code validFrom}/{@code valid_until} bound it in time. Neither is set by the seed data,
 * because no discount has been approved.</p>
 */
@Entity
@Table(name = "discount_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DiscountRule extends BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RETIRED = "RETIRED";

    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_DRAFT;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "service_type", length = 50)
    private String serviceType;

    @Column(name = "percent_off", precision = 5, scale = 2)
    private BigDecimal percentOff;

    @Column(name = "fixed_amount_off", precision = 12, scale = 2)
    private BigDecimal fixedAmountOff;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(length = 500)
    private String description;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isCurrentlyInWindow(LocalDateTime now) {
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || now.isBefore(validUntil);
    }

    public boolean hasRemainingUses() {
        return maxUses == null || usedCount == null || usedCount < maxUses;
    }
}
