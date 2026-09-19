package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "secondary_study_planner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SecondaryStudyPlanner extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlannerStatus status = PlannerStatus.PLANNED;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "notes")
    private String notes;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    public enum PlannerStatus {
        PLANNED, IN_PROGRESS, COMPLETED, SKIPPED
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}
