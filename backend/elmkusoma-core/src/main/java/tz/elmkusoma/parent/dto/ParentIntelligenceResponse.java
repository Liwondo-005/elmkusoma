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
public class ParentIntelligenceResponse {

    private String childId;
    private String childName;

    private List<AttentionItem> needsAttention;
    private List<PositiveSignal> doingWell;
    private List<RecommendationItem> recommendations;
    private WeeklyBrief weeklyBrief;
    private List<UpcomingItem> upcoming;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttentionItem {
        private String id;
        private String type;
        private String priority;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String relatedEntityType;
        private String relatedEntityId;
        private String actionLabel;
        private String actionUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositiveSignal {
        private String id;
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String relatedEntityType;
        private String relatedEntityId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationItem {
        private String id;
        private String type;
        private String title;
        private String description;
        private String actionLabel;
        private String actionUrl;
        private String evidenceSource;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyBrief {
        private int lessonsCompleted;
        private int assignmentsCompleted;
        private int assignmentsPending;
        private int assignmentsOverdue;
        private int liveClassesAttended;
        private int assessmentsCompleted;
        private int presentDays;
        private int absentDays;
        private int lateDays;
        private List<String> highlights;
        private List<String> focusNextWeek;
        private List<UpcomingItem> upcomingThisWeek;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingItem {
        private String id;
        private String type;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String relatedEntityType;
        private String relatedEntityId;
        private String actionLabel;
    }
}
