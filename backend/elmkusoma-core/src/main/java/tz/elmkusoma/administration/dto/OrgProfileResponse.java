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
public class OrgProfileResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String description;
    private String address;
    private String city;
    private String region;
    private UUID regionId;
    private UUID districtId;
    private String country;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    private String bannerUrl;
    private String motto;
    private Integer foundedYear;
    private Integer totalCapacity;
    private Boolean isActive;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private List<String> enabledServices;
    private PeopleSummary peopleSummary;
    private ActivitySummary activitySummary;

    @Data
    @Builder
    public static class PeopleSummary {
        private long totalUsers;
        private long totalTeachers;
        private long totalStudents;
        private long totalParents;
        private long activeUsers;
        private long pendingInvitations;
    }

    @Data
    @Builder
    public static class ActivitySummary {
        private long unreadNotifications;
        private List<ActivityItem> recentActivity;
    }

    @Data
    @Builder
    public static class ActivityItem {
        private UUID id;
        private String actorName;
        private String activityType;
        private String title;
        private String description;
        private LocalDateTime createdAt;
    }
}
