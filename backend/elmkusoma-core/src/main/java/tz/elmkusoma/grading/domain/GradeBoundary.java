package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grade_boundaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GradeBoundary extends BaseEntity {

    @Column(name = "grading_scale_id", nullable = false)
    private UUID gradingScaleId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "grade_label", nullable = false)
    private String gradeLabel;

    @Column(name = "grade_name")
    private String gradeName;

    @Column(name = "min_percentage", nullable = false)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false)
    private BigDecimal maxPercentage;

    @Column(name = "gpa_points")
    private BigDecimal gpaPoints;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}