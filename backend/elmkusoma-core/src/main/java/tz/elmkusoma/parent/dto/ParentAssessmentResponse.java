package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentAssessmentResponse {

    private String studentName;
    private String className;
    private List<AssessmentItem> upcoming;
    private List<AssessmentItem> completed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssessmentItem {
        private String id;
        private String title;
        private String subject;
        private String subjectId;
        Integer totalMarks;
        Integer passMarks;
        Integer score;
        Boolean isPassed;
        private Double percentage;
        private LocalDateTime createdAt;
        private String remarks;
        private String status;
    }
}
