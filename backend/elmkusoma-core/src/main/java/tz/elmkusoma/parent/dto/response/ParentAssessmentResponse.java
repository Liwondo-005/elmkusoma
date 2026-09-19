package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentAssessmentResponse {

    private String studentName;
    private String className;
    private List<AssessmentItem> upcoming;
    private List<AssessmentItem> completed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentItem {
        private String id;
        private String title;
        private String subject;
        private String subjectId;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Integer totalMarks;
        private Integer passMarks;
        private Boolean isPublished;
        private Integer score;
        private Boolean isPassed;
        private String feedback;
        private Double percentage;
        private LocalDateTime createdAt;
        private String status;
    }
}
