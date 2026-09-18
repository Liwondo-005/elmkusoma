package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "competency_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompetencyAssessment extends BaseEntity {

    @Column(name = "competency_id", nullable = false)
    private UUID competencyId;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Builder.Default
    @Column(name = "weight", nullable = false)
    private Integer weight = 100;
}
