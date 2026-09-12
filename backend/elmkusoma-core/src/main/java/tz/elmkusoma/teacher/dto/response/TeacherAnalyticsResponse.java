package tz.elmkusoma.teacher.dto.response;

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
public class TeacherAnalyticsResponse {

    private long totalStudents;
    private long totalAssignments;
    private long totalLessons;
    private double averageAttendance;
    private long pendingGrading;
    private long classesCount;
    private List<UpcomingDeadline> upcomingDeadlines;
    private List<RecentSubmission> recentSubmissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingDeadline {
        private String title;
        private String dueDate;
        private String subjectName;
        private String className;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSubmission {
        private String studentName;
        private String assignmentTitle;
        private LocalDateTime submittedAt;
        private Boolean graded;
    }
}
