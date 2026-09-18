package tz.elmkusoma.audit.dto;

import java.util.List;

public class ComplianceReportResponse {

    private Long totalAuditLogs;
    private Long totalSecurityEvents;
    private Long unresolvedSecurityEvents;
    private Long criticalEvents;
    private Long failedLoginAttempts;
    private List<EventTypeCount> topEventTypes;
    private List<SeverityCount> severityBreakdown;

    public Long getTotalAuditLogs() { return totalAuditLogs; }
    public void setTotalAuditLogs(Long totalAuditLogs) { this.totalAuditLogs = totalAuditLogs; }
    public Long getTotalSecurityEvents() { return totalSecurityEvents; }
    public void setTotalSecurityEvents(Long totalSecurityEvents) { this.totalSecurityEvents = totalSecurityEvents; }
    public Long getUnresolvedSecurityEvents() { return unresolvedSecurityEvents; }
    public void setUnresolvedSecurityEvents(Long unresolvedSecurityEvents) { this.unresolvedSecurityEvents = unresolvedSecurityEvents; }
    public Long getCriticalEvents() { return criticalEvents; }
    public void setCriticalEvents(Long criticalEvents) { this.criticalEvents = criticalEvents; }
    public Long getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Long failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public List<EventTypeCount> getTopEventTypes() { return topEventTypes; }
    public void setTopEventTypes(List<EventTypeCount> topEventTypes) { this.topEventTypes = topEventTypes; }
    public List<SeverityCount> getSeverityBreakdown() { return severityBreakdown; }
    public void setSeverityBreakdown(List<SeverityCount> severityBreakdown) { this.severityBreakdown = severityBreakdown; }

    public static ComplianceReportResponse of(Long totalAuditLogs, Long totalSecurityEvents,
                                              Long unresolvedSecurityEvents, Long criticalEvents,
                                              Long failedLoginAttempts, List<EventTypeCount> topEventTypes,
                                              List<SeverityCount> severityBreakdown) {
        ComplianceReportResponse resp = new ComplianceReportResponse();
        resp.totalAuditLogs = totalAuditLogs;
        resp.totalSecurityEvents = totalSecurityEvents;
        resp.unresolvedSecurityEvents = unresolvedSecurityEvents;
        resp.criticalEvents = criticalEvents;
        resp.failedLoginAttempts = failedLoginAttempts;
        resp.topEventTypes = topEventTypes;
        resp.severityBreakdown = severityBreakdown;
        return resp;
    }

    public static class EventTypeCount {
        private String eventType;
        private Long count;

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }

        public static EventTypeCount of(String eventType, Long count) {
            EventTypeCount etc = new EventTypeCount();
            etc.eventType = eventType;
            etc.count = count;
            return etc;
        }
    }

    public static class SeverityCount {
        private String severity;
        private Long count;

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }

        public static SeverityCount of(String severity, Long count) {
            SeverityCount sc = new SeverityCount();
            sc.severity = severity;
            sc.count = count;
            return sc;
        }
    }
}