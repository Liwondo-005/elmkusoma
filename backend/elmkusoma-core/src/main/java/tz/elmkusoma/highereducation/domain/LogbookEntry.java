package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logbook_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LogbookEntry extends BaseEntity {

    @Column(name = "placement_id", nullable = false)
    private UUID placementId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "activities", nullable = false, columnDefinition = "TEXT")
    private String activities;

    @Column(name = "hours_worked")
    private Double hoursWorked;

    @Column(name = "skills_used", columnDefinition = "TEXT")
    private String skillsUsed;

    @Column(name = "challenges", columnDefinition = "TEXT")
    private String challenges;

    @Column(name = "learning_outcomes", columnDefinition = "TEXT")
    private String learningOutcomes;

    @Column(name = "supervisor_comments", columnDefinition = "TEXT")
    private String supervisorComments;

    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
