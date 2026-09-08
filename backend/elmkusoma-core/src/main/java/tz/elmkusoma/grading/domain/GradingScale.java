package tz.elmkusoma.grading.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grading_scales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScale extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "scale_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ScaleType scaleType;

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum ScaleType {
        LETTER,      // A, B, C, D, F
        NUMERIC,     // 1-10, 1-100
        PERCENTAGE   // 0-100%
    }
}