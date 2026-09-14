package tz.elmkusoma.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerDashboardResponse {

    private long enrolledCourses;
    private long completedCourses;
    private double overallProgress;
    private List<EnrollmentResponse> recentEnrollments;
    private List<EnrollmentResponse> continueLearning;
    private List<CourseSummaryResponse> recommended;
    private long unreadNotifications;
}
