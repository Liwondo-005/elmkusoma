package tz.elmkusoma.highereducation.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LearningModuleDTO {
    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private String moduleTitle;
    private String moduleCode;
    private String description;
    private Integer creditHours;
    private UUID instructorId;
    private UUID institutionId;
    private String semester;
    private String academicYear;
    private String status;
    private Integer progressPercent;
    private String grade;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalAssignments;
    private Integer completedAssignments;
    private Integer totalAssessments;
    private Integer completedAssessments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
