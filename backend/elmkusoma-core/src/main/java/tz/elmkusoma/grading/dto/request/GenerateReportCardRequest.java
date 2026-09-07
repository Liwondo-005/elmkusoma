package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class GenerateReportCardRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Term ID is required")
    private UUID termId;

    @NotNull(message = "Grading scale ID is required")
    private UUID gradingScaleId;

    private String remarks;
}