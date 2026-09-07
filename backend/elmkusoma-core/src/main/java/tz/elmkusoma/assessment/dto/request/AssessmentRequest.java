package tz.elmkusoma.assessment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssessmentRequest {

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Integer timeLimitMinutes;

    @NotNull(message = "Total marks are required")
    @Min(1)
    private Integer totalMarks;

    @NotNull(message = "Pass marks are required")
    @Min(1)
    private Integer passMarks;

    private Boolean isPublished = false;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
