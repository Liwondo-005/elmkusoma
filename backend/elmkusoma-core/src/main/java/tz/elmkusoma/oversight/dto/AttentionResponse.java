package tz.elmkusoma.oversight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Governance attention centre (§Nationaladmin.md §12): operational alerts plus
 * governance items (pending verifications, open content reports, data-quality
 * findings) for the caller's jurisdiction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttentionResponse {
    private List<AttentionItem> items;
    private AttentionSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttentionItem {
        private String id;
        /** ATTENDANCE | PERFORMANCE | VERIFICATION | CONTENT_REPORT | DATA_QUALITY */
        private String category;
        private String type;
        private String title;
        private String message;
        /** HIGH | MEDIUM | LOW */
        private String severity;
        private String jurisdiction;
        private String jurisdictionId;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttentionSummary {
        private Long total;
        private Long high;
        private Long medium;
        private Long low;
        private Long attendance;
        private Long performance;
        private Long verification;
        private Long contentReport;
        private Long dataQuality;
    }
}
