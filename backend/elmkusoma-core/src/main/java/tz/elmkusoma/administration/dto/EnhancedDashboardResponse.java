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
    private String currentUserRole;
    private List<String> userPermissions;

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
    private long liveClassesLiveNow;
    private long upcomingLiveClasses;

    private long pendingInvitations;
    private long unreadNotifications;

    private List<String> enabledServices;
    private List<RecentActivityItem> recentActivity;
    private List<AttentionItem> attentionItems;
    private List<QuickAction> quickActions;
    private WorkQueueSummary workQueueSummary;
    private OrganizationHealthSummary healthSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String entityId;
        private String entityType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttentionItem {
        private String type;
        private String title;
        private String description;
        private int count;
        private String severity; // CRITICAL, WARNING, INFO
        private String actionUrl;
        private String actionLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String id;
        private String label;
        private String icon;
        private String actionUrl;
        private String requiredPermission;
        private boolean available;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkQueueSummary {
        private long pendingApprovals;
        private long pendingReviews;
        private long pendingVerifications;
        private String queueUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationHealthSummary {
        private String overallStatus; // HEALTHY, DEGRADED, CRITICAL
        private List<HealthMetric> metrics;
        private LocalDateTime lastChecked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthMetric {
        private String name;
        private String status; // HEALTHY, DEGRADED, DOWN
        private String value;
        private String threshold;
    }
}
