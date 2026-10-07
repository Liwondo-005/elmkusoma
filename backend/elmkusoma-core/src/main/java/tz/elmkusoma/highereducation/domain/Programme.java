package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "programmes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Programme extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "programme_type", nullable = false)
    private ProgrammeType programmeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level")
    private EducationLevel educationLevel;

    @Column(name = "duration_months")
    private Integer durationMonths;

    /**
     * Owning department. This is the real Department -> Programme relationship; the legacy
     * departments.programme_ids jsonb array is not the source of truth. Nullable so existing
     * programmes and institutions that do not model departments keep working.
     */
    @Column(name = "department_id")
    private UUID departmentId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
