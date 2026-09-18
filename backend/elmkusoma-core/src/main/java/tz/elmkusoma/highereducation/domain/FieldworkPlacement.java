package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fieldwork_placements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FieldworkPlacement extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "programme_id")
    private UUID programmeId;

    @Column(name = "organisation_name", nullable = false)
    private String organisationName;

    @Column(name = "placement_title", nullable = false)
    private String placementTitle;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Column(name = "supervisor_email")
    private String supervisorEmail;

    @Column(name = "supervisor_phone")
    private String supervisorPhone;

    @Column(name = "institution_supervisor_id")
    private UUID institutionSupervisorId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private PlacementStatus status = PlacementStatus.PLANNING;

    @Column(name = "total_hours_required")
    private Integer totalHoursRequired;

    @Builder.Default
    @Column(name = "total_hours_completed")
    private Integer totalHoursCompleted = 0;

    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
