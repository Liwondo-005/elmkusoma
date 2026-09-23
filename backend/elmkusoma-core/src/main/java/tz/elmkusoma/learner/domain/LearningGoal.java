package tz.elmkusoma.learner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity(name = "LearnerGoal")
@Table(name = "learner_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LearningGoal extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", length = 30)
    private GoalType goalType = GoalType.PERSONAL;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;

    public enum GoalType {
        PERSONAL, ACADEMIC, CAREER, SKILL, CERTIFICATION, PROJECT
    }

    public enum GoalStatus {
        ACTIVE, COMPLETED, PAUSED, CANCELLED
    }
}