package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "professional_development_goals")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProfessionalDevelopmentGoal extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "goal_type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DevGoalType goalType = DevGoalType.SKILL_DEVELOPMENT;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DevGoalStatus status = DevGoalStatus.NOT_STARTED;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "category", length = 100)
    private String category;
}
