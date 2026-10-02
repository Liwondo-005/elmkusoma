package tz.elmkusoma.oversight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Read-only data-quality report (§Nationaladmin.md §22). Every check is derived
 * from live tables — nothing is persisted, nothing is auto-fixed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityResponse {
    /** Percentage of checks with zero affected records (0–100). */
    private Double score;
    private Long totalIssues;
    private List<Check> checks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Check {
        private String id;
        private String title;
        private String description;
        /** HIGH | MEDIUM | LOW */
        private String severity;
        private Long affectedCount;
        private List<String> sample;
    }
}
