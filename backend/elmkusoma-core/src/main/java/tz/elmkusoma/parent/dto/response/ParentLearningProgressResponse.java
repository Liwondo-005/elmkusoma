package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentLearningProgressResponse {

    private String studentName;
    private String className;
    private Double overallProgress;
    private List<CourseProgress> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgress {
        private String subjectId;
        private String subjectName;
        private String courseId;
        private String courseName;
        private Integer totalLessons;
        private Integer completedLessons;
        private Integer pendingLessons;
        private Double completionPercentage;
        private Double progressPercentage;
        private String status;
    }
}
