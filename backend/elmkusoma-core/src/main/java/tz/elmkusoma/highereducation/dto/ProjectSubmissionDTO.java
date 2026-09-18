package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.SubmissionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubmissionDTO {

    private UUID id;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID milestoneId;

    @NotNull(message = "Submission type is required")
    private SubmissionType submissionType;

    private String title;

    private String fileUrl;

    private String description;

    private String feedback;

    private String grade;

    private LocalDateTime submittedAt;
}
