package tz.elmkusoma.bundle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

/**
 * A bundle: one purchasable thing that grants several services.
 *
 * <p>There is deliberately no price column. A bundle is just another service type, and its
 * price is resolved through the pricing service exactly like any other product. Storing a price
 * here would quietly create a second, ungoverned pricing path that the approved pricing policies
 * never see - which is how a discounted, unapproved price reaches a checkout.
 *
 * <p>{@code status} is enforced at purchase time, not merely used to hide a row from a listing. A
 * DRAFT or SUSPENDED bundle must refuse the sale outright; presentation is not a rule.
 */
@Entity
@Table(name = "bundles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Bundle extends BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";



    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /** The pricing key. Resolved through the pricing service; never a hard-coded amount. */
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    /** How long the granted entitlements run. Required before a bundle may be published. */
    @Column(name = "access_days")
    private Integer accessDays;

    @Column(name = "status", nullable = false)
    private String status = STATUS_DRAFT;

    /** Only a published bundle may be bought. */
    public boolean isPurchasable() {
        return STATUS_PUBLISHED.equals(status);
    }
}
