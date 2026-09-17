package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentSubjectPerformanceResponse {

    private List<SubjectItem> subjects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectItem {
        private String subjectId;
        private String subjectName;
        private Double averageMark;
        private String grade;
        private Double gpa;
        private Integer totalAssessments;
        private Integer completedAssessments;
        private String trend;
    }
}
