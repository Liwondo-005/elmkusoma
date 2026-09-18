package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentsResponse {
    private Long totalAssessments;
    private Long completedAssessments;
    private Double averageScore;
    private Double passRate;
    private Long pendingGrading;
    private List<SchoolAssessment> schoolAssessments;
    private List<RecentAssessment> recentAssessments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolAssessment {
        private String institutionId;
        private String institutionName;
        private String institutionCode;
        private Long assessmentCount;
        private Long completedCount;
        private Double averageScore;
        private Double passRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentAssessment {
        private String id;
        private String title;
        private String institutionName;
        private String subjectName;
        private String scheduledDate;
        private String status;
        private Long participantCount;
    }
}
