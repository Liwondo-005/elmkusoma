package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "provider_service_entitlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProviderServiceEntitlement extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "seats_used")
    private Integer seatsUsed = 0;

    @Column(name = "max_seats")
    private Integer maxSeats;

    @Column(name = "starts_at")
    private java.time.LocalDateTime startsAt;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;
}
