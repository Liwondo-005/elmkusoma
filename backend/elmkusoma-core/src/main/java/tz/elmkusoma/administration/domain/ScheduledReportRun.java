package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_report_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScheduledReportRun extends BaseEntity {

    @Column(name = "scheduled_report_id", nullable = false)
    private UUID scheduledReportId;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String error;
}
