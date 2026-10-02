package tz.elmkusoma.teacher.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "teacher_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignment extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "academic_year")
    private String academicYear;

    /** ACTIVE / ENDED / CANCELLED — ended rows stay for history. */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TeacherAssignmentStatus status = TeacherAssignmentStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private Teacher teacher;
}
