package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "institution_services", uniqueConstraints = @UniqueConstraint(columnNames = {"institution_id", "feature_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InstitutionService extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "feature_key", nullable = false, length = 60)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "configuration", columnDefinition = "JSONB")
    private String configuration;

    @Column(name = "enabled_at")
    private java.time.LocalDateTime enabledAt;

    @Column(name = "enabled_by")
    private UUID enabledBy;
}