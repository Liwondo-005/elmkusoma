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

    @Column(name = "connection_status", nullable = false, length = 20)
    private String connectionStatus = "UNKNOWN";

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "webhook_status", length = 20)
    private String webhookStatus;

    @Column(name = "retry_status", length = 20)
    private String retryStatus;

    @Column(name = "config_status", nullable = false, length = 20)
    private String configStatus = "UNKNOWN";

    @Column(columnDefinition = "TEXT")
    private String diagnostics;

    @Column(name = "probe_detail", columnDefinition = "TEXT")
    private String probeDetail;
}
