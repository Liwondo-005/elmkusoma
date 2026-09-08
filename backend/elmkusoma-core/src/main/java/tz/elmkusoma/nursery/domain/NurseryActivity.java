package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryActivity extends BaseEntity {

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "activity_name", nullable = false)
    private String activityName;

    @Column(name = "activity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "materials_needed")
    private String materialsNeeded;

    @Column(name = "learning_objectives")
    private String learningObjectives;

    @Column(name = "age_group")
    private String ageGroup;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityStatus status = ActivityStatus.PLANNED;

    @Column(name = "conducted_by", nullable = false)
    private UUID conductedBy;

    public enum ActivityType {
        GAME,
        SONG,
        STORY,
        CRAFT,
        PHYSICAL,
        EDUCATIONAL
    }

    public enum ActivityStatus {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}