package tz.elmkusoma.learner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LearningGoalResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private String goalType;
    private LocalDate targetDate;
    private Integer progressPercentage;
    private String status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}