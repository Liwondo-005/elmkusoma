package tz.elmkusoma.pricing.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;

/**
 * One versioned row of platform pricing policy.
 *
 * <p>Prices are configuration, not code. Scattering "TZS 2,000" through services would make a
 * price change a redeploy and a historical receipt unreproducible, so every amount the platform
 * charges is read from an ACTIVE row here and stamped onto the transaction when it is created.</p>
 *
 * <p>Two shapes share the table, told apart by which columns are populated:</p>
 * <ul>
 *   <li><b>Band</b> - {@code educationLevel} set with {@code minAmount}/{@code maxAmount}, the
 *       ceiling and floor providers may price within for that level.</li>
 *   <li><b>Default price</b> - {@code serviceType} set with {@code defaultAmount}, the platform's
 *       own starting price for that class of service.</li>
 * </ul>
 *
 * <p>Status is the activation gate. A policy is born {@code DRAFT} and only an authorized
 * approval moves it to {@code ACTIVE}; nothing in this class activates itself, and pricing is
 * never derived from a draft. Retiring a policy leaves the row in place so a historical
 * transaction can still be read back against the version it was sold under.</p>
 */
@Entity
@Table(name = "pricing_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PricingPolicy extends BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RETIRED = "RETIRED";

    /** Groups rows into a coherent policy family, e.g. EDUCATION_LEVEL_BAND or SERVICE_DEFAULT. */
    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    /** DRAFT, ACTIVE or RETIRED. Only ACTIVE rows are ever used to price anything. */
    @Column(nullable = false, length = 20)
    private String status;

    /** Set on a default-price row; null on an education-level band row. */
    @Column(name = "service_type", length = 50)
    private String serviceType;

    /** Set on a band row as the EducationLevel name; null on a default-price row. */
    @Column(name = "education_level", length = 30)
    private String educationLevel;

    /** Floor of an education-level band. Null on default-price rows. */
    @Column(name = "min_amount", precision = 12, scale = 2)
    private BigDecimal minAmount;

    /** Ceiling of an education-level band. Null on default-price rows. */
    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    /** Platform starting price. Null on band rows. */
    @Column(name = "default_amount", precision = 12, scale = 2)
    private BigDecimal defaultAmount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "TZS";

    /** Access length the price buys, in days. Null where the price is not an access pass. */
    @Column(name = "access_days")
    private Integer accessDays;

    @Column(length = 500)
    private String description;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}
