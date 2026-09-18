package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertsResponse {
    private List<Alert> alerts;
    private AlertSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Alert {
        private String id;
        private String type;
        private String title;
        private String message;
        private String severity;
        private String institutionName;
        private String institutionId;
        private String indicator;
        private String value;
        private String threshold;
        private LocalDateTime timestamp;
        private String status;
        private String acknowledgedBy;
        private LocalDateTime acknowledgedAt;
        private String resolvedBy;
        private LocalDateTime resolvedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertSummary {
        private Long total;
        private Long high;
        private Long medium;
        private Long low;
        private Long new_;
        private Long acknowledged;
        private Long resolved;
    }
}
