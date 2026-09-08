package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
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
}