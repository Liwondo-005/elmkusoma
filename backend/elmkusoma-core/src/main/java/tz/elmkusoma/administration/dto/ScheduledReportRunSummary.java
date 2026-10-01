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
public class ScheduledReportRunSummary {
    private UUID id;
    private UUID scheduledReportId;
    private LocalDateTime runAt;
    private String status;
    private String summary;
    private String error;
}
