package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateRubricRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    private UUID subjectId;

    @NotNull(message = "Total points is required")
    @DecimalMin(value = "1.00")
    private BigDecimal totalPoints;

    /** Optional criteria lines created together with the rubric. */
    private List<tz.elmkusoma.grading.dto.request.CreateRubricCriteriaRequest> criteria;
}