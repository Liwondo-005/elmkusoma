package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.EnrollmentStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseEnrollmentDTO {
    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private UUID programmeId;
    private String semester;
    private String academicYear;
    private Integer creditHours;
    private EnrollmentStatus status;
    private LocalDate enrolledDate;
    private LocalDate completedDate;
    private String grade;
    private Double gradePoints;
    private UUID instructorId;
    private UUID institutionId;
}
