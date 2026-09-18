package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.StudyTaskPriority;
import tz.elmkusoma.highereducation.domain.StudyTaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyTaskDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @Builder.Default
    private StudyTaskType taskType = StudyTaskType.STUDY;

    @Builder.Default
    private StudyTaskPriority priority = StudyTaskPriority.MEDIUM;

    private UUID subjectId;

    private LocalDate scheduledDate;

    private LocalTime scheduledTime;

    private Integer durationMinutes;

    @Builder.Default
    private Boolean isCompleted = false;

    private LocalDateTime completedDate;

    private String notes;

    private UUID institutionId;
}
