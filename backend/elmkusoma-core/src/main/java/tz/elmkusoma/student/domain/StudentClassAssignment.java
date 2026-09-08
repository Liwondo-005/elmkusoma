package tz.elmkusoma.student.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_class_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentClassAssignment extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "assigned_date", nullable = false)
    private LocalDateTime assignedDate = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
