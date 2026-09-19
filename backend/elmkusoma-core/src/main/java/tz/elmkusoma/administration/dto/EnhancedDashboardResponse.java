package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedDashboardResponse {
    private UUID institutionId;
    private String institutionName;
    private String institutionType;

    private long totalStudents;
    private long totalTeachers;
    private long totalParents;
    private long activeStudents;
    private long certificatesIssued;
    private long pendingImportJobs;
    private long totalCourses;
    private long publishedCourses;
    private long draftCourses;
    private long totalModules;
    private long totalLessons;
    private long liveClassesScheduled;

    private long pendingInvitations;
    private long unreadNotifications;

    private List<String> enabledServices;
    private List<RecentActivityItem> recentActivity;
    private List<AttentionItem> attentionItems;

    @Data
    @Builder
    public static class RecentActivityItem {
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class AttentionItem {
        private String type;
        private String title;
        private String description;
        private int count;
        private String actionUrl;
    }
}
