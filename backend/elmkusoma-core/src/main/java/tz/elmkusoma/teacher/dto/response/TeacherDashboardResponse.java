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
public class TeacherDashboardResponse {

    private long totalStudents;
    private long totalClasses;
    private long totalAssignments;
    private long totalAssessments;
    private long pendingSubmissions;
    private long pendingGrading;
    private List<TeacherClassSummary> classes;
    private List<RecentActivity> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherClassSummary {
        private String classGroupId;
        private String className;
        private String subjectName;
        private long enrolledStudents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
    }
}
