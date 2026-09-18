package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentGoalResponse {

    private List<GoalItem> goals;
    private long activeCount;
    private long completedCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalItem {
        private String id;
        private String title;
        private String description;
        private String goalType;
        private String status;
        private BigDecimal progressPercentage;
        private LocalDate targetDate;
        private LocalDateTime completedAt;
        private String relatedEntityType;
        private String relatedEntityId;
    }
}
