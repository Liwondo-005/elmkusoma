package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rubric_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricCriteria extends BaseEntity {

    @Column(name = "rubric_id", nullable = false)
    private UUID rubricId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "max_points", nullable = false)
    private BigDecimal maxPoints;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}