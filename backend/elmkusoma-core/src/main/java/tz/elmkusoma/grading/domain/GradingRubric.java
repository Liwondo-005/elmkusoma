package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grading_rubrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingRubric extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "total_points", nullable = false)
    private BigDecimal totalPoints;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}