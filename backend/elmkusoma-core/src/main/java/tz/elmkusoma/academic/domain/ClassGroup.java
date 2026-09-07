package tz.elmkusoma.academic.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "class_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClassGroup extends BaseEntity {

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "section")
    private String section;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "class_teacher_id")
    private UUID classTeacherId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
