package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.ProjectStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID subjectId;

    @NotBlank(message = "Project title is required")
    private String title;

    private String objective;

    private String description;

    @Builder.Default
    private ProjectStatus status = ProjectStatus.IDEATION;

    private UUID instructorId;

    private LocalDate startDate;

    private LocalDate dueDate;

    private LocalDate completedDate;

    private UUID institutionId;
}
