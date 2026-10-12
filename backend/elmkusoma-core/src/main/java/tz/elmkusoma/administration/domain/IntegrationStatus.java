package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IntegrationStatus extends BaseEntity {

    @Column(name = "integration_key", nullable = false, unique = true, length = 50)
    private String integrationKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 50)
    private String category;

/**
     * Length 32, not 20: the honest email vocabulary includes CONFIGURED_UNVERIFIED (21 chars),
     * which the old width rejected outright - a truthful status would have surfaced as a 500
     * instead. V154 widens the same columns on migrated databases; declaring it here keeps the
     * test schema (built from the entity, Flyway disabled) identical to production.
     */
    @Column(name = "connection_status", nullable = false, length = 32)
    @Builder.Default
    private String connectionStatus = "UNKNOWN";

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Integer failureCount = 0;

    @Column(name = "webhook_status", length = 20)
    private String webhookStatus;

    @Column(name = "retry_status", length = 20)
    private String retryStatus;

    @Column(name = "config_status", nullable = false, length = 32)
    @Builder.Default
    private String configStatus = "UNKNOWN";

    @Column(columnDefinition = "TEXT")
    private String diagnostics;

    @Column(name = "probe_detail", columnDefinition = "TEXT")
    private String probeDetail;
}
