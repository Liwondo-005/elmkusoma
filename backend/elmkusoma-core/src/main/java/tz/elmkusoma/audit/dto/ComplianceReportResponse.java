package tz.elmkusoma.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceReportResponse {

    private Long totalAuditLogs;
    private Long totalSecurityEvents;
    private Long unresolvedSecurityEvents;
    private Long criticalEvents;
    private Long failedLoginAttempts;
    private List<EventTypeCount> topEventTypes;
    private List<SeverityCount> severityBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventTypeCount {
        private String eventType;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeverityCount {
        private String severity;
        private Long count;
    }
}
