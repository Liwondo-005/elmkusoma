package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "competency_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompetencyRecord extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "competency_id", nullable = false)
    private UUID competencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CompetencyStatus status = CompetencyStatus.NOT_STARTED;

    @Column(name = "evidence")
    private String evidence;

    @Column(name = "assessed_by")
    private UUID assessedBy;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "last_practice_date")
    private LocalDate lastPracticeDate;

    @Column(name = "notes")
    private String notes;
}
