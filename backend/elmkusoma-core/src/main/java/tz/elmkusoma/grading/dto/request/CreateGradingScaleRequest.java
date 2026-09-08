package tz.elmkusoma.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateGradingScaleRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Scale type is required")
    private String scaleType;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private Boolean isDefault = false;
}