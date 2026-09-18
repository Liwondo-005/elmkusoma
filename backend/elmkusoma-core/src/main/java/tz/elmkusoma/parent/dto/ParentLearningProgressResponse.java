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
public class ParentLearningProgressResponse {

    private List<CourseProgress> courses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseProgress {
        private String courseId;
        private String courseName;
        private Double progressPercentage;
        private Integer totalLessons;
        private Integer completedLessons;
        private Integer pendingLessons;
        private String currentLesson;
        private String lastActivity;
        private LocalDateTime lastActivityAt;
        private String nextLesson;
        private String status;
    }
}
