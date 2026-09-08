package tz.elmkusoma.attendance.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attendance_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AttendanceSummary extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "total_school_days", nullable = false)
    private Integer totalSchoolDays = 0;

    @Column(name = "days_present", nullable = false)
    private Integer daysPresent = 0;

    @Column(name = "days_absent", nullable = false)
    private Integer daysAbsent = 0;

    @Column(name = "days_late", nullable = false)
    private Integer daysLate = 0;

    @Column(name = "days_excused", nullable = false)
    private Integer daysExcused = 0;

    @Column(name = "attendance_percentage")
    private BigDecimal attendancePercentage;
}