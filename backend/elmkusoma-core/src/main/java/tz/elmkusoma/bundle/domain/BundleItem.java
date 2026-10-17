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

import java.util.UUID;

/**
 * One service inside a bundle.
 *
 * <p>{@code serviceId} is exactly what the entitlement grant keys on, so an item that points at
 * nothing would grant access to nothing while still appearing as purchased. Items are ordered so a
 * bundle presents deterministically, and the unique constraint on (bundle, service) stops the same
 * service being listed twice and therefore granted twice.
 */
@Entity
@Table(name = "bundle_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BundleItem extends BaseEntity {


    @Column(name = "bundle_id", nullable = false)
    private UUID bundleId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "position", nullable = false)
    private Integer position = 0;
}
