package tz.elmkusoma.nfe.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRequest {

    private UUID providerId;

    private UUID programId;

    @NotBlank(message = "Assessment title is required")
    private String title;

    private String description;

    @NotBlank(message = "Assessment type is required")
    private String assessmentType;

    private Integer totalMarks;

    private Integer passMarks;

    private Integer timeLimitMinutes;

    private Boolean isPublished;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
