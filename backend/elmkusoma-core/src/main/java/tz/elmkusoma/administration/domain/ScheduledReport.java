package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScheduledReport extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "region_id")
    private UUID regionId;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String frequency;

    @Column(length = 2000)
    private String recipients;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "run_count", nullable = false)
    private Integer runCount = 0;
}
