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
public class ParentAchievementResponse {

    private List<AchievementItem> achievements;
    private long totalAchievements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AchievementItem {
        private String id;
        private String title;
        private String description;
        private String achievementType;
        private String icon;
        private String color;
        private String relatedEntityType;
        private String relatedEntityId;
        private LocalDateTime achievedAt;
    }
}
