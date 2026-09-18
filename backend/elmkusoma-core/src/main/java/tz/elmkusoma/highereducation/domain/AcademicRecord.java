package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "academic_records")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AcademicRecord extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "programme_id")
    private UUID programmeId;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "total_credit_hours")
    private Integer totalCreditHours;

    @Column(name = "earned_credit_hours")
    private Integer earnedCreditHours;

    @Column(name = "semester_gpa")
    private Double semesterGpa;

    @Column(name = "cumulative_gpa")
    private Double cumulativeGpa;

    @Column(name = "total_courses")
    private Integer totalCourses;

    @Column(name = "completed_courses")
    private Integer completedCourses;

    @Column(name = "failed_courses")
    private Integer failedCourses;

    @Column(name = "academic_standing", length = 30)
    private String academicStanding;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "total_students_in_class")
    private Integer totalStudentsInClass;
}
