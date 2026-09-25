package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_course_enrollments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StudentCourseEnrollment extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "programme_id")
    private UUID programmeId;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "credit_hours")
    private Integer creditHours;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "enrolled_date")
    private LocalDate enrolledDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "grade_points")
    private Double gradePoints;

    @Column(name = "instructor_id")
    private UUID instructorId;
}
