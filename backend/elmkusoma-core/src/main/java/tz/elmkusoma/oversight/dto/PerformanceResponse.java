package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceResponse {
    private Double overallAverage;
    private Double passRate;
    private Long totalAssessments;
    private Long totalReportCards;
    private List<SchoolPerformance> schoolPerformance;
    private List<SubjectPerformance> subjectPerformance;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolPerformance {
        private String institutionId;
        private String institutionName;
        private String institutionCode;
        private Double averageScore;
        private Double passRate;
        private Long studentCount;
        private Long assessmentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectPerformance {
        private String subjectName;
        private String subjectCode;
        private Double averageScore;
        private Double passRate;
        private Long assessmentCount;
    }
}
