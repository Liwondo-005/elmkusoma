package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "learning_modules")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LearningModule extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "module_title", nullable = false, length = 300)
    private String moduleTitle;

    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "credit_hours")
    private Integer creditHours;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ModuleStatus status = ModuleStatus.NOT_STARTED;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "total_lessons")
    private Integer totalLessons;

    @Column(name = "completed_lessons")
    private Integer completedLessons;

    @Column(name = "total_assignments")
    private Integer totalAssignments;

    @Column(name = "completed_assignments")
    private Integer completedAssignments;

    @Column(name = "total_assessments")
    private Integer totalAssessments;

    @Column(name = "completed_assessments")
    private Integer completedAssessments;
}
