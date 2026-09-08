package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryMilestone extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private MilestoneCategory category;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "description")
    private String description;

    @Column(name = "expected_age_months")
    private Integer expectedAgeMonths;

    @Column(name = "achieved_date")
    private LocalDate achievedDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MilestoneStatus status = MilestoneStatus.PENDING;

    @Column(name = "observed_by")
    private UUID observedBy;

    @Column(name = "evidence_notes")
    private String evidenceNotes;

    public enum MilestoneCategory {
        PHYSICAL,
        COGNITIVE,
        SOCIAL,
        EMOTIONAL,
        LANGUAGE,
        MOTOR
    }

    public enum MilestoneStatus {
        PENDING,
        ACHIEVED,
        IN_PROGRESS,
        NOT_OBSERVED
    }
}