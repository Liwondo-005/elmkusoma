package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** One rubric criterion line (§36 rubric-based assessment). */
@Data
public class CreateRubricCriteriaRequest {

    @NotBlank(message = "Criterion name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Max points is required")
    @DecimalMin(value = "0.00")
    private BigDecimal maxPoints;

    private Integer sortOrder;
}
