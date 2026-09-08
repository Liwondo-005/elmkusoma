package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateGradeBoundaryRequest {

    @NotNull(message = "Grading scale ID is required")
    private UUID gradingScaleId;

    @NotBlank(message = "Grade label is required")
    @Size(max = 10)
    private String gradeLabel;

    @Size(max = 100)
    private String gradeName;

    @NotNull(message = "Min percentage is required")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal minPercentage;

    @NotNull(message = "Max percentage is required")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal maxPercentage;

    private BigDecimal gpaPoints;

    private Integer sortOrder = 0;
}