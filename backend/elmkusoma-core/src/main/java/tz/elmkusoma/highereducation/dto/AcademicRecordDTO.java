package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicRecordDTO {
    private UUID id;
    private UUID studentId;
    private UUID programmeId;
    private String academicYear;
    private String semester;
    private Integer totalCreditHours;
    private Integer earnedCreditHours;
    private Double semesterGpa;
    private Double cumulativeGpa;
    private Integer totalCourses;
    private Integer completedCourses;
    private Integer failedCourses;
    private String academicStanding;
    private Integer classRank;
    private Integer totalStudentsInClass;
    private UUID institutionId;
}
