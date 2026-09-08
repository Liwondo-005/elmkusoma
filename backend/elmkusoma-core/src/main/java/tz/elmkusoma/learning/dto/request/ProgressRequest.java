package tz.elmkusoma.learning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProgressRequest {

    @NotNull(message = "Lesson ID is required")
    private UUID lessonId;

    @NotNull(message = "Completion percentage is required")
    @Min(0)
    @Max(100)
    private Double completionPercentage;
}
