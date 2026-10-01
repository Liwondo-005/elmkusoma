package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReportSummary {
    private UUID id;
    private String title;
    private String reportType;
    private String frequency;
    private String status;
    private String recipients;
    private UUID regionId;
    private UUID districtId;
    private String jurisdiction;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private Integer runCount;
    private LocalDateTime createdAt;
}
